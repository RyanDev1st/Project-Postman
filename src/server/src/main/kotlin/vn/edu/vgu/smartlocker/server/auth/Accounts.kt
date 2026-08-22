package vn.edu.vgu.smartlocker.server.auth

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.str

/**
 * Passwords on an account, and the lockout that guards them.
 *
 * Moved here from `src/otp-server/` on 2026-08-22 (P2-09), where it kept its
 * own SQLite file and its own `accounts` table. It does not need either: in
 * this server the account **is** the receiver, and the receiver is a row we
 * already have. Three columns on `receivers` say everything the separate
 * table said, and a parcel and a password can no longer disagree about who
 * somebody is.
 *
 * **On disk, not in memory**, because the lockout counter has to survive a
 * power cut - [ADR 0012](../../../../../../../docs/adr/0012-passwords-on-a-phone-account.md).
 * A counter that resets when the process dies gives unlimited guesses to
 * anybody who can pull a plug, and this runs on a Raspberry Pi in a corridor.
 *
 * There is no register-with-a-password route, and no reset. A password is set
 * on an account a one-time code already proved, so forgetting one is the
 * ordinary code again - the shortest reset path is the one that already
 * exists, and it needs no email address the product otherwise never collects.
 */
class Accounts(
    private val db: Db,
    private val triesBeforeLock: Int,
    private val lockMinutes: Long,
) {

    private data class Row(val id: String, val hash: String, val tries: Int, val lockedUntil: Long)

    /**
     * Set or replace the password on an account the caller already proved.
     *
     * Returns a refusal code, or null when it is set. Setting one clears any
     * lockout: the person has just proved themselves another way, and leaving
     * them locked out of a password they only now chose would be absurd.
     */
    fun setPassword(receiverId: String, password: String): String? {
        Passwords.refuse(password)?.let { return it }
        db.exec(
            """UPDATE receivers
                 SET password_hash = ?, password_tries = 0, password_locked_until = 0
               WHERE id = ?""",
            Passwords.hash(password), receiverId,
        )
        return null
    }

    /** Whether this account has a password at all. Never exposed by a route. */
    fun hasPassword(receiverId: String): Boolean =
        db.row("SELECT password_hash FROM receivers WHERE id = ?", receiverId) { it.str("password_hash") }
            ?.isNotEmpty() == true

    /**
     * Check a password. The receiver id when it is right, null otherwise.
     *
     * **One answer covers wrong, locked out, and no such account, on purpose.**
     * Telling them apart answers two questions a sign-in screen must never
     * answer: whether that number is registered here, and whether the lockout
     * has started. It is the rule the one-time code already follows.
     */
    fun check(phoneE164: String, password: String): String? {
        val row = read(phoneE164)

        // Hash even when there is no such account, against a throwaway hash of
        // the same shape. Without it an unknown number answers in a
        // millisecond and a known one in tens of them, and that gap is a way
        // to ask "is this person registered?" without being told.
        val ok = Passwords.matches(password, row?.hash?.takeIf(String::isNotEmpty) ?: ABSENT)
        if (row == null || row.hash.isEmpty()) return null

        if (row.lockedUntil > now()) return null
        if (!ok) {
            recordWrongTry(row)
            return null
        }
        if (row.tries != 0) write(row.id, 0, 0)
        return row.id
    }

    private fun recordWrongTry(row: Row) {
        val tries = row.tries + 1
        if (tries >= triesBeforeLock) write(row.id, 0, now() + lockMinutes * 60 * 1000)
        else write(row.id, tries, 0)
    }

    private fun read(phoneE164: String): Row? = db.row(
        """SELECT id, password_hash, password_tries, password_locked_until
             FROM receivers WHERE phone = ?""",
        phoneE164,
    ) { Row(it.str("id"), it.str("password_hash"), it.num("password_tries").toInt(), it.num("password_locked_until")) }

    private fun write(receiverId: String, tries: Int, lockedUntil: Long) = db.exec(
        "UPDATE receivers SET password_tries = ?, password_locked_until = ? WHERE id = ?",
        tries, lockedUntil, receiverId,
    )

    private companion object {
        /** Hashed once, so a request for an unknown number still does the work. */
        val ABSENT: String = Passwords.hash("no account with this number exists")
    }
}
