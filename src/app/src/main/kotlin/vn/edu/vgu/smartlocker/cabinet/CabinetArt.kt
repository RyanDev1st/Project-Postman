package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * One door that is yours.
 *
 * `at` and `left` are what the panel under the render shows; `pct` is how
 * much time is left, as the drain rule's width; `soon` decides whether that
 * rule and the switch's dot are amber.
 */
data class YourDoor(val n: String, val at: String, val left: String, val pct: Float, val soon: Boolean)

/** How much of the frame the chosen door should fill once the screen has
 * pushed into it. 0.36: the door fills the panel but its neighbours stay in
 * frame — the move teaches *which* door, which needs the doors around it
 * still there. */
const val ZOOM_COVERAGE = 0.36f

/**
 * The cabinet, drawn from the real render.
 *
 * The doors that are yours are tinted, and the tint is a **color blend**:
 * hue and saturation from the amber, luminance from the render underneath.
 * The number stencilled on the door, its shadow and the specular on its top
 * edge all survive being lit, because nothing is covered — the whole reason
 * the app is built around a render is that the receiver is looking at the
 * thing they will walk up to.
 *
 * [full] drains the render of colour and dims it — absence is not a message,
 * so the badge that says the thing in words is drawn by the caller over this.
 */
@Composable
fun CabinetArt(
    yours: List<YourDoor>,
    free: List<String> = emptyList(),
    full: Boolean = false,
    framedDoor: String? = null,
    onDoorTapped: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current

    // The one timeline: arrive, light, then (one box only) push in.
    val arrive = remember { Animatable(0.94f) }
    val light = remember { Animatable(0f) }
    // One number for the whole camera: 0 is the wide shot, 1 is framed on
    // `framing`. The design tweens scale, xPercent and yPercent in a single
    // call with one duration and one ease, so they are one parameter here —
    // and a single parameter cannot get the scale and the pan out of step
    // with each other, which is what happened when they were separate.
    val travel = remember { Animatable(0f) }
    var framing by remember { mutableStateOf<Zoom?>(null) }

    // Which door the camera is on: the one that was tapped, or — when a
    // single box is yours — that one, because the screen frames it unasked.
    val activeDoor = framedDoor ?: yours.singleOrNull()?.n
    val zoomTarget = remember(activeDoor) { activeDoor?.let { zoomFor(it) } }

    LaunchedEffect(Unit) {
        arrive.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(yours.isNotEmpty()) {
        if (yours.isNotEmpty()) light.animateTo(1f, tween(420, easing = LinearOutSlowInEasing))
    }
    // The camera. This used to run only when framedDoor was null, so tapping
    // a door computed a target and then never moved to it — the zoom simply
    // did nothing.
    //
    // `framing` is the shot being travelled to, or travelled away from, and
    // it is held until the travel finishes. That is the whole of the broken
    // zoom out: the pan was read live off `zoomTarget`, which goes null the
    // instant you ask for the wide shot, so the picture jumped sideways to
    // centre and only then scaled down — and left the next push-in starting
    // from a place the camera was never at.
    //
    // Same duration both ways, because the design gives one: `duration: 0.72`
    // on the tween that carries scale and pan together.
    LaunchedEffect(activeDoor, framedDoor) {
        val target = zoomTarget
        if (target == null) {
            travel.animateTo(0f, tween(720, easing = LinearOutSlowInEasing))
            framing = null
        } else {
            framing = target
            // The unasked push-in waits; one you asked for goes at once.
            if (framedDoor == null) delay(600)
            travel.animateTo(1f, tween(720, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
            .graphicsLayer {
                val shot = framing
                val p = travel.value
                val z = 1f + ((shot?.scale ?: 1f) - 1f) * p
                scaleX = z * arrive.value
                scaleY = z * arrive.value
                // tx and ty are fractions of the render; translation is in
                // pixels. Without the size they were fractions of a pixel,
                // so the camera scaled but never panned onto the door.
                translationX = (shot?.tx ?: 0f) * p * size.width
                translationY = (shot?.ty ?: 0f) * p * size.height
                transformOrigin = TransformOrigin.Center
            }
            // Tapping the thing you are looking at is the first instinct; the
            // chip below the picture is the second. The mock-up puts an
            // invisible polygon on each lit door for exactly this, and the
            // port had the callback but nothing to call it — `onDoorTapped`
            // was a dead parameter, so the render did not respond to touch
            // at all.
            //
            // After graphicsLayer in the chain, so the point arrives in the
            // render's own coordinates and the polygons still fit it however
            // far the camera has pushed in.
            .pointerInput(yours) {
                detectTapGestures { at ->
                    doorAt(at, size.width.toFloat(), size.height.toFloat(), yours)
                        ?.let(onDoorTapped)
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.cabinet),
            contentDescription = "The VGU parcel locker cabinet, front elevation",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (full) {
                // saturate(.22) brightness(.72) — drained and dimmed, so the
                // badge on top has something to be louder than.
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            0.33f, 0.33f, 0.33f, 0f, 0f,
                            0.33f, 0.33f, 0.33f, 0f, 0f,
                            0.33f, 0.33f, 0.33f, 0f, 0f,
                            0f, 0f, 0f, 0.72f, 0f,
                        )
                    )
                )
            } else null,
        )

        if (light.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val tintAlpha = light.value

                yours.forEach { door ->
                    val path = doorPath(door.n, w, h)
                    // Halo around the door — wide strokes at low alpha stand
                    // in for the bloom filter.
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.14f),
                        style = Stroke(52f * w / 2100f),
                    )
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.18f),
                        style = Stroke(26f * w / 2100f),
                    )
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.55f),
                        style = Stroke(7f * w / 2100f),
                    )
                    // The tint itself: hue and saturation from the amber,
                    // luminance from the render — BlendMode.Color.
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = tintAlpha),
                        blendMode = BlendMode.Color,
                    )
                }

                free.forEach { n ->
                    val path = doorPath(n, w, h)
                    drawPath(path, color = t.free.copy(alpha = 0.9f))
                    drawPath(
                        path,
                        color = t.free.copy(alpha = 0.95f),
                        style = Stroke(5f * w / 2100f),
                    )
                }
            }
        }
    }
}

/**
 * Which of [yours] was tapped, or null for a tap on the cabinet's body.
 *
 * Only lit doors are targets. A free door is information, not a control — the
 * mock-up gives it no hit polygon either.
 */
private fun doorAt(at: Offset, w: Float, h: Float, yours: List<YourDoor>): String? {
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
private fun doorPath(n: String, w: Float, h: Float): Path {
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
