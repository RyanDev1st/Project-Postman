package vn.edu.vgu.smartlocker.net

import androidx.annotation.StringRes
import vn.edu.vgu.smartlocker.R

/**
 * Every way the server can say no, and the words the screen shows for it.
 *
 * The list is the refusal table in `docs/reference/api-contract.md`. The codes
 * are the server's vocabulary; the words are ours, and they are the only part
 * of that contract a user ever reads.
 *
 * **The wording lives here, once.** A screen that invents its own sentence for
 * `SESSION_EXPIRED` is a screen that will word it differently from the next
 * one, and a user who sees two sentences for one problem thinks they have two
 * problems.
 *
 * Only the codes the **app** can receive carry text. The cabinet screen has
 * its own refusals - `PHONE_NOT_REGISTERED`, `NO_FREE_BOX`, `CODE_REJECTED`
 * and the rest - and those belong to `src/cabinet/`, not here. Repeating them
 * in the app would mean maintaining the same sentence in two languages of
 * code, and the app can never be shown them.
 */
enum class Refusal(@param:StringRes val message: Int?) {

    /** The QR the phone scanned has aged out. Scan the screen again. */
    SESSION_EXPIRED(R.string.refused_session_expired),

    /** Nothing waiting for this person at this cabinet. */
    NO_PARCEL_HERE(R.string.refused_no_parcel_here),

    /** The box is out of order. Staff are told by the server, not by us. */
    BOX_FAULTY(R.string.refused_box_faulty),

    /** Wrong one-time code while registering. */
    WRONG_CODE(R.string.refused_wrong_code),

    /**
     * Asked for a code too soon, or too many were sent across everybody.
     *
     * **Missing from this list until 2026-08-21**, so it arrived as [UNKNOWN]
     * and the screen said *"Something went wrong. Try again."* - which invites
     * the one action that cannot work, on a cooldown, and says nothing about
     * waiting. Reachable by tapping Resend inside the minute, so it was always
     * a real screen a real person could reach; the hourly cap (BUG-015) only
     * made it common. Seen on the emulator while re-testing BUG-012.
     *
     * The sentence says to wait, and never says whether the number has an
     * account - the two limits are worded the same on purpose.
     */
    RATE_LIMITED(R.string.refused_rate_limited),

    /**
     * The SMS provider did not confirm it sent anything.
     *
     * Missing for the same reason and found in the same run. This is what a
     * real person sees the moment the provider is misconfigured or out of
     * balance, which is exactly the state the SpeedSMS account is in today
     * (BUG-016) - so it is not a rare path, it is the current one.
     */
    SEND_FAILED(R.string.refused_send_failed),

    /**
     * The phone number and the password do not go together.
     *
     * **One answer for every way it can fail** - a number this server has
     * never seen, a right password on an account five wrong tries into a
     * lockout, an account that never set one. The server answers this to all
     * of them on purpose, so the endpoint cannot be asked which numbers are
     * worth attacking, and the sentence here has to keep that promise: it
     * never says whether the number has an account.
     *
     * That is also why it does not say "locked". Being told the lockout
     * started is being told the password was right.
     */
    WRONG_PASSWORD(R.string.refused_wrong_password),

    /**
     * The password offered is too short, or too long to be worth hashing.
     *
     * Unlike [WRONG_PASSWORD] these say which way it was wrong, and that is
     * safe: they are only ever reached by somebody who has already proved the
     * number and is choosing a password, so there is nothing left to leak.
     * Refusing without saying why would just be a screen that will not move
     * on and will not say what to type.
     */
    PASSWORD_TOO_SHORT(R.string.refused_password_too_short),
    PASSWORD_TOO_LONG(R.string.refused_password_too_long),

    /**
     * The Google account is not a university one.
     *
     * **The one refusal in this app that says why.** Every other way a
     * sign-in can fail gives one flat answer, so that nobody can poke at it
     * to learn which part they got past. This one is different because the
     * person reading it has done nothing wrong and cannot possibly guess the
     * fix: they tapped the button with the personal Gmail their phone was
     * already signed in to, and they have to be told to pick the other
     * account. Refusing in silence here would read as an app that is broken.
     */
    GOOGLE_DOMAIN(R.string.refused_google_domain),

    /**
     * Google's signature, audience or expiry did not check out.
     *
     * One flat answer for every one of those, on purpose. Which check failed
     * is exactly what somebody forging a token wants to know.
     */
    GOOGLE_INVALID(R.string.refused_google_invalid),

    /**
     * The server was started without a Google client id, so endpoint 19 is
     * off.
     *
     * Nobody's fault and nothing a student can do, so it says to use the
     * other way in rather than describing a setting on a machine they will
     * never see.
     */
    GOOGLE_OFF(R.string.refused_google_off),

    /**
     * The token has run out.
     *
     * **No message.** This one is not shown - the app sends the user back to
     * register, quietly, because a person who has been away for a month does
     * not need to be told about tokens. Once, with no loop. See rule 5 in
     * architecture.md.
     */
    TOKEN_EXPIRED(null),

    /**
     * A code the contract does not list.
     *
     * Reached when the server sends something newer than this build knows. It
     * is deliberately not fatal: an app that crashes on an unknown refusal
     * cannot be fixed by shipping a server change.
     */
    UNKNOWN(R.string.refused_unknown);

    companion object {
        /** Map a code from the server onto this list. Never throws. */
        fun of(code: String): Refusal =
            entries.firstOrNull { it.name == code.trim().uppercase() } ?: UNKNOWN
    }
}
