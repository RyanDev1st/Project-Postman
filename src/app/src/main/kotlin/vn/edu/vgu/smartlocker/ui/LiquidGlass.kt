package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The glass — the nav bar and the app-bar beads.
 *
 * Ported from `rdev/liquid-glass-react` at its defaults, which is the material
 * Ryan named and the one its own demo shows. Four parts, and only four:
 *
 * 1. **A blurred, saturated copy of what is behind it** — [backdropBlur].
 * 2. **A lens over that copy**, bending it at the edges — [Refraction].
 * 3. **A rim**, two masked gradient rings — [rim].
 * 4. **A drop shadow** — [glassShadow].
 *
 * There is no fifth. Nothing is painted on the face: the library's glass
 * element computes `background: rgba(0, 0, 0, 0)` and its whole appearance is
 * what 1 and 2 let through. Earlier passes here had a 157° body gradient, a
 * corner bloom and two white bands across the face, all lifted from the
 * mock-up's `.lg`. Every one of them was a sheet of white over the only thing
 * worth seeing, and together they were what "super frosty and cheap" meant.
 *
 * The one thing that does sit on the face is the press highlight, and it is
 * transparent until a finger is down.
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

/**
 * The press highlight, `radial-gradient(circle at 50% 0%, ...)` — index.tsx
 * 562-607. Drawn only while a finger is down, and only on a control that has
 * something to press. [PANE] therefore goes unused today, because no pane is
 * clickable; it is kept because the geometry belongs with its sibling and a
 * pane that gains a tap will want it.
 */
private enum class Bloom(
    val rx: Float, val ry: Float, val cx: Float, val cy: Float,
    val peak: Float, val mid: Float?, val midStop: Float, val end: Float,
) {
    /**
     * `radial-gradient(circle at 50% 0%, rgba(255,255,255,.5), transparent 50%)`
     *
     * Top **centre**, not the top-left corner. The mock-up put the light at
     * 20% -14%, off the corner, and a corner light on a 300dp-wide bar lands
     * as a bright patch over the first tab with the other two in shade — it
     * reads as a stain on the glass rather than as glass. One light straight
     * above the middle is symmetrical, which is what makes a bar of three
     * equal tabs look like one object.
     */
    PANE(1.00f, 0.62f, 0.50f, 0.00f, 0.22f, 0.06f, 0.24f, 0.50f),

    /**
     * The same light, tighter and brighter — a bead is nearly all edge, so
     * almost none of its face is left to carry a gradient.
     * `radial-gradient(circle at 50% 0%, rgba(255,255,255,1), transparent 80%)`
     */
    BEAD(0.90f, 0.75f, 0.50f, 0.00f, 0.72f, 0.24f, 0.34f, 0.80f),
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

    // `scale(0.96)` while held — index.tsx 444. The library's own indication,
    // and it has no ripple, so this one has none either.
    val presses = remember { MutableInteractionSource() }
    val pressed by presses.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.96f else 1f,
        animationSpec = tween(200),
        label = "press",
    )

    Box(
        modifier = modifier
            .then(
                if (onClick == null) Modifier
                else Modifier.graphicsLayer { scaleX = squash; scaleY = squash }
            )
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
            //
            // How heavy the scrim is depends on what the pane is over, and
            // the two are not the same risk. A pane floats over the page and
            // a word read through it is a failure. A bead sits in the app
            // bar, where there is nothing behind it but ground - the content
            // starts below the bar and never runs under it - so the same
            // scrim buys no safety at all and costs the bead its whole
            // material. At 94% every corner control was a black disc with a
            // lit rim, which is what "the 2 UI elements on the top left and
            // right - all bad" was describing.
            .then(
                if (backdrop?.working == true) Modifier
                else Modifier.background(
                    t.ground.copy(alpha = if (bloom == Bloom.BEAD) 0.30f else 0.94f),
                    shape,
                )
            )
            // No gradient on the face. Not a wash, not a band, not a bloom.
            //
            // This is the correction, and it is a structural one rather than a
            // number. `liquid-glass-react` has two white gradients and this
            // port had both of them — but painted across the whole pane. In
            // the library they are two `<span>` overlays carrying
            // `padding: 1.5px` and `maskComposite: exclude`, which masks out
            // everything except the border. The gradients are the RIM. The
            // face gets nothing: the library's own glass element computes to
            // `background: rgba(0, 0, 0, 0)`.
            //
            // A gradient laid over the face is the definition of frost, and it
            // hides the one thing this material is for — the backdrop seen
            // through it. Lowering the alphas made it a fainter frost. Moving
            // them to the rim makes it glass. See [rim].
            //
            // What is left is a flat veil, and it is a **deliberate departure
            // from the library**, so it is worth being clear why.
            //
            // The library never needs one: every pane in its demo sits on a
            // colour photograph, so blur and saturation alone give it a body.
            // This app's nav bar sits at the foot of the screen, and
            // [lockerGround] runs its light out to 900px — the bar is at 1450.
            // What is behind it is one flat near-black, and blurring a flat
            // colour returns the same flat colour. Ported exactly, the bar
            // came out as a hairline ring around nothing.
            //
            // Apple's own dark material does lift over dark content; the
            // library gestures at the same idea from the other side with
            // `overLight`, which darkens over bright content. The figure is
            // read off that behaviour rather than picked: ground is about RGB
            // 12, an ultra-thin dark material lands near 28, and
            // 12 + a(255 - 12) = 28 gives 0.07.
            //
            // Flat, not graded. A flat veil says the glass is faintly milky.
            // A graded one says somebody painted a highlight on it, and that
            // is the difference the last three builds kept getting wrong.
            .background(
                if (dark) Color.White.copy(alpha = 0.07f)
                else Color.Black.copy(alpha = 0.10f),
                shape,
            )
            .drawWithContent {
                drawContent()
                // index.tsx 562-607: the radial highlight exists only while
                // the control is hovered or held. At rest its opacity is 0.
                if (onClick != null && pressed) bloom(bloom, dark)
                rim(dark, shape)
            }
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(
                    interactionSource = presses,
                    indication = null,
                    onClick = onClick,
                )
            ),
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
 * The rim — and in this material the rim is nearly the whole component.
 *
 * `liquid-glass-react` builds it out of two overlay spans, index.tsx 509-559.
 * Each is the size of the pane, each carries `padding: 1.5px` with
 * `maskComposite: exclude`, and that mask is what makes them rings: the
 * middle is cut out and only a pixel and a half at the edge survives. Both
 * carry the same two inset shadows, and both are filled with a diagonal white
 * gradient. They differ in how they blend:
 *
 *     layer 1   mix-blend-mode: screen    opacity 0.2   stops .00 .12 .40 .00
 *     layer 2   mix-blend-mode: overlay   opacity 1.0   stops .00 .32 .60 .00
 *
 * The gradient is `135deg`, so its axis runs top-left to bottom-right, and
 * both peaks sit past the middle of it. Around a ring that puts two bright
 * arcs on opposite flanks with the two diagonal corners dark — which is what a
 * curved edge does under one light, and it is the single most recognisable
 * thing about this material. A ring at one flat alpha is a drawn border.
 *
 * The blend modes are ported rather than approximated, because they are the
 * reason the rim is quiet here and blazing in the library's own demo. `screen`
 * and `overlay` both read the backdrop: over the demo's colour photograph they
 * bloom, over our near-black ground they stay a suggestion. That is the
 * material behaving correctly, not the port failing.
 *
 * Under the two gradient rings, unchanged, the pair of inset shadows that both
 * spans carry: `0 0 0 0.5px rgba(255,255,255,.5) inset` and
 * `0 1px 3px rgba(255,255,255,.25) inset`. A **complete** ring — not an arc.
 *
 * This reverses a decision. The previous edge was a sweep lit from the
 * top-left that fell to nothing across about three quarters of the perimeter,
 * on the mock-up's argument that a full ring is "chalk" and that a real
 * specular highlight is bright on one side only. That is true of a polished
 * curved solid. It is not true of a thin sheet with a *cut edge*, which is
 * what this material is: the cut catches light all the way round and the
 * gradient across the face says which way the sheet is tilted. Fading three
 * quarters of it away left the pane with no perceptible boundary along its
 * bottom and right, which is a large part of why it read as a smudge rather
 * than as an object with a shape.
 *
 * Half a pixel, so it stays a hairline at any density and never becomes a
 * drawn border.
 */
private fun DrawScope.rim(dark: Boolean, shape: Shape) {
    val ring = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@rim)) }
    val hair = if (dark) 0.50f else 0.85f

    // The soft return, first and underneath: a white glow thrown down from
    // the inside of the top edge. It is what gives the hairline something to
    // sit on, and without it the ring reads as a drawn outline.
    clipPath(ring) {
        val depth = (1f + 3f).dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (dark) 0.14f else 0.42f),
                1f to Color.Transparent,
                startY = 0f,
                endY = depth,
            ),
            size = Size(size.width, depth),
        )
    }

    drawPath(
        ring,
        color = Color.White.copy(alpha = hair),
        style = Stroke(width = 0.5.dp.toPx()),
    )

    // The two gradient rings. Stroked at twice their width and clipped to the
    // outline, so what is left is 1.5dp lying inside the edge — which is what
    // `padding: 1.5px` plus an excluded mask leaves.
    val band = Stroke(width = 3.dp.toPx())
    clipPath(ring) {
        drawPath(ring, brush = diagonal(0.12f, 0.40f), style = band, alpha = 0.2f, blendMode = BlendMode.Screen)
        drawPath(ring, brush = diagonal(0.32f, 0.60f), style = band, blendMode = BlendMode.Overlay)
    }
}

/**
 * CSS `linear-gradient(135deg, ...)` over this pane, as a Compose brush.
 *
 * The angle is not the corner-to-corner diagonal. CSS runs the line at exactly
 * 135° and then makes it long enough that the two far corners project onto its
 * ends, which is `(w + h) * cos(45°)`. On a nav bar 358dp by 59dp the two are
 * nowhere near each other, and using the corners instead would tilt the whole
 * lighting by about forty degrees.
 */
private fun DrawScope.diagonal(mid: Float, peak: Float): Brush {
    val reach = (size.width + size.height) / 4f
    val centre = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        0.00f to Color.White.copy(alpha = 0f),
        0.33f to Color.White.copy(alpha = mid),
        0.66f to Color.White.copy(alpha = peak),
        1.00f to Color.White.copy(alpha = 0f),
        start = Offset(centre.x - reach, centre.y - reach),
        end = Offset(centre.x + reach, centre.y + reach),
    )
}

/**
 * One shadow, wide and soft: `0 12px 40px rgba(0,0,0,.25)`.
 *
 * The mock-up stacks two on `.lg` and three on `.lg-pop`, at up to 60% black.
 * Stacked dark shadows under a pane whose whole job is to let light through
 * put a bruise on the ground beneath it, and on a near-black ground that
 * bruise is the most visible thing about the component. The liquid-glass
 * shadow is a single wide fall at a quarter black — enough to say the pane is
 * off the surface, not enough to draw attention to itself.
 *
 * A popped pane keeps a deeper one, because it genuinely is further off.
 * Light mode tints rather than blackens: black on a pale steel ground goes
 * grey and muddy where a tinted shade stays clean.
 */
private fun Modifier.glassShadow(dark: Boolean, lift: Lift, shape: Shape): Modifier {
    val tint = if (dark) Color.Black else Color(0xFF091620)
    val (elevation, alpha) = when {
        lift == Lift.POPPED && dark -> 20.dp to 0.34f
        lift == Lift.POPPED -> 20.dp to 0.16f
        dark -> 12.dp to 0.25f
        else -> 12.dp to 0.12f
    }
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = tint.copy(alpha = alpha),
        spotColor = tint.copy(alpha = alpha),
    )
}
