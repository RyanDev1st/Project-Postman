package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import vn.edu.vgu.smartlocker.ui.theme.DoorLight

/**
 * The light on the cabinet: which doors are lit, and how far the rest of it
 * goes back.
 *
 * Split out of [CabinetArt] when that file passed the 300-line cap. It holds
 * the two colour filters and the glow, which are the picture's lighting; the
 * file it came from is left with the camera and the timeline.
 */

/**
 * The whole cabinet, knocked back so the lit doors come forward.
 *
 * Twenty identical doors with two of them tinted still reads as twenty doors:
 * the amber says "this one is yours", but nothing says the other eighteen are
 * not. Taking the light off them is what makes two boxes look lit rather than
 * merely coloured.
 *
 * **Darkened, not faded.** The first version of this dropped the alpha to 0.55
 * and drained the colour, and on a screen it barely showed. The render is
 * already grey — measured, its median saturation is 0.036 — so draining it
 * moves a pixel by under three parts in 255, and the alpha only mixed a grey
 * cabinet into a grey card. It came out milky rather than off, and the lit
 * doors measured *darker* than their unlit neighbours: 0.69 of their
 * luminance, the highlight the dimmest thing on the cabinet. With the light
 * on the colour rows they measure 1.39 of it.
 *
 * Alpha stays at 1. Transparency dims by borrowing the background, so it would
 * lighten the cabinet in the day scheme and darken it at night — two opposite
 * readings of one state.
 *
 * A filter and not a black rectangle over the top: the render is a cabinet on
 * a transparent field, so a rectangle would darken the ground around it too
 * and leave a dark square sitting on the page. A filter only touches pixels
 * that were drawn.
 *
 * @param d how far the light has come up, 0 to 1. Driven by the same
 *   animation that raises the door tint, so the cabinet sinks as the doors
 *   come up — one movement rather than two.
 */
internal fun dimFilter(d: Float): ColorFilter {
    val keep = 1f - 0.62f * d          // how much colour is left
    val grey = (1f - keep) / 3f
    val lit = 1f - 0.42f * d           // how much light is left
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                lit * (keep + grey), lit * grey, lit * grey, 0f, 0f,
                lit * grey, lit * (keep + grey), lit * grey, 0f, 0f,
                lit * grey, lit * grey, lit * (keep + grey), 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
}

/** saturate(.22) brightness(.72) — the cabinet when every door is taken.
 * Drained and dimmed, so the badge drawn on top has something to be louder
 * than. Absence is not a message, so the words are the caller's job. */
internal val FullFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 0.72f, 0f,
        )
    )
)

/**
 * One lit door: the halo around it, then the amber on it.
 *
 * The tint is a **colour blend** — hue and saturation from the amber,
 * luminance from the render. The number stencilled on the door, its shadow and
 * the specular on its top edge all survive being lit, because nothing is
 * covered.
 *
 * @param focus 1 for a door the eye is on, falling to 0 as the camera travels
 *   away from it.
 */
internal fun DrawScope.drawDoorLight(door: String, focus: Float, tintAlpha: Float) {
    val w = size.width
    val path = doorPath(door, w, size.height)

    // Wide strokes at low alpha stand in for the bloom filter.
    drawPath(path, DoorLight.copy(alpha = 0.14f * focus), style = Stroke(52f * w / 2100f))
    drawPath(path, DoorLight.copy(alpha = 0.18f * focus), style = Stroke(26f * w / 2100f))
    drawPath(path, DoorLight.copy(alpha = 0.55f * focus), style = Stroke(7f * w / 2100f))
    drawPath(path, DoorLight.copy(alpha = tintAlpha * focus), blendMode = BlendMode.Color)
}

/** A door nobody has taken. Flat fill and a rim, in the free colour. */
internal fun DrawScope.drawFreeDoor(door: String, free: Color) {
    val w = size.width
    val path = doorPath(door, w, size.height)
    drawPath(path, free.copy(alpha = 0.9f))
    drawPath(path, free.copy(alpha = 0.95f), style = Stroke(5f * w / 2100f))
}
