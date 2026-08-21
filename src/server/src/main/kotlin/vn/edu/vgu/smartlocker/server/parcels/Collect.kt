package vn.edu.vgu.smartlocker.server.parcels

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.cabinet.Commands
import vn.edu.vgu.smartlocker.server.cabinet.Sessions
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.str

/**
 * **The rule.** Somebody scanned a cabinet; may a door open, and which one?
 *
 * Endpoint 6, and the reason the server exists. Everything else in this
 * project is how a person gets to this function.
 *
 * The five checks below are "The scanned code - what protects it" in
 * `api-contract.md`, in order. The app performs four of them before it calls,
 * and **not one of those four counts**: they run on the caller's phone, and an
 * attacker posts to `/parcels/collect` directly without ever running our app.
 * If a check is not in this file, it does not exist.
 *
 * The order is not arbitrary. Staleness is answered **first**, so a dead code
 * gets the same answer whichever cabinet it names - otherwise a stranger with
 * an old photograph of a screen could learn where somebody's parcels are by
 * reading which refusal came back.
 */
class Collect(
    private val db: Db,
    private val sessions: Sessions,
    private val commands: Commands,
    /** How long an unconfirmed open is believed. See [recoverStranded]. */
    private val openTimeoutSeconds: Long = 120,
) {

    sealed interface Result {
        /** A door was told to open. Not yet that it moved. */
        data class Opening(val boxNumber: String, val cabinetName: String) : Result

        data object SessionExpired : Result
        data object NoParcelHere : Result
        data object BoxFaulty : Result
    }

    fun byScan(receiverId: String, sessionCode: String): Result = db.transaction {

        // 0. Give back anything stranded mid-collect. See [recoverStranded].
        recoverStranded()

        // 1. A code this server issued, still inside its window, by this
        //    server's clock. Anything else is expired, including invented.
        val cabinetId = sessions.cabinetFor(sessionCode) ?: return@transaction Result.SessionExpired

        // 2. A parcel that is BOTH this person's AND at this cabinet. The two
        //    conditions are one query on purpose - splitting them would let a
        //    different refusal come back for "yours, elsewhere" than for
        //    "here, somebody else's", and those must read the same.
        val parcel = db.row(
            """SELECT p.id AS id, p.box_number AS box_number, b.state AS box_state,
                      c.name AS cabinet_name
                 FROM parcels p
                 JOIN cabinets c ON c.id = p.cabinet_id
                 LEFT JOIN boxes b ON b.cabinet_id = p.cabinet_id AND b.number = p.box_number
                WHERE p.receiver_id = ? AND p.cabinet_id = ? AND p.state = 'waiting'
                ORDER BY p.arrived_at ASC LIMIT 1""",
            receiverId, cabinetId,
        ) {
            Found(it.str("id"), it.str("box_number"), it.str("box_state"), it.str("cabinet_name"))
        } ?: return@transaction Result.NoParcelHere

        // 3. A box that will not open is its own answer. Staff are told by the
        //    server, and the person is not left pulling at a dead door.
        if (parcel.boxState == "faulty") return@transaction Result.BoxFaulty

        // 4. One open per session code, per parcel. A captured request replayed
        //    while the code still lives stops here, refused by the primary key
        //    on `session_use` rather than by a check somebody remembered.
        //    It answers NO_PARCEL_HERE, the same as an empty cabinet.
        if (!sessions.claim(sessionCode, parcel.id)) return@transaction Result.NoParcelHere

        // 5. Only now does metal move. The command, the parcel's state and the
        //    log entry are one transaction: a crash between them would open a
        //    door the log never mentions.
        val commandId = commands.open(cabinetId, parcel.boxNumber, "collect")
        db.exec(
            "UPDATE parcels SET state = 'opening', opening_since = ? WHERE id = ?",
            now(), parcel.id,
        )
        db.exec(
            """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, parcel_id, action, detail)
               VALUES (?, ?, ?, ?, ?, ?, 'collect-opened', ?)""",
            Ids.id(), now(), receiverId, cabinetId, parcel.boxNumber, parcel.id, commandId,
        )

        Result.Opening(parcel.boxNumber, parcel.cabinetName)
    }

    /**
     * Give back a parcel whose door was opened and never reported shut.
     *
     * A scan moves a parcel to `opening`, and only a door-closed moves it on.
     * There is no sensor — [ADR 0006] — so that report is a person tapping a
     * screen, and it may simply never come: they walk off, the screen is
     * asleep, the cabinet reboots.
     *
     * Before this, such a parcel stayed `opening` for good. Step 2 only ever
     * matches `waiting`, so its owner scanned again and was told **nothing is
     * waiting here** — locked out of their own parcel, in a box the server
     * still believed was theirs. The typed backup code was gone too, so both
     * ways in were closed at once.
     *
     * So an open that nobody confirmed expires, and the parcel goes back to
     * `waiting`. The box stays `taken`, because the parcel really is still in
     * it. The worst this can do is open the same door twice for the same
     * person, which is what they were trying to achieve.
     */
    private fun recoverStranded() = db.exec(
        """UPDATE parcels SET state = 'waiting', opening_since = NULL
            WHERE state = 'opening'
              AND (opening_since IS NULL OR opening_since < ?)""",
        now() - openTimeoutSeconds * 1000,
    )

    private data class Found(
        val id: String,
        val boxNumber: String,
        val boxState: String,
        val cabinetName: String,
    )
}
