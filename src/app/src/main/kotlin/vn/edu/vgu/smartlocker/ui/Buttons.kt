package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The one primary control: full width, at least 52dp tall, filled with the
 * accent-deep and shaded by a fixed overlay — never a gradient built from
 * tokens, which would snap on a theme change.
 *
 * The shadow is neutral and offset. A shadow tinted with the button's own
 * colour is a glow, and a glow under a large saturated slab is what makes it
 * read as a web control rather than as a key about to be pressed.
 */
@Composable
fun GoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = LocalLockerTokens.current
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (enabled) {
                    Modifier
                        // Shadow first. Mid-chain it became a layer over the
                        // fill, and its dark rim read as a lighter panel
                        // inset inside the button — on every primary control
                        // in the app.
                        .shadow(
                            elevation = 6.dp,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.55f),
                            spotColor = Color.Black.copy(alpha = 0.55f),
                        )
                        .clip(shape)
                        .background(t.accentDeep)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.26f), shape)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                        .clip(shape)
                        .background(t.accentDeep.copy(alpha = 0.36f))
                }
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            trailing?.invoke()
        }
    }
}

/** The quiet back door under a primary control. Not a second button. */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val t = LocalLockerTokens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) t.ink3 else t.ink3.copy(alpha = 0.5f),
        )
    }
}

/**
 * The outlined alternative — the way out of a dead end. A disabled primary
 * states the problem and offers nothing; a live outlined one gives the next
 * real place to go.
 */
@Composable
fun AltButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .border(1.dp, t.hair, shape)
            .background(
                Brush.verticalGradient(listOf(t.lip.copy(alpha = 0.4f), Color.Transparent)),
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = t.ink,
        )
    }
}

/**
 * The free ways in, as one rank rather than two more choices: paired and
 * outlined, they read as one alternative at a glance.
 */
@Composable
fun SocialButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val t = LocalLockerTokens.current
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(1.dp, t.hair, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box { leading() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = t.ink,
        )
    }
}
