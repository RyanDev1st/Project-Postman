package vn.edu.vgu.smartlocker.server.cabinet

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.str

/**
 * What the cabinet hardware is told to do.
 *
 * The ESP32 cannot be dialled. It takes a DHCP address on campus Wi-Fi, it has
 * no name, and nothing outside can reach it. So it **asks, and is never told**:
 * it polls [take] about once a second and does what comes back. ADR 0020.
 *
 * **A command is handed over exactly once.** `taken_at` is stamped inside the
 * same transaction that reads the row, so an ESP32 that loses the reply and
 * polls again gets nothing rather than opening the door a second time. Rule 3
 * in `architecture.md` says an open never comes from a retry; this is where
 * that stops being a wish.
 *
 * The cost of asking rather than being told is one poll of delay. A student
 * who has just scanned will accept a second; the poll interval is a setting so
 * that number can be argued with rather than found in the code.
 */
class Commands(private val db: Db) {

    data class Waiting(
        val id: String,
        val boxNumber: String,
        val action: String,
        val purpose: String,
    )

    /**
     * Queue a door to open. Returns the id, which lands in the event log.
     *
     * **`purpose` says why, and the hardware could not work it out.** The same
     * door opens for a courier putting a parcel in and for a student taking
     * one out, and what happens when it shuts is opposite in the two cases.
     * Until BUG-009 the command said only `open`, so an ESP32 reporting the
     * door shut had to guess which it had been - and a guess there books a
     * collection that never happened, or loses one that did.
     */
    fun open(cabinetId: String, boxNumber: String, purpose: String): String =
        queue(cabinetId, boxNumber, "open", purpose)

    private fun queue(cabinetId: String, boxNumber: String, action: String, purpose: String): String {
        val id = Ids.id()
        db.exec(
            """INSERT INTO commands (id, cabinet_id, box_number, action, purpose, created_at)
               VALUES (?, ?, ?, ?, ?, ?)""",
            id, cabinetId, boxNumber, action, purpose, now(),
        )
        return id
    }

    /**
     * Everything waiting for this cabinet, marked as handed over.
     *
     * Read and stamp are one transaction. Two ESP32s on the same cabinet - or
     * one that reconnected while the old socket was still open - cannot both
     * receive the same row.
     */
    fun take(cabinetId: String): List<Waiting> = db.transaction {
        val waiting = db.rows(
            """SELECT id, box_number, action, purpose FROM commands
               WHERE cabinet_id = ? AND taken_at IS NULL
               ORDER BY created_at ASC LIMIT 16""",
            cabinetId,
        ) { Waiting(it.str("id"), it.str("box_number"), it.str("action"), it.str("purpose")) }

        waiting.forEach {
            db.exec("UPDATE commands SET taken_at = ? WHERE id = ?", now(), it.id)
        }
        waiting
    }

    /**
     * The hardware reporting back.
     *
     * Only for the record. Nothing waits on it, because a cabinet that loses
     * power between opening a door and saying so must not leave a student
     * standing at an open box being told it failed.
     */
    fun done(id: String, cabinetId: String, result: String) {
        db.exec(
            "UPDATE commands SET done_at = ?, result = ? WHERE id = ? AND cabinet_id = ?",
            now(), result.take(40), id, cabinetId,
        )
    }

    /**
     * Drop anything nobody collected.
     *
     * A command older than this is not opened late. A door that opens two
     * minutes after a student gave up and walked away is worse than one that
     * never opened, because nobody is watching it.
     */
    fun expireStale(olderThanMs: Long) {
        db.exec(
            "DELETE FROM commands WHERE taken_at IS NULL AND created_at < ?",
            now() - olderThanMs,
        )
    }
}
