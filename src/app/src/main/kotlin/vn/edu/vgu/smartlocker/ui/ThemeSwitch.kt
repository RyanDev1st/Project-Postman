package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The day/night switch, ported from `.theme-switch` in the mock-up's
 * glass.css — the component Ryan supplied, not a Material switch.
 *
 * The parts, and what each does between light and dark:
 *
 *  - the track goes from a daylight blue to night;
 *  - the clouds sit along the bottom and **drop out of frame**;
 *  - the stars sit above the frame and **come down to the middle**;
 *  - the knob slides right, and inside it a **moon slides across the sun**,
 *    clipped by the sun's own edge. The sun does not fade or morph into the
 *    moon; one passes in front of the other, which is what makes it read as
 *    a sky rather than as a state change.
 *
 * Every number is the CSS one. `--toggle-size` is 10px and every child
 * inherits it as its font size, so 1em is 10dp here.
 *
 * The easing carries an overshoot on purpose. It is the one place in the app
 * allowed to feel sprung — DESIGN.md bans `back` and `elastic` everywhere
 * else, because metal that springs is metal that is not latched, but a
 * switch is not a locker door.
 */
@Composable
fun ThemeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(500, easing = CubicBezierEasing(0f, -0.02f, 0.4f, 1.25f)),
        label = "theme-switch",
    )
    val knob by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(300, easing = CubicBezierEasing(0f, -0.02f, 0.35f, 1.17f)),
        label = "theme-switch-knob",
    )

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = TRACK_W.dp, height = TRACK_H.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Canvas(modifier = Modifier.size(TRACK_W.dp, TRACK_H.dp)) {
            val px = size.height / TRACK_H          // dp -> px for this canvas
            val u: (Float) -> Float = { it * px }   // one CSS unit in px

            val track = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = size.width, bottom = size.height,
                        radiusX = size.height / 2f, radiusY = size.height / 2f,
                    )
                )
            }

            // The pill's own two shadows, drawn first so they sit behind it.
            // A box-shadow changes no layout, so these are allowed outside
            // the canvas' bounds and nothing here is clipped to it.
            //
            // The white one is the whole read: 94% white, a touch under the
            // pill. Without it the toggle is a flat lozenge lying on the
            // screen rather than a dish set into it.
            shadow(track, dy = -u(0.62f), blur = u(0.62f), color = Color.Black.copy(alpha = 0.25f))
            shadow(track, dy = u(0.62f), blur = u(1.25f), color = Color.White.copy(alpha = 0.94f))

            // Everything is inside the track's clip, the knob included.
            //
            // The knob used to be drawn outside it, on the reasoning that a
            // halo spilling past the edge is what gives it lift. That was a
            // guess, and the CSS says the opposite in one word: the pill is
            // `overflow: hidden`, so the four white rings round the knob stop
            // at its edge and read as the sky lightening around the sun.
            // Unclipped they are four grey circles sitting on the screen
            // behind the toggle, which is BUG-002.
            clipPath(track) {
                drawRect(lerp(DAY_SKY, NIGHT_SKY, t))
                drawClouds(u, t)
                drawStars(u, t)

                val left = u(CIRCLE_OFFSET) + knob * u(TRACK_W - CIRCLE_D + 2 * -CIRCLE_OFFSET)
                translate(left = left, top = u(CIRCLE_OFFSET)) {
                    drawKnob(u, t)
                }

                // The rim, thrown inward from the top edge. Two identical
                // inset shadows in the CSS, 25% black each, so 44% together,
                // over 0.05em of offset plus 0.187em of blur. It is the last
                // thing painted - the CSS puts it on z-index 1 and the knob
                // on 0, so it falls across the sun as well as the sky.
                val rim = u(0.5f + 1.87f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.44f),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = rim,
                    ),
                    size = Size(size.width, rim),
                )
            }
        }
    }
}

/**
 * One CSS `box-shadow`, cast by this path and nothing else.
 *
 * A transparent fill with a shadow layer under it, which is how the platform
 * draws a shadow without also drawing the thing casting it. CSS states a blur
 * *diameter* and Android wants a radius, hence the half.
 */
private fun DrawScope.shadow(path: Path, dy: Float, blur: Float, color: Color) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(blur / 2f, 0f, dy, color.toArgb())
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

/** The white halo, the sun, and the moon crossing it. */
private fun DrawScope.drawKnob(u: (Float) -> Float, t: Float) {
    val r = u(CIRCLE_D) / 2f
    val centre = Offset(r, r)

    // Four shadows in the CSS, two circles here, and that is not a shortcut.
    //
    // The two `inset 0 0 0 3.375em` fill the knob itself with white 10% twice
    // over; the two spreads, 0.625em and 1.25em, ring it outside. An outer
    // box-shadow does not paint under its own border box, so the insets own
    // the middle and the spreads own the ring - and 10% over 10% is 19% in
    // both places. So the disc out to r + 6.25 is one even 19%, which is what
    // these two circles paint, and the band beyond it is 10%.
    //
    // A third circle used to sit on top at 20% to stand for the insets. It
    // was painting 19% + 20% = 35% over them, and the knob well came out
    // brighter than the sky it is supposed to be a dip in.
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r + u(12.5f), center = centre)
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r + u(6.25f), center = centre)

    val sunR = u(SUN_D) / 2f
    val sun = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre - Offset(sunR, sunR), Size(sunR * 2, sunR * 2))) }
    clipPath(sun) {
        drawCircle(SUN, radius = sunR, center = centre)
        // The moon comes in from translateX(100%) and lands on 0.
        translate(left = (1f - t) * u(SUN_D)) {
            val topLeft = centre - Offset(sunR, sunR)
            drawCircle(MOON, radius = sunR, center = centre)
            // Craters, in the CSS's own order and sizes.
            drawCircle(SPOT, u(2.5f) / 2f, topLeft + Offset(u(8.12f) + u(1.25f), u(3.12f) + u(1.25f)))
            drawCircle(SPOT, u(3.75f) / 2f, topLeft + Offset(u(13.75f) + u(1.87f), u(9.37f) + u(1.87f)))
            drawCircle(SPOT, u(7.5f) / 2f, topLeft + Offset(u(3.12f) + u(3.75f), u(7.5f) + u(3.75f)))
        }
    }
}

/**
 * The cloud bank. The CSS builds it from fifteen box-shadows on one circle;
 * these are those offsets, in the same two tones — the near tone in front,
 * the far tone behind it.
 */
private fun DrawScope.drawClouds(u: (Float) -> Float, t: Float) {
    val r = u(12.5f) / 2f
    // bottom: -0.625em -> -4.062em, so the bank falls 3.437em out of frame.
    val fall = t * u(34.37f)
    val baseX = u(3.12f)
    val baseY = size.height - u(12.5f) + u(6.25f) + fall

    val far = listOf(
        -3.12f to -3.12f, 5f to -1.25f, 12.5f to -0.62f, 20f to -3.12f,
        26.25f to 0f, 33.75f to -4.37f, 40f to -6.25f, 41.25f to -21.25f,
    )
    val near = listOf(
        0f to 0f, 9.37f to 3.12f, 14.37f to 3.75f, 21.87f to 0f,
        29.37f to 3.12f, 36.25f to -0.62f, 45f to -3.12f, 46.25f to -17.5f,
    )
    far.forEach { (dx, dy) ->
        drawCircle(BACK_CLOUD, r, Offset(baseX + u(dx) + r, baseY + u(dy) + r))
    }
    near.forEach { (dx, dy) ->
        drawCircle(CLOUD, r, Offset(baseX + u(dx) + r, baseY + u(dy) + r))
    }
}

/**
 * The sparkles. The CSS ships them as one SVG on a 144x55 viewBox at
 * 2.75em wide; these are the centres of its eight four-pointed stars,
 * scaled the same way. They sit above the frame and drop to the middle.
 */
private fun DrawScope.drawStars(u: (Float) -> Float, t: Float) {
    val scale = u(27.5f) / 144f
    val left = u(3.12f)
    // top: -100% -> 50% with translateY(-50%): above the frame, then centred.
    val top = -size.height + t * (size.height * 1.5f - 55f * scale / 2f)

    listOf(
        137f to 4.4f, 35f to 23.4f, 4f to 36.4f, 58f to 25.4f,
        85f to 25.4f, 140f to 36.4f, 103f to 50.4f,
    ).forEach { (sx, sy) ->
        star(Offset(left + sx * scale, top + sy * scale), 4.5f * scale)
    }
}

/** A four-pointed sparkle: two concave arcs each side, drawn as a diamond
 *  with pinched waists, which is what the source SVG's path is. */
private fun DrawScope.star(centre: Offset, r: Float) {
    val waist = r * 0.26f
    val p = Path().apply {
        moveTo(centre.x, centre.y - r)
        quadraticBezierTo(centre.x + waist, centre.y - waist, centre.x + r, centre.y)
        quadraticBezierTo(centre.x + waist, centre.y + waist, centre.x, centre.y + r)
        quadraticBezierTo(centre.x - waist, centre.y + waist, centre.x - r, centre.y)
        quadraticBezierTo(centre.x - waist, centre.y - waist, centre.x, centre.y - r)
        close()
    }
    drawPath(p, Color.White)
}

// --- the CSS's own numbers, at --toggle-size: 10px -------------------------

private const val TRACK_W = 56.25f      // 5.625em
private const val TRACK_H = 25f         // 2.5em
private const val CIRCLE_D = 33.75f     // 3.375em
private const val SUN_D = 21.25f        // 2.125em
private const val CIRCLE_OFFSET = -4.375f // (3.375em - 2.5em) / 2 * -1

private val DAY_SKY = Color(0xFF3D7EAE)
private val NIGHT_SKY = Color(0xFF1D1F2C)
private val SUN = Color(0xFFECCA2F)
private val MOON = Color(0xFFC4C9D1)
private val SPOT = Color(0xFF959DB1)
private val CLOUD = Color(0xFFF3FDFF)
private val BACK_CLOUD = Color(0xFFAACADF)
