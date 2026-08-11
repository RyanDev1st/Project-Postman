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

/** The ground, with the accent light falling from above the app bar. */
@Composable
fun LockerBackdrop(content: @Composable BoxScope.() -> Unit) {
    val t = LocalLockerTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(t.accent.copy(alpha = 0.14f), Color.Transparent, Color.Transparent),
                    startY = 0f,
                    endY = 900f,
                ),
            )
            .background(t.ground),
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
            .background(t.field, shape)
            .background(
                Brush.verticalGradient(
                    listOf(t.recessIn, Color.Transparent, Color.Transparent, t.recessLit),
                    startY = 0f,
                    endY = 200f,
                ),
                shape,
            ),
        content = content,
    )
}

/**
 * Floating chrome — the nav bar, the app-bar buttons.
 *
 * Four layers, and dropping any one kills the effect: the body lighter at
 * the top than the bottom, an inner lip where the light catches, a hairline
 * edge, and an outer shadow with a real offset. Light mode is not the same
 * material with different numbers: on a pale ground a white lip has nothing
 * to be brighter than, so the shade under the bottom edge carries the
 * thickness instead.
 */
@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
    pop: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // The app's scheme, not the phone's. With the in-app toggle these
    // disagree, and a dark-recipe pane (white at 2-15% alpha) over a pale
    // ground is invisible — the nav read as completely transparent.
    val dark = LocalLockerTokens.current.dark
    val body = if (dark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.055f),
                Color.White.copy(alpha = 0.02f),
            ),
            start = Offset.Zero,
            end = Offset(900f, 400f),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.68f),
                Color.White.copy(alpha = 0.55f),
            ),
            start = Offset.Zero,
            end = Offset(900f, 400f),
        )
    }
    val edge = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.9f)
    val shadow = if (dark) Color.Black.copy(alpha = 0.44f) else Color(0x20091A20)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (pop) 14.dp else 8.dp,
                shape = shape,
                ambientColor = shadow,
                spotColor = shadow,
            )
            .background(body, shape)
            .border(1.dp, edge, shape),
        content = content,
    )
}

/** A small round glass bead — a corner control in the app bar. */
@Composable
fun GlassBead(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalLockerTokens.current
    val dark = t.dark
    Box(
        modifier = modifier
            // Shadow first, so it is cast behind the bead. Last in the chain
            // it became a layer drawn over the fill, which put a dark blob
            // across the icon — the back arrow and the menu lines both.
            .shadow(
                elevation = 4.dp,
                shape = androidx.compose.foundation.shape.CircleShape,
                ambientColor = if (dark) Color.Black.copy(alpha = 0.44f) else Color(0x20091A20),
                spotColor = if (dark) Color.Black.copy(alpha = 0.44f) else Color(0x20091A20),
            )
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                if (dark) {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.055f),
                            Color.White.copy(alpha = 0.02f),
                        ),
                        start = Offset.Zero,
                        end = Offset(120f, 60f),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.68f)),
                        start = Offset.Zero,
                        end = Offset(120f, 60f),
                    )
                }
            )
            .border(1.dp, t.glassEdge, androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick),
        content = content,
    )
}
