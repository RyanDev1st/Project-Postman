package vn.edu.vgu.smartlocker.server.bookings

import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.cabinet.Boxes
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.str

/** One held door, as the app and the cabinet both see it. */
data class Booking(
    val id: String,
    val receiverId: String,
    val cabinetId: String,
    val cabinetName: String,
    val boxNumber: String,
    val expiresAt: Long,
)

/**
 * A box held for somebody before their parcel arrives. ADR 0026, task P2-13.
 *
 * **This is what makes an unverified phone number safe.** No SMS is sent, so
 * nothing proves the number belongs to whoever typed it - and nothing has to.
 * A booking is made by a signed-in member of the university, for a delivery
 * they are expecting themselves, so a wrong digit breaks their own delivery
 * and nobody else's. The number is a delivery address, not a credential.
 *
 * Two rules live in the schema rather than here, because a rule in code is a
 * rule somebody can forget to check:
 *
 *  - `bookings.receiver_id` is **UNIQUE** - one live booking per account.
 *  - `(cabinet_id, box_number)` is **UNIQUE** - one booker per door.
 *
 * A booking that expires, is cancelled or is filled is **deleted**, so those
 * constraints always describe live bookings and nothing else. That is why
 * there is no `state` column here: a row exists or it does not.
 */
class Bookings(
    private val db: Db,
    private val boxes: Boxes,
    private val liveHours: Long,
) {

    /**
     * Hold a free box of this size at this cabinet.
     *
     * Returns null when nothing of that size is free - **never a different
     * size**. Only the person expecting the parcel knows whether a smaller
     * door will do, which is the same rule endpoint 11 follows at the cabinet.
     *
     * Throws [BookingExists] rather than returning null for the other refusal,
     * because the two need different words on the screen and a null cannot
     * say which happened.
     *
     * Expired bookings are swept first, inside the same transaction. Without
     * that, a cabinet whose doors are all held by yesterday's bookings has no
     * free box until something else happens to call [sweep] - and nothing in
     * this system runs on a timer.
     */
    fun create(receiverId: String, cabinetId: String, size: String): Booking? = db.transaction {
        sweep()
        if (mine(receiverId) != null) throw BookingExists()

        val number = boxes.claimFree(cabinetId, size, holding = "booked")
            ?: return@transaction null

        val id = Ids.id()
        val expiresAt = now() + liveHours * 60 * 60 * 1000
        db.exec(
            """INSERT INTO bookings
                 (id, receiver_id, cabinet_id, box_number, created_at, expires_at)
               VALUES (?, ?, ?, ?, ?, ?)""",
            id, receiverId, cabinetId, number, now(), expiresAt,
        )
        db.exec(
            """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, action, detail)
               VALUES (?, ?, ?, ?, ?, 'booked', ?)""",
            Ids.id(), now(), receiverId, cabinetId, number, size,
        )
        Booking(id, receiverId, cabinetId, nameOf(cabinetId), number, expiresAt)
    }

    /** This account's live booking, or null. Expired ones are swept first. */
    fun mine(receiverId: String): Booking? {
        sweep()
        return db.row(
            """SELECT b.id, b.receiver_id, b.cabinet_id, b.box_number, b.expires_at, c.name
                 FROM bookings b JOIN cabinets c ON c.id = b.cabinet_id
                WHERE b.receiver_id = ?""",
            receiverId,
        ) { it.toBooking() }
    }

    /**
     * Give the door back. True if there was one to give back.
     *
     * The box goes to `free`, not to `taken`: nothing was ever put in it.
     * [Boxes.release] leaves a faulty door faulty, which is what stops a
     * cancelled booking quietly returning a broken box to the pool.
     */
    fun cancel(receiverId: String): Boolean = db.transaction {
        val held = mine(receiverId) ?: return@transaction false
        db.exec("DELETE FROM bookings WHERE id = ?", held.id)
        boxes.release(held.cabinetId, held.boxNumber)
        db.exec(
            """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, action, detail)
               VALUES (?, ?, ?, ?, ?, 'booking-cancelled', '')""",
            Ids.id(), now(), receiverId, held.cabinetId, held.boxNumber,
        )
        true
    }

    /**
     * Drop every booking that has run out, and free its door.
     *
     * Called from every read and every write rather than from a timer. A
     * background thread would be a second thing to start, a second thing to
     * fail quietly, and a second thing to reason about at three in the
     * morning - and this runs in microseconds on a table that holds at most
     * one row per student.
     *
     * The event is written per booking so the log can answer *why did that
     * door open up again*, which is the question somebody who arrived at
     * 24 hours and one minute will ask.
     */
    fun sweep() {
        val dead = db.rows(
            """SELECT b.id, b.receiver_id, b.cabinet_id, b.box_number, b.expires_at, c.name
                 FROM bookings b JOIN cabinets c ON c.id = b.cabinet_id
                WHERE b.expires_at <= ?""",
            now(),
        ) { it.toBooking() }
        if (dead.isEmpty()) return

        dead.forEach { held ->
            db.exec("DELETE FROM bookings WHERE id = ?", held.id)
            boxes.release(held.cabinetId, held.boxNumber)
            db.exec(
                """INSERT INTO events (id, at, receiver_id, cabinet_id, box_number, action, detail)
                   VALUES (?, ?, ?, ?, ?, 'booking-expired', '')""",
                Ids.id(), now(), held.receiverId, held.cabinetId, held.boxNumber,
            )
        }
    }

    /**
     * The live booking at this cabinet for this receiver, if there is one.
     *
     * The cabinet's ladder asks this at rung A: a drop for somebody who booked
     * **here** goes into the door they reserved, and `claimFree` is not
     * consulted. A booking at a different cabinet is not a match, because the
     * parcel is not there.
     */
    fun at(cabinetId: String, receiverId: String): Booking? =
        mine(receiverId)?.takeIf { it.cabinetId == cabinetId }

    /**
     * Fill a booking: the parcel is in, so the hold is over and the door is
     * `taken`. Called by the drop, once the box really has something in it.
     */
    fun fill(booking: Booking) = db.transaction {
        db.exec("DELETE FROM bookings WHERE id = ?", booking.id)
        db.exec(
            "UPDATE boxes SET state = 'taken' WHERE cabinet_id = ? AND number = ?",
            booking.cabinetId, booking.boxNumber,
        )
    }

    private fun nameOf(cabinetId: String): String =
        db.row("SELECT name FROM cabinets WHERE id = ?", cabinetId) { it.str("name") }.orEmpty()

    private fun java.sql.ResultSet.toBooking() = Booking(
        id = str("id"),
        receiverId = str("receiver_id"),
        cabinetId = str("cabinet_id"),
        cabinetName = str("name"),
        boxNumber = str("box_number"),
        expiresAt = num("expires_at"),
    )
}

/** One live booking per account, and this account already has one. */
class BookingExists : RuntimeException("this account already holds a booking")
