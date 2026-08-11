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
import otp.google.GoogleCerts
import otp.google.GoogleTokens
import otp.password.Accounts
import otp.password.passwordRoutes

@Serializable
private data class RequestCodeBody(val phone_number: String = "")

@Serializable
private data class VerifyCodeBody(val phone_number: String = "", val code: String = "")

@Serializable
private data class GoogleBody(val id_token: String = "")

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

/** Google checking, when a client id is configured. Null switches it off. */
private fun googleFromEnv(): GoogleTokens? =
    System.getenv("GOOGLE_CLIENT_ID")?.takeIf { it.isNotBlank() }
        ?.let { GoogleTokens(it, GoogleCerts()) }

/** Routes over an injected provider and store, so tests wire their own. */
fun Application.module(
    provider: SmsProvider,
    store: AuthStore,
    google: GoogleTokens? = null,
    accounts: Accounts? = null,
) {
    install(ContentNegotiation) { json() }

    routing {
        if (accounts != null) passwordRoutes(store, accounts)

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

        /**
         * Sign in with Google, and link a Google account to a phone.
         *
         * One route, because the two differ only by whether a bearer is
         * present, and splitting them would mean checking the same ID token
         * in two places.
         *
         *  - With a bearer: "I am already signed in as this phone. Remember
         *    this Google account for it." Both proofs are on the wire at once.
         *  - Without one: "Let me in as whichever phone this Google account
         *    belongs to." No link yet means PHONE_REQUIRED, and the app falls
         *    back to the one-time code, which is where an account is made.
         */
        post("/auth/google") {
            if (google == null) {
                call.respond(HttpStatusCode.NotImplemented, Refusal("GOOGLE_OFF"))
                return@post
            }
            val body = call.receive<GoogleBody>()
            val sub = google.subjectOf(body.id_token.trim())
            if (sub == null) {
                call.respond(HttpStatusCode.BadRequest, Refusal("GOOGLE_INVALID"))
                return@post
            }
            val bearer = call.bearer()
            val phone = if (bearer == null) {
                store.phoneOfGoogle(sub) ?: run {
                    call.respond(HttpStatusCode.BadRequest, Refusal("PHONE_REQUIRED"))
                    return@post
                }
            } else {
                store.phoneOf(bearer)?.also { store.linkGoogle(sub, it) } ?: run {
                    call.respond(HttpStatusCode.Unauthorized, Refusal("TOKEN_EXPIRED"))
                    return@post
                }
            }
            val token = store.issueToken(phone)
            call.respond(HttpStatusCode.OK, SessionOut(token.value, token.expiresAtMs.toString()))
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
    val google = googleFromEnv()
    // Passwords live in a file so the lockout counter survives a restart.
    val accounts = Accounts(System.getenv("ACCOUNTS_DB") ?: "accounts.db")
    println("Responding at http://0.0.0.0:$port")
    println("Google sign-in: " + if (google == null) "off (no GOOGLE_CLIENT_ID)" else "on")
    embeddedServer(Netty, port = port) {
        module(provider, AuthStore(provider), google, accounts)
    }.start(wait = true)
}
