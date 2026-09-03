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

    // --- Google - endpoint 19 ----------------------------------------------

    /**
     * No client id is configured on this server, so the check cannot be made.
     *
     * Not an error on the caller's part, and deliberately its own code: the
     * app is meant to hide the Google button rather than show one that
     * cannot work. Answering `GOOGLE_INVALID` here would tell a user their
     * Google account was refused, which is not what happened.
     */
    GOOGLE_OFF(HttpStatusCode.NotImplemented),

    /**
     * Every way an ID token can be wrong, in one code and on purpose.
     *
     * Bad signature, wrong `aud`, wrong issuer, expired, `alg` swapped,
     * unknown `kid`, not three parts at all. Saying which check failed is
     * free help to somebody probing the verifier - see `GoogleTokens`, which
     * returns null for all of them for the same reason.
     */
    GOOGLE_INVALID(HttpStatusCode.BadRequest),

    /**
     * A genuine Google token from outside the university. ADR 0026.
     *
     * **The deliberate exception to the rule above**, and the only one. Every
     * other way a token can be wrong is [GOOGLE_INVALID], because naming the
     * failed check helps a person probing the verifier. This one names it,
     * because the person seeing it is a student who tapped the button with
     * their personal Gmail and has to be told which account to use instead.
     * That this locker serves VGU is written on the cabinet, so saying it
     * again reveals nothing.
     *
     * A missing `hd` lands here too. Personal Google accounts carry no `hd`
     * at all, so requiring one is what keeps them out.
     */
    GOOGLE_DOMAIN(HttpStatusCode.Forbidden),

    // --- Booking a box - endpoints 25 and 29 -------------------------------

    /**
     * Endpoint 25 needs a phone number, and this account has never given one.
     *
     * **Not a failure**, and it changed meaning on 2026-09-02. It used to be
     * the cue to ask for a number and a one-time code. There is no code, and
     * the account already exists by the time this can be returned - so it is
     * now the cue to show the number field on the booking screen. Once given,
     * the number is kept and never asked for again; endpoint 29 is the only
     * way it changes.
     */
    PHONE_REQUIRED(HttpStatusCode.BadRequest),

    /**
     * That phone number is already on another account.
     *
     * **Names nobody.** Not whose account, not whether they have a parcel
     * coming - only that these digits are taken. First claim wins, and a
     * second person typing them has almost certainly mistyped their own.
     */
    PHONE_IN_USE(HttpStatusCode.Conflict),

    /**
     * This account already holds a live booking, and may hold only one.
     *
     * One per account is what stops twenty reservations filling a twenty-door
     * cabinet that has nothing inside it. Cancel at endpoint 27, or wait for
     * the 24 hours to run out.
     */
    BOOKING_EXISTS(HttpStatusCode.Conflict),

    /** No live booking on this account. Endpoint 27 with nothing to cancel. */
    NO_BOOKING(HttpStatusCode.NotFound),

    /** The cabinet key was missing, or is not one we issued. */
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
