package vn.edu.vgu.smartlocker.server.auth

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Phone
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.refuse

/**
 * Endpoints 1 to 4 of the contract - the way into an account.
 *
 * The field names below are the app's, not ours to choose: `Api.kt` sends
 * `phone_number` and reads `token` and `expires_at`. A rename here is a
 * silently broken login, because the app's parsers are lenient by design and
 * a missing field reads as empty rather than throwing.
 */
fun Route.authRoutes(db: Db, otp: Otp, tokens: Tokens) {

    /** 1. Ask for a one-time code. No token: this is the way in. */
    post("/auth/request-code") {
        val asked = call.receive<PhoneRequest>()
        val phone = Phone.normalise(asked.phoneNumber) ?: call.refuse(Refusal.PHONE_INVALID)

        when (otp.request(phone)) {
            Otp.Sent.OK -> call.respond(Empty())
            Otp.Sent.RATE_LIMITED -> call.refuse(Refusal.RATE_LIMITED)
            Otp.Sent.SEND_FAILED -> call.refuse(Refusal.SEND_FAILED)
        }
    }

    /**
     * 2. Send the code back and get a token.
     *
     * A number that has never been seen becomes an account here, and one that
     * has been seen before finds its parcels. The two are the same call on
     * purpose - see [Receivers.findOrCreate].
     */
    post("/auth/verify-code") {
        val asked = call.receive<VerifyRequest>()
        // An unparseable number and a wrong code answer the same thing. A
        // different answer would say which numbers are worth guessing at.
        val phone = Phone.normalise(asked.phoneNumber) ?: call.refuse(Refusal.WRONG_CODE)

        if (!otp.verify(phone, asked.code)) call.refuse(Refusal.WRONG_CODE)

        val issued = tokens.mint(Receivers.findOrCreate(db, phone))
        call.respond(SessionResponse(issued.token, issued.expiresAt.toString()))
    }

    /** 3. Swap a tiring token for a fresh one. The old one dies. */
    post("/auth/refresh") {
        val token = call.bearer() ?: call.refuse(Refusal.TOKEN_EXPIRED)
        val issued = tokens.refresh(token) ?: call.refuse(Refusal.TOKEN_EXPIRED)
        call.respond(SessionResponse(issued.token, issued.expiresAt.toString()))
    }

    /**
     * 4. Log out.
     *
     * Answers ok even when the token was already dead. The app clears its own
     * copy whatever this says, so a refusal here would be a message about
     * something the person has already achieved.
     */
    post("/auth/logout") {
        call.bearer()?.let(tokens::revoke)
        call.respond(Empty())
    }
}

@Serializable
data class PhoneRequest(@SerialName("phone_number") val phoneNumber: String = "")

@Serializable
data class VerifyRequest(
    @SerialName("phone_number") val phoneNumber: String = "",
    val code: String = "",
)

/**
 * `expires_at` is a string, because the app reads it with `optString`.
 *
 * A number there would be read as `""` by `Session.from` and the app would
 * think every token expires immediately.
 */
@Serializable
data class SessionResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

/** `{}` - what an endpoint that only has to succeed answers. */
@Serializable
class Empty
