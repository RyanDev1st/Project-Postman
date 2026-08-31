package vn.edu.vgu.smartlocker.parcels.map

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where the phone is, and therefore which walk to draw.
 *
 * Held here rather than inside [LiveMap] because two things need the same
 * answer and must not disagree: the map draws the line, and the card under it
 * writes the distance. They read one [Walk].
 *
 * **This used to be a single shot.** It asked the phone for its last known
 * position once, when the composable first ran, and never again — so the map
 * was a photograph of wherever the phone had last been told it was, held for
 * the life of the screen. Two faults came out of that and Ryan hit both from
 * home: the cached fix had no age check, so a fix taken on the campus was
 * still "here" days later; and nothing recomputed, so even a correct answer
 * could not become wrong-then-right again as he moved.
 *
 * Now: a fresh cached fix if there is one, then live updates for as long as
 * this is on screen and the app is in front.
 *
 * **What that costs, deliberately bounded.** The listener runs only while this
 * composable is resumed — walk away from the screen or background the app and
 * it is torn down, so a phone in a pocket asks the radio for nothing. The
 * platform will not call back more than every ten seconds or under twenty
 * metres of movement. And a callback is not a route: the routing call happens
 * only when the phone has moved [REROUTE_METRES] from wherever the drawn line
 * starts, or when it crosses the walkable boundary. Standing still with the
 * card open makes no network calls at all after the first.
 */
@Composable
internal fun rememberWalk(granted: Boolean): Walk {
    val context = androidx.compose.ui.platform.LocalContext.current
    var walk by remember { mutableStateOf<Walk>(Walk.Baked) }
    // Where the drawn walk was worked out from. The distance between this and
    // a new fix is what decides whether the fix is worth a routing call.
    var drawnFrom by remember { mutableStateOf<Location?>(null) }
    val scope = rememberCoroutineScope()

    // Resumed, not merely composed. `LaunchedEffect` would keep the listener
    // alive behind another app; this stops with the screen and starts again
    // with it.
    LifecycleResumeEffect(granted, LocalLifecycleOwner.current) {
        val job = if (!granted) {
            walk = Walk.Baked
            drawnFrom = null
            null
        } else {
            scope.launch {
                // The cached fix first, if it is young enough to mean anything.
                // It is what makes the card draw the right thing immediately
                // rather than after the radio answers.
                freshCachedFix(context)?.let { seed ->
                    walk = walkFrom(seed)
                    drawnFrom = seed
                }
                locationUpdates(context).collectLatest { here ->
                    if (worthRedrawing(drawnFrom, here, walk)) {
                        walk = walkFrom(here)
                        drawnFrom = here
                    }
                }
            }
        }
        onPauseOrDispose { job?.cancel() }
    }

    return walk
}

/**
 * Whether a new fix changes the picture enough to redraw it.
 *
 * Three ways it does, and every other fix is dropped:
 *
 * - there is nothing drawn yet;
 * - the phone has moved [REROUTE_METRES] from where the drawn line starts;
 * - it has crossed the walkable boundary, so the *kind* of answer changes —
 *   a route becomes a distance or the other way round. That one matters at
 *   any size of move, because it is the difference between a line and no
 *   line.
 */
private fun worthRedrawing(from: Location?, here: Location, drawn: Walk): Boolean {
    if (from == null) return true
    val moved = crowMetres(from.latitude, from.longitude, here.latitude, here.longitude)
    if (moved >= REROUTE_METRES) return true
    val nowFar = crowMetres(
        here.latitude, here.longitude,
        Route.GATE_POINT.latitude(), Route.GATE_POINT.longitude(),
    ) > WALKABLE_METRES
    // Baked means we had no fix at all, so any fix is worth drawing.
    if (drawn is Walk.Baked) return true
    return nowFar != (drawn is Walk.TooFar)
}

/**
 * The walk from one fix. **Blocking on the routing call, so off the main
 * thread.**
 *
 * Straight-line first, before asking anyone anything. If the gate is half a
 * province away the answer is a distance, and there is no call worth making.
 */
private suspend fun walkFrom(here: Location): Walk {
    val asCrow = crowMetres(
        here.latitude, here.longitude,
        Route.GATE_POINT.latitude(), Route.GATE_POINT.longitude(),
    )
    if (asCrow > WALKABLE_METRES) {
        return Walk.TooFar(
            metres = asCrow.toInt(),
            you = org.maplibre.geojson.Point.fromLngLat(here.longitude, here.latitude),
        )
    }
    // The router did not answer: no network, a refusal, a shape that will not
    // parse. Not knowing the path is not the same as not knowing where you
    // are, and falling back to the baked campus line would claim both.
    return withContext(Dispatchers.IO) {
        routeToGate(here.latitude, here.longitude)
    } ?: Walk.Unrouted(
        metres = asCrow.toInt(),
        you = org.maplibre.geojson.Point.fromLngLat(here.longitude, here.latitude),
    )
}
