package vn.edu.vgu.smartlocker.notices

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.vgu.smartlocker.net.Backend

/**
 * Where a notice arrives. Tasks P4-02 and P4-03.
 *
 * Android hands a message to this service whether the app is open, in the
 * background, or was never started since the last reboot - which is the whole
 * point, because a parcel arrives while somebody is in a lecture.
 *
 * ## Why every message is a data message
 *
 * The server sends `notification` **and** `data`. Android delivers a message
 * carrying `notification` straight to the tray on its own when the app is in
 * the background, and only calls this service when the app is open. That
 * would mean two different-looking notices for the same event and no tap
 * target on one of them.
 *
 * Rather than fight that, both are sent and this draws its own from the data
 * half whenever it is called. The tray copy Android draws by itself carries
 * the same title and body, so the two are indistinguishable to a person.
 */
class LockerMessaging : FirebaseMessagingService() {

    /**
     * A new address for this phone.
     *
     * Called when Firebase issues a token: first run, after the app data is
     * cleared, after a restore onto a new handset. **Every one of those is a
     * phone whose old address stops working**, so this has to reach the
     * server or the person silently stops being told about parcels.
     */
    override fun onNewToken(token: String) {
        Log.i(TAG, "a new device address was issued")
        CoroutineScope(Dispatchers.IO).launch {
            // Fails quietly when nobody is signed in - the address is stored
            // against an account, and there is no account yet. `Devices.sync`
            // sends it again after the next sign-in.
            runCatching { Backend(applicationContext).registerDevice(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val target = NotificationTarget.from(message.data)
        val kind = target?.kind.orEmpty()

        // The words come from the `notification` half when it is there, and
        // the data half is what says which kind it is. Neither is trusted to
        // exist: a message with no words draws nothing rather than an empty
        // notification, which looks like a broken app.
        val title = message.notification?.title.orEmpty()
        val body = message.notification?.body.orEmpty()
        if (title.isBlank() && body.isBlank()) {
            Log.w(TAG, "a message arrived with nothing to draw: $kind")
            return
        }

        Notices.show(applicationContext, target ?: NotificationTarget(kind), title, body)
    }

    private companion object {
        const val TAG = "LockerMessaging"
    }
}

/**
 * Telling the server where to send this phone's notices.
 *
 * Called after every sign-in, not only after the first. The address is stored
 * against an account, and one phone can be signed in to a different account
 * than it was yesterday - a shared handset, or somebody testing. Registering
 * only once would leave the notice going to whoever signed in first.
 */
object Devices {

    /**
     * Ask Firebase for this phone's address and send it on.
     *
     * Quiet on failure by design. A phone with no Play Services, or no
     * network at that moment, still has a working app: the parcel list is
     * fetched when the app opens, so a missing notice is a slower path to the
     * same place rather than a broken one.
     */
    suspend fun sync(backend: Backend) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            backend.registerDevice(token)
            Log.i("Devices", "this phone's address was sent to the server")
        }.onFailure {
            Log.w("Devices", "could not register for notices: ${it.message}")
        }
    }
}

/**
 * A Play Services task, awaited.
 *
 * Written here rather than pulling in `kotlinx-coroutines-play-services`,
 * which is a whole artifact for this one function.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { block ->
        addOnCompleteListener { done ->
            val problem = done.exception
            when {
                problem != null -> block.resumeWith(Result.failure(problem))
                done.isCanceled -> block.cancel()
                else -> block.resumeWith(Result.success(done.result))
            }
        }
    }
