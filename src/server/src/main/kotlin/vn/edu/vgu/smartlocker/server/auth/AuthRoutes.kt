package vn.edu.vgu.smartlocker.server.auth

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
fun Route.authRoutes(
    db: Db,
    otp: Otp,
    tokens: Tokens,
    accounts: Accounts,
    /** Null when no `GOOGLE_CLIENT_ID` is set. Endpoint 19 then answers
     *  `GOOGLE_OFF` rather than pretending to check anything. */
    google: GoogleTokens? = null,
) {

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

        val issued = tokens.mint(Receivers.findOrCreate(db, phone, asked.fullName))
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

    /**
     * 20. Set or change the password. ADR 0012.
     *
     * **It needs a live session, which is what makes it the reset path too.**
     * Somebody who has forgotten their password asks for a one-time code the
     * ordinary way, signs in with it, and sets a new one here. That is why
     * there is no reset endpoint and no reset email: the shortest reset path
     * is the one that already existed, and it needs no email address the
     * product otherwise never collects.
     */
    post("/auth/set-password") {
        val me = call.receiverId(tokens)
        val asked = call.receive<PasswordRequest>()
        when (accounts.setPassword(me, asked.password)) {
            null -> call.respond(Empty())
            "PASSWORD_TOO_SHORT" -> call.refuse(Refusal.PASSWORD_TOO_SHORT)
            else -> call.refuse(Refusal.PASSWORD_TOO_LONG)
        }
    }

    /**
     * 21. Sign in with a phone number and a password.
     *
     * Everything that can go wrong answers `WRONG_PASSWORD`, including a
     * number this server has never seen and an account five wrong tries into
     * a lockout. See the refusal's own note.
     */
    post("/auth/password-login") {
        val asked = call.receive<PasswordLoginRequest>()
        // A number that will not parse is answered the same way, so the
        // endpoint cannot be used to learn which numbers are worth trying.
        val phone = Phone.normalise(asked.phoneNumber) ?: call.refuse(Refusal.WRONG_PASSWORD)
        val me = accounts.check(phone, asked.password) ?: call.refuse(Refusal.WRONG_PASSWORD)
        val issued = tokens.mint(me)
        call.respond(SessionResponse(issued.token, issued.expiresAt.toString()))
    }

    /**
     * 19. Sign in with Google, or link Google to the account already signed in.
     *
     * **Google can never make an account here.** A Google account has no
     * phone number, and the shipper finds a receiver by typing a phone number
     * on the cabinet screen - so an account made this way could be signed in
     * to and never be sent a parcel. ADR 0011.
     *
     * That gives the endpoint two jobs, told apart by whether a receiver
     * token rides along:
     *
     *  - **No token.** Sign in, if this Google account is already linked. If
     *    it is not, the answer is `PHONE_REQUIRED`, which is not a failure -
     *    it is the app's cue to take a phone number and a one-time code and
     *    come back here with the token.
     *  - **With a token.** Link this Google account to that receiver, and
     *    hand back a fresh token. This is the second half of the flow above,
     *    and the only way a link is ever made.
     *
     * The ID token is checked here rather than at Google's tokeninfo
     * endpoint: that keeps the network, and somebody else's rate limit, off
     * the login path. Every check is in [GoogleTokens] with a test attacking
     * it, and every failure is the same refusal.
     */
    post("/auth/google") {
        val checker = google ?: call.refuse(Refusal.GOOGLE_OFF)
        val asked = call.receive<GoogleRequest>()
        val sub = checker.subjectOf(asked.idToken.trim()) ?: call.refuse(Refusal.GOOGLE_INVALID)

        val me = when (val bearer = call.bearer()) {
            null -> Receivers.findByGoogle(db, sub) ?: call.refuse(Refusal.PHONE_REQUIRED)
            else -> tokens.receiverFor(bearer)
                ?.also { Receivers.linkGoogle(db, it, sub) }
                ?: call.refuse(Refusal.TOKEN_EXPIRED)
        }

        val issued = tokens.mint(me)
        call.respond(SessionResponse(issued.token, issued.expiresAt.toString()))
    }

    /**
     * 24. Who is signed in.
     *
     * The app knew the name and the number only at the moment they were
     * typed. After a relaunch it had neither, so Settings and the Home
     * greeting drew the parameter defaults - a real tester registered as
     * Tran Thi Mai and was greeted as Minh. BUG-021.
     *
     * The identity comes from the token and nothing else. There is no
     * parameter to name somebody else, which is what stops this being a
     * directory of everyone's phone numbers.
     */
    get("/me") {
        val me = call.receiverId(tokens)
        call.respond(
            MeResponse(
                fullName = Receivers.name(db, me),
                phoneNumber = Receivers.phone(db, me),
            ),
        )
    }
}

/**
 * The account, as its owner. **Not masked**: this is the one screen where the
 * whole name and the whole number are correct to show, because the person
 * reading it is the person they belong to. The cabinet screen asks endpoint
 * 10 instead and gets a masked name.
 */
@Serializable
data class MeResponse(
    @SerialName("full_name") val fullName: String,
    @SerialName("phone_number") val phoneNumber: String,
)

@Serializable
data class GoogleRequest(@SerialName("id_token") val idToken: String = "")

@Serializable
data class PasswordRequest(val password: String = "")

@Serializable
data class PasswordLoginRequest(
    @SerialName("phone_number") val phoneNumber: String = "",
    val password: String = "",
)

@Serializable
data class PhoneRequest(@SerialName("phone_number") val phoneNumber: String = "")

@Serializable
data class VerifyRequest(
    @SerialName("phone_number") val phoneNumber: String = "",
    val code: String = "",
    /**
     * The name the shipper will be shown, masked, at the cabinet.
     *
     * Optional, and it defaults to empty so an older build keeps working. It
     * is taken only when the account has no name yet - see
     * [Receivers.findOrCreate]. Nothing else in the contract collects one, so
     * without this every account masks to `***` and the shipper is asked to
     * confirm a person the screen cannot name.
     */
    @SerialName("full_name") val fullName: String = "",
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
