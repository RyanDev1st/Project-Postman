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
import androidx.compose.ui.text.style.TextDecoration
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
    // Which way the light has to fall. The slab is near-white in the dark
    // scheme and near-black in the light one, so a fixed white top highlight
    // is a highlight on one and nothing at all on the other. Either way the
    // light comes from above: a pale slab is shaded at its foot, a dark one
    // is caught along its head.
    val pale = t.dark
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
                        // The light catches the top eighth and stops.
                        //
                        // It used to run the full 52dp - white at 22% down to
                        // nothing - which on a near-black slab lifted the head
                        // by twenty points of value. That is not light falling
                        // on a face, it is a gradient painted onto one, and it
                        // is the single loudest thing the old screens did.
                        .background(
                            if (pale) Brush.verticalGradient(
                                0.86f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.07f),
                            ) else Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0.10f),
                                0.14f to Color.Transparent,
                            )
                        )
                        .border(
                            1.dp,
                            if (pale) Color.Black.copy(alpha = 0.07f)
                            else Color.White.copy(alpha = 0.12f),
                            shape,
                        )
                        .clickable(onClick = onClick)
                } else {
                    // Off, and it has to look off.
                    //
                    // This was the primary's own fill at 36% under a white
                    // label, which on the sign-in screen is a solid pill
                    // sitting exactly where the live button sits. Ryan read
                    // it as pressable and it is not. A control that cannot be
                    // used gives up the fill and keeps only the outline.
                    Modifier
                        .clip(shape)
                        .background(t.field)
                        .border(1.dp, t.hair, shape)
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
                color = if (enabled) t.onAccent else t.ink3,
            )
            trailing?.invoke()
        }
    }
}

/**
 * The quiet back door under a primary control. Not a second button.
 *
 * Quiet, and still obviously a control. It was `ink3` body text under a large
 * filled button and read as a caption — on the Cabinet tab that caption is
 * *Use a code*, the way out for somebody whose camera will not focus, which
 * is to say the person already having trouble. Colour cannot carry that here:
 * the accent is now the end of the value scale rather than a hue, so a link
 * tinted with it is just text. The underline does the work instead, which is
 * what an underline has always been for.
 */
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
            ),
            color = if (enabled) t.ink2 else t.ink3.copy(alpha = 0.5f),
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
            // A hairline and nothing else. The sheen that used to sit
            // inside it belongs to the slab above, which is the button you
            // are meant to press; wearing it too, the quiet way out was
            // shouting the same volume.
            .border(1.dp, t.hair, shape)
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
