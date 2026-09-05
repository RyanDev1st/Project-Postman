package vn.edu.vgu.smartlocker.parcels

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.vgu.smartlocker.net.Parcel

class WaitingTargetTest {
    private val now = Instant.parse("2026-09-05T10:00:00Z")
    private val phrases = Phrases({ t, _, _ -> t }, { it.toString() }, { t, _, _ -> t })
    private val window = Window(48, 6)
    private val zone = ZoneId.of("UTC")

    private fun parcel(id: String, hoursAgo: Long) = Parcel(
        id = id, cabinetName = "Gate", boxNumber = id, arrivedAt = now.minusSeconds(hoursAgo * 3600).toString(),
    )

    @Test
    fun `target parcel becomes primary`() {
        val (big, rest) = waiting(listOf(parcel("old", 40), parcel("new", 2)), window, now, zone, phrases, "new")
        assertEquals("new", big?.id)
        assertEquals(listOf("old"), rest.map { it.id })
    }

    @Test
    fun `missing target preserves urgency order`() {
        val (big, rest) = waiting(listOf(parcel("old", 40), parcel("new", 2)), window, now, zone, phrases, "gone")
        assertEquals("old", big?.id)
        assertEquals(listOf("new"), rest.map { it.id })
    }
}
