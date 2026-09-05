package vn.edu.vgu.smartlocker.notices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTargetTest {
    @Test
    fun `server data preserves a valid parcel selector`() {
        val target = NotificationTarget.from(
            mapOf("kind" to Notices.WAITING, "parcel_id" to "parcel-42"),
        )

        assertEquals(Notices.WAITING, target?.kind)
        assertEquals("parcel-42", target?.parcelId)
        assertTrue(target?.isParcelWaiting() == true)
    }

    @Test
    fun `blank kind is not a route`() {
        assertNull(NotificationTarget.from(mapOf("kind" to " ", "parcel_id" to "parcel-42")))
    }

    @Test
    fun `invalid parcel selector is discarded`() {
        val target = NotificationTarget.from(
            mapOf("kind" to Notices.WAITING, "parcel_id" to "../other-parcel"),
        )

        assertEquals(Notices.WAITING, target?.kind)
        assertNull(target?.parcelId)
        assertTrue(target?.isParcelWaiting() == false)
    }

    @Test
    fun `server data preserves the route used by a notification tap`() {
        val target = NotificationTarget.from(
            mapOf("kind" to Notices.WAITING, "parcel_id" to "parcel_42"),
        )

        assertEquals(
            NotificationTarget(Notices.WAITING, "parcel_42"),
            target,
        )
    }
}
