package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The `.lg` material — the nav bar and the app-bar beads.
 *
 * Four things make it, and the port had two of them:
 *
 * 1. **The body.** A 157° gradient, brightest at the top-left corner.
 * 2. **The specular bloom** — `.lg::before`, a radial highlight hanging off
 *    the top-left corner at `126% 74% at 20% -14%`. This was missing
 *    entirely, and it is most of what makes the material look lit rather
 *    than tinted.
 * 3. **A lip on all four edges**, each a different strength: the top is a
 *    catch of light at 40%, the sides much weaker, the bottom weaker still.
 *    The port drew one flat 1px ring at a single alpha the whole way round.
 * 4. **The rim.** Not a border — `liquid.js` is explicit that a hard inset
 *    ring "was the chalk". It is a specular arc that reaches zero at both
 *    ends, so about three quarters of the perimeter carries no line at all.
 *
 * The refraction in `liquid.js` — the SDF displacement that bends the
 * backdrop at the rim — is **not here yet**. It needs a runtime shader, and
 * that is API 33; this is the material underneath it.
 */

/** How far off the surface a pane sits. `.lg` sits in the page; `.lg-pop`
 * floats over it and gets three stacked shadows rather than two. */
enum class Lift { IN_PAGE, POPPED }

@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp),
    pop: Boolean = false,
    backdrop: BackdropState? = null,
    content: @Composable BoxScope.() -> Unit,
) = Glass(
    modifier = modifier,
    shape = shape,
    lift = if (pop) Lift.POPPED else Lift.IN_PAGE,
    backdrop = backdrop,
    bloom = Bloom.PANE,
    content = content,
)

/** A small round bead — a corner control in the app bar. The whole thing is
 * edge, so the lip carries almost all of the read and the bloom is tighter
 * and much brighter (48% against the pane's 30%). */
@Composable
fun GlassBead(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    backdrop: BackdropState? = null,
    content: @Composable BoxScope.() -> Unit,
) = Glass(
    modifier = modifier,
    shape = CircleShape,
    lift = Lift.IN_PAGE,
    backdrop = backdrop,
    bloom = Bloom.BEAD,
    onClick = onClick,
    // A round button centres what is in it. A Box defaults to its top-left
    // corner instead, so every bead in the app bar has been drawing its icon
    // up in the corner of a 30dp circle rather than in the middle of it —
    // which is the whole of "the top left and top right icons are awkwardly
    // offset". The same fault as the nav's tabs, in a different component.
    contentAlignment = Alignment.Center,
    content = content,
)

/** The two bloom geometries, straight off `.lg::before` and `.lg-bead::before`. */
private enum class Bloom(
    val rx: Float, val ry: Float, val cx: Float, val cy: Float,
    val peak: Float, val mid: Float?, val midStop: Float, val end: Float,
) {
    /** `radial-gradient(126% 74% at 20% -14%, .30, .06 42%, transparent 62%)` */
    PANE(1.26f, 0.74f, 0.20f, -0.14f, 0.30f, 0.06f, 0.42f, 0.62f),

    /** `radial-gradient(84% 60% at 30% 0%, .48, transparent 58%)` */
    BEAD(0.84f, 0.60f, 0.30f, 0.00f, 0.48f, null, 0f, 0.58f),
}

@Composable
private fun Glass(
    modifier: Modifier,
    shape: Shape,
    lift: Lift,
    backdrop: BackdropState?,
    bloom: Bloom,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    // The app's scheme, not the phone's. With the in-app toggle these
    // disagree, and a dark-recipe pane over a pale ground is invisible.
    val t = LocalLockerTokens.current
    val dark = t.dark

    // `rdev/liquid-glass-react`, at its defaults — the material Ryan named.
    //
    // Two stacked bands rather than one fade. Each runs transparent at both
    // ends and peaks in the middle third, so the sheen is a BAND crossing the
    // pane, not a wash pouring off one corner. That is the difference a
    // linear fade cannot make: a fade says "lit from over there", a band says
    // "this is a curved surface catching one light". The mock-up's own `.lg`
    // is the fade, and it is why the pane read as flat.
    //
    // Numbers from src/index.tsx 525-556, with the mouse terms at rest.
    val bandLow = Brush.linearGradient(
        0.00f to Color.White.copy(alpha = 0.00f),
        0.33f to Color.White.copy(alpha = 0.12f),
        0.66f to Color.White.copy(alpha = 0.40f),
        1.00f to Color.White.copy(alpha = 0.00f),
        start = Offset.Zero,
        end = Offset(0f, 900f),
    )
    val bandHigh = Brush.linearGradient(
        0.00f to Color.White.copy(alpha = 0.00f),
        0.33f to Color.White.copy(alpha = 0.32f),
        0.66f to Color.White.copy(alpha = 0.60f),
        1.00f to Color.White.copy(alpha = 0.00f),
        start = Offset.Zero,
        end = Offset(900f, 0f),
    )

    Box(
        modifier = modifier
            .glassShadow(dark, lift, shape)
            .clip(shape)
            .then(if (backdrop != null) Modifier.backdropBlur(backdrop, shape) else Modifier)
            // Nothing between the blur and the white film when the blur is
            // real. The material is `background` plus `backdrop-filter` and
            // that is all it is; the CSS has no dark layer anywhere in it.
            //
            // There used to be one: the ground colour, near-black, at 55%.
            // With the blur working that is a smoked pane rather than a
            // frosted one - the light the film is supposed to carry was being
            // painted out from underneath before the film went on. It is the
            // difference between glass and a tinted window, and it is why the
            // bar read as a dark slab with a lit border.
            //
            // Without a blur there is no glass to have. The scrim then is a
            // safety, not a material: a pane you can read a word through is a
            // window, and the word COLLECTED could be read straight through
            // the nav bar on the build that shipped.
            .then(
                if (backdrop?.working == true) Modifier
                else Modifier.background(t.ground.copy(alpha = 0.94f), shape)
            )
            // The two bands are faint on their own and are meant to be: at
            // rest this material is mostly the refraction and the edge. They
            // are drawn at a fraction on a dark ground, where white at 40%
            // across a whole pane would be a headlight.
            .background(bandLow, shape, alpha = if (dark) 0.35f else 0.65f)
            .background(bandHigh, shape, alpha = if (dark) 0.22f else 0.45f)
            .drawWithContent {
                bloom(bloom, dark)
                drawContent()
                lip(dark)
                rim(dark, shape)
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * `.lg::before` — the bloom hanging off the top-left corner.
 *
 * CSS radial gradients are ellipses and Compose's are circles, so the canvas
 * is squashed on Y and a circle is drawn into it. On the nav bar the two
 * radii are 411dp and 43dp; a circle at either one is not an approximation
 * of the other, it is a different shape.
 */
private fun DrawScope.bloom(b: Bloom, dark: Boolean) {
    // On a pale ground a white bloom has nothing to be brighter than.
    val gain = if (dark) 1f else 0.45f
    val rx = b.rx * size.width
    val ry = b.ry * size.height
    if (rx <= 0f || ry <= 0f) return

    val stops = buildList {
        add(0f to Color.White.copy(alpha = b.peak * gain))
        b.mid?.let { add(b.midStop to Color.White.copy(alpha = it * gain)) }
        add(b.end to Color.Transparent)
        add(1f to Color.Transparent)
    }.toTypedArray()

    val centre = Offset(b.cx * size.width, b.cy * size.height)
    withTransform({
        scale(1f, ry / rx, pivot = centre)
    }) {
        drawCircle(
            brush = Brush.radialGradient(*stops, center = centre, radius = rx),
            radius = rx,
            center = centre,
        )
    }
}

/**
 * The four inset edges, each its own strength.
 *
 * Dark: top .40, left .10, right .06, bottom .05 — a catch of light on top
 * and almost nothing elsewhere. Light mode is not the same recipe at lower
 * alpha: a white lip on a pale ground has nothing to be brighter than, so
 * the top goes to solid white and the *bottom* carries the thickness as a
 * shade instead.
 */
private fun DrawScope.lip(dark: Boolean) {
    val px = 1.dp.toPx()
    val w = size.width
    val h = size.height
    if (dark) {
        drawRect(Color.White.copy(alpha = 0.40f), Offset.Zero, Size(w, px))
        drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(px, h))
        drawRect(Color.White.copy(alpha = 0.06f), Offset(w - px, 0f), Size(px, h))
        drawRect(Color.White.copy(alpha = 0.05f), Offset(0f, h - px), Size(w, px))
    } else {
        drawRect(Color.White, Offset.Zero, Size(w, px))
        drawRect(Color(0xFF091620).copy(alpha = 0.06f), Offset(0f, h - px), Size(w, px))
    }
}

/**
 * The rim — a specular arc, not a border.
 *
 * `liquid.js` rejected the drawn ring outright: "no hard inset ring at all —
 * that was the chalk". What Apple's edge actually is, is a highlight bright
 * along the arc the light catches and gone on the opposite side. So this is
 * a sweep that reaches zero at both ends, lit from the top-left where the
 * bloom's light source is, leaving about three quarters of the perimeter
 * with no line on it at all.
 */
private fun DrawScope.rim(dark: Boolean, shape: Shape) {
    val px = 1.dp.toPx()
    val peak = if (dark) 0.55f else 0.95f
    val ring = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@rim)) }
    val sweep = Brush.sweepGradient(
        0.00f to Color.White.copy(alpha = peak * 0.20f),
        0.10f to Color.White.copy(alpha = peak),
        0.28f to Color.White.copy(alpha = peak * 0.35f),
        0.45f to Color.Transparent,
        0.80f to Color.Transparent,
        1.00f to Color.White.copy(alpha = peak * 0.20f),
        center = Offset(size.width * 0.20f, 0f),
    )
    drawPath(ring, brush = sweep, style = Stroke(width = px))
}

/**
 * `.lg` gets two shadows, `.lg-pop` three — and in light mode they are the
 * ink's own colour rather than black, because black on a pale steel ground
 * goes grey and muddy where a tinted shade stays clean.
 */
private fun Modifier.glassShadow(dark: Boolean, lift: Lift, shape: Shape): Modifier {
    val tint = if (dark) Color.Black else Color(0xFF091620)
    val (elevation, alpha) = when {
        lift == Lift.POPPED && dark -> 18.dp to 0.50f
        lift == Lift.POPPED -> 18.dp to 0.20f
        dark -> 10.dp to 0.44f
        else -> 10.dp to 0.18f
    }
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = tint.copy(alpha = alpha),
        spotColor = tint.copy(alpha = alpha),
    )
}
