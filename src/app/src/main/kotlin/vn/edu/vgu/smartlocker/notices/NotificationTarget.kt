package vn.edu.vgu.smartlocker.notices

import android.content.Intent

/** Safe routing hint carried by a parcel notification. It never authorizes access. */
data class NotificationTarget(
    val kind: String,
    val parcelId: String? = null,
) {
    fun isParcelWaiting(): Boolean = kind == Notices.WAITING && !parcelId.isNullOrBlank()

    companion object {
        const val PARCEL_ID = "vn.edu.vgu.smartlocker.PARCEL_ID"

        fun from(data: Map<String, String>): NotificationTarget? {
            val kind = data["kind"].orEmpty()
            if (kind.isBlank()) return null
            val parcelId = data["parcel_id"]?.takeIf(::validParcelId)
            return NotificationTarget(kind, parcelId)
        }

        fun from(intent: Intent?): NotificationTarget? {
            if (intent == null) return null
            val kind = intent.getStringExtra(Notices.GOES_TO)
                ?: intent.getStringExtra("kind")
                ?: return null
            if (kind.isBlank()) return null
            return NotificationTarget(
                kind = kind,
                parcelId = (intent.getStringExtra(PARCEL_ID)
                    ?: intent.getStringExtra("parcel_id"))
                    ?.takeIf(::validParcelId),
            )
        }

        private fun validParcelId(value: String): Boolean =
            value.length <= 128 && value.all { it.isLetterOrDigit() || it in "-_" }
    }
}
