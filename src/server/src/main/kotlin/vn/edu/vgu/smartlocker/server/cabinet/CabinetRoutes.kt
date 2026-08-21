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
import vn.edu.vgu.smartlocker.server.Phone
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.maskName
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * Endpoints 9, 10, 11 and 18 - the shipper's flow at the cabinet screen.
 *
 * **The cabinet is a public terminal.** Anyone can walk up to it. So nothing
 * here ever returns a full name, a phone number, or a list of parcels, however
 * convenient that would be to draw. Rule 6 in `architecture.md`.
 */
fun Route.cabinetRoutes(
    db: Db,
    sessions: Sessions,
    boxes: Boxes,
    commands: Commands,
    pickupCodeHours: Long,
) {

    /**
     * 9. The QR session code to display.
     *
     * The screen asks for one of these every half minute. What it draws is
     * *which cabinet, at what moment* - the code is random and says nothing on
     * its own, which is why photographing the screen opens nothing.
     */
    get("/cabinet/session") {
        val me = call.cabinetId(db)
        val live = sessions.issue(me)
        call.respond(SessionCodeResponse(live.code, sessions.liveSeconds()))
    }

    /**
     * 10. Look up a receiver by phone number.
     *
     * Returns a **masked** name and an opaque reference, never the real name
     * and never the number back. The shipper already knows who he is
     * delivering to; he only needs to confirm he has the right person.
     */
    get("/cabinet/receiver") {
        call.cabinetId(db)
        val typed = call.request.queryParameters["phone_number"].orEmpty()
        val phone = Phone.normalise(typed) ?: call.refuse(Refusal.PHONE_NOT_REGISTERED)
        val receiverId = Receivers.find(db, phone) ?: call.refuse(Refusal.PHONE_NOT_REGISTERED)

        call.respond(
            ReceiverResponse(
                ref = receiverId,
                maskedName = maskName(Receivers.name(db, receiverId)).ifBlank { "***" },
            ),
        )
    }

    /**
     * 11. Start a drop.
     *
     * Picks a free box of the asked size, opens it, and writes the parcel
     * against **that receiver**. The `receiver_id` column written here is the
     * one the Server team's schema did not have, and the reason endpoint 6 can
     * later answer *is this parcel yours*.
     *
     * The typed backup code is made here too and stored only as a hash. It
     * travels to the receiver with the notice, never back to this screen - a
     * code shown on a public terminal is a code the next person in the queue
     * has read.
     */
    post("/cabinet/drop") {
        val me = call.cabinetId(db)
        val asked = call.receive<DropRequest>()

        val exists = db.row("SELECT id FROM receivers WHERE id = ?", asked.receiverRef) {
            it.str("id")
        }
        if (exists == null) call.refuse(Refusal.PHONE_NOT_REGISTERED)

        val box = boxes.claimFree(me, asked.size.ifBlank { "medium" })
            ?: call.refuse(Refusal.NO_FREE_BOX)

        // The typed code is generated here and kept only as a hash. It reaches
        // the receiver with the notice - never back to this screen, because a
        // code shown on a public terminal has been read by the next person in
        // the queue.
        val pickupCode = Ids.digits(6)
        db.transaction {
            db.exec(
                """INSERT INTO parcels
                     (id, receiver_id, cabinet_id, box_number, state, arrived_at,
                      pickup_code_hash, pickup_code_expires_at)
                   VALUES (?, ?, ?, ?, 'waiting', ?, ?, ?)""",
                Ids.id(), asked.receiverRef, me, box, now(),
                Ids.hash(pickupCode), now() + pickupCodeHours * 60 * 60 * 1000,
            )
            val commandId = commands.open(me, box, "drop")
            db.exec(
                """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, action, detail)
                   VALUES (?, ?, ?, ?, ?, 'drop-opened', ?)""",
                Ids.id(), now(), asked.receiverRef, me, box, commandId,
            )
        }

        call.respond(DropResponse(boxNumber = box))
    }

    /**
     * 18. Which boxes here are free.
     *
     * Free numbers, or an empty list. Never who is in a taken box, never a
     * name, never a number. `free: []` says everything a full cabinet needs to.
     */
    get("/cabinet/free") {
        val me = call.cabinetId(db)
        call.respond(FreeResponse(total = boxes.total(me), free = boxes.free(me)))
    }
}

@Serializable
data class SessionCodeResponse(
    @SerialName("session_code") val sessionCode: String,
    @SerialName("lives_seconds") val livesSeconds: Long,
)

@Serializable
data class ReceiverResponse(
    val ref: String,
    @SerialName("masked_name") val maskedName: String,
)

@Serializable
data class DropRequest(
    @SerialName("receiver_ref") val receiverRef: String = "",
    val size: String = "medium",
)

@Serializable
data class DropResponse(@SerialName("box_number") val boxNumber: String)

@Serializable
data class FreeResponse(val total: Int, val free: List<String>)
