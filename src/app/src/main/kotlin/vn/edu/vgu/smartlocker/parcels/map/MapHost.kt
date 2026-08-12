package vn.edu.vgu.smartlocker.parcels.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

/**
 * A MapLibre [MapView] that follows the screen's lifecycle.
 *
 * MapLibre draws with OpenGL on its own surface, so unlike a composable it
 * holds a real resource and has to be told when the screen stops. Miss
 * `onStop` and it keeps a GL context alive behind a screen nobody is looking
 * at; miss `onDestroy` and it leaks the whole view.
 *
 * `MapLibre.getInstance` has to run before the view is constructed. It is
 * idempotent, and it takes no key — that is the entire reason this is not
 * Google's SDK. See ADR 0014.
 */
@Composable
internal fun rememberMapView(): MapView {
    val ctx = LocalContext.current
    val view = remember {
        MapLibre.getInstance(ctx)
        MapView(ctx).apply { onCreate(null) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                Lifecycle.Event.ON_DESTROY -> view.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            view.onStop()
            view.onDestroy()
        }
    }
    return view
}
