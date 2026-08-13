package vn.edu.vgu.smartlocker.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R

/**
 * The lens — what makes this liquid glass rather than frosted glass.
 *
 * **This is `liquid-glass-react`'s own displacement map, not a reimplementation
 * of it.** `res/drawable/liquid_displacement.jpg` is the image that library
 * ships inline as base64 in `utils.ts`, decoded to a file: 256 by 256, and —
 * measured, not assumed — a flat linear ramp in both axes. Red falls 250 to 2
 * from side to side and blue falls 254 to 0 from top to bottom, evenly the
 * whole way. There is no ring in it and no bevel; the rim look comes from how
 * it is mapped, and from the three colour passes. See [SOURCE].
 *
 * That distinction matters, because the library has two ways of doing this and
 * this port had copied the wrong one. `mode: "shader"` computes a rounded-rect
 * signed distance field at runtime, and the library's own README calls it "the
 * most accurate but not the most stable". The DEFAULT is `mode: "standard"`,
 * which does no maths at all — it hands this baked image to
 * `feDisplacementMap`. The demo everybody looks at is the default. So a
 * faithful SDF port was a faithful port of the mode nobody uses, and on a 59dp
 * bar it came out to almost nothing: measured on an emulator, multiplying its
 * strength by five changed not one pixel.
 *
 * How the image is read is `feDisplacementMap`'s contract:
 * `xChannelSelector="R"`, `yChannelSelector="B"`, and 128 means do not move.
 * So `(R - 0.5)` and `(B - 0.5)` are the direction, and `scale` is how far.
 *
 * Three samples, one per colour channel, at three slightly different
 * distances. That is the chromatic aberration, and it is most of the
 * difference between a lens and a smudge. The scales are the library's own, at
 * its documented defaults — `displacementScale: 70`, `aberrationIntensity: 2`,
 * `mode: "standard"`:
 *
 *     R: 70 * -1                = -70
 *     G: 70 * (-1 - 2 * 0.05)   = -77
 *     B: 70 * (-1 - 2 * 0.10)   = -84
 */
class Refraction internal constructor(
    private val shader: RuntimeShader,
    mapWidth: Float,
    mapHeight: Float,
) {
    private var lastKey: String? = null
    private var lastEffect: androidx.compose.ui.graphics.RenderEffect? = null

    init {
        shader.setFloatUniform("mapSize", mapWidth, mapHeight)
    }

    /**
     * The effect for a pane of this size, cached.
     *
     * A new `RenderEffect` every frame would be a fresh native object on every
     * draw of every pane.
     */
    fun effectFor(
        layer: Size,
        pane: Size,
        contentAt: Offset,
        contentSize: Size,
        density: Density,
    ): androidx.compose.ui.graphics.RenderEffect? {
        if (pane.width < 1f || pane.height < 1f) return null
        if (contentSize.width < 1f || contentSize.height < 1f) return null
        val key = "${layer.width.toInt()}x${layer.height.toInt()}x${pane.width.toInt()}" +
            "@${contentAt.x.toInt()},${contentAt.y.toInt()}"
        if (key == lastKey) return lastEffect

        // Where real pixels are, in the layer's own coordinates. Not the same
        // as the layer: the recording is the pane plus a margin, and near the
        // edge of the screen the source simply stops before the margin does.
        // A sample past that point fetches nothing, comes back transparent,
        // and the page shows through the glass at full sharpness — which is
        // what put a crisp "2 Aug" inside the right-hand end of the nav bar
        // while the middle of the bar was properly dissolved.
        val lo = Offset(maxOf(0f, contentAt.x) + 0.5f, maxOf(0f, contentAt.y) + 0.5f)
        val hi = Offset(
            minOf(layer.width, contentAt.x + contentSize.width) - 0.5f,
            minOf(layer.height, contentAt.y + contentSize.height) - 0.5f,
        )
        shader.setFloatUniform("lo", lo.x, lo.y)
        shader.setFloatUniform("hi", hi.x, hi.y)

        // The layer is the pane plus a margin on every side — see [pad]. The
        // margin is symmetrical, so the layer's centre is still the pane's.
        shader.setFloatUniform("size", layer.width, layer.height)
        // `preserveAspectRatio="xMidYMid slice"` over a filter region of
        // 170% — index.tsx 51-52. `slice` means cover: the square map is
        // scaled until it covers the region, keeping its aspect, and the
        // overflow is cropped. So the side it is drawn at is the region's
        // LONGER edge, and it is centred on the pane.
        shader.setFloatUniform("side", REGION * maxOf(pane.width, pane.height))
        // 70 CSS pixels is 70dp here. Negative, because `mode: "standard"`
        // negates the scale.
        shader.setFloatUniform("scale", -DISPLACEMENT * density.density)

        val effect = RenderEffect
            .createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()

        lastKey = key
        lastEffect = effect
        return effect
    }

    companion object {
        /** `displacementScale`, the library's default. */
        const val DISPLACEMENT = 70f

        /** `<filter x="-35%" width="170%">` — index.tsx 51. */
        const val REGION = 1.70f

        /**
         * How far outside itself a pane has to reach, in dp.
         *
         * The lens samples away from the centre, so the pixels along an edge
         * come from beyond that edge. Give it nothing to reach into and the
         * result is one of two faults, both of which shipped: sample off the
         * layer and you get transparent, so the page shows through the rim
         * unblurred; clamp the sample instead and a whole band of pixels all
         * read the same border column, which on a pale ground is a hard
         * vertical streak down each end of the nav bar.
         *
         * So the layer is recorded larger than the pane and this is the
         * margin. It is not a guess. The pull at a point is
         * `(p - centre) * 2A * scale / side`, and `side` is `1.7` times the
         * pane's longer edge, so at the far edge of that edge it comes to
         * `scale / 3.4` however big the pane is — the geometry cancels. Times
         * 1.2 for the blue channel, which reaches furthest:
         *
         *     70 / 3.4 * 1.2 = 24.7dp
         */
        val PAD = 25.dp
    }
}

/**
 * Null below API 33 — AGSL is a Tiramisu API and this app runs from 24. A pane
 * without it keeps its blur and its edge, and loses only the bend.
 */
@Composable
fun rememberRefraction(): Refraction? {
    val context: Context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@remember null
        runCatching {
            val map = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.liquid_displacement,
            ) ?: error("liquid_displacement did not decode")

            val shader = RuntimeShader(SOURCE)
            shader.setInputShader(
                "map",
                BitmapShader(map, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
            )
            Refraction(shader, map.width.toFloat(), map.height.toFloat())
        }
            .onFailure { Log.e("Refraction", "no lens; panes keep blur and edge", it) }
            .getOrNull()
    }
}

/**
 * AGSL, doing what `feDisplacementMap` does, three times over.
 *
 * **The map is covered over the pane, not stretched across it**, and that one
 * word is the difference between a lens and a smear. Measured off the decoded
 * file, the map is a linear ramp in both axes — R runs 250 to 2 straight
 * across, B runs 254 to 0 straight down — so what it describes is not a rim
 * effect at all. It is a uniform pull away from the centre, which sampled back
 * is a uniform *minification*: the pane shows a little more than the patch it
 * covers, shrunk to fit. About 0.9 at these numbers.
 *
 * Because it is a plain ramp, the aspect ratio is the whole of it. Stretched
 * over a nav bar 358dp by 59dp the pull is six times stronger down the short
 * axis than across the long one, and content from well above and below the bar
 * gets crushed into 59dp — a horizontal smear, which is exactly what the last
 * build showed. `slice` keeps the two equal, which is why the library asks for
 * it.
 *
 * One deliberate difference from the library. A sample that walks off the pane
 * has nothing to fetch: in SVG it returns transparent, and the library lives
 * with that because its two bright rings sit over the rim and hide it. Here
 * transparent meant the page showed through unblurred, and small sharp text
 * appeared along both ends of the nav bar. So the pane is recorded with a
 * margin round it — [Refraction.PAD] — and the lens reaches into real content
 * instead of into nothing. The clamp below is only a guard on that margin.
 */
private const val SOURCE = """
uniform shader content;
uniform shader map;
uniform float2 size;
uniform float2 mapSize;
uniform float2 lo;
uniform float2 hi;
uniform float side;
uniform float scale;

half4 main(float2 frag) {
    float2 centre = size * 0.5;

    // Where this pixel falls inside the covered square, 0 to 1. Outside that
    // range the sampler's CLAMP repeats the border rather than wrapping.
    float2 uv = (frag - centre) / side + 0.5;
    half4 m = map.eval(uv * mapSize);

    // 128 is "do not move". Anything either side of it is a direction.
    float2 dir = float2(m.r - 0.5, m.b - 0.5);

    // One sample per colour, at three distances - the same order and the same
    // fractions as the library's three feDisplacementMap passes.
    half4 cr = content.eval(clamp(frag + dir * scale, lo, hi));
    half4 cg = content.eval(clamp(frag + dir * (scale * 1.10), lo, hi));
    half4 cb = content.eval(clamp(frag + dir * (scale * 1.20), lo, hi));

    return half4(cr.r, cg.g, cb.b, cr.a);
}
"""
