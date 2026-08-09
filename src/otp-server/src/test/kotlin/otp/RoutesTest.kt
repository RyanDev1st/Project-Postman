package otp

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Refusal(val code: String)

    @Serializable
    private data class SessionOut(val token: String, val expires_at: String)

    private class FailingSms : SmsProvider {
        override fun send(toE164: String, content: String) = false
    }

    private suspend fun ApplicationTestBuilder.withServer(block: suspend (LogSms) -> Unit) {
        val log = LogSms()
        application {
            module(log, AuthStore(log))
        }
        block(log)
    }

    private suspend fun io.ktor.client.HttpClient.requestCode(phone: String) =
        post("/auth/request-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone_number":"$phone"}""")
        }

    @Test
    fun fullRegistrationFlow() = testApplication {
        withServer { log ->
            val sent = client.requestCode("0908619328")
            assertEquals(HttpStatusCode.OK, sent.status)
            val code = Regex("\\d{6}").find(log.lastContent!!)!!.value

            val wrong = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"000000"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, wrong.status)
            assertEquals("WRONG_CODE", json.decodeFromString<Refusal>(wrong.bodyAsText()).code)

            val ok = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"$code"}""")
            }
            assertEquals(HttpStatusCode.OK, ok.status)
            val session = json.decodeFromString<SessionOut>(ok.bodyAsText())
            assertNotNull(session.token)
            assertNotNull(session.expires_at)

            // Single use: replaying the same code fails.
            val replay = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"$code"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, replay.status)

            // The token works for refresh; logout kills it.
            val refresh = client.post("/auth/refresh") { bearerAuth(session.token) }
            assertEquals(HttpStatusCode.OK, refresh.status)
            val fresh = json.decodeFromString<SessionOut>(refresh.bodyAsText())

            val logout = client.post("/auth/logout") { bearerAuth(fresh.token) }
            assertEquals(HttpStatusCode.OK, logout.status)

            val afterLogout = client.post("/auth/refresh") { bearerAuth(fresh.token) }
            assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
        }
    }

    @Test
    fun invalidPhoneIsRefused() = testApplication {
        withServer {
            val res = client.requestCode("123")
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("PHONE_INVALID", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun resendWithinCooldownIsRefused() = testApplication {
        withServer {
            assertEquals(HttpStatusCode.OK, client.requestCode("0908619328").status)
            val again = client.requestCode("0908619328")
            assertEquals(HttpStatusCode.TooManyRequests, again.status)
            assertEquals("RATE_LIMITED", json.decodeFromString<Refusal>(again.bodyAsText()).code)
        }
    }

    @Test
    fun unknownTokenIsRefused() = testApplication {
        withServer {
            val res = client.post("/auth/refresh") { bearerAuth("bogus") }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            assertEquals("TOKEN_EXPIRED", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun failingProviderYieldsSendFailed() = testApplication {
        val provider = FailingSms()
        application {
            module(provider, AuthStore(provider))
        }
        val res = client.requestCode("0908619328")
        assertEquals(HttpStatusCode.BadGateway, res.status)
        assertEquals("SEND_FAILED", json.decodeFromString<Refusal>(res.bodyAsText()).code)
    }
}
