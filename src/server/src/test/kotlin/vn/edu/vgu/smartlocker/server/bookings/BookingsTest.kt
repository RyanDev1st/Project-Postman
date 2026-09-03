package vn.edu.vgu.smartlocker.server.bookings

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Pepper
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.cabinet.Boxes
import vn.edu.vgu.smartlocker.server.cabinet.Cabinets
import vn.edu.vgu.smartlocker.server.str

/**
 * A held door, on a real database file. Task P2-13, ADR 0026.
 *
 * Every rule here is one the schema enforces or one a person's parcel depends
 * on, so none of it is checked against a fake: a fake store has whatever
 * behaviour the fake was written to have, and the two `UNIQUE` constraints
 * doing the real work live in SQLite.
 */
class BookingsTest {

    private lateinit var file: File
    private lateinit var db: Db
    private lateinit var boxes: Boxes
    private lateinit var bookings: Bookings

    private val cabinet = "vgu-back-gate"
    private lateinit var minh: String
    private lateinit var an: String

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 5 }))
        Pepper.load(key)
        file = File.createTempFile("bookings", ".db").apply { delete() }
        db = Db(file)
        boxes = Boxes(db, wrongTriesBeforeLock = 5, lockMinutes = 15)
        bookings = Bookings(db, boxes, liveHours = 24)

        Cabinets.create(db, cabinet, "VGU Back Gate", boxes = 20)
        minh = Receivers.findOrCreateByGoogle(db, "google-minh", "Nguyễn Văn Minh")
        an = Receivers.findOrCreateByGoogle(db, "google-an", "Trần Thị An")
    }

    @AfterTest
    fun close() {
        db.close()
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    // --- Holding a door ----------------------------------------------------

    /**
     * A booking holds **one named door**, not a promise of some door later.
     * The shipper walks to a number, so the number has to exist before he
     * arrives.
     */
    @Test
    fun `a booking holds one named door, and that door stops being free`() {
        val held = assertNotNull(bookings.create(minh, cabinet, "medium"))

        assertEquals(cabinet, held.cabinetId)
        assertEquals("VGU Back Gate", held.cabinetName)
        assertTrue(held.boxNumber.isNotEmpty())
        assertEquals("booked", boxes.state(cabinet, held.boxNumber))
        assertTrue(held.boxNumber !in boxes.free(cabinet))
    }

    /**
     * **The rule the whole reservation rests on.** A booked door must never be
     * handed to a walk-up drop for somebody else - that stranger would put a
     * parcel in a box its owner then opens with their own login, legitimately.
     *
     * Every medium door is booked, so `claimFree` has nothing left of that
     * size to give and must say so rather than reach for a booked one.
     */
    @Test
    fun `a booked door is never handed to a walk-up drop`() {
        val mediums = boxes.freeBySize(cabinet)["medium"] ?: 0
        assertTrue(mediums > 0, "the fixture cabinet has no medium doors")

        val held = assertNotNull(bookings.create(minh, cabinet, "medium"))
        repeat(mediums - 1) { assertNotNull(boxes.claimFree(cabinet, "medium")) }

        assertNull(boxes.claimFree(cabinet, "medium"))
        assertEquals("booked", boxes.state(cabinet, held.boxNumber))
    }

    /** Asked for a size with nothing free, the answer is nothing - not a smaller door. */
    @Test
    fun `a size with nothing free is refused rather than downgraded`() {
        val larges = boxes.freeBySize(cabinet)["large"] ?: 0
        repeat(larges) { assertNotNull(boxes.claimFree(cabinet, "large")) }

        assertNull(bookings.create(minh, cabinet, "large"))
        assertNull(bookings.mine(minh), "a refused booking must leave no row behind")
    }

    /** One live booking per account, and the second attempt says which. */
    @Test
    fun `one account cannot hold two doors`() {
        assertNotNull(bookings.create(minh, cabinet, "medium"))
        assertFailsWith<BookingExists> { bookings.create(minh, cabinet, "small") }
        assertEquals(1, db.row("SELECT COUNT(*) AS n FROM bookings") { it.getInt("n") })
    }

    /** Two people, two doors, and never the same one. */
    @Test
    fun `two people get two different doors`() {
        val hers = assertNotNull(bookings.create(an, cabinet, "medium"))
        val his = assertNotNull(bookings.create(minh, cabinet, "medium"))
        assertNotEquals(hers.boxNumber, his.boxNumber)
    }

    // --- Giving it back ----------------------------------------------------

    @Test
    fun `cancelling gives the door back`() {
        val held = assertNotNull(bookings.create(minh, cabinet, "medium"))
        assertTrue(bookings.cancel(minh))

        assertNull(bookings.mine(minh))
        assertEquals("free", boxes.state(cabinet, held.boxNumber))
        assertTrue(held.boxNumber in boxes.free(cabinet))
    }

    /** Cancelling twice says so the second time, rather than pretending. */
    @Test
    fun `cancelling nothing is not a success`() {
        assertNotNull(bookings.create(minh, cabinet, "medium"))
        assertTrue(bookings.cancel(minh))
        assertTrue(!bookings.cancel(minh))
    }

    /**
     * A cancelled booking must not quietly return a **faulty** door to the
     * pool. The door was broken before the booking and is broken after it.
     */
    @Test
    fun `cancelling does not un-break a faulty door`() {
        val held = assertNotNull(bookings.create(minh, cabinet, "medium"))
        boxes.markFaulty(cabinet, held.boxNumber)
        bookings.cancel(minh)
        assertEquals("faulty", boxes.state(cabinet, held.boxNumber))
    }

    /**
     * **The expiry, and it is the whole answer to a starved cabinet.** Twenty
     * live bookings fill a twenty-door cabinet with nothing inside it; what
     * bounds that is this clock and the one-per-account rule.
     *
     * Run with a booking that lasts no time at all, so the sweep has
     * something real to find rather than a row edited to look expired.
     */
    @Test
    fun `a booking that runs out frees its door`() {
        val brief = Bookings(db, boxes, liveHours = 0)
        val held = assertNotNull(brief.create(minh, cabinet, "medium"))

        assertNull(brief.mine(minh))
        assertEquals("free", boxes.state(cabinet, held.boxNumber))
        assertEquals(
            "booking-expired",
            db.row("SELECT action FROM events WHERE action = 'booking-expired'") { it.str("action") },
        )
    }

    /** And an expired booking is not in the way of the next one. */
    @Test
    fun `a booking that ran out does not block the next one`() {
        val brief = Bookings(db, boxes, liveHours = 0)
        brief.create(minh, cabinet, "medium")
        assertNotNull(bookings.create(minh, cabinet, "medium"))
    }

    // --- Which cabinet -----------------------------------------------------

    /**
     * A booking at the library is not a booking at the back gate. The parcel
     * is not there, so the ladder must fall through to a walk-up drop rather
     * than open a door in another building.
     */
    @Test
    fun `a booking belongs to the cabinet it was made at`() {
        Cabinets.create(db, "vgu-library", "VGU Library", boxes = 20)
        val held = assertNotNull(bookings.create(minh, "vgu-library", "medium"))

        assertEquals(held.boxNumber, bookings.at("vgu-library", minh)?.boxNumber)
        assertNull(bookings.at(cabinet, minh))
    }

    /** Filling it ends the hold and marks the door as holding something. */
    @Test
    fun `filling a booking ends it and the door reads taken`() {
        val held = assertNotNull(bookings.create(minh, cabinet, "medium"))
        bookings.fill(held)

        assertNull(bookings.mine(minh))
        assertEquals("taken", boxes.state(cabinet, held.boxNumber))
    }

    // --- The number, claimed once ------------------------------------------

    /** First claim wins, and a second person typing the same digits is refused. */
    @Test
    fun `a number belongs to one account`() {
        assertTrue(Receivers.claimPhone(db, minh, "+84908619328"))
        assertTrue(!Receivers.claimPhone(db, an, "+84908619328"))
        assertEquals("+84908619328", Receivers.phone(db, minh))
        assertEquals("", Receivers.phone(db, an))
    }

    /**
     * Claiming the same number twice is not an error. A phone that retried a
     * call it never saw the answer to must not be punished for it.
     */
    @Test
    fun `claiming the same number again is fine`() {
        assertTrue(Receivers.claimPhone(db, minh, "+84908619328"))
        assertTrue(Receivers.claimPhone(db, minh, "+84908619328"))
    }

    /**
     * **The number is typed once.** A second booking that sends a different
     * number does not change it - only endpoint 29 does, and it says so by
     * passing `replace`.
     */
    @Test
    fun `a second number does not overwrite the first by accident`() {
        assertTrue(Receivers.claimPhone(db, minh, "+84908619328"))
        assertTrue(!Receivers.claimPhone(db, minh, "+84912345678"))
        assertEquals("+84908619328", Receivers.phone(db, minh))

        assertTrue(Receivers.claimPhone(db, minh, "+84912345678", replace = true))
        assertEquals("+84912345678", Receivers.phone(db, minh))
    }

    /** Endpoint 29 still cannot take a number somebody else holds. */
    @Test
    fun `changing a number cannot steal one`() {
        Receivers.claimPhone(db, an, "+84908619328")
        Receivers.claimPhone(db, minh, "+84912345678")
        assertTrue(!Receivers.claimPhone(db, minh, "+84908619328", replace = true))
        assertEquals("+84912345678", Receivers.phone(db, minh))
    }

    /**
     * A Google account starts with **no number at all**, and that is a normal
     * state rather than a broken one. It can sign in and read its own
     * parcels; it cannot be sent one until it books.
     */
    @Test
    fun `a brand new account has no number`() {
        assertEquals("", Receivers.phone(db, minh))
        assertTrue(!Receivers.hasPhone(db, minh))
        assertEquals("Nguyễn Văn Minh", Receivers.name(db, minh))
    }
}
