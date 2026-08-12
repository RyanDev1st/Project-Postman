package vn.edu.vgu.smartlocker.parcels.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
fun LiveMap(
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onClick: () -> Unit = {},
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
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
                    // to switch off.
                    isAttributionEnabled = true
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
                if (!wiring.framed) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(Route.BOUNDS, edgePad),
                    )
                    wiring.framed = true
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
                    style.addRoute(accent, underlay)
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

    /** The camera is placed once. Setting it on every update would drag the
     * view back to the route the moment you panned away from it. */
    var framed = false
}

/**
 * The walk: a wide quiet underlay so it reads over any tile colour, then the
 * accent on top. The same two-line order the drawn plan used, and the gate as
 * a dot at the end of it.
 */
private fun Style.addRoute(accent: Int, underlay: Int) {
    addSource(GeoJsonSource(SRC_LINE, LineString.fromLngLats(Route.LINE)))
    addSource(GeoJsonSource(SRC_GATE, Route.GATE_POINT))

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

/**
 * The walk, baked.
 *
 * A real pedestrian route from valhalla1.openstreetmap.de over OSM data
 * (ODbL), fetched 2026-08-11 and kept in `docs/designs/mockup/route.json`.
 * 543 m, 6 min 32 s. Baked because the gate does not move, and because a
 * routing call is the one part of a map that nobody serves for free.
 */
private object Route {
    /** GeoJSON is longitude first. Reading route.json's lat,lon pairs in
     * order here would put the walk in the South China Sea. */
    val LINE: List<Point> = listOf(
        11.105934 to 106.614255,
        11.105837 to 106.613982,
        11.105716 to 106.613638,
        11.105885 to 106.613576,
        11.106110 to 106.613492,
        11.106231 to 106.613447,
        11.106769 to 106.613244,
        11.108100 to 106.612743,
        11.107839 to 106.612073,
        11.107516 to 106.611253,
        11.107471 to 106.611139,
    ).map { (lat, lon) -> Point.fromLngLat(lon, lat) }

    /** OSM node 12093474313, barrier=gate, open 06:00–23:00. */
    val GATE_POINT: Point = Point.fromLngLat(106.611139, 11.107471)

    /**
     * What the camera is asked to fit.
     *
     * Built from the walk itself rather than written down, so correcting a
     * coordinate cannot leave the framing pointing at the old one. There was a
     * hand-computed centre and zoom here before, worked out for a card assumed
     * to be 326dp by 112dp; the map knows its own size and the bounds let it
     * use that.
     */
    val BOUNDS: LatLngBounds = LatLngBounds.Builder()
        .includes(LINE.map { LatLng(it.latitude(), it.longitude()) })
        .build()
}
