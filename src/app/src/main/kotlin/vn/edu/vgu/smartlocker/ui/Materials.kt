package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * Three materials, three jobs (`docs/designs/mockup/screens.css`).
 *
 * - [GlassPane] floats **over** content — the nav bar, the app-bar buttons.
 * - [CardMaterial] is a plane lifted **off** the ground — grouped rows, the
 *   profile, the claim ticket.
 * - [Recess] is set **into** the ground — fields, code boxes, number chips.
 *
 * Glass was the only material this app had, and glass floats; everything that
 * was not floating chrome had to be a card or nothing, which is why a field,
 * a number chip and an icon well all looked like the same flat grey square
 * with a hairline round it. The two additions are the design's own.
 */

/**
 * The ground, with the accent light falling from above the app bar, and the
 * theme curtain between the two.
 *
 * Order matters twice here. The ground is laid **first** and the wash goes
 * on top of it — the other way round the opaque ground painted the wash out
 * completely and the screens had no light on them at all.
 *
 * Then the curtain, above the ground and below everything else, which is
 * where [ThemeWipe] needs it: the ground wipes while every card and letter
 * stays put on top and recolours in place.
 */
/**
 * The ground and the light on it, as a modifier.
 *
 * Extracted because two things have to paint it, and they must not disagree.
 * [LockerBackdrop] paints it under every screen — and the glass has to sample
 * it too. A pane samples a recorded layer, and the ground was painted by a
 * parent *outside* that recording, so what the nav bar sampled was a
 * transparent sheet with a few widgets floating on it. Blur a transparent
 * sheet and you get a transparent sheet: the bar had no material at all and
 * the page showed through it, sharp.
 *
 * Painting it twice costs one rectangle. It only makes the two copies
 * identical if they agree about where the top of the page is, and [topInWindow]
 * is how they do.
 *
 * They did not agree, and it was visible. [LockerBackdrop] covers the whole
 * window, while the shell's copy sits inside the 17dp gutter and the system
 * bar inset — about 78px lower. Both started their light at their own y = 0,
 * so inside the gutter the page carried two lights instead of one and came out
 * warmer than the margin around it. That is the square over the top bar: a
 * rectangle from x 34 to 746 and y 78 to 166, warmer by four or five counts.
 * It shows at the top and nowhere else because the light has run out by 900px,
 * and it is the accent colour, so it reads orange in light and blue in dark.
 *
 * Pass the copy's distance from the top of the window and its light picks up
 * exactly where the one underneath it had got to.
 */
@Composable
fun Modifier.lockerGround(topInWindow: Float = 0f): Modifier {
    val t = LocalLockerTokens.current
    val curtain = LocalThemeCurtain.current
    val windowHeight = LocalWindowInfo.current.containerSize.height.toFloat()

    return this
        .background(t.ground)
        .background(
            Brush.verticalGradient(
                listOf(t.accent.copy(alpha = 0.14f), Color.Transparent, Color.Transparent),
                startY = -topInWindow,
                endY = 900f - topInWindow,
            ),
        )
        // The theme curtain belongs to the ground, and to every copy of it.
        //
        // It used to be drawn once, by [LockerBackdrop], underneath
        // everything. That worked while the ground was painted once. It stopped
        // working when the shell began painting its own copy so the glass had
        // something to sample: the shell's copy carries the NEW colour and is
        // drawn on top, so it painted the curtain out everywhere except the
        // 17dp margin. The page inside snapped instantly while a thin frame
        // around it wiped — the override over the top bar.
        //
        // Drawn here it is part of the ground by construction, so both copies
        // carry it and neither can cover the other's.
        //
        // Measured in the WINDOW, not in this node, for the same reason the
        // light above is: two copies at different heights would otherwise wipe
        // at different rates and show a step where they meet.
        .drawBehind {
            if (curtain.fraction <= 0f || curtain.color == Color.Transparent) return@drawBehind
            if (windowHeight <= 0f) return@drawBehind
            val covered = windowHeight * curtain.fraction
            val topInWin = if (curtain.fromTop) 0f else windowHeight - covered
            drawRect(
                color = curtain.color,
                topLeft = Offset(0f, topInWin - topInWindow),
                size = Size(size.width, covered),
            )
        }
}

@Composable
fun LockerBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .lockerGround(),
        content = content,
    )
}

/**
 * A plane lifted off the ground.
 *
 * Depth in the design's order: **edge first, shadow second, fill last**. A
 * hairline ring and a lit top edge carry the read; the shadow has a real
 * offset; the fill is a picked surface, lighter at the top than the bottom.
 */
@Composable
fun CardMaterial(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalLockerTokens.current
    Box(
        modifier = modifier
            // Shadow before the clip. The other way round the card clipped
            // away its own shadow, so every clickable card — each Settings
            // row, the map — sat flat on the ground with no lift at all.
            .drawBehind { cardShadow(shape, t.shadow) }
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
            // The colour that changes with the theme, and the shading that
            // does not, kept apart on purpose. A gradient between two theme
            // tokens cannot be interpolated, so on a theme change half the
            // card animates and half of it jumps. Fixed white and black
            // alphas over one flat surface colour never have that problem.
            .background(t.surface, shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.028f), Color.Black.copy(alpha = 0.05f)),
                ),
                shape,
            )
            .border(1.dp, t.hair, shape)
            // The lit top edge: `border-top-color` and `inset 0 1px 0 0`,
            // which is two lit pixels stacked, not one. Drawn after the
            // border so it replaces the hairline along the top rather than
            // sitting under it.
            .drawBehind {
                clipPath(Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }) {
                    drawRect(color = t.lip, size = Size(size.width, 2.dp.toPx()))
                }
            },
        content = content,
    )
}

/**
 * `0 10px 26px -18px` — and the third number is the one that matters.
 *
 * A negative spread shrinks the shape **before** it is blurred, so the shadow
 * ends up smaller than the card and tucked under it: visible as a soft seam at
 * the foot, nothing at the sides. `Modifier.shadow` cannot say that at all —
 * it takes an elevation and throws a symmetrical halo, which is a different
 * material. Hence a path drawn by hand, inset by the spread, offset down, and
 * blurred by a shadow layer under a transparent fill.
 */
/**
 * An `inset` box-shadow: cast by the edge, falling inward.
 *
 * Drawn the way the platform allows one at all — the shape is punched out of a
 * far larger rectangle with an even-odd fill, and it is that ring which casts
 * the shadow. Everything outside the shape is then clipped away, leaving only
 * the part that fell inside. A gradient cannot stand in for this: a gradient
 * runs one way, and an inset shadow comes in from all four edges at once, by a
 * different amount on each depending on where the offset pushed it.
 */
private fun DrawScope.innerShadow(shape: Shape, color: Color, dy: Float, blur: Float) {
    val outline = Path().apply {
        addOutline(shape.createOutline(size, layoutDirection, this@innerShadow))
    }
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-size.width, -size.height, size.width * 2f, size.height * 2f))
        addPath(outline)
    }
    clipPath(outline) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur / 2f, 0f, dy, color.toArgb())
            }
            canvas.nativeCanvas.drawPath(ring.asAndroidPath(), paint)
        }
    }
}

private fun DrawScope.cardShadow(shape: Shape, color: Color) {
    val spread = 18.dp.toPx()
    val w = size.width - 2 * spread
    val h = size.height - 2 * spread
    // A card shorter than 36dp has no shadow left once the spread is taken
    // off it. That is what the CSS does too, and it is not a fault.
    if (w <= 0f || h <= 0f) return

    val path = Path().apply {
        addOutline(shape.createOutline(Size(w, h), layoutDirection, this@cardShadow))
        translate(Offset(spread, spread))
    }
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            // CSS states a blur diameter; the platform wants a radius.
            setShadowLayer(26.dp.toPx() / 2f, 0f, 10.dp.toPx(), color.toArgb())
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

/**
 * Set into the ground — an input, a code box, a number chip.
 *
 * Two inset shadows and nothing else: the ground's own shade thrown inward
 * from the top edge, and a lit return along the bottom where the surface
 * comes back up. That is the honest version of soft UI — no outer shadow, no
 * fake extrusion. The light scheme swaps the shadows' roles rather than
 * reusing the dark numbers at a lower alpha.
 */
@Composable
fun Recess(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val t = LocalLockerTokens.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(t.field)
            .drawBehind {
                // `inset 0 1.5px 3px 0 var(--recess-in)` — the ground's shade
                // thrown in from the edge. From EVERY edge, which is the part
                // a gradient down from the top cannot do: the shadow's box is
                // dropped 1.5dp, so the top gets 1.5 + 3 of shade, the sides
                // get the blur alone, and the bottom is left almost clear.
                // That difference between the four edges is the whole read.
                // Shaded on the top only, the control looks like a card with
                // a dark lip; shaded on all four it is a dish.
                innerShadow(
                    shape = shape,
                    color = t.recessIn,
                    dy = 1.5f.dp.toPx(),
                    blur = 3f.dp.toPx(),
                )

                // `inset 0 -1px 0 0 var(--recess-lit)` — where the surface
                // comes back up. One pixel, no blur.
                val lit = 1.dp.toPx()
                drawRect(
                    color = t.recessLit,
                    topLeft = Offset(0f, size.height - lit),
                    size = Size(size.width, lit),
                )
            },
        content = content,
    )
}

// GlassPane and GlassBead moved to LiquidGlass.kt. The `.lg` material is four
// layers deep once its bloom and its specular rim are in, which is more than
// belongs beside the card and the recess.
