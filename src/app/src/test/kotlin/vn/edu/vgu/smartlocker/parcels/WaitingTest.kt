package vn.edu.vgu.smartlocker.parcels

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vgu.smartlocker.net.Event
import vn.edu.vgu.smartlocker.net.Parcel

/**
 * What the server sends, turned into what the ticket draws.
 *
 * The two numbers in the design's own samples are the fixed points: 6 hours
 * left is drawn at 0.12, and 31 at 0.65. Both are `hours / 48`, so the bar
 * shows what is **left**. If somebody reads it the other way round later, the
 * first two tests here fail.
 */
class WaitingTest {

    private val vn = ZoneId.of("Asia/Ho_Chi_Minh")
    private val window = Window(hours = 48, soonHours = 12)

    /** Vietnam is UTC+7, so 01:14Z is 08:14 on the ticket. */
    private val dropped = "2026-08-15T01:14:00Z"
    private val now = Instant.parse("2026-08-16T00:00:00Z")

    private val say = Phrases(
        at = { time, day, date -> "$time ${day.name.lowercase()} $date" },
        left = { hours -> "$hours hours left" },
        dropped = { time, day, date -> "dropped $time ${day.name.lowercase()} $date" },
    )

    private fun parcel(
        box: String = "04",
        arrived: String = dropped,
        cabinet: String = "Back Gate",
    ) = Parcel(id = "p-$box", cabinetName = cabinet, boxNumber = box, arrivedAt = arrived)

    // --- the two numbers the design fixes -----------------------------------

    @Test
    fun `six hours left fills the bar to the design's 0_12`() {
        // Arrived 42 hours before now, in a 48 hour window.
        val at = now.minusSeconds(42 * 3600).toString()
        val claim = claim(parcel(arrived = at), window, now, vn, say)

        assertEquals("6 hours left", claim.left)
        assertEquals(0.125f, claim.pct, 0.001f)
    }

    @Test
    fun `thirty-one hours left fills the bar to the design's 0_65`() {
        val at = now.minusSeconds(17 * 3600).toString()
        val claim = claim(parcel(arrived = at), window, now, vn, say)

        assertEquals("31 hours left", claim.left)
        assertEquals(0.646f, claim.pct, 0.001f)
    }

    @Test
    fun `six hours is urgent and thirty-one is not`() {
        val soon = claim(parcel(arrived = now.minusSeconds(42 * 3600).toString()), window, now, vn, say)
        val later = claim(parcel(arrived = now.minusSeconds(17 * 3600).toString()), window, now, vn, say)

        assertTrue(soon.soon)
        assertTrue(!later.soon)
    }

    // --- the clock ----------------------------------------------------------

    @Test
    fun `times are drawn in Vietnam, not in UTC`() {
        val claim = claim(parcel(), window, now, vn, say)
        // 01:14Z is 08:14 in Ho Chi Minh City, which is what the shipper saw.
        assertEquals("08:14", claim.dropped)
    }

    @Test
    fun `a part hour rounds up, so it never says one hour with ninety minutes left`() {
        val at = now.minusSeconds(46 * 3600 + 30 * 60).toString()
        assertEquals("2 hours left", claim(parcel(arrived = at), window, now, vn, say).left)
    }

    @Test
    fun `a window already gone reads zero and an empty bar, never a negative`() {
        val at = now.minusSeconds(60 * 3600).toString()
        val claim = claim(parcel(arrived = at), window, now, vn, say)

        assertEquals("0 hours left", claim.left)
        assertEquals(0f, claim.pct, 0.001f)
        assertTrue(claim.soon)
    }

    // --- which parcel goes on the big ticket ---------------------------------

    @Test
    fun `the most urgent parcel gets the big ticket, whatever order the server sent`() {
        // The server sends newest first. The oldest is the one running out.
        val newest = parcel(box = "09", arrived = now.minusSeconds(2 * 3600).toString())
        val oldest = parcel(box = "04", arrived = now.minusSeconds(40 * 3600).toString())

        val (big, rest) = waiting(listOf(newest, oldest), window, now, vn, say)

        assertEquals("04", big?.box)
        assertEquals(listOf("09"), rest.map { it.box })
    }

    @Test
    fun `no parcels means no ticket and nothing also-waiting`() {
        val (big, rest) = waiting(emptyList(), window, now, vn, say)

        assertNull(big)
        assertEquals(emptyList<SmallClaim>(), rest)
    }

    // --- a server that says something we cannot read --------------------------

    @Test
    fun `an unreadable arrival time leaves the times blank and keeps the box number`() {
        // Lenient, like every reader in Models.kt. The box number is the part
        // that gets somebody to their parcel, and it is still right.
        val claim = claim(parcel(arrived = "whenever"), window, now, vn, say)

        assertEquals("04", claim.box)
        assertEquals("Back Gate", claim.cabinet)
        assertEquals("", claim.dropped)
        assertEquals("", claim.collectBy)
        assertEquals("", claim.left)
        assertEquals(0f, claim.pct, 0.001f)
    }

    // --- the ledger -----------------------------------------------------------

    @Test
    fun `the collected ledger lists collections only, not drops`() {
        val events = listOf(
            Event(at = dropped, boxNumber = "02", action = "collect-opened", cabinetName = "Library"),
            Event(at = dropped, boxNumber = "17", action = "drop-opened", cabinetName = "Back Gate"),
            Event(at = dropped, boxNumber = "09", action = "collect-by-code", cabinetName = "Library"),
        )

        val rows = ledger(events, vn)

        assertEquals(listOf("02", "09"), rows.map { it.box })
        assertEquals(listOf("Library", "Library"), rows.map { it.cabinet })
    }

    @Test
    fun `a ledger row with a time nobody can read is dropped, not drawn blank`() {
        val events = listOf(
            Event(at = "", boxNumber = "02", action = "collect-opened", cabinetName = "Library"),
        )

        assertEquals(emptyList<LedgerEntry>(), ledger(events, vn))
    }
}
