package vn.edu.vgu.smartlocker.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
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

    /**
     * Whether a pane may actually rely on the blur.
     *
     * [supported] says the API exists. It does not say the source ever drew:
     * if [backdropSource] is not on the tree, or is on a node with no size,
     * the layer stays empty and every pane sampling it gets nothing — which
     * is how a nav bar ends up transparent enough to read COLLECTED through.
     *
     * A pane asks this, not [supported], so the failure is safe: no proven
     * recording means a near-opaque frost, which is a duller material than
     * the design and still a material, rather than a window.
     */
    var working by mutableStateOf(false)
        internal set
}

@Composable
fun rememberBackdrop(): BackdropState {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    return remember(layer) {
        BackdropState(layer).also {
            if (it.supported) {
                val r = with(density) { BLUR.toPx() }
                layer.renderEffect = frostEffect(r)
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
            if (state.supported && size.minDimension > 0f) {
                state.layer.record { this@drawWithContent.drawContent() }
                if (!state.working) state.working = true
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

    // The pane's own layer. The source layer holds the whole screen blurred;
    // this holds just the patch under this pane, and it is what carries the
    // refraction — a render effect belongs to a layer, and the source layer
    // is shared by every pane at once.
    val paneLayer = rememberGraphicsLayer()
    val refraction = rememberRefraction()

    return this
        .onGloballyPositioned { paneOrigin = it.positionInRoot() }
        .drawWithContent {
            val path = Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent))
            }
            paneLayer.renderEffect = refraction?.effectFor(size, this@drawWithContent)
            paneLayer.record(IntSize(size.width.toInt(), size.height.toInt())) {
                val d = state.sourceOrigin - paneOrigin
                translate(d.x, d.y) { drawLayer(state.layer) }
            }
            clipPath(path) { drawLayer(paneLayer) }
            drawContent()
        }
}

private val BLUR = 24.dp

/**
 * `backdrop-filter: blur(24px) saturate(190%) brightness(1.06)`.
 *
 * All three, and the two that were missing are most of the look. A blur alone
 * gives a grey smear: colour is exactly what a 24px blur averages away, and
 * averaged colour is dull colour. Pushing saturation to 190% afterwards puts
 * it back and then some, which is why glass over a photograph glows instead of
 * greying, and the 6% brightness lifts the whole film off a dark ground.
 *
 * Order matters — saturate the blurred result, not the sharp source. The
 * colour filter therefore takes the blur as its input rather than the other
 * way round.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun frostEffect(radius: Float): RenderEffect {
    val blur = android.graphics.RenderEffect.createBlurEffect(
        radius, radius, android.graphics.Shader.TileMode.DECAL,
    )
    val boost = ColorMatrix().apply {
        setSaturation(1.90f)
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    1.06f, 0f, 0f, 0f, 0f,
                    0f, 1.06f, 0f, 0f, 0f,
                    0f, 0f, 1.06f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
    return android.graphics.RenderEffect
        .createColorFilterEffect(ColorMatrixColorFilter(boost), blur)
        .asComposeRenderEffect()
}
