package vn.edu.vgu.smartlocker.parcels.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.Gravity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The map on the Home card. Real tiles, and the phone's own position on them.
 *
 * The card used to be a plan drawn at build time. The geometry was real, but
 * it was baked, so it showed the same picture of the same campus wherever the
 * phone was and the "you are here" dot was a fixed coordinate.
 *
 * Tiles come from **OpenFreeMap** and are drawn by MapLibre. No key, no
 * account, no card on file, and its own FAQ puts no limit on requests. Google
 * wanted a ₫630,000 authorisation before it would serve a map whose native
 * SKU it charges nothing for. See ADR 0014.
 *
 * Attribution is required and MapLibre draws it — that is what
 * `isAttributionEnabled` is, and it stays on.
 *
 * Gestures are off. The card is 112dp tall inside a screen that scrolls, and
 * a map that pans under the finger eats the scroll. A tap hands off to the
 * Maps app for the walk, which is what [onClick] does.
 */
@Composable
internal fun LiveMap(
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onClick: () -> Unit = {},
    /** Which walk to draw. The caller works it out because the caption under
     * the map has to describe the same one — see [rememberWalk]. */
    walk: Walk = Walk.Baked,
) {
    val t = LocalLockerTokens.current
    val ctx = LocalContext.current
    val view = rememberMapView()
    val wiring = remember { MapWiring() }
    val click by rememberUpdatedState(onClick)

    // The blue dot needs permission; the map does not. Refused, the card
    // still shows the walk to the gate — it just cannot show where you are.
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // Both are asked for together, which is the only way the system offers a
    // choice: the dialog shows "Precise" and "Approximate" side by side, and
    // asking for FINE alone removes the second and the person's say with it.
    // Approximate still gets a blue dot; it does not get a route, because a
    // fix that can be two kilometres out cannot start a walk of five hundred
    // metres. See ADR 0016.
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = it[Manifest.permission.ACCESS_FINE_LOCATION] == true }
    LaunchedEffect(Unit) {
        if (!granted) {
            ask.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    // The app's scheme, not the phone's: the in-app toggle can disagree with
    // the system, and a daylight map under a dark app is the loudest thing on
    // the screen. Positron and dark are OpenFreeMap's own two.
    val styleUri = if (t.dark) "$STYLES/dark" else "$STYLES/positron"
    val accent = t.accent.toArgb()
    val underlay = t.ground2.toArgb()

    // Room round the walk so neither end sits on the edge. The card is small
    // and can spare little; opened out there is room to breathe.
    val edgePad = with(LocalDensity.current) { (if (interactive) 56.dp else 16.dp).roundToPx() }
    val hairPad = with(LocalDensity.current) { 6.dp.roundToPx() }
    val quietInk = LocalLockerTokens.current.ink3.toArgb()

    AndroidView(
        factory = { view },
        modifier = modifier,
        update = { v ->
            v.getMapAsync { map ->
                map.uiSettings.apply {
                    // On the card the map is a picture: a map that pans under
                    // the finger eats the scroll of the screen it sits in.
                    // Opened out it is a map, and it moves.
                    isScrollGesturesEnabled = interactive
                    isZoomGesturesEnabled = interactive
                    isDoubleTapGesturesEnabled = interactive
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                    isLogoEnabled = false
                    // Required by OpenFreeMap and by OSM's licence. Not ours
                    // to switch off - but ours to place and to tint.
                    //
                    // Left alone it drew a teal circled i floating in the
                    // middle of the card, in a colour that appears nowhere
                    // else in the product, and it read as a bug. MapLibre's
                    // default margins are written for a full-screen map; on
                    // a 112dp card they put the mark in the picture. Corner,
                    // small margin, and the same ink as a caption.
                    isAttributionEnabled = true
                    setAttributionGravity(Gravity.BOTTOM or Gravity.END)
                    setAttributionMargins(0, 0, hairPad, hairPad)
                    setAttributionTintColor(quietInk)
                }
                // Frame by the route's own bounds, not by a zoom number.
                //
                // The zoom was worked out on paper for a card assumed to be
                // 326dp by 112dp. On any other size - a wider phone, the
                // opened-out sheet, a font scale that changes the row height -
                // it is the wrong number, and the walk sits off the picture
                // with the gate cut off the end. Bounds ask the map what size
                // it actually is, which is also the rule for the cabinet
                // screen: never assume a size.
                // Re-framed when the walk changes, not once and for ever: the
                // route arrives a moment after the map does, and a camera set
                // only on the first pass would stay on the baked line while a
                // different line was drawn under it.
                if (wiring.framedFor != walk) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsOf(walk), edgePad),
                    )
                    wiring.framedFor = walk
                }

                // Only the card hands the tap on. Opened out, a tap is how
                // you use the map.
                if (!interactive && !wiring.clickBound) {
                    map.addOnMapClickListener { click(); true }
                    wiring.clickBound = true
                }

                // setStyle replaces every source and layer, so the route has
                // to be added again inside the callback each time the scheme
                // changes. It is not a leak; it is a new style object.
                map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                    style.addRoute(walk, accent, underlay)
                    if (granted) map.showWhereYouAre(ctx, style)
                }
            }
        },
    )
}

/** Whether the one-time listener has been attached. `update` runs again on
 * every scheme change, and MapLibre would stack a second listener each time. */
private class MapWiring {
    var clickBound = false

    /**
     * Which walk the camera was placed for, or null before the first one.
     *
     * A boolean until the route stopped being fixed. The live route arrives a
     * moment after the map does, so a camera placed once and never again sat
     * on the baked line with a different line drawn under it. Holding the walk
     * itself re-frames exactly when the thing being framed changes, and still
     * leaves a pan alone.
     */
    var framedFor: Walk? = null
}

/**
 * What the camera should fit.
 *
 * A real route is framed on itself. Too far to walk, and it frames **you and
 * the cabinet together** — that is the whole fact at that distance, and the
 * gap on screen says it better than the caption does.
 *
 * That last case used to frame the gate alone, on the reasoning that a box
 * holding both would be "a view of a province with two invisible dots in it".
 * The dots are not invisible: both markers draw at a fixed size on the
 * screen. What the old framing actually produced was a street-level view of a
 * campus the person was forty kilometres from, with a walking line across it
 * — the app looking like it thought they were standing at the gate.
 *
 * With no fix at all there is still nothing else to show, so that keeps the
 * baked approach.
 */
private fun boundsOf(walk: Walk): LatLngBounds = when (walk) {
    is Walk.FromYou -> LatLngBounds.Builder()
        .includes(walk.line.map { LatLng(it.latitude(), it.longitude()) })
        .build()
    is Walk.TooFar -> LatLngBounds.Builder()
        .includes(
            listOf(
                LatLng(walk.you.latitude(), walk.you.longitude()),
                LatLng(Route.GATE_POINT.latitude(), Route.GATE_POINT.longitude()),
            )
        )
        .build()
    Walk.Baked -> Route.BOUNDS
}

/**
 * The walk: a wide quiet underlay so it reads over any tile colour, then the
 * accent on top. The same two-line order the drawn plan used, and the gate as
 * a dot at the end of it.
 *
 * **Too far to walk draws no line.** Only the gate dot goes down, and the
 * camera puts the person's own dot in frame with it. A line here would be the
 * baked campus approach, forty kilometres from where the phone is — see
 * [Walk.TooFar].
 */
private fun Style.addRoute(walk: Walk, accent: Int, underlay: Int) {
    val line = when (walk) {
        is Walk.FromYou -> walk.line
        Walk.Baked -> Route.LINE
        is Walk.TooFar -> null
    }

    if (line != null) {
        addSource(GeoJsonSource(SRC_LINE, LineString.fromLngLats(line)))
        addLayer(
            LineLayer(LAYER_UNDER, SRC_LINE).withProperties(
                PropertyFactory.lineColor(underlay),
                PropertyFactory.lineWidth(7.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
        )
        addLayer(
            LineLayer(LAYER_LINE, SRC_LINE).withProperties(
                PropertyFactory.lineColor(accent),
                PropertyFactory.lineWidth(3.4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
        )
    }

    // The cabinet, always. It is the one thing the card exists to point at,
    // and it is the only mark on the map when the walk is not a walk.
    addSource(GeoJsonSource(SRC_GATE, Route.GATE_POINT))
    addLayer(
        CircleLayer(LAYER_GATE, SRC_GATE).withProperties(
            PropertyFactory.circleRadius(5.5f),
            PropertyFactory.circleColor(accent),
            PropertyFactory.circleStrokeWidth(2.2f),
            PropertyFactory.circleStrokeColor(underlay),
        )
    )
}

/** The blue dot. Called only with permission granted, which is what the
 * lint suppression is standing on. */
@SuppressLint("MissingPermission")
private fun MapLibreMap.showWhereYouAre(
    ctx: android.content.Context,
    style: Style,
) {
    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(ctx, style).build()
    )
    locationComponent.isLocationComponentEnabled = true
    // The camera stays on the walk. Following the dot would swing the card
    // away from the thing it is there to show.
    locationComponent.cameraMode = CameraMode.NONE
    locationComponent.renderMode = RenderMode.NORMAL
}

private const val STYLES = "https://tiles.openfreemap.org/styles"
private const val SRC_LINE = "route-line"
private const val SRC_GATE = "route-gate"
private const val LAYER_UNDER = "route-underlay"
private const val LAYER_LINE = "route-accent"
private const val LAYER_GATE = "route-gate-dot"
