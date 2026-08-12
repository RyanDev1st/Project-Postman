package vn.edu.vgu.smartlocker.parcels

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import vn.edu.vgu.smartlocker.BuildConfig
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The real map, on the Home card.
 *
 * The card used to be a drawn plan: real OSM geometry, but baked at build
 * time, so it showed the same picture of the same campus wherever the phone
 * was. This is the map itself — Google's tiles, and the phone's own position
 * on them.
 *
 * **Lite mode.** The card is 112dp tall and sits in a scrolling screen. A
 * full GL map there is a whole rendering surface that swallows the scroll and
 * costs a frame budget to show something the size of a stamp. Lite mode draws
 * one bitmap, still styled, still carrying the route line and the blue dot,
 * and lets the tap through to [onClick] so the card still hands off to the
 * Maps app for the actual walking directions.
 *
 * **Nothing here is billed.** Displaying a map is free; what costs money is
 * asking Google for a route, and the route is baked (543 m, 6 min 32 s, from
 * `docs/designs/mockup/route.json`). See ADR 0013.
 *
 * Drawn only when [hasMapsKey] — a blank key gives a grey square with an
 * error in the log, which is worse than the plan the card had before.
 */
@Composable
fun LiveMap(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    val ctx = LocalContext.current

    // The blue dot needs permission; the map does not. Refused, the card
    // still shows the route to the gate — it just cannot show where you are.
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

    val camera: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(Route.MID, Route.ZOOM)
    }

    // The app's scheme, not the phone's — the in-app toggle can disagree with
    // the system, and a daylight map under a dark app is the loudest thing on
    // the screen.
    val style = remember(t.dark) {
        MapStyleOptions.loadRawResourceStyle(
            ctx,
            if (t.dark) R.raw.map_dark else R.raw.map_light,
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        properties = MapProperties(
            isMyLocationEnabled = granted,
            mapStyleOptions = style,
        ),
        uiSettings = MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = false,
        ),
        onMapClick = { onClick() },
    ) {
        // Two lines, one fact, the same order the drawn plan used: a wide
        // quiet underlay so the route reads over any tile colour, then the
        // accent on top of it.
        Polyline(points = Route.SHAPE, color = t.ground2.copy(alpha = 0.92f), width = 22f)
        Polyline(points = Route.SHAPE, color = t.accent, width = 9f)

        // The gate. A dot rather than Google's red pin: the pin is the one
        // piece of stock chrome that would land in the middle of the card.
        Circle(
            center = Route.GATE,
            radius = 11.0,
            fillColor = t.accent,
            strokeColor = Color.White,
            strokeWidth = 5f,
        )
    }
}

/** True when this build was given a Maps key. See `local.properties`. */
val hasMapsKey: Boolean get() = BuildConfig.MAPS_API_KEY.isNotBlank()

/**
 * The walk, baked.
 *
 * A real pedestrian route from valhalla1.openstreetmap.de over OSM data
 * (ODbL), fetched 2026-08-11 and kept in `docs/designs/mockup/route.json`.
 * It is baked because asking for it at runtime is the part Google bills for,
 * and because the gate does not move.
 */
private object Route {
    val SHAPE = listOf(
        LatLng(11.105934, 106.614255),
        LatLng(11.105837, 106.613982),
        LatLng(11.105716, 106.613638),
        LatLng(11.105885, 106.613576),
        LatLng(11.106110, 106.613492),
        LatLng(11.106231, 106.613447),
        LatLng(11.106769, 106.613244),
        LatLng(11.108100, 106.612743),
        LatLng(11.107839, 106.612073),
        LatLng(11.107516, 106.611253),
        LatLng(11.107471, 106.611139),
    )

    /** OSM node 12093474313, barrier=gate, open 06:00–23:00. */
    val GATE = LatLng(11.107471, 106.611139)

    /** Centre of the route's own bounds, not of the campus. */
    val MID = LatLng(11.106908, 106.612697)

    /**
     * Chosen so the whole walk fits the card, height first.
     *
     * A Google zoom level puts `256 * 2^z` dp round the earth, so at latitude
     * 11.1° one metre is `256 * 2^z / 39_326_000` dp. The route's bounds are
     * about 350 m across and 270 m tall; the card is roughly 326 dp by 112 dp.
     * Width alone would allow z 17.1, height only 15.96, and the smaller wins
     * or the ends fall off the top and bottom. 15.6 leaves a margin.
     */
    const val ZOOM = 15.6f
}
