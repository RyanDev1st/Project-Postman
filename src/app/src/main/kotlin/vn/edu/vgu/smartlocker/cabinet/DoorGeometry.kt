package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Where the doors are on the render, and what the camera does to reach one.
 *
 * Geometry, not drawing. `CabinetArt.kt` holds the picture — the lighting, the
 * camera and the dimming — and reads these; splitting them apart was forced by
 * the 300-line cap, and it lands on a real seam: nothing here knows what a
 * door looks like, and nothing there knows where one is.
 *
 * Every coordinate is a fraction of the render, so it survives any screen.
 * [DOOR_POLYGONS] is the shared source of truth for both.
 */

/**
 * Which of [yours] was tapped, or null for a tap on the cabinet's body.
 *
 * Only lit doors are targets. A free door is information, not a control — the
 * mock-up gives it no hit polygon either.
 */
internal fun doorAt(at: Offset, w: Float, h: Float, yours: List<YourDoor>): String? {
    if (w <= 0f || h <= 0f) return null
    val x = at.x / w
    val y = at.y / h
    return yours.firstOrNull { door ->
        DOOR_POLYGONS[unpad(door.n)]?.let { encloses(it, x, y) } == true
    }?.n
}

/**
 * Ray casting, in the projection's own unit square.
 *
 * The doors are convex quads and a bounding box would nearly work, but they
 * sit edge to edge: a box test lets a tap near a shared border pick the
 * neighbour. Counting crossings cannot get a corner wrong.
 */
private fun encloses(corners: List<Pair<Float, Float>>, x: Float, y: Float): Boolean {
    var inside = false
    var j = corners.lastIndex
    for (i in corners.indices) {
        val (xi, yi) = corners[i]
        val (xj, yj) = corners[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/** A door's four projected corners as a path in the current draw size. */
internal fun doorPath(n: String, w: Float, h: Float): Path {
    val corners = DOOR_POLYGONS[unpad(n)] ?: return Path()
    return Path().apply {
        corners.forEachIndexed { i, (x, y) ->
            val px = x * w
            val py = y * h
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

/** "04" -> "4", "07" -> "7" — the projections are keyed by the bare number. */
internal fun unpad(n: String): String = n.toIntOrNull()?.toString() ?: n

/** Centre and size of a door, in fractions of the frame. */
internal fun boxOf(n: String): BoxDim {
    val c = DOOR_POLYGONS[unpad(n)] ?: return BoxDim(0.5f, 0.5f, 0.1f, 0.1f)
    val xs = c.map { it.first }
    val ys = c.map { it.second }
    return BoxDim(
        cx = (xs.min() + xs.max()) / 2,
        cy = (ys.min() + ys.max()) / 2,
        w = xs.max() - xs.min(),
        h = ys.max() - ys.min(),
    )
}

internal data class BoxDim(val cx: Float, val cy: Float, val w: Float, val h: Float)

/**
 * The scale and slide that put one door in the middle of the frame.
 *
 * The render is an orthographic elevation, so a door is the same rectangle
 * wherever it sits and this is only a scale and a slide. The origin stays at
 * the centre and the pan is solved for, so every state is a plain
 * (scale, x, y) any two of which tween cleanly. With
 * `translate(t) scale(k)` about the centre, an image point p lands at
 * `0.5 + k(p - 0.5) + t`, so `t = -k(c - 0.5)` puts c in the middle.
 *
 * The centre is clamped to keep the frame inside the picture: after scaling
 * by k the visible window is 1/k of the image wide, so the centre can only
 * travel to within half of that of each edge.
 */
internal fun zoomFor(n: String): Zoom {
    val b = boxOf(n)
    val scale = ZOOM_COVERAGE / b.w
    val half = 0.5f / scale
    val cx = b.cx.coerceIn(half, 1 - half)
    val cy = b.cy.coerceIn(half, 1 - half)
    return Zoom(scale, -scale * (cx - 0.5f), -scale * (cy - 0.5f))
}

internal data class Zoom(val scale: Float, val tx: Float, val ty: Float)
