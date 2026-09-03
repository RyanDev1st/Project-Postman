package vn.edu.vgu.smartlocker.server.bookings

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Phone
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.auth.Tokens
import vn.edu.vgu.smartlocker.server.auth.receiverId
import vn.edu.vgu.smartlocker.server.cabinet.Boxes
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * Endpoints 25 to 27, 29 and 30 - the receiver books a door. ADR 0026.
 *
 * Every route here is the **app's**, behind a receiver token. The cabinet
 * screen never books anything: a booking is a promise made by the person
 * expecting the parcel, and the whole reason an unverified phone number is
 * safe is that only they can make it.
 */
fun Route.bookingRoutes(db: Db, tokens: Tokens, bookings: Bookings, boxes: Boxes) {

    /**
     * 25. Book a box.
     *
     * The number is sent **only the first time**, then kept on the account.
     * Every re-typing is another chance to introduce the very typo the
     * cabinet's ladder exists to survive, so this asks once and then stops
     * asking.
     *
     * The answer echoes the number **in its stored form**, `+84…`, and the
     * app shows that on a confirm panel before the booking is accepted. A
     * panel that repeats the same shape somebody just typed is a panel the
     * eye slides over; one that shows `+84 908 619 328` where they typed
     * `0908619328` is one they actually read.
     */
    post("/bookings") {
        val me = call.receiverId(tokens)
        val asked = call.receive<BookRequest>()

        val phone = call.claimNumber(db, me, asked.phoneNumber)

        val cabinet = db.row("SELECT name FROM cabinets WHERE id = ?", asked.cabinetRef) {
            it.str("name")
        } ?: call.refuse(Refusal.CABINET_UNKNOWN)

        val held = try {
            bookings.create(me, asked.cabinetRef, asked.size.ifBlank { "medium" })
        } catch (e: BookingExists) {
            call.refuse(Refusal.BOOKING_EXISTS)
        } ?: call.refuse(Refusal.NO_FREE_BOX)

        call.respond(held.answer(phone).copy(cabinetName = cabinet))
    }

    /**
     * 26. My booking, or nothing. `booked` says which.
     *
     * One shape either way, so the app has one parser and cannot mistake an
     * empty answer for a failed call.
     */
    get("/bookings") {
        val me = call.receiverId(tokens)
        val held = bookings.mine(me)
        call.respond(
            held?.answer(Receivers.phone(db, me)) ?: BookingResponse(booked = false),
        )
    }

    /**
     * 27. Give the door back.
     *
     * `NO_BOOKING` rather than a quiet success, because the app draws a
     * different screen for *cancelled* and *there was nothing to cancel*, and
     * a person who taps Cancel twice deserves to be told the second tap did
     * nothing rather than to wonder.
     */
    delete("/bookings") {
        val me = call.receiverId(tokens)
        if (!bookings.cancel(me)) call.refuse(Refusal.NO_BOOKING)
        call.respond(BookingResponse(booked = false))
    }

    /**
     * 29. Change my number. Task P2-17.
     *
     * The other half of the notice that says the courier's label and the
     * booked number disagree: somebody told their number is wrong needs one
     * tap to correct it.
     *
     * **A live booking keeps the old number.** The parcel already on its way
     * was addressed to what was typed then, and moving the number under it
     * would make the label match nothing. The new number applies from the
     * next booking on.
     */
    put("/me/phone") {
        val me = call.receiverId(tokens)
        val asked = call.receive<PhoneChangeRequest>()
        val phone = Phone.normalise(asked.phoneNumber) ?: call.refuse(Refusal.PHONE_INVALID)
        if (!Receivers.claimPhone(db, me, phone, replace = true)) call.refuse(Refusal.PHONE_IN_USE)
        call.respond(PhoneChangeResponse(phone))
    }

    /**
     * 30. Which cabinets exist, and how much room is at each.
     *
     * Added 2026-09-03 with P2-13, because the booking screen had no way to
     * name a cabinet. Endpoint 18 answers the same question but takes a
     * **cabinet key**, which a phone does not have and must never be given.
     *
     * It returns counts and not door numbers. A phone choosing where to book
     * needs to know *is there room*, and a list of which specific doors stand
     * empty is a map of the cabinet's occupancy for anybody with an account -
     * rule 6 in `architecture.md`, which is about what crosses the wire.
     */
    get("/cabinets") {
        call.receiverId(tokens)
        val list = db.rows("SELECT id, name FROM cabinets ORDER BY name") {
            it.str("id") to it.str("name")
        }
        call.respond(
            CabinetsResponse(
                list.map { (id, name) ->
                    val free = boxes.freeBySize(id)
                    CabinetSummary(
                        ref = id,
                        name = name,
                        total = boxes.total(id),
                        freeSmall = free["small"] ?: 0,
                        freeMedium = free["medium"] ?: 0,
                        freeLarge = free["large"] ?: 0,
                    )
                },
            ),
        )
    }
}

/**
 * Take the number if this account has none, or check the one it has.
 *
 * Three cases, and only the first two are allowed to proceed:
 *
 *  - **The account already has a number.** Anything sent is ignored. The
 *    number is typed once; endpoint 29 is the only way it changes.
 *  - **No number, and one was sent.** It is normalised and claimed.
 *  - **No number, and none was sent.** `PHONE_REQUIRED` - not a failure, the
 *    app's cue to show the number field on the booking screen.
 */
private suspend fun ApplicationCall.claimNumber(db: Db, me: String, typed: String): String {
    val held = Receivers.phone(db, me)
    if (held.isNotEmpty()) return held
    if (typed.isBlank()) refuse(Refusal.PHONE_REQUIRED)
    val phone = Phone.normalise(typed) ?: refuse(Refusal.PHONE_INVALID)
    if (!Receivers.claimPhone(db, me, phone)) refuse(Refusal.PHONE_IN_USE)
    return phone
}

private fun Booking.answer(phone: String) = BookingResponse(
    booked = true,
    cabinetRef = cabinetId,
    cabinetName = cabinetName,
    boxNumber = boxNumber,
    phoneNumber = phone,
    expiresAt = expiresAt.toString(),
)

@Serializable
data class BookRequest(
    @SerialName("cabinet_ref") val cabinetRef: String = "",
    val size: String = "medium",
    /** Sent only the first time. Ignored once the account has one. */
    @SerialName("phone_number") val phoneNumber: String = "",
)

/**
 * One shape for endpoints 25, 26 and 27, so the app has one parser.
 *
 * `expires_at` is a string for the same reason `SessionResponse`'s is: the
 * app reads it with `optString`, and a number there arrives as `""`.
 */
@Serializable
data class BookingResponse(
    val booked: Boolean,
    @SerialName("cabinet_ref") val cabinetRef: String = "",
    @SerialName("cabinet_name") val cabinetName: String = "",
    @SerialName("box_number") val boxNumber: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
)

@Serializable
data class PhoneChangeRequest(@SerialName("phone_number") val phoneNumber: String = "")

@Serializable
data class PhoneChangeResponse(@SerialName("phone_number") val phoneNumber: String)

@Serializable
data class CabinetsResponse(val cabinets: List<CabinetSummary>)

@Serializable
data class CabinetSummary(
    val ref: String,
    val name: String,
    val total: Int,
    @SerialName("free_small") val freeSmall: Int,
    @SerialName("free_medium") val freeMedium: Int,
    @SerialName("free_large") val freeLarge: Int,
)
