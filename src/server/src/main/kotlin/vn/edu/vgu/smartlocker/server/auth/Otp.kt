package vn.edu.vgu.smartlocker.server.auth

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.str

/**
 * The one-time code, from asking for it to spending it.
 *
 * The code is the only thing standing between a phone number and an account,
 * so the rules are the whole of this file:
 *
 * - **A code that was not sent is never usable.** If the provider does not
 *   confirm, the row is removed. Otherwise a broken SMS gateway becomes a way
 *   in for anybody who can guess six digits at leisure.
 * - **Five tries, then it is gone.** Not five tries per minute - five, ever.
 * - **One live code per number.** Asking again replaces the old one, so two
 *   codes are never valid at once.
 * - **Wrong, expired and already spent all answer `WRONG_CODE`.** Telling them
 *   apart says whether that number has an account.
 */
class Otp(
    private val db: Db,
    private val sms: Sms,
    private val digits: Int = 6,
    private val liveMinutes: Long = 5,
    private val triesAllowed: Int = 5,
    private val cooldownSeconds: Long = 60,
) {

    enum class Sent { OK, RATE_LIMITED, SEND_FAILED }

    /**
     * Make a code, send it, and remember only its hash.
     *
     * The cooldown is per number and is checked before anything is generated,
     * so a loop of requests costs one SMS a minute rather than one per call.
     */
    fun request(phone: String): Sent {
        val lastSent = db.row("SELECT last_sent_at FROM otp WHERE phone = ?", phone) {
            it.num("last_sent_at")
        }
        if (lastSent != null && now() - lastSent < cooldownSeconds * 1000) {
            return Sent.RATE_LIMITED
        }

        val code = Ids.digits(digits)
        db.exec(
            """INSERT INTO otp (phone, code_hash, expires_at, tries_left, last_sent_at)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT(phone) DO UPDATE SET
                 code_hash = excluded.code_hash,
                 expires_at = excluded.expires_at,
                 tries_left = excluded.tries_left,
                 last_sent_at = excluded.last_sent_at""",
            phone, Ids.hash(code), now() + liveMinutes * 60 * 1000, triesAllowed, now(),
        )

        if (!sms.send(phone, text(code))) {
            // Nobody received this code, so nobody may spend it.
            db.exec("DELETE FROM otp WHERE phone = ?", phone)
            return Sent.SEND_FAILED
        }
        return Sent.OK
    }

    /**
     * Spend a code. True only if it was the right one, in time, with tries
     * left - and it is dead either way it goes after that.
     */
    fun verify(phone: String, typed: String): Boolean = db.transaction {
        val row = db.row(
            "SELECT code_hash, expires_at, tries_left FROM otp WHERE phone = ?", phone,
        ) { Triple(it.str("code_hash"), it.num("expires_at"), it.num("tries_left")) }
            ?: return@transaction false

        val (storedHash, expiresAt, triesLeft) = row

        if (expiresAt <= now() || triesLeft <= 0) {
            db.exec("DELETE FROM otp WHERE phone = ?", phone)
            return@transaction false
        }

        if (!Ids.same(storedHash, Ids.hash(typed))) {
            val left = triesLeft - 1
            if (left <= 0) db.exec("DELETE FROM otp WHERE phone = ?", phone)
            else db.exec("UPDATE otp SET tries_left = ? WHERE phone = ?", left, phone)
            return@transaction false
        }

        // Right. Spend it - a code works once.
        db.exec("DELETE FROM otp WHERE phone = ?", phone)
        true
    }

    /** Vietnamese, because the people typing it are. */
    private fun text(code: String) =
        "Mã xác thực của bạn là $code. Có hiệu lực trong $liveMinutes phút."
}
