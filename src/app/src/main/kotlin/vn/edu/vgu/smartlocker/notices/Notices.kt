package vn.edu.vgu.smartlocker.notices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import vn.edu.vgu.smartlocker.MainActivity
import vn.edu.vgu.smartlocker.R

/**
 * Drawing the notice that a parcel arrived. Tasks P4-02, P4-03 and P4-05.
 *
 * ## Two channels, not one
 *
 * Android lets a person switch off one kind of notification and keep another,
 * and these two are genuinely different: one says a parcel is waiting, the
 * other says the number on the label was wrong. Somebody who silences the
 * first because they collect parcels daily must still be told the second,
 * which happens once and cannot be discovered any other way.
 *
 * ## What is never in one
 *
 * The server decides this and it is enforced there, with tests - see
 * `notices/Notices.kt` on the server. It bears repeating here because this is
 * the code that draws whatever arrives: **no pickup code, no full name**. A
 * notification is readable on a locked screen by anybody holding the phone.
 */
object Notices {

    const val WAITING = "parcel_waiting"
    const val MISMATCH = "number_mismatch"

    /**
     * Make the channels. Safe to call as often as you like.
     *
     * Called at start-up rather than at first notice, because a channel that
     * does not exist yet means the first notification of all is dropped on
     * Android 8 and later - and the first one is the one that matters.
     */
    fun prepare(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                WAITING,
                context.getString(R.string.channel_waiting),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_waiting_why) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                MISMATCH,
                context.getString(R.string.channel_mismatch),
                // Also high. It is rare, it is about a mistake, and it is the
                // only warning the person will ever get about it.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_mismatch_why) }
        )
    }

    /**
     * Put one notice in the tray.
     *
     * [kind] chooses the channel and the tap target. An unknown kind still
     * draws, on the parcel channel: a server that learns to send something
     * new must not produce a silent app.
     */
    fun show(context: Context, target: NotificationTarget, title: String, body: String) {
        prepare(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(GOES_TO, target.kind)
            target.parcelId?.let { putExtra(NotificationTarget.PARCEL_ID, it) }
        }

        val requestCode = (target.kind + ":" + target.parcelId.orEmpty()).hashCode()
        val tap = PendingIntent.getActivity(
            context,
            requestCode,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channel = if (target.kind == MISMATCH) MISMATCH else WAITING
        val notice = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notice)
            .setContentTitle(title)
            .setContentText(body)
            // The mismatch sentence names two phone numbers and does not fit
            // on one line. Without this it is cut off mid-number, which makes
            // it useless for the one thing it exists to do.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            // Throws when the person has refused notifications, which is
            // their right and not an error. There is nothing to recover: the
            // parcel list in the app still shows the parcel.
            NotificationManagerCompat.from(context).notify(requestCode, notice)
        }
    }

    /** The extra that tells [MainActivity] which screen a tap meant. */
    const val GOES_TO = "vn.edu.vgu.smartlocker.GOES_TO"
}
