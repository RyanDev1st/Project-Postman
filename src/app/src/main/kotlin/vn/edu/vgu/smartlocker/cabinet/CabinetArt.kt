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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.edu.vgu.smartlocker.R
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

    // Which door is under a finger right now.
    //
    // The render is the door selector - the segmented switch that used to do
    // this job was removed as a duplicate - and until now it was the only
    // control in the app with no pressed state at all. A picture that does
    // not answer a touch is a picture, so a receiver with two parcels had no
    // way of learning that the second one was reachable.
    var pressed by remember { mutableStateOf<String?>(null) }

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
                detectTapGestures(
                    onPress = { at ->
                        val n = doorAt(at, size.width.toFloat(), size.height.toFloat(), yours)
                        if (n != null) {
                            pressed = n
                            tryAwaitRelease()
                            pressed = null
                        }
                    },
                    onTap = { at ->
                        doorAt(at, size.width.toFloat(), size.height.toFloat(), yours)
                            ?.let(onDoorTapped)
                    },
                )
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
            // The rest of the cabinet goes see-through, not dark. See
            // [ghostAlpha] in CabinetLight.kt.
            alpha = ghostAlpha(full = full, anyLit = anyLit, light = light.value),
        )

        // The lit doors, at the render's own colour and fully solid, drawn back
        // over the ghost. Drawing the picture a second time and clipping it to
        // the doors is what lets those doors stay present while everything
        // around them fades — one alpha cannot be undone in a patch, and the
        // amber below takes its luminance from whatever it lands on, so
        // tinting a faded door only gives a faded amber.
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
                // The same `focus` the dimming reads, so a door cannot be lit
                // here and dark underneath. The others go out as the camera
                // comes in: two lit doors says "both of these are yours",
                // which is the right answer to the wide shot, but once you
                // have chosen one the question is "which box does the button
                // open" and a second lit door answers the old one over the
                // top of the new.
                focus.forEach { (door, f) ->
                    if (f > 0f) drawDoorLight(door.n, f, tintAlpha = light.value)
                }
                free.forEach { n -> drawFreeDoor(n, t.free) }

                // The pressed door, brightened while a finger is on it. Drawn
                // over the light rather than instead of it, and in white
                // rather than in more amber: amber says *this door is yours*,
                // and a door does not become more yours for being touched.
                pressed?.let { n ->
                    clipPath(doorPath(n, size.width, size.height)) {
                        drawRect(Color.White.copy(alpha = 0.20f))
                    }
                }
            }
        }
    }
}
