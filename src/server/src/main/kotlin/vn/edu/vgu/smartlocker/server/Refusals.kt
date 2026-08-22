package vn.edu.vgu.smartlocker.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/**
 * Every way this server is allowed to say no.
 *
 * The list is the refusal table in `docs/reference/api-contract.md`, and the
 * app's `net/Refusal.kt` is the other half of it. A code sent from here that
 * the app does not know falls to its `UNKNOWN` and shows a generic sentence -
 * not a crash, but not useful either, so adding one here means adding it there.
 *
 * **A refusal carries no message.** Only the code. The words a user reads are
 * chosen by the front-end, in the language that user picked, and a sentence
 * written here would be a second copy of them in the wrong language. That rule
 * is in the contract's ground rules: *a message the server does not expect it
 * to show raw*.
 *
 * **Nothing here says more than it has to.** `NO_PARCEL_HERE` is returned for
 * "you have no parcel here", for "that parcel is not yours" and for "that code
 * was already used", on purpose. Telling them apart would answer a question
 * somebody probing wants answered.
 */
enum class Refusal(val status: HttpStatusCode) {

    // --- Registering -------------------------------------------------------

    /** Not a Vietnamese mobile number. */
    PHONE_INVALID(HttpStatusCode.BadRequest),

    /** A code was asked for less than a minute ago. */
    RATE_LIMITED(HttpStatusCode.TooManyRequests),

    /** The SMS provider did not confirm it sent anything. */
    SEND_FAILED(HttpStatusCode.BadGateway),

    /** Wrong, expired, or already used one-time code. All three, one answer. */
    WRONG_CODE(HttpStatusCode.Unauthorized),

    /** No token, an unknown token, or one that has run out. */
    TOKEN_EXPIRED(HttpStatusCode.Unauthorized),

    // --- Picking up --------------------------------------------------------

    /** The QR is older than its window, or this server never issued it. */
    SESSION_EXPIRED(HttpStatusCode.Forbidden),

    /**
     * Nothing here for you.
     *
     * Also the answer when the parcel belongs to somebody else, and when this
     * session code has already opened it. The three must read the same.
     */
    NO_PARCEL_HERE(HttpStatusCode.Forbidden),

    /** The box will not open, and staff have been told. */
    BOX_FAULTY(HttpStatusCode.Conflict),

    // --- The cabinet screen ------------------------------------------------

    /** No account for that phone number. */
    PHONE_NOT_REGISTERED(HttpStatusCode.NotFound),

    /** Nothing free in that size. */
    NO_FREE_BOX(HttpStatusCode.Conflict),

    /** A box the server thought was free turned out not to be. */
    BOX_ALREADY_FULL(HttpStatusCode.Conflict),

    /** Wrong, used, or expired typed pickup code. All three, one answer. */
    CODE_REJECTED(HttpStatusCode.Forbidden),

    /** Too many wrong tries at this box. */
    BOX_LOCKED_OUT(HttpStatusCode.TooManyRequests),

    /** The cabinet key was missing, or is not one we issued. */
    /**
     * Endpoint 20 only. The two length rules are the whole password policy.
     *
     * A pile of must-contain-a-symbol rules mostly teaches people to write
     * Password1! and reuse it everywhere, so length carries the weight and
     * the refusal says which way it was wrong - unlike [WRONG_PASSWORD],
     * because this one is answering somebody who has already proved who they
     * are and is choosing a password right now.
     */
    PASSWORD_TOO_SHORT(HttpStatusCode.BadRequest),
    PASSWORD_TOO_LONG(HttpStatusCode.BadRequest),

    /**
     * Endpoint 21's only answer, and it covers three different failures.
     *
     * Wrong password, locked out, and no account with that number all come
     * back as this. Splitting them would answer two things a sign-in screen
     * must never answer - whether that number is registered here, and whether
     * the lockout has started. Same rule as [WRONG_CODE].
     */
    WRONG_PASSWORD(HttpStatusCode.Unauthorized),

    CABINET_UNKNOWN(HttpStatusCode.Unauthorized),
}

/** The body of every refusal. One field, and never a message. */
@Serializable
data class RefusalBody(val code: String)

/**
 * Answer a call with a refusal and nothing else.
 *
 * Returns [Nothing] so a route can write `return call.refuse(...)` and the
 * compiler knows the handler is finished. Without that, a route that refuses
 * and then carries on reading a body is a compile-time success and a runtime
 * hole.
 */
suspend fun ApplicationCall.refuse(reason: Refusal): Nothing {
    respond(reason.status, RefusalBody(reason.name))
    throw Refused(reason)
}

/**
 * Thrown by [refuse] so the route stops.
 *
 * Caught in `Main.kt` and turned into nothing at all - the response has
 * already been written by the time this is thrown.
 */
class Refused(val reason: Refusal) : RuntimeException(reason.name)
