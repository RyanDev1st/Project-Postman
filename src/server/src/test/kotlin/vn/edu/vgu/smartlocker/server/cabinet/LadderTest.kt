package vn.edu.vgu.smartlocker.server.cabinet

import java.io.File
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Pepper
import vn.edu.vgu.smartlocker.server.Receivers
import vn.edu.vgu.smartlocker.server.bookings.Bookings

/**
 * The five rungs the shipper walks. Tasks P3-08 to P3-11, ADR 0026.
 *
 * The number he types came off a parcel label, which came from whatever the
 * receiver typed into a shop days earlier - so a miss is far more often a
 * typo than a stranger, and the person who made it is not standing there to
 * be asked. These are the rules that decide how much the server is willing to
 * guess, and where it stops.
 */
class LadderTest {

    private lateinit var file: File
    private lateinit var db: Db
    private lateinit var boxes: Boxes
    private lateinit var bookings: Bookings
    private lateinit var ladder: Ladder

    private val gate = "vgu-back-gate"
    private val library = "vgu-library"

    @BeforeTest
    fun open() {
        val key = File.createTempFile("pepper", ".key").apply { deleteOnExit() }
        key.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 4 }))
        Pepper.load(key)
        file = File.createTempFile("ladder", ".db").apply { delete() }
        db = Db(file)
        boxes = Boxes(db, wrongTriesBeforeLock = 5, lockMinutes = 15)
        bookings = Bookings(db, boxes, liveHours = 24)
        ladder = Ladder(db, bookings)
        Cabinets.create(db, gate, "VGU Back Gate", boxes = 20)
        Cabinets.create(db, library, "VGU Library", boxes = 20)
    }

    @AfterTest
    fun close() {
        db.close()
        file.delete()
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
    }

    /** An account with a number, and optionally a booking at [cabinet]. */
    private fun person(sub: String, name: String, phone: String, cabinet: String? = null): String {
        val id = Receivers.findOrCreateByGoogle(db, sub, name)
        Receivers.claimPhone(db, id, phone)
        cabinet?.let { bookings.create(id, it, "medium") }
        return id
    }

    // --- Rung A and B: the number is exactly right -------------------------

    /**
     * Rung A. A live booking here means the drop goes into **that** door, and
     * the answer says which - so a reserved box is never handed to somebody
     * else's parcel.
     */
    @Test
    fun `an exact number with a booking names the door it is holding`() {
        val minh = person("g-minh", "Nguyễn Văn Minh", "+84908619328", gate)
        val held = assertNotNull(bookings.mine(minh))

        val found = assertNotNull(ladder.byNumber(gate, "+84908619328"))
        assertEquals(minh, found.receiverId)
        assertEquals(Confidence.BOOKING, found.confidence)
        assertEquals(held.boxNumber, found.boxNumber)
        assertEquals("Nguyễn V. M***", found.maskedName)
    }

    /** Rung B. Registered, no booking - any free door, as the cabinet always did. */
    @Test
    fun `an exact number with no booking names no door`() {
        person("g-an", "Trần Thị An", "+84912345678")
        val found = assertNotNull(ladder.byNumber(gate, "+84912345678"))
        assertEquals(Confidence.EXACT, found.confidence)
        assertNull(found.boxNumber)
    }

    /**
     * A booking at the library is not a booking at the back gate. The parcel
     * is not there, so the drop falls back to a free door here rather than
     * naming a door in another building.
     */
    @Test
    fun `a booking somewhere else does not name a door here`() {
        person("g-minh", "Nguyễn Văn Minh", "+84908619328", library)
        val found = assertNotNull(ladder.byNumber(gate, "+84908619328"))
        assertEquals(Confidence.EXACT, found.confidence)
        assertNull(found.boxNumber)
    }

    // --- Rung C: within two digits -----------------------------------------

    /** One digit wrong, one candidate. Offered. */
    @Test
    fun `one digit out finds the one booking it is close to`() {
        val minh = person("g-minh", "Nguyễn Văn Minh", "+84908619328", gate)
        val found = assertNotNull(ladder.byNumber(gate, "+84908619327"))
        assertEquals(minh, found.receiverId)
        assertEquals(Confidence.NEAR, found.confidence)
    }

    /**
     * **A transposition costs one, not two.** `...328` typed as `...382` is
     * the commonest way a person mistypes a number, and plain Levenshtein
     * scores it as two edits - which at a tolerance of two would swallow the
     * whole budget and miss a transposition with any other slip beside it.
     */
    @Test
    fun `two digits swapped is one edit, not two`() {
        person("g-minh", "Nguyễn Văn Minh", "+84908619328", gate)
        assertEquals(1, Ladder.distance("908619328", "908619382"))
        assertNotNull(ladder.byNumber(gate, "+84908619382"))
    }

    /** Two digits out is still found. Three is not - the line is drawn at two. */
    @Test
    fun `two digits out is found and three is not`() {
        person("g-minh", "Nguyễn Văn Minh", "+84908619328", gate)
        assertNotNull(ladder.byNumber(gate, "+84908619311"))
        assertNull(ladder.byNumber(gate, "+84908619111"))
    }

    /**
     * **The rule that actually makes this safe.** Two people whose numbers
     * differ by one digit is not a hypothetical - students buy SIMs in
     * batches, so adjacent numbers really do sit in one cabinet. The screen
     * never offers a choice between two people; it asks for the name.
     */
    @Test
    fun `close to two people is close to nobody`() {
        person("g-minh", "Nguyễn Văn Minh", "+84908619328", gate)
        person("g-an", "Trần Thị An", "+84908619329", gate)
        assertNull(ladder.byNumber(gate, "+84908619327"))
    }

    /** A near miss is only ever offered about somebody expecting a parcel here. */
    @Test
    fun `a registered number with no booking is not offered as a near miss`() {
        person("g-minh", "Nguyễn Văn Minh", "+84908619328")
        assertNull(ladder.byNumber(gate, "+84908619327"))
    }

    /** An expired booking holds nothing and offers nothing. */
    @Test
    fun `a booking that ran out is not a near miss`() {
        val brief = Bookings(db, boxes, liveHours = 0)
        val id = Receivers.findOrCreateByGoogle(db, "g-minh", "Nguyễn Văn Minh")
        Receivers.claimPhone(db, id, "+84908619328")
        brief.create(id, gate, "medium")

        assertNull(ladder.byNumber(gate, "+84908619327"))
        // The exact number still finds the account - it just holds no door.
        assertEquals(Confidence.EXACT, assertNotNull(ladder.byNumber(gate, "+84908619328")).confidence)
    }

    // --- Rung D: the name off the label ------------------------------------

    /**
     * The case this rung exists for. `Phong` and `Phúc` mask identically, so
     * a list could not tell them apart - the server holds the full names and
     * can.
     */
    @Test
    fun `the name off the label finds Phong and not Phuc`() {
        val phong = person("g-phong", "Nguyễn Văn Phong", "+84908619328", gate)
        person("g-phuc", "Nguyễn Văn Phúc", "+84912345678", gate)

        assertEquals(phong, assertNotNull(ladder.byName(gate, "Nguyen Van Phong")).receiverId)
        assertEquals(Confidence.NAMED, assertNotNull(ladder.byName(gate, "Nguyen Van Phong")).confidence)
        assertEquals("Nguyễn V. P***", assertNotNull(ladder.byName(gate, "Nguyen Van Phong")).maskedName)
    }

    /** Accents and case come off both sides. A courier cannot type tone marks. */
    @Test
    fun `accents and case do not matter`() {
        val phong = person("g-phong", "Nguyễn Văn Phong", "+84908619328", gate)
        listOf(
            "Nguyễn Văn Phong",
            "Nguyen Van Phong",
            "NGUYEN VAN PHONG",
            "  nguyen   van   phong  ",
        ).forEach { assertEquals(phong, assertNotNull(ladder.byName(gate, it), "on <$it>").receiverId) }
    }

    /**
     * `đ` is a letter, not a `d` with a mark, so stripping combining marks
     * leaves it alone. Without handling it, `Đặng` typed as `Dang` would
     * never match and that family would be sent to ABO every time.
     */
    @Test
    fun `the Vietnamese d matches a plain d`() {
        val dang = person("g-dang", "Đặng Thị Hoa", "+84908619328", gate)
        assertEquals(dang, assertNotNull(ladder.byName(gate, "Dang Thi Hoa")).receiverId)
    }

    /** Two people with one name resolve to neither. Same rule as the numbers. */
    @Test
    fun `a name two people share matches nobody`() {
        person("g-1", "Nguyễn Văn Phong", "+84908619328", gate)
        person("g-2", "Nguyen Van Phong", "+84912345678", gate)
        assertNull(ladder.byName(gate, "Nguyen Van Phong"))
    }

    /** Rung E. Nobody by that name is expecting a parcel here. */
    @Test
    fun `a name nobody here has matches nobody`() {
        person("g-phong", "Nguyễn Văn Phong", "+84908619328", gate)
        assertNull(ladder.byName(gate, "Le Minh Khoi"))
        assertNull(ladder.byName(gate, ""))
        assertNull(ladder.byName(gate, "   "))
    }

    /** Part of a name is not a name. `Phong` alone must not open a door. */
    @Test
    fun `part of a name is not a match`() {
        person("g-phong", "Nguyễn Văn Phong", "+84908619328", gate)
        assertNull(ladder.byName(gate, "Phong"))
        assertNull(ladder.byName(gate, "Nguyen"))
        assertNull(ladder.byName(gate, "Nguyen Van"))
    }

    /** And a name at another cabinet is not a name here. */
    @Test
    fun `a name booked elsewhere does not match here`() {
        person("g-phong", "Nguyễn Văn Phong", "+84908619328", library)
        assertNull(ladder.byName(gate, "Nguyen Van Phong"))
    }

    // --- The distance itself -----------------------------------------------

    /**
     * The country code and the trunk zero are stripped before comparing, so
     * writing the same number two ways is not a typo. Without that,
     * `0908619328` and `+84908619328` differ by three edits and the ladder
     * would refuse the very number it was given.
     */
    @Test
    fun `the same number written three ways is zero edits apart`() {
        assertTrue(Ladder.within(0, "+84908619328", "0908619328"))
        assertTrue(Ladder.within(0, "84908619328", "908619328"))
    }

    @Test
    fun `the distance counts the four kinds of slip`() {
        assertEquals(1, Ladder.distance("908619328", "908619329"), "a substitution")
        assertEquals(1, Ladder.distance("908619328", "90861932"), "a deletion")
        assertEquals(1, Ladder.distance("908619328", "9086193285"), "an insertion")
        assertEquals(1, Ladder.distance("908619328", "908619382"), "a transposition")
        assertEquals(0, Ladder.distance("908619328", "908619328"))
    }
}
