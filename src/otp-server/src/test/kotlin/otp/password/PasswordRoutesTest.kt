package otp.password

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import otp.AuthStore
import otp.LogSms
import otp.module
import kotlin.test.Test
import kotlin.test.assertEquals

class PasswordRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val phone = "0908619328"

    @Serializable
    private data class Refusal(val code: String)

    @Serializable
    private data class SessionOut(val token: String, val expires_at: String)

    private suspend fun ApplicationTestBuilder.withServer(block: suspend (LogSms) -> Unit) {
        val dir = Files.createTempDirectory("routes-test")
        val log = LogSms()
        Accounts(dir.resolve("accounts.db").toString()).use { accounts ->
            application { module(log, AuthStore(log), null, accounts) }
            block(log)
        }
    }

    /** Register by one-time code and return the session token. */
    private suspend fun io.ktor.client.HttpClient.signIn(log: LogSms): String {
        post("/auth/request-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone_number":"$phone"}""")
        }
        val code = Regex("\\d{6}").find(log.lastContent!!)!!.value
        val res = post("/auth/verify-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone_number":"$phone","code":"$code"}""")
        }
        return json.decodeFromString<SessionOut>(res.bodyAsText()).token
    }

    private suspend fun io.ktor.client.HttpClient.setPassword(password: String, bearer: String?) =
        post("/auth/set-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"password":"$password"}""")
            if (bearer != null) bearerAuth(bearer)
        }

    private suspend fun io.ktor.client.HttpClient.login(number: String, password: String) =
        post("/auth/password-login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone_number":"$number","password":"$password"}""")
        }

    @Test
    fun setAPasswordThenSignInWithIt() = testApplication {
        withServer { log ->
            val session = client.signIn(log)
            assertEquals(HttpStatusCode.OK, client.setPassword("a good password", session).status)

            val res = client.login(phone, "a good password")
            assertEquals(HttpStatusCode.OK, res.status)
            val token = json.decodeFromString<SessionOut>(res.bodyAsText()).token

            // A real session, not a stub.
            assertEquals(HttpStatusCode.OK, client.post("/auth/refresh") { bearerAuth(token) }.status)
        }
    }

    @Test
    fun aPasswordCannotBeSetWithoutProvingWhoYouAre() = testApplication {
        withServer {
            assertEquals(HttpStatusCode.Unauthorized, client.setPassword("a good password", null).status)
            assertEquals(HttpStatusCode.Unauthorized, client.setPassword("a good password", "bogus").status)
        }
    }

    @Test
    fun theWrongPasswordIsRefused() = testApplication {
        withServer { log ->
            client.setPassword("a good password", client.signIn(log))
            val res = client.login(phone, "not the password")
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("WRONG_PASSWORD", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun aNumberThatIsNotEvenAPhoneNumberSaysTheSameThing() = testApplication {
        withServer {
            // Not PHONE_INVALID. On a sign-in screen that would answer a
            // question about the account, not about what was typed.
            val res = client.login("123", "anything")
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("WRONG_PASSWORD", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun aShortPasswordIsRefusedWithAReason() = testApplication {
        withServer { log ->
            val res = client.setPassword("short", client.signIn(log))
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("PASSWORD_TOO_SHORT", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun theSameNumberInAnyFormatSignsInTheSameAccount() = testApplication {
        withServer { log ->
            client.setPassword("a good password", client.signIn(log))
            listOf("0908619328", "84908619328", "+84908619328").forEach {
                assertEquals(HttpStatusCode.OK, client.login(it, "a good password").status, "failed for $it")
            }
        }
    }

    @Test
    fun passwordsAreOffUntilAStoreIsGiven() = testApplication {
        val log = LogSms()
        application { module(log, AuthStore(log)) }
        assertEquals(HttpStatusCode.NotFound, client.login(phone, "a good password").status)
    }
}
