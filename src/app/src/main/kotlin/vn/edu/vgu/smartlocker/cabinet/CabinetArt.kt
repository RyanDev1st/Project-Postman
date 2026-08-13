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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
        // Which doors are lit, and how strongly — the dimming below and the
        // light above both read it and have to agree.
        //
        // Read in composition, so the two animations recompose this while they
        // run. That is 420ms once when the lights come up and 720ms on a zoom,
        // and it buys one list instead of the same rule written out in three
        // draw scopes that could drift apart.
        val focus = yours.map { door ->
            door to if (activeDoor == null || door.n == activeDoor) 1f else 1f - travel.value
        }
        val anyLit = light.value > 0f && focus.any { it.second > 0f }

        Image(
            painter = painterResource(R.drawable.cabinet),
            contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_cabinet_render),
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
            } else if (anyLit) {
                // **The rest of the cabinet goes back so the lit doors come
                // forward.** Twenty identical doors with two of them tinted
                // still reads as twenty doors: the amber says "this one is
                // yours" but nothing says the other eighteen are not. Taking
                // the light off them is what makes two boxes look lit rather
                // than merely coloured.
                //
                // A colour filter and not a black rectangle over the top. The
                // render is a cabinet on a transparent field, so a rectangle
                // would darken the ground around it too and leave a dark
                // square sitting on the page. A filter only touches pixels
                // that were drawn.
                //
                // Same shape as `full` above and weaker: drained most of the
                // way and dimmed, on `light` so the cabinet sinks as the doors
                // come up, one movement rather than two.
                val d = light.value
                val keep = 1f - 0.62f * d          // how much colour is left
                val grey = (1f - keep) / 3f
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            keep + grey, grey, grey, 0f, 0f,
                            grey, keep + grey, grey, 0f, 0f,
                            grey, grey, keep + grey, 0f, 0f,
                            0f, 0f, 0f, 1f - 0.45f * d, 0f,
                        )
                    )
                )
            } else null,
        )

        // The lit doors, at the render's own colour, punched back through the
        // dimmed cabinet. Drawing the picture a second time and clipping it to
        // the doors is what lets those doors keep their real brightness while
        // everything around them is knocked back — a filter cannot be undone
        // in one place, and the amber below takes its luminance from whatever
        // it lands on, so tinting a dimmed door only gives a dim amber.
        // One per door, each with its own alpha: a door on its way out has to
        // sink back on its own curve, and a single copy carrying one alpha for
        // all of them would drag the door you are looking at down with it.
        if (anyLit && !full) {
            focus.forEach { (door, f) ->
                if (f <= 0f) return@forEach
                Image(
                    painter = painterResource(R.drawable.cabinet),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipPath(doorPath(door.n, size.width, size.height)) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentScale = ContentScale.Fit,
                    alpha = f,
                )
            }
        }

        if (light.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val tintAlpha = light.value

                yours.forEach { door ->
                    // The others go out as the camera comes in.
                    //
                    // Two lit doors says "both of these are yours", which is
                    // the right answer to the wide shot. Once you have chosen
                    // one the question has changed to "which box does the
                    // button open", and a second lit door is then answering
                    // the old question over the top of the new one.
                    //
                    // Tied to `travel` rather than to a switch of its own, so
                    // the light and the camera are one movement and cannot
                    // get out of step. Travelling back out brings the other
                    // doors up again, on the same curve.
                    val focus =
                        if (activeDoor == null || door.n == activeDoor) 1f
                        else 1f - travel.value
                    if (focus <= 0f) return@forEach

                    val path = doorPath(door.n, w, h)
                    // Halo around the door — wide strokes at low alpha stand
                    // in for the bloom filter.
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.14f * focus),
                        style = Stroke(52f * w / 2100f),
                    )
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.18f * focus),
                        style = Stroke(26f * w / 2100f),
                    )
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = 0.55f * focus),
                        style = Stroke(7f * w / 2100f),
                    )
                    // The tint itself: hue and saturation from the amber,
                    // luminance from the render — BlendMode.Color.
                    drawPath(
                        path,
                        color = DoorLight.copy(alpha = tintAlpha * focus),
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
