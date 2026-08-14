package vn.edu.vgu.smartlocker.parcels.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where the phone is, and therefore which walk to draw.
 *
 * Held here rather than inside [LiveMap] because two things need the same
 * answer and must not disagree: the map draws the line, and the card under it
 * writes the distance. They read one [Walk].
 */
@Composable
internal fun rememberWalk(granted: Boolean): Walk {
    val context = androidx.compose.ui.platform.LocalContext.current
    var walk by remember { mutableStateOf<Walk>(Walk.Baked) }

    LaunchedEffect(granted) {
        if (!granted) {
            walk = Walk.Baked
            return@LaunchedEffect
        }
        val here = withContext(Dispatchers.IO) { lastKnown(context) }
        if (here == null) {
            walk = Walk.Baked
            return@LaunchedEffect
        }

        // Straight-line first, before asking anyone anything. If the gate is
        // half a province away the answer is a distance, and there is no call
        // worth making.
        val asCrow = crowMetres(
            here.latitude, here.longitude,
            Route.GATE_POINT.latitude(), Route.GATE_POINT.longitude(),
        )
        walk = if (asCrow > WALKABLE_METRES) {
            Walk.TooFar(asCrow.toInt())
        } else {
            withContext(Dispatchers.IO) {
                routeToGate(here.latitude, here.longitude)
            } ?: Walk.Baked
        }
    }

    return walk
}

/**
 * The freshest fix the phone already has.
 *
 * Last known rather than a live one: this is a 112dp card on a screen a person
 * glances at, and turning on the GPS to draw it would cost battery for an
 * accuracy the card cannot show. If the phone has no fix at all the answer is
 * null and the baked line stands.
 *
 * Both providers are asked and the newer wins — GPS is more accurate and often
 * staler indoors, network is rougher and usually current, and neither is
 * reliably the better one.
 */
@SuppressLint("MissingPermission")
private fun lastKnown(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val providers = buildList {
        if (fine) add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }
    return providers.mapNotNull { p ->
        try {
            lm.getLastKnownLocation(p)
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            // The provider does not exist on this device. An emulator with no
            // GPS is the common one.
            null
        }
    }.maxByOrNull { it.time }
}

/** Great-circle metres — the same rule [Route] measures its own line with. */
private fun crowMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}
