package vn.edu.vgu.smartlocker.parcels.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Where the phone is, as it changes.
 *
 * **A fix has an age, and the old code never read it.** It asked
 * `getLastKnownLocation` and used whatever came back, so the fix from the last
 * time the phone sat on the campus was "where you are" for as long as nothing
 * replaced it — which is days, if you never open a maps app. Ryan, testing
 * from home: *"showing stale plotting to the cabinet as opposed to reflecting
 * the real GPS data"*. The map was not lying about the route. It was telling
 * the truth about a place he had left.
 *
 * So a cached fix is a **starting guess with a shelf life**, and the phone is
 * asked for a live one either way.
 *
 * **The fused provider does the asking.** It is the standard Android location
 * API — one answer merged from GPS, wifi, cell and the phone's own motion
 * sensors, with the battery cost stated as a [Priority] the platform honours
 * and fixes batched in hardware rather than waking the app for each one. Play
 * Services is already here by way of ML Kit's barcode scanner, argued in ADR
 * 0018, so this is a library and not a new vendor.
 *
 * Where Play Services is missing — a de-Googled phone, most emulators — every
 * function here falls back to the platform's own [LocationManager]. The
 * fallback is worse and it is not a stub: it asks the same providers with the
 * same throttles and returns the same shapes.
 */

/**
 * How old a cached fix may be and still be called "here".
 *
 * Two minutes. Long enough that opening the app twice in a row does not wait
 * for the radio, short enough that it cannot survive a journey — at walking
 * pace two minutes is about 150 m, which is inside the accuracy this card
 * draws at.
 */
internal const val FIX_FRESH_MS = 2 * 60 * 1000L

/**
 * How far the phone must move before the route is asked for again.
 *
 * 120 m. GPS jitters tens of metres while a phone lies still, and every
 * re-route is a call to a public service that costs somebody else money. This
 * is the difference between "the map keeps up with you" and "the map hammers
 * a router because a fix wobbled".
 */
internal const val REROUTE_METRES = 120.0

/**
 * The slowest useful update, and the smallest move worth waking us for.
 *
 * Ten seconds is the *fastest* the app will accept, not a poll: with
 * `PRIORITY_BALANCED_POWER_ACCURACY` the platform is free to answer more
 * slowly and usually does, reusing fixes other apps have already paid for.
 * That priority is wifi-and-cell accuracy, roughly a hundred metres, which is
 * the right trade for a 112 dp card — asking for `HIGH_ACCURACY` here would
 * light the GPS chip to draw a line a finger covers.
 */
private const val MIN_INTERVAL_MS = 10_000L
private const val MIN_MOVE_M = 20f

/** Whether either location permission is granted right now. */
internal fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/** Whether the fused provider can actually be reached on this device. */
private fun fusedAvailable(context: Context): Boolean =
    GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

/**
 * The freshest cached fix, or null if there is none young enough.
 *
 * The age gate is the whole point: an old fix is not a bad fix, it is a fix
 * about somewhere else, and there is no averaging that repairs that.
 */
@SuppressLint("MissingPermission")
internal suspend fun freshCachedFix(
    context: Context,
    now: Long = System.currentTimeMillis(),
): Location? {
    if (!hasLocationPermission(context)) return null
    val newest = if (fusedAvailable(context)) {
        try {
            lastFused(context)
        } catch (e: SecurityException) {
            null
        }
    } else {
        lastFromManager(context)
    } ?: return null
    return if (now - newest.time <= FIX_FRESH_MS) newest else null
}

@SuppressLint("MissingPermission")
private suspend fun lastFused(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

@SuppressLint("MissingPermission")
private fun lastFromManager(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return providers(context).mapNotNull { p ->
        try {
            lm.getLastKnownLocation(p)
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            // The provider is not on this device.
            null
        }
    }.maxByOrNull { it.time }
}

/**
 * Live fixes, for as long as the flow is collected.
 *
 * **Every update costs battery, so this stops the moment nothing is
 * listening.** `awaitClose` removes the callback, and the caller ties
 * collection to the map being on screen and the app being in the foreground —
 * so a phone in a pocket with the app behind another one is asking the radio
 * for nothing at all. The work a callback triggers is throttled again, much
 * harder, by [REROUTE_METRES]: a fix is cheap, a routing call is not.
 */
@SuppressLint("MissingPermission")
internal fun locationUpdates(context: Context): Flow<Location> = callbackFlow {
    if (!hasLocationPermission(context)) {
        close()
        return@callbackFlow
    }

    val stop: () -> Unit = if (fusedAvailable(context)) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, MIN_INTERVAL_MS,
        )
            .setMinUpdateDistanceMeters(MIN_MOVE_M)
            // The platform may not answer faster than this even if a fix is
            // going spare, which caps how often the app is woken.
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            // A first answer worth drawing, rather than whatever is cached.
            // `freshCachedFix` has already had its turn by the time we get
            // here, and it declined.
            .setWaitForAccurateLocation(false)
            .build()
        try {
            client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
        } catch (e: SecurityException) {
            close()
        }
        ({ client.removeLocationUpdates(callback) })
    } else {
        managerUpdates(context) { trySend(it) } ?: run {
            close()
            return@callbackFlow
        }
    }

    awaitClose {
        try {
            stop()
        } catch (e: SecurityException) {
            // Permission was revoked while we were listening. Nothing to undo.
        }
    }
}

/**
 * The fallback listener. Returns how to stop it, or null if no provider on
 * this device would take the request.
 */
@SuppressLint("MissingPermission")
private fun managerUpdates(context: Context, onFix: (Location) -> Unit): (() -> Unit)? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val listener = android.location.LocationListener { onFix(it) }
    val asked = providers(context).count { p ->
        try {
            lm.requestLocationUpdates(
                p, MIN_INTERVAL_MS, MIN_MOVE_M, listener, android.os.Looper.getMainLooper(),
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    return if (asked == 0) null else ({ lm.removeUpdates(listener) })
}

/**
 * Which providers the fallback asks, cheapest first.
 *
 * Network only, without the fine permission — asking for GPS with coarse
 * permission throws, and the card would rather draw a rough position than
 * none.
 */
private fun providers(context: Context): List<String> = buildList {
    add(LocationManager.NETWORK_PROVIDER)
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        add(LocationManager.GPS_PROVIDER)
    }
}

/** Great-circle metres. The rule [Route] measures its own line with. */
internal fun crowMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}
