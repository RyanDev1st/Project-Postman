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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
@Composable
fun LockerBackdrop(content: @Composable BoxScope.() -> Unit) {
    val t = LocalLockerTokens.current
    val curtain = LocalThemeCurtain.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(t.ground)
            .background(
                Brush.verticalGradient(
                    listOf(t.accent.copy(alpha = 0.14f), Color.Transparent, Color.Transparent),
                    startY = 0f,
                    endY = 900f,
                ),
            )
            .drawWithContent {
                if (curtain.fraction > 0f && curtain.color != Color.Transparent) {
                    // Origin top: the bottom edge is what moves, upward.
                    drawRect(
                        color = curtain.color,
                        size = size.copy(height = size.height * curtain.fraction),
                    )
                }
                drawContent()
            },
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
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = t.shadow,
                spotColor = t.shadow,
            )
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
            .background(Brush.verticalGradient(listOf(t.surface, t.surface2)), shape)
            .border(1.dp, t.hair, shape)
            .background(
                Brush.verticalGradient(
                    listOf(t.lip, Color.Transparent),
                    startY = 0f,
                    endY = 1.5f,
                ),
                shape,
            ),
        content = content,
    )
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
                // thrown a short way in from the top edge, and it is SHORT:
                // 1.5dp of offset plus 3dp of blur, so 4.5dp in total.
                //
                // This used to be a gradient run to a fixed `endY = 200f`,
                // which is not a length the design ever mentions and is not a
                // length at all on a box of a different height: on a tall
                // recess the shade never finished, and on a 44dp settings
                // field it was cut off part-way. Either way the edge read as
                // a soft wash across the whole control rather than as a lip
                // it is set down behind.
                val depth = (1.5f + 3f).dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to t.recessIn,
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = depth,
                    ),
                    size = Size(size.width, depth),
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
