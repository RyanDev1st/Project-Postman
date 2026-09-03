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
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.bookings.Bookings
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * Endpoints 9, 10, 11, 18 and 28 - the shipper's flow at the cabinet screen.
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
    bookings: Bookings,
    ladder: Ladder,
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
     * 10. Look up a receiver by phone number. **Rungs A to C of the ladder.**
     *
     * Returns a **masked** name and an opaque reference, never the real name
     * and never the number back. The shipper already knows who he is
     * delivering to; he only needs to confirm he has the right person.
     *
     * `match` says how sure the server is, and the screen words itself from
     * it: `booking` and `exact` are a statement, `near` is a question -
     * *"Did you mean Nguyễn V. A***?"*. `box_number` is present only when a
     * live booking here is holding a door.
     *
     * `PHONE_NOT_REGISTERED` here is **rung D**, and the screen's next move
     * is to ask for the name, not to send the parcel to ABO. It covers a
     * number that matched nobody and a number that was close to two people,
     * in one code on purpose: the screen does the same thing either way, and
     * a second code would only tell somebody probing which happened.
     */
    get("/cabinet/receiver") {
        val me = call.cabinetId(db)
        val typed = call.request.queryParameters["phone_number"].orEmpty()
        val phone = Phone.normalise(typed) ?: call.refuse(Refusal.PHONE_NOT_REGISTERED)
        val found = ladder.byNumber(me, phone) ?: call.refuse(Refusal.PHONE_NOT_REGISTERED)
        call.respond(found.answer())
    }

    /**
     * 28. Is this the name on the parcel? **Rung D.**
     *
     * The shipper reads the name off the label and types it. The server holds
     * the full names and answers with **one masked name or nothing** - never
     * a list, never a phone number.
     *
     * This was a masked *list* until 2026-09-02, and the list was measured
     * and dropped: `Nguyễn Văn Phong`, `Nguyễn Văn Phúc` and
     * `Nguyễn Văn Phương` all mask to `Nguyễn V. P***`, and two masks collide
     * in 56% of full cabinets - so a shipper choosing between two identical
     * rows was a coin flip that ends with a parcel in a stranger's reserved
     * box, which that stranger then opens legitimately with their own login.
     *
     * Typing inverts it, and leaks strictly less: a list hands over twenty
     * names at once, while this answers yes or no about one guess.
     */
    post("/cabinet/confirm-name") {
        val me = call.cabinetId(db)
        val asked = call.receive<ConfirmNameRequest>()
        val found = ladder.byName(me, asked.name) ?: call.refuse(Refusal.PHONE_NOT_REGISTERED)
        // The number the shipper typed is carried so the drop can tell the
        // receiver which digits disagreed. It is never echoed back.
        call.respond(found.answer())
    }

    /**
     * 11. Start a drop.
     *
     * Picks a free box of the asked size, opens it, and writes the parcel
     * against **that receiver**. The `receiver_id` column written here is the
     * one the Server team's schema did not have, and the reason endpoint 6 can
     * later answer *is this parcel yours*.
     *
     * **A live booking here wins.** The parcel goes into the door that person
     * reserved and `claimFree` is not consulted, so a reserved door is never
     * handed to somebody else's drop. Without a booking it behaves exactly as
     * it did before. Task P3-08.
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

        val held = bookings.at(me, asked.receiverRef)
        val box = held?.boxNumber
            ?: boxes.claimFree(me, asked.size.ifBlank { "medium" })
            ?: call.refuse(Refusal.NO_FREE_BOX)
        // The hold is over the moment the parcel is going in: the door stops
        // being `booked` and becomes `taken`, so nothing can book it again
        // while something is inside it.
        held?.let(bookings::fill)

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

/**
 * Who the shipper is delivering to. **No phone number, ever.**
 *
 * `match` is the rung the answer came from, lowercased: `booking`, `exact`,
 * `near` or `named`. The screen words itself from it - a `near` match is a
 * question and the others are statements - and the drop reads it to decide
 * whether the receiver is told afterwards that the digits did not agree.
 *
 * `box_number` is present only when a live booking here is holding a door.
 */
@Serializable
data class ReceiverResponse(
    val ref: String,
    @SerialName("masked_name") val maskedName: String,
    val match: String,
    @SerialName("box_number") val boxNumber: String? = null,
)

/**
 * Endpoint 28. The number the shipper already typed rides along so the drop
 * can tell the receiver which digits disagreed; it is never echoed back.
 */
@Serializable
data class ConfirmNameRequest(
    @SerialName("phone_number") val phoneNumber: String = "",
    val name: String = "",
)

private fun Match.answer() = ReceiverResponse(
    ref = receiverId,
    maskedName = maskedName,
    match = confidence.name.lowercase(),
    boxNumber = boxNumber,
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
