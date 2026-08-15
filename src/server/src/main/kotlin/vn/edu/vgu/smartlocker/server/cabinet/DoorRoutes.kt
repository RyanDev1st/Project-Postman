package vn.edu.vgu.smartlocker.server.cabinet

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.asIso
import vn.edu.vgu.smartlocker.server.auth.Empty
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * Doors: the typed backup code, a door closing, a faulty box, and the two
 * calls the ESP32 makes.
 *
 * Endpoints 12, 13 and 14 of the contract, plus 22 and 23 which were added
 * with ADR 0020 because the hardware needs a way to be told things and cannot
 * be dialled.
 */
fun Route.doorRoutes(db: Db, boxes: Boxes, commands: Commands) {

    /**
     * 12. A door closed.
     *
     * Named for the event, not for what causes it. There is no sensor - ADR
     * 0006 - so today the screen fires this when the shipper taps "closed".
     * If a sensor is fitted later it fires the same call and nothing else
     * changes.
     *
     * A drop closing leaves the box taken and the parcel waiting. A collect
     * closing frees the box and writes the parcel off as collected. With no
     * sensor we cannot know the parcel actually left, and ADR 0006 accepts
     * that: it is traceable afterwards, not preventable beforehand.
     */
    post("/cabinet/door-closed") {
        val me = call.cabinetId(db)
        val asked = call.receive<DoorClosedRequest>()
        val box = asked.boxNumber

        db.transaction {
            if (asked.purpose == "collect") {
                db.exec(
                    """UPDATE parcels SET state = 'collected', collected_at = ?,
                         pickup_code_hash = NULL
                       WHERE cabinet_id = ? AND box_number = ? AND state = 'opening'""",
                    now(), me, box,
                )
                boxes.release(me, box)
            }
            db.exec(
                """INSERT INTO events (id, at, cabinet_id, box_number, action, detail)
                   VALUES (?, ?, ?, ?, 'door-closed', ?)""",
                Ids.id(), now(), me, box, asked.purpose,
            )
        }
        call.respond(Empty())
    }

    /**
     * 13. Pick up by typed code - the backup path.
     *
     * **The one call with no token behind it.** Everything else proves who is
     * asking from a login; this proves it from the code alone, so the code
     * carries the whole weight and the rules are not optional.
     *
     * The box number comes with the code because the notice carries both, and
     * because a lockout has to be *per box* - which cannot be counted before
     * you know which box was guessed at.
     */
    post("/cabinet/collect-by-code") {
        val me = call.cabinetId(db)
        val asked = call.receive<TypedCodeRequest>()
        val box = asked.boxNumber

        val lockedUntil = boxes.lockedUntil(me, box)
        if (lockedUntil > 0) call.refuse(Refusal.BOX_LOCKED_OUT)

        val parcel = db.row(
            """SELECT id, receiver_id FROM parcels
                WHERE cabinet_id = ? AND box_number = ? AND state = 'waiting'
                  AND pickup_code_hash = ? AND pickup_code_expires_at > ?""",
            me, box, Ids.hash(asked.code), now(),
        ) { it.str("id") to it.str("receiver_id") }

        if (parcel == null) {
            // Wrong, used and expired all answer the same. Splitting them
            // would let somebody at the keypad work out which codes are real.
            val until = boxes.wrongTry(me, box)
            call.refuse(if (until > 0) Refusal.BOX_LOCKED_OUT else Refusal.CODE_REJECTED)
        }

        if (boxes.state(me, box) == "faulty") call.refuse(Refusal.BOX_FAULTY)

        val (parcelId, receiverId) = parcel
        db.transaction {
            // The code works once. It dies here, before the door moves, so a
            // crash mid-open cannot leave it usable.
            db.exec(
                "UPDATE parcels SET state = 'opening', pickup_code_hash = NULL WHERE id = ?",
                parcelId,
            )
            boxes.clearTries(me, box)
            val commandId = commands.open(me, box)
            db.exec(
                """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, parcel_id, action, detail)
                   VALUES (?, ?, ?, ?, ?, ?, 'collect-by-code', ?)""",
                Ids.id(), now(), receiverId, me, box, parcelId, commandId,
            )
        }
        call.respond(OpenedBox(box))
    }

    /** 14. Report a faulty box. Nothing opens it again until staff clear it. */
    post("/cabinet/fault") {
        val me = call.cabinetId(db)
        val asked = call.receive<FaultRequest>()
        db.transaction {
            boxes.markFaulty(me, asked.boxNumber)
            db.exec(
                """INSERT INTO events (id, at, cabinet_id, box_number, action, detail)
                   VALUES (?, ?, ?, ?, 'fault', ?)""",
                Ids.id(), now(), me, asked.boxNumber, asked.what.take(200),
            )
        }
        call.respond(Empty())
    }

    // --- The hardware -------------------------------------------------------

    /**
     * 22. Anything for me?
     *
     * The ESP32 polls this about once a second. A row comes back once and only
     * once - `taken_at` is stamped in the same transaction that reads it - so
     * an ESP32 that loses the reply and asks again does not open a door twice.
     */
    get("/cabinet/commands") {
        val me = call.cabinetId(db)
        commands.expireStale(STALE_MS)
        val waiting = commands.take(me).map { CommandJson(it.id, it.boxNumber, it.action) }
        call.respond(CommandsResponse(waiting, now().asIso()))
    }

    /** 23. The hardware saying what happened. For the record only. */
    post("/cabinet/command-done") {
        val me = call.cabinetId(db)
        val asked = call.receive<CommandDoneRequest>()
        commands.done(asked.id, me, asked.result)
        call.respond(Empty())
    }
}

/**
 * How long an unclaimed command is worth delivering.
 *
 * A door that opens two minutes after a student gave up and walked away is
 * worse than one that never opened, because nobody is standing at it.
 */
private const val STALE_MS = 90_000L

@Serializable
data class DoorClosedRequest(
    @SerialName("box_number") val boxNumber: String = "",
    /** `drop` or `collect`. */
    val purpose: String = "drop",
)

@Serializable
data class TypedCodeRequest(
    @SerialName("box_number") val boxNumber: String = "",
    val code: String = "",
)

@Serializable
data class FaultRequest(
    @SerialName("box_number") val boxNumber: String = "",
    val what: String = "",
)

@Serializable
data class OpenedBox(@SerialName("box_number") val boxNumber: String)

@Serializable
data class CommandJson(val id: String, val box: String, val action: String)

@Serializable
data class CommandsResponse(val commands: List<CommandJson>, val at: String)

@Serializable
data class CommandDoneRequest(val id: String = "", val result: String = "")
