package vn.edu.vgu.smartlocker.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * What the glass has to bend.
 *
 * The design's chrome is `backdrop-filter: blur(24px) saturate(190%)
 * brightness(1.06)`. Compose has no backdrop filter, so this builds one: the
 * screen's content is recorded into a graphics layer, that layer carries a
 * blur, and each pane draws the layer underneath itself — shifted so the
 * region beneath the pane is the region that gets drawn, and clipped to the
 * pane's own shape.
 *
 * Without it a pane is a translucent rectangle with sharp text behind it,
 * which is not glass; it is a window. The nav read as fully transparent, and
 * the word COLLECTED could be read straight through it.
 *
 * **Blur needs API 31.** Below that [supported] is false and the panes fall
 * back to a solid frost — lighter than glass, but nothing shows through, and
 * a control the eye cannot separate from the page behind it is worse than a
 * plain one.
 */
class BackdropState internal constructor(internal val layer: GraphicsLayer) {

    /** Where the recorded content sits, in root coordinates. */
    internal var sourceOrigin by mutableStateOf(Offset.Zero)

    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Composable
fun rememberBackdrop(): BackdropState {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    return remember(layer) {
        BackdropState(layer).also {
            if (it.supported) {
                val r = with(density) { BLUR.toPx() }
                layer.renderEffect = BlurEffect(r, r, TileMode.Decal)
            }
        }
    }
}

/**
 * Marks the content the glass bends. Recorded once per frame, then drawn
 * normally — the recording carries the blur, the direct draw does not, so
 * the page itself is untouched.
 *
 * Put this on the content only, never on a parent that also holds the glass,
 * or a pane would be sampling a layer it is itself inside.
 */
fun Modifier.backdropSource(state: BackdropState): Modifier =
    this
        .onGloballyPositioned { state.sourceOrigin = it.positionInRoot() }
        .drawWithContent {
            if (state.supported) {
                state.layer.record { this@drawWithContent.drawContent() }
            }
            drawContent()
        }

/**
 * Draws the blurred backdrop under a pane, in the pane's own shape.
 *
 * Composable, because each pane has to remember where it sits — a shared
 * offset would have every pane sampling the same patch of screen.
 */
@Composable
fun Modifier.backdropBlur(state: BackdropState, shape: Shape): Modifier {
    if (!state.supported) return this
    var paneOrigin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { paneOrigin = it.positionInRoot() }
        .drawWithContent {
            val path = Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent))
            }
            clipPath(path) {
                val d = state.sourceOrigin - paneOrigin
                translate(d.x, d.y) { drawLayer(state.layer) }
            }
            drawContent()
        }
}

private val BLUR = 24.dp
