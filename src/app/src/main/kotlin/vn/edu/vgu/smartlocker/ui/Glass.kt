package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The cabinet, behind everything.
 *
 * A single soft light falling from the upper left across painted metal. It
 * is nearly invisible on its own, and that is the point: it exists so the
 * card above it has something to float over. Glass laid on flat grey reads
 * as a rectangle.
 */
@Composable
fun LockerBackdrop(content: @Composable BoxScope.() -> Unit) {
    val dark = isSystemInDarkTheme()
    val ground = MaterialTheme.colorScheme.background

    // Two stops, both within a few percent of the ground. Any more and it
    // stops being light on metal and becomes a decorative gradient.
    val lit = if (dark) Color.White.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.55f)
    val shade = if (dark) Color.Black.copy(alpha = 0.22f) else Color(0x1A0B1620)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ground)
            .background(
                Brush.linearGradient(
                    colors = listOf(lit, Color.Transparent, shade),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            ),
        content = content,
    )
}

/**
 * The one glass plane in the app.
 *
 * It carries the waiting parcel and nothing else. Glass used on every
 * container is a texture; used once, it says *this is the thing you came
 * for*.
 *
 * Three parts make it read as a thick slab rather than a pale rectangle: a
 * shadow with a real offset and blur, a body that is lighter at the top than
 * the bottom, and a hairline along the top edge where the light catches.
 * Drop any one of them and it flattens.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val shape = MaterialTheme.shapes.extraLarge

    // Opaque on the light ground. A translucent body lets the elevation
    // shadow show through the card and paints a hard-edged plate inside it.
    // Against pale steel a translucent white reads as white anyway, so the
    // transparency bought nothing and cost the artifact.
    val body = if (dark) {
        listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f))
    } else {
        listOf(Color.White, Color(0xFFEFF4F8))
    }

    // The specular edge, brighter at the top where a light from above would
    // catch. A gradient brush on a 1dp border renders as a visible inner
    // plate on some devices, so this is a flat stroke and the body gradient
    // carries the fall-off.
    val edge = if (dark) Color.White.copy(alpha = 0.16f) else Color.White

    Column(
        modifier = modifier
            .shadow(
                elevation = if (dark) 4.dp else 24.dp,
                shape = shape,
                ambientColor = Color(0xFF0B1620),
                spotColor = Color(0xFF0B1620),
            )
            .background(Brush.verticalGradient(body), shape)
            .border(1.dp, edge, shape)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
