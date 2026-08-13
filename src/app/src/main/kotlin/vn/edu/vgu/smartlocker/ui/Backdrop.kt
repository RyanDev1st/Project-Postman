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
import androidx.compose.ui.geometry.Size
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
    // No render effect here. The blur belongs to the layer that reaches the
    // screen, and this one never does — see [backdropBlur].
    return remember(layer) { BackdropState(layer) }
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
            // Drawn normally, not as the layer. A GraphicsLayer is recorded
            // once and drawn once in a frame; drawing it here as well as
            // inside the pane's own layer made the pane's copy come out
            // blank, and the whole nav bar went see-through.
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
    val density = LocalDensity.current
    val blurPx = with(density) { BLUR.toPx() }
    // How much wider than the pane the recording is, on every side, so the
    // lens has real content to bend rather than a hole. See [Refraction.PAD].
    val padPx = with(density) { Refraction.PAD.toPx() }

    return this
        .onGloballyPositioned { paneOrigin = it.positionInRoot() }
        .drawWithContent {
            val path = Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent))
            }

            // Both effects go on THIS layer, because this is the one that is
            // drawn to the screen.
            //
            // The blur used to sit on the source layer instead, which is only
            // ever drawn nested inside this layer's recording and never
            // straight to a canvas. Meanwhile this layer's effect was set from
            // `refraction` alone — and refraction is AGSL, which needs API 33,
            // so on any phone below that it is null and the pane was drawing
            // a raw, sharp copy of the screen behind it. That is why the word
            // COLLECTED could be read through the nav bar at full sharpness
            // while the blur was nominally 20dp: there was no lens, only a
            // tinted shape. Every number tuned on top of that was paint on a
            // window.
            //
            // Chained, so the frost runs first and the refraction bends the
            // frosted result — the order the material needs, and the order
            // `backdrop-filter` uses.
            // The recording is the pane plus a margin all round. The blur
            // wants it as much as the lens does: `DECAL` treats everything
            // past the layer as transparent, so without the margin the frost
            // thinned out towards each edge exactly where the rim needs to be
            // solid.
            val outer = Size(size.width + 2f * padPx, size.height + 2f * padPx)

            // Where the recorded screen lands inside that margin. The lens
            // needs it as well as the recording does — see [Refraction].
            val d = state.sourceOrigin - paneOrigin
            val at = Offset(d.x + padPx, d.y + padPx)
            val recorded = state.layer.size

            val frost = frostEffect(blurPx)
            val bend = refraction
                ?.effectFor(
                    layer = outer,
                    pane = size,
                    contentAt = at,
                    contentSize = Size(recorded.width.toFloat(), recorded.height.toFloat()),
                    density = this@drawWithContent,
                )
                ?.asAndroidRenderEffect()
            // Lens first, frost second — `createChainEffect(outer, inner)`.
            //
            // The library goes the other way: `backdrop-filter` blurs, then
            // `filter: url(#…)` displaces the blurred result. This did too,
            // and it put a hard vertical seam down one end of the nav bar with
            // sharp page text showing through beyond it.
            //
            // The reason is that a blur grows the bounds it hands on. Probed
            // on the emulator by painting `frag / size` straight out as
            // colour: with the blur inner, the shader's coordinate space came
            // back 887px wide where the layer is 810, and the pane's left edge
            // landed at 125 rather than at the 50 of the margin. So `centre`
            // was 37px off and the clamp was biting into live content — an
            // error on one side only, which is what a one-sided seam is.
            //
            // Inner effects run on the layer itself, where the coordinates are
            // known exactly, so the lens goes there and the frost takes its
            // output. The cost is that the frost now softens the colour
            // fringes rather than the fringes being made from softened
            // content. The library does the same thing at the end of its own
            // chain with a `feGaussianBlur` after the three passes.
            paneLayer.renderEffect =
                (if (bend != null) RenderEffectApi.createChainEffect(frost, bend) else frost)
                    .asComposeRenderEffect()

            paneLayer.record(IntSize(outer.width.toInt(), outer.height.toInt())) {
                translate(at.x, at.y) { drawLayer(state.layer) }
            }
            // Drawn back with the margin hanging outside, then clipped to the
            // pane, so the extra never reaches the screen.
            clipPath(path) {
                translate(-padPx, -padPx) { drawLayer(paneLayer) }
            }
            drawContent()
        }
}

private typealias RenderEffectApi = android.graphics.RenderEffect

/**
 * Eleven — under the demo's twenty, and deliberately.
 *
 * The demo's twenty sits over a busy colour photograph, where a heavy blur
 * still leaves shape and colour to work with. Our backdrop is a dark UI: thin
 * type, hairlines and flat panels. Twenty pixels of blur turns that into
 * even grey, and even grey is what "super frosty and cheap" means. It also
 * destroys the only thing the refraction has to bend — a lens over mush shows
 * nothing, so the material loses the very effect that makes it liquid glass
 * rather than frosted glass.
 *
 * Eleven still stops a word being read through the bar, which is the one
 * thing the blur must do, and leaves enough structure for the edge to bend.
 *
 * (History, so this does not move again on the wrong evidence: the README's
 * default computes to 6px; the live demo runs 20. Neither is a target on its
 * own — the right figure depends on what is behind the glass.)
 *
 * `liquid-glass-react` documents `blurAmount: 0.0625`, and the filter is
 * `blur(4 + blurAmount * 32)`, so the documented default is 6px. The demo at
 * liquid-glass.maxrovensky.com — which is the thing Ryan looked at and asked
 * for — computes `blur(20px) saturate(1.4)`, so it runs `blurAmount: 0.5`.
 *
 * That is worth writing down because it went the wrong way once already:
 * 24 was dropped to 6 on the README's authority, when the number being judged
 * against was 20 all along. The saturation was the part that was really wrong,
 * at 190 against the demo's 140.
 *
 * Read the docs, then go and look at the thing.
 */
private val BLUR = 11.dp

/**
 * `backdrop-filter: blur(6px) saturate(140%)` — the liquid-glass defaults.
 *
 * Two filters, not three. The mock-up's own `.lg` asks for 190% saturation
 * and a 6% brightness lift on top of a 24px blur, and all three together are
 * what made the pane look like frosted plastic: over-blurred, then
 * over-corrected to hide it. The library Ryan pointed at does far less -
 * a light blur and a modest saturation - and lets the refraction carry the
 * material.
 *
 * Order matters: saturate the blurred result, not the sharp source, so the
 * colour filter takes the blur as its input rather than the other way round.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun frostEffect(radius: Float): android.graphics.RenderEffect {
    val blur = android.graphics.RenderEffect.createBlurEffect(
        radius, radius, android.graphics.Shader.TileMode.DECAL,
    )
    val boost = ColorMatrix().apply { setSaturation(1.40f) }
    return android.graphics.RenderEffect
        .createColorFilterEffect(ColorMatrixColorFilter(boost), blur)
}
