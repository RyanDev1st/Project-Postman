package vn.edu.vgu.smartlocker.server.cabinet

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.str

/**
 * The QR session code on the cabinet screen.
 *
 * **It is not a key.** Photograph the screen and you have *which cabinet, at
 * what moment*, which opens nothing on its own. The token in the same request
 * proves who is asking. That was always the design - ADR 0003.
 *
 * What this file adds is the half the app cannot do. The app checks a code's
 * age before it calls, and **that check protects nothing**: it runs on the
 * caller's phone, and an attacker does not run our app. So every rule is here,
 * on the server, or it is not a rule:
 *
 * - A code must be **a row in this table** - one this server handed out. It is
 *   32 random bytes and is not derived from anything, so it cannot be invented.
 * - It must be inside its window **by this server's clock**. A phone's clock
 *   is set by the person holding the phone.
 * - It survives a restart, because it is on disk. If a restart forgot, every
 *   code in the air would become unverifiable and the whole check would have
 *   to be skipped.
 */
class Sessions(private val db: Db, private val liveSeconds: Long) {

    data class Live(val code: String, val cabinetId: String, val expiresAt: Long)

    /**
     * Issue a code for a cabinet, for endpoint 9.
     *
     * A cabinet may hold several live codes at once, which is deliberate: the
     * screen refreshes every 30 seconds while a code lives for 60, so somebody
     * who started walking on the old one is not turned away when it changes.
     */
    fun issue(cabinetId: String): Live {
        sweep()
        val code = format(cabinetId, now())
        val expiresAt = now() + liveSeconds * 1000
        db.exec(
            "INSERT INTO sessions (code, cabinet_id, issued_at, expires_at) VALUES (?, ?, ?, ?)",
            code, cabinetId, now(), expiresAt,
        )
        return Live(code, cabinetId, expiresAt)
    }

    /**
     * Which cabinet this code names, or null if it is not one of ours or has
     * aged out.
     *
     * Both answers are one lookup, so a made-up code and an expired one take
     * the same path and the same time.
     */
    fun cabinetFor(code: String): String? = db.row(
        "SELECT cabinet_id FROM sessions WHERE code = ? AND expires_at > ?",
        code, now(),
    ) { it.str("cabinet_id") }

    /**
     * Claim this code for this parcel, once.
     *
     * The primary key on `session_use` is the rule. A replayed request - the
     * same captured body sent twice while the code still lives - hits the key
     * and returns false here, so the door is not opened again. It is enforced
     * by the database rather than by a check somebody has to remember.
     */
    fun claim(code: String, parcelId: String): Boolean =
        db.update(
            "INSERT OR IGNORE INTO session_use (code, parcel_id, used_at) VALUES (?, ?, ?)",
            code, parcelId, now(),
        ) == 1

    /** How long a code lives, for the screen's countdown. */
    fun liveSeconds(): Long = liveSeconds

    /**
     * Drop what has aged out.
     *
     * `session_use` rows outlive their session on purpose - a used code must
     * stay used even after it expires, or a replay would start working again
     * the moment the session row was swept.
     */
    private fun sweep() {
        db.exec("DELETE FROM sessions WHERE expires_at <= ?", now() - ONE_DAY)
        db.exec("DELETE FROM session_use WHERE used_at <= ?", now() - ONE_DAY)
    }

    companion object {
        private const val ONE_DAY = 24L * 60 * 60 * 1000

        /** The format marker. The app's `SessionCode.kt` reads the same word. */
        const val FORMAT = "VGU1"

        /**
         * `VGU1|<cabinet-id>|<unix-seconds>|<random>`
         *
         * The first three fields are readable by the app before it has any
         * signal, which is the point of them: standing in a corridor, it can
         * say "that code is old, scan again" or "this is not one of ours"
         * without a round trip. Those answers are a courtesy and **decide
         * nothing** - the server checks all of it again in [cabinetFor].
         *
         * The fourth field is the one that matters. Without it the code would
         * be a cabinet id and a clock reading, which anybody can type: an
         * attacker would guess `VGU1|vgu-back-gate|<any second>` until one
         * landed. 32 random bytes is what makes the code unguessable, and it
         * is why the first three fields can be readable without costing
         * anything.
         */
        fun format(cabinetId: String, atMillis: Long): String =
            "$FORMAT|$cabinetId|${atMillis / 1000}|${Ids.secret()}"
    }
}
