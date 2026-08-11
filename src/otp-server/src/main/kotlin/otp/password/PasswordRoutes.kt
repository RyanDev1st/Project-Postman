package otp.password

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import otp.AuthStore
import otp.Phone

@Serializable
private data class SetPasswordBody(val password: String = "")

@Serializable
private data class PasswordLoginBody(val phone_number: String = "", val password: String = "")

@Serializable
private data class Refusal(val code: String)

@Serializable
private data class SessionOut(val token: String, val expires_at: String)

/**
 * Password routes, added by [ADR 0012](../../../../../../docs/adr/0012-passwords-on-a-phone-account.md).
 *
 * There is no email anywhere here, and no register-with-a-password route.
 * A password is set on an account a one-time code already proved, and the
 * account is the phone number - the thing a shipper types at the cabinet.
 * An account made from an email could never be sent a parcel.
 *
 * That also means there is no reset endpoint and no reset email. Forgetting
 * a password is the ordinary one-time code, then setting a new one. The
 * shortest reset path is the one that already existed.
 */
fun Route.passwordRoutes(store: AuthStore, accounts: Accounts) {

    /** Set or change the password. Needs a live session, so it is also reset. */
    post("/auth/set-password") {
        val phone = store.phoneOf(call.bearer().orEmpty())
        if (phone == null) {
            call.respond(HttpStatusCode.Unauthorized, Refusal("TOKEN_EXPIRED"))
            return@post
        }
        val body = call.receive<SetPasswordBody>()
        val refusal = accounts.setPassword(phone, body.password)
        if (refusal != null) {
            call.respond(HttpStatusCode.BadRequest, Refusal(refusal))
        } else {
            call.respond(HttpStatusCode.OK, mapOf<String, String>())
        }
    }

    /** Sign in with a phone number and a password. */
    post("/auth/password-login") {
        val body = call.receive<PasswordLoginBody>()
        val e164 = Phone.normalize(body.phone_number)
        if (e164 == null) {
            // Not PHONE_INVALID: answering that here would say "no account
            // could exist for what you typed", which is one bit more than a
            // sign-in screen ever needs to give away.
            call.respond(HttpStatusCode.BadRequest, Refusal("WRONG_PASSWORD"))
            return@post
        }
        val refusal = accounts.check(e164, body.password)
        if (refusal != null) {
            call.respond(HttpStatusCode.BadRequest, Refusal(refusal))
            return@post
        }
        val token = store.issueToken(e164)
        call.respond(HttpStatusCode.OK, SessionOut(token.value, token.expiresAtMs.toString()))
    }
}

private fun ApplicationCall.bearer(): String? {
    val header = request.headers[HttpHeaders.Authorization] ?: return null
    return header.removePrefix("Bearer ").takeIf { it != header }
}
