package vn.edu.vgu.smartlocker.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * The bend at the rim — `liquid.js`'s displacement, as a runtime shader.
 *
 * The mock-up's own header explains why the obvious approaches were dropped:
 * blur-and-tint is "a painting of glass", true Snell refraction bends about
 * 9px and looks nothing like Apple's, and ramp maps warp the whole panel.
 * What works is a rounded-rect SDF that ramps the displacement to zero, so
 * **the centre is not displaced at all and only the rim warps**.
 *
 * The five lines of `liquidGlassFragment` are kept verbatim, including the
 * odd-looking SDF arguments (0.3, 0.2, 0.6 on a uv already centred at zero)
 * which come from rdev's shader and are the shape of the bezel.
 *
 * `feDisplacementMap` moves a pixel by `scale * (channel - 0.5)`, and the map
 * stores the returned uv, so the offset is `scale * (uv - 0.5) * scaled`.
 * That is what the shader computes directly — there is no map texture here,
 * because AGSL can evaluate the same function per pixel.
 *
 * **Chromatic aberration** is three samples at three scales: red at 1, green
 * at `1 - 2*0.05`, blue at `1 - 2*0.1`. The SVG blends the three isolated
 * channels with `screen`, which for disjoint channels is exactly a per-channel
 * pick, so that is what this does.
 *
 * The edge mask is not needed. It exists in the SVG to keep the aberration
 * off the middle, and here the middle is already still: at the centre
 * `uv - 0.5` is zero, so all three samples land on the same pixel.
 *
 * **API 33.** `RuntimeShader` is Tiramisu and minSdk is 24. Below it there is
 * no refraction and the pane is the material without its bend.
 */
class Refraction internal constructor(private val shader: RuntimeShader) {

    private var lastKey = ""
    private var lastEffect: androidx.compose.ui.graphics.RenderEffect? = null

    /**
     * The effect for a pane of this size, rebuilt only when the size changes.
     *
     * A new `RenderEffect` every frame would be a fresh native object on every
     * draw of every pane.
     */
    fun effectFor(size: Size, density: Density): androidx.compose.ui.graphics.RenderEffect? {
        if (size.width < 1f || size.height < 1f) return null
        val key = "${size.width.toInt()}x${size.height.toInt()}"
        if (key == lastKey) return lastEffect

        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("bend", bendFor(size, density))
        shader.setFloatUniform("maxScale", maxScaleFor(size))
        val effect = RenderEffect
            .createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()

        lastKey = key
        lastEffect = effect
        return effect
    }

    /**
     * How far the rim bends the backdrop, in pixels.
     *
     * rdev ships a flat `displacementScale` of 70, which is 35px on every
     * element whatever its size — measured against these panels that came to
     * 80% of the nav bar's height. Apple's bend lives in the bezel, so it
     * scales with the object: a fraction of the shorter side, capped so a big
     * panel does not get a wide smear.
     *
     * The doubling is `bendFor`'s: the map only ever reaches half of `scale`,
     * so the scale has to be twice the bend actually wanted.
     */
    private fun bendFor(size: Size, density: Density): Float {
        // The design's numbers are CSS px, which are dp here, so the clamp
        // has to happen in dp and convert back — clamping in pixels would
        // give a 3x phone three times the bend of a 1x one.
        val shortDp = min(size.width, size.height) / density.density
        val bendDp = min(BEND_MAX, max(BEND_MIN, shortDp * BEND_RATIO))
        return bendDp * density.density * 2f
    }

    /**
     * The largest displacement anywhere on the panel, which the map is
     * normalised by.
     *
     * `shaderMap` finds this by walking every pixel. The function is smooth
     * and symmetric about the centre, so a coarse grid finds the same maximum
     * for a fraction of the work — and this runs once per pane size, not per
     * frame. Floored at 1 exactly as the source does, so a degenerate panel
     * cannot divide by zero.
     */
    private fun maxScaleFor(size: Size): Float {
        var worst = 1f
        val steps = 48
        for (yi in 0..steps) {
            for (xi in 0..steps) {
                val ix = xi.toFloat() / steps - 0.5f
                val iy = yi.toFloat() / steps - 0.5f
                val d = roundedRectSDF(ix, iy, 0.3f, 0.2f, 0.6f)
                val scaled = ramp(0f, 1f, ramp(0.8f, 0f, d - 0.15f))
                val k = scaled - 1f
                worst = max(worst, max(abs(ix * k * size.width), abs(iy * k * size.height)))
            }
        }
        return worst
    }

    private fun ramp(a: Float, b: Float, t: Float): Float {
        val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun roundedRectSDF(x: Float, y: Float, w: Float, h: Float, r: Float): Float {
        val qx = abs(x) - w + r
        val qy = abs(y) - h + r
        return min(max(qx, qy), 0f) + hypot(max(qx, 0f), max(qy, 0f)) - r
    }

    private companion object {
        const val BEND_RATIO = 0.30f
        const val BEND_MAX = 18f
        const val BEND_MIN = 5f
    }
}

/**
 * The refraction, or null where there can be none.
 *
 * AGSL is compiled by the driver at `RuntimeShader(...)`, not by the Kotlin
 * compiler, so a syntax error here is a runtime `IllegalArgumentException` on
 * every API 33 phone and nothing at all on the build machine. A shader fault
 * must cost the bend, never the app, so it is caught: the panes then draw the
 * material without its refraction, which is what every phone below 33 gets
 * anyway.
 */
@Composable
fun rememberRefraction(): Refraction? = remember {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@remember null
    runCatching { Refraction(RuntimeShader(SOURCE)) }
        .onFailure { Log.e("Refraction", "AGSL did not compile; panes lose the bend", it) }
        .getOrNull()
}

/**
 * AGSL, matching `liquidGlassFragment` line for line.
 *
 * `smoothStep` is written out rather than using the builtin: the JS is called
 * as `smoothStep(0.8, 0, x)` with the edges the wrong way round, which GLSL
 * leaves undefined and which is the whole reason the ramp decreases.
 */
private const val SOURCE = """
uniform shader content;
uniform float2 size;
uniform float bend;
uniform float maxScale;

const float ABERRATION = 2.0;

float ramp(float a, float b, float t) {
    float x = clamp((t - a) / (b - a), 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
}

float roundedRectSDF(float x, float y, float w, float h, float r) {
    float qx = abs(x) - w + r;
    float qy = abs(y) - h + r;
    return min(max(qx, qy), 0.0) + length(float2(max(qx, 0.0), max(qy, 0.0))) - r;
}

half4 main(float2 frag) {
    float2 uv = frag / size;
    float ix = uv.x - 0.5;
    float iy = uv.y - 0.5;

    float distanceToEdge = roundedRectSDF(ix, iy, 0.3, 0.2, 0.6);
    float displacement = ramp(0.8, 0.0, distanceToEdge - 0.15);
    float scaled = ramp(0.0, 1.0, displacement);

    // The map holds the displacement in PIXELS: dx = pos.x*w - x, which is
    // w * ix * (scaled - 1). That factor is zero everywhere `scaled` is 1 -
    // the whole middle - which is what keeps the centre pin sharp. Reading
    // it as `ix * scaled` instead puts the largest displacement in the
    // middle and none at the rim, i.e. exactly inside out.
    float2 raw = float2(ix, iy) * (scaled - 1.0) * size;

    // The outermost two pixels are faded, or the map ends on a hard step and
    // the rim shows a seam.
    float edge = min(1.0, min(min(frag.x, frag.y),
                              min(size.x - frag.x - 1.0, size.y - frag.y - 1.0)) / 2.0);

    // Written to an 8-bit canvas as value/maxScale + 0.5 and CLAMPED there,
    // which is what holds the map to half of `bend` in each direction.
    float2 ch = clamp(raw * edge / maxScale + 0.5, 0.0, 1.0) - 0.5;

    half4 r = content.eval(frag + bend * ch);
    half4 g = content.eval(frag + bend * (1.0 - ABERRATION * 0.05) * ch);
    half4 b = content.eval(frag + bend * (1.0 - ABERRATION * 0.10) * ch);

    return half4(r.r, g.g, b.b, r.a);
}
"""
