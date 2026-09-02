package vn.edu.vgu.smartlocker.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The two functions the whole delivery rests on. Task P2-15.
 *
 * Neither had a test in the shipped server, and ADR 0026 changed what they
 * are worth. With no one-time code sent, **[Phone.normalise] is the only
 * automatic thing standing between a typo and a parcel that never arrives**:
 * the receiver types a number into the app, the shipper types one off a label
 * days later on a different keyboard, and nothing else compares them.
 *
 * [maskName] is what the cabinet screen shows a shipper who is about to open
 * a door. It is a public terminal - rule 6 in `architecture.md` - so it must
 * never leak more than a name can be checked against, and it must never
 * throw on a name somebody actually typed.
 */
class PhoneTest {

    private val stored = "+84908619328"

    // --- One number, one stored form ---------------------------------------

    /**
     * The three shapes a person really types, and the one form they all
     * become. This is the whole reason `receivers.phone` can be `UNIQUE`: if
     * the app stored `0908619328` and the cabinet looked up `+84908619328`,
     * every parcel would miss and nothing would say why.
     */
    @Test
    fun `every shape a person types reaches one stored form`() {
        listOf(
            "+84908619328",
            "84908619328",
            "0908619328",
            "908619328",
            "0908 619 328",
            "+84 908 619 328",
            " 0908-619-328 ",
            "(090) 861 9328",
        ).forEach { assertEquals(stored, Phone.normalise(it), "from <$it>") }
    }

    /** All five mobile leads, and each one keeps its own digits. */
    @Test
    fun `all five mobile leads are accepted`() {
        mapOf(
            "0312345678" to "+84312345678",
            "0512345678" to "+84512345678",
            "0712345678" to "+84712345678",
            "0812345678" to "+84812345678",
            "0912345678" to "+84912345678",
        ).forEach { (typed, expected) -> assertEquals(expected, Phone.normalise(typed), "from <$typed>") }
    }

    // --- What is not a mobile ----------------------------------------------

    /**
     * A landline, and the shapes it arrives in. `028` is Ho Chi Minh City and
     * `024` is Hanoi; both are eleven digits with the trunk zero, so they are
     * refused on length before the lead is even looked at.
     */
    @Test
    fun `a landline is refused`() {
        listOf(
            "02838221234",
            "+842838221234",
            "0243 826 3111",
        ).forEach { assertNull(Phone.normalise(it), "accepted <$it>") }
    }

    /**
     * The service ranges, and the leads the 2018 renumbering emptied.
     *
     * `1900` and `1800` are the numbers a company puts on a poster, and `2`
     * is landline. `4` and `6` have never been mobile leads. Each is exactly
     * the right length, so length cannot save us here - only the lead can.
     */
    @Test
    fun `a service number and a dead lead are refused`() {
        listOf(
            "1900561234",
            "1800123456",
            "0212345678",
            "0412345678",
            "0612345678",
            "0012345678",
        ).forEach { assertNull(Phone.normalise(it), "accepted <$it>") }
    }

    /** Too short, too long, and nothing at all. */
    @Test
    fun `a number that is not a number at all is refused`() {
        listOf(
            "",
            "   ",
            "090861932",
            "09086193280",
            "+8490861932",
            "abcdefghij",
            "+1 415 555 0100",
        ).forEach { assertNull(Phone.normalise(it), "accepted <$it>") }
    }

    /**
     * Nine digits with no country code and no trunk zero is taken as
     * national, so `908619328` is the same person as `0908619328`. That is
     * deliberate - people leave the zero off - and it is why nine digits
     * starting with a dead lead must still be refused.
     */
    @Test
    fun `nine digits are read as national, dead leads included`() {
        assertEquals(stored, Phone.normalise("908619328"))
        assertNull(Phone.normalise("208619328"))
    }

    // --- The masked number -------------------------------------------------

    /** Enough to recognise, not enough to dial. */
    @Test
    fun `a masked number keeps the ends and nothing between`() {
        assertEquals("+84*******28", Phone.masked(stored))
        assertEquals("***", Phone.masked("+84"))
        assertEquals("***", Phone.masked(""))
    }

    // --- The masked name ---------------------------------------------------

    /**
     * The shape the contract promises at endpoint 10, on the name the
     * contract uses as its example.
     */
    @Test
    fun `a full Vietnamese name masks to family, initial and one letter`() {
        assertEquals("Nguyễn V. A***", maskName("Nguyễn Văn An"))
        assertEquals("Trần T. M***", maskName("Trần Thị Mai"))
    }

    /** Two middle names keep two initials. Nothing is dropped silently. */
    @Test
    fun `a longer name keeps every part`() {
        assertEquals("Nguyễn T. T. H***", maskName("Nguyễn Thị Thu Hà"))
    }

    /**
     * The names that would crash a naive version, and each of them is a name
     * somebody really has or a field somebody really leaves empty. A cabinet
     * screen that throws here is a shipper standing at a locker with nothing
     * on it.
     */
    @Test
    fun `a short or empty name does not crash`() {
        assertEquals("K***", maskName("Khoi"))
        assertEquals("***", maskName(""))
        assertEquals("***", maskName("    "))
        assertEquals("Lê H***", maskName("Lê  Hà"))
    }

    /**
     * **The collision that killed the masked list**, kept here so nobody
     * puts it back. Three different people, one mask. ADR 0026 replaced the
     * list at the cabinet with the shipper typing the name, because a screen
     * offering these three rows is a coin flip between strangers' parcels.
     */
    @Test
    fun `different people can share one mask`() {
        val one = maskName("Nguyễn Văn Phong")
        assertEquals(one, maskName("Nguyễn Văn Phúc"))
        assertEquals(one, maskName("Nguyễn Văn Phương"))
        assertEquals("Nguyễn V. P***", one)
    }
}
