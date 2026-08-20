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
 *   The spent row stays behind holding `last_sent_at`, because that is what
 *   the cooldown is counted from; deleting it handed the guesser five more.
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

        // **A spent code is emptied, never deleted.**
        //
        // The row is also where `last_sent_at` lives, and `request` reads that
        // to hold the one-code-a-minute cooldown. Deleting it here deleted the
        // cooldown with it, so burning the five tries bought five more
        // immediately: request, guess five, request, guess five, with nothing
        // in the way. Five tries *ever* became five tries per request and
        // requests were free.
        //
        // A six-digit code is a million guesses. At the rate this server was
        // measured at that is minutes, and the prize is somebody else's
        // account and their parcels. Found by probing on 2026-08-18; the row
        // now stays, tries at zero, until the cooldown lets a new code replace
        // it. Five tries a minute is four years for a million.
        if (expiresAt <= now() || triesLeft <= 0) {
            db.exec("UPDATE otp SET tries_left = 0 WHERE phone = ?", phone)
            return@transaction false
        }

        if (!Ids.same(storedHash, Ids.hash(typed))) {
            db.exec("UPDATE otp SET tries_left = ? WHERE phone = ?", triesLeft - 1, phone)
            return@transaction false
        }

        // Right. Spend it - a code works once.
        db.exec("DELETE FROM otp WHERE phone = ?", phone)
        true
    }

    /**
     * Drop the codes that are past being useful.
     *
     * Needed because [verify] stopped deleting rows. It had to stop - the row
     * carries `last_sent_at`, and deleting it handed a guesser five more tries
     * (BUG-013) - but the deleting was also the only thing keeping this table
     * down, by accident. Without a sweep it grew to 88,524 rows, every one of
     * them expired, found by Ryan asking where the data was kept.
     *
     * **Only rows whose code has expired.** A row's job outlives its code by
     * exactly the cooldown, and a code lives five minutes against a
     * sixty-second cooldown - so by the time `expires_at` has passed, the
     * cooldown it was holding is long spent and the row is safe to drop.
     * Sweeping any harder would reopen the hole it was written to close.
     *
     * At startup, like [Tokens.sweep]. The table is keyed by phone number, so
     * on a campus it is bounded by the number of people anyway - this is about
     * the numbers that asked once and never came back.
     */
    fun sweep() = db.exec("DELETE FROM otp WHERE expires_at <= ?", now())

    /** Vietnamese, because the people typing it are. */
    private fun text(code: String) =
        "Mã xác thực của bạn là $code. Có hiệu lực trong $liveMinutes phút."
}
