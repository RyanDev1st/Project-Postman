package vn.edu.vgu.smartlocker.pickup

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.OnDoorLight
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * Screen 5. A door opened.
 *
 * **The one screen in the app that is drenched in colour.** Every other
 * screen is quiet steel; this one fills with the door light. Somebody
 * glancing at the phone from three metres down a corridor, or holding it at
 * arm's length while reaching for a handle, can tell this screen from the
 * others without reading a word. That is the entire reason the amber exists
 * and the reason it is spent nowhere else.
 *
 * It is also the same amber in the dark scheme. A door being open is a fact
 * about the world, not about the phone's settings.
 *
 * **Shown only when the server confirmed the door opened.** If the answer
 * was unclear, the app says it was unclear - task P5-06. A false success
 * sends somebody away from their own parcel, which is the worst thing this
 * app can do.
 */
@Composable
fun OpenedScreen(box: String = "04", onDone: () -> Unit) {
    LightBarsOnAmber()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorLight)
            // The light falls off toward the bottom, so the field reads as a
            // lit surface rather than a flat fill.
            .background(
                Brush.verticalGradient(
                    0f to androidx.compose.ui.graphics.Color.White.copy(alpha = 0.20f),
                    0.55f to androidx.compose.ui.graphics.Color.Transparent,
                    1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f),
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = box,
                    style = MaterialTheme.typography.displayLarge,
                    color = OnDoorLight,
                )
                Text(
                    text = stringResource(R.string.is_open),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnDoorLight,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.take_and_close),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnDoorLight.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = onDone,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnDoorLight,
                    contentColor = DoorLight,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
            ) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Dark clock and battery icons while this screen is up, and the theme's own
 * back when it leaves.
 *
 * Every other screen lets the system bars follow the light or dark scheme.
 * This one does not have a scheme - it is always amber - so in dark mode the
 * phone drew white icons on a bright yellow ground and the clock became hard
 * to read. The bar has to follow what is actually behind it.
 */
@Composable
private fun LightBarsOnAmber() {
    val view = LocalView.current
    if (view.isInEditMode) return

    val wasDark = isSystemInDarkTheme()
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val bars = WindowCompat.getInsetsController(window, view)
        bars.isAppearanceLightStatusBars = true
        bars.isAppearanceLightNavigationBars = true
        onDispose {
            bars.isAppearanceLightStatusBars = !wasDark
            bars.isAppearanceLightNavigationBars = !wasDark
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OpenedPreview() {
    PreviewTheme { OpenedScreen(onDone = {}) }
}
