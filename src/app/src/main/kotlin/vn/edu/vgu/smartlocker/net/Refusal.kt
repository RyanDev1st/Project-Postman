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
