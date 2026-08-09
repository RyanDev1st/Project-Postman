package otp

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RequestCodeBody(val phone_number: String = "")

@Serializable
private data class VerifyCodeBody(val phone_number: String = "", val code: String = "")

@Serializable
private data class Refusal(val code: String)

@Serializable
private data class SessionOut(val token: String, val expires_at: String)

/** The provider the environment asks for: SpeedSMS with a token, else demo. */
private fun providerFromEnv(): SmsProvider {
    val token = System.getenv("SPEEDSMS_TOKEN").orEmpty()
    return if (System.getenv("DEMO_MODE") == "1" || token.isBlank()) {
        LogSms()
    } else {
        SpeedSms(token)
    }
}

/** Routes over an injected provider and store, so tests wire their own. */
fun Application.module(provider: SmsProvider, store: AuthStore) {
    install(ContentNegotiation) { json() }

    routing {
        post("/auth/request-code") {
            val body = call.receive<RequestCodeBody>()
            val e164 = Phone.normalize(body.phone_number)
            if (e164 == null) {
                call.respond(HttpStatusCode.BadRequest, Refusal("PHONE_INVALID"))
                return@post
            }
            when (val refusal = store.requestCode(e164)) {
                null -> call.respond(HttpStatusCode.OK, mapOf<String, String>())
                "RATE_LIMITED" -> call.respond(HttpStatusCode.TooManyRequests, Refusal(refusal))
                else -> call.respond(HttpStatusCode.BadGateway, Refusal(refusal))
            }
        }

        post("/auth/verify-code") {
            val body = call.receive<VerifyCodeBody>()
            val e164 = Phone.normalize(body.phone_number)
            if (e164 == null) {
                call.respond(HttpStatusCode.BadRequest, Refusal("PHONE_INVALID"))
                return@post
            }
            when (store.verifyCode(e164, body.code.trim())) {
                null -> {
                    val token = store.issueToken(e164)
                    call.respond(HttpStatusCode.OK, SessionOut(token.value, token.expiresAtMs.toString()))
                }
                else -> call.respond(HttpStatusCode.BadRequest, Refusal("WRONG_CODE"))
            }
        }

        post("/auth/refresh") {
            val token = call.bearer()
            val fresh = token?.let { store.refresh(it) }
            if (fresh == null) {
                call.respond(HttpStatusCode.Unauthorized, Refusal("TOKEN_EXPIRED"))
            } else {
                call.respond(HttpStatusCode.OK, SessionOut(fresh.value, fresh.expiresAtMs.toString()))
            }
        }

        post("/auth/logout") {
            val token = call.bearer()
            if (token != null) store.revoke(token)
            call.respond(HttpStatusCode.OK, mapOf<String, String>())
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.bearer(): String? {
    val header = request.headers[HttpHeaders.Authorization] ?: return null
    return header.removePrefix("Bearer ").takeIf { it != header }
}

fun main() {
    val provider = providerFromEnv()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8443
    embeddedServer(Netty, port = port) { module(provider, AuthStore(provider)) }.start(wait = true)
}
