package vn.edu.vgu.smartlocker.server.cabinet

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.str

/**
 * The doors, and the counter that locks one after too many wrong guesses.
 *
 * The counter is **on disk**, in the `lockouts` table, and that is the whole
 * reason this project has a database rather than a map in memory. A counter
 * that resets when the process dies hands the guesses back to anybody who can
 * crash the server, and a locker is worth crashing a server for.
 */
class Boxes(
    private val db: Db,
    private val wrongTriesBeforeLock: Int,
    private val lockMinutes: Long,
) {

    /**
     * Which numbers are free, for endpoint 18.
     *
     * **Free numbers only, never taken ones.** A door that is taken is drawn
     * as taken and nothing more - returning nineteen taken numbers tells an
     * attacker the cabinet's occupancy pattern over time. Rule 6 in
     * `architecture.md`.
     */
    fun free(cabinetId: String): List<String> = db.rows(
        "SELECT number FROM boxes WHERE cabinet_id = ? AND state = 'free' ORDER BY number",
        cabinetId,
    ) { it.str("number") }

    /**
     * How many doors are free at each size. Endpoint 30, for the app.
     *
     * **Counts, never numbers.** A phone deciding where to book needs to know
     * whether there is room; a list of which specific doors stand empty is a
     * map of the cabinet's occupancy handed to anybody with an account. Same
     * rule endpoint 18 follows, one step tighter, because 18 answers a
     * cabinet and this answers a phone.
     */
    fun freeBySize(cabinetId: String): Map<String, Int> = db.rows(
        "SELECT size, COUNT(*) AS n FROM boxes WHERE cabinet_id = ? AND state = 'free' GROUP BY size",
        cabinetId,
    ) { it.str("size") to it.num("n").toInt() }.toMap()

    fun total(cabinetId: String): Int =
        db.row("SELECT COUNT(*) AS n FROM boxes WHERE cabinet_id = ?", cabinetId) {
            it.num("n").toInt()
        } ?: 0

    /**
     * Take a free box of a size, or null if there is none.
     *
     * Asked for `large` and none free? This answers null rather than quietly
     * handing over a small one. The shipper is told to try a smaller size and
     * decides for himself, because only he can see the parcel.
     *
     * [holding] is the state the door lands in. `taken` means a parcel is
     * inside; `booked` means somebody has reserved it and nothing is inside
     * yet (ADR 0026). Both are equally not-free to this function, which is
     * the point - a booked door can never be handed to a walk-up drop,
     * because the query below only ever selects `free`.
     */
    fun claimFree(cabinetId: String, size: String, holding: String = "taken"): String? = db.transaction {
        val number = db.row(
            """SELECT number FROM boxes
                WHERE cabinet_id = ? AND state = 'free' AND size = ?
                ORDER BY number LIMIT 1""",
            cabinetId, size,
        ) { it.str("number") } ?: return@transaction null

        db.exec(
            "UPDATE boxes SET state = ? WHERE cabinet_id = ? AND number = ?",
            holding, cabinetId, number,
        )
        number
    }

    fun release(cabinetId: String, number: String) = db.exec(
        "UPDATE boxes SET state = 'free' WHERE cabinet_id = ? AND number = ? AND state != 'faulty'",
        cabinetId, number,
    )

    fun markFaulty(cabinetId: String, number: String) = db.exec(
        "UPDATE boxes SET state = 'faulty' WHERE cabinet_id = ? AND number = ?",
        cabinetId, number,
    )

    fun state(cabinetId: String, number: String): String =
        db.row(
            "SELECT state FROM boxes WHERE cabinet_id = ? AND number = ?", cabinetId, number,
        ) { it.str("state") }.orEmpty()

    // --- The lockout counter ------------------------------------------------

    /** When this box stops refusing codes, or 0 if it is not locked. */
    fun lockedUntil(cabinetId: String, number: String): Long {
        val until = db.row(
            "SELECT locked_until FROM lockouts WHERE cabinet_id = ? AND box_number = ?",
            cabinetId, number,
        ) { it.num("locked_until") } ?: 0
        return if (until > now()) until else 0
    }

    /**
     * Count a wrong guess. Returns the moment it unlocks, or 0.
     *
     * The count is written before the answer is given, so a caller that hangs
     * up mid-request has still used a try.
     */
    fun wrongTry(cabinetId: String, number: String): Long = db.transaction {
        val tries = 1 + (db.row(
            "SELECT wrong_tries FROM lockouts WHERE cabinet_id = ? AND box_number = ?",
            cabinetId, number,
        ) { it.num("wrong_tries") } ?: 0)

        val until = if (tries >= wrongTriesBeforeLock) now() + lockMinutes * 60 * 1000 else 0
        val counted = if (until > 0) 0 else tries

        db.exec(
            """INSERT INTO lockouts (cabinet_id, box_number, wrong_tries, locked_until)
               VALUES (?, ?, ?, ?)
               ON CONFLICT(cabinet_id, box_number) DO UPDATE SET
                 wrong_tries = excluded.wrong_tries,
                 locked_until = excluded.locked_until""",
            cabinetId, number, counted, until,
        )
        until
    }

    /** A right answer clears the count. */
    fun clearTries(cabinetId: String, number: String) = db.exec(
        "DELETE FROM lockouts WHERE cabinet_id = ? AND box_number = ?", cabinetId, number,
    )
}
