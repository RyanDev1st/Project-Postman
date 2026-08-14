package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
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
 * How solid the cabinet itself is drawn.
 *
 * **The cabinet goes see-through, and the doors that are yours stay solid.**
 * Twenty identical doors with two of them tinted still reads as twenty doors:
 * the amber says "this one is yours", but nothing says the other eighteen are
 * not. Fading the cabinet to a ghost and leaving two doors at full strength
 * says it without a word — the solid things are the ones that are yours.
 *
 * Ryan asked for it in those terms, and against two earlier attempts:
 *
 * 1. Alpha at 0.55, with the colour drained. It barely showed. The render is
 *    already grey — its median saturation is 0.036 — so draining it moves a
 *    pixel by under three parts in 255, and 0.55 over a pale card left the
 *    cabinet milky rather than gone. That failure was one of degree and it was
 *    read as one of kind.
 * 2. Darkening instead, by multiplying the colour rows. That worked, in that
 *    the lit doors went from 0.69 of their neighbours' luminance to 1.39 of
 *    it. But a dark cabinet is still a cabinet, drawn heavier than the page
 *    around it, and it says "switched off" rather than "not yours".
 *
 * The [LIT_GHOST] figure is low on purpose. The cabinet's grey sits close to
 * the card behind it, so anything gentler than this reads as a smudge instead
 * of a ghost.
 *
 * Transparency borrows the background, which means the cabinet recedes into a
 * pale page by day and a dark one at night. That is the point, not a fault:
 * receding is the same instruction in both, and the doors that stay solid
 * carry the message either way.
 *
 * @param light how far the lights have come up, 0 to 1 — the same animation
 *   that raises the door tint, so the cabinet fades as the doors arrive: one
 *   movement rather than two.
 */
internal fun ghostAlpha(full: Boolean, anyLit: Boolean, light: Float): Float = when {
    // Every door taken. Nothing here is yours, so the whole picture steps
    // back and the badge the caller draws over it is what is left to read.
    // Not animated: `light` only rises when a door IS yours, so it never
    // moves in this state.
    full -> FULL_GHOST
    anyLit -> 1f - (1f - LIT_GHOST) * light
    else -> 1f
}

/** The cabinet behind your lit doors. */
private const val LIT_GHOST = 0.28f

/** The cabinet when every door is taken. Held a little more present than
 * [LIT_GHOST]: there is no lit door to carry the picture, so a fainter
 * cabinet would leave the badge floating over nothing. */
private const val FULL_GHOST = 0.38f

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
