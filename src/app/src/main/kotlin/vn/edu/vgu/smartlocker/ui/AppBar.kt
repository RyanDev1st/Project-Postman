package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The app bar, arranged the way the draft arranges it: brand on the left,
 * actions on the right. The brand is NOT centred — a centred bar makes the
 * bar symmetrical, and a symmetrical bar gives the scan button no more weight
 * than the bell.
 */
@Composable
fun AppBar(
    brand: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val t = LocalLockerTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = brand,
                style = MaterialTheme.typography.titleLarge,
                color = t.ink,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** A small round glass bead — a corner control in the app bar. */
@Composable
fun AppBarBead(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: BackdropState? = null,
) {
    val t = LocalLockerTokens.current
    GlassBead(modifier = modifier.size(30.dp), onClick = onClick, backdrop = backdrop) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = t.ink2,
        )
    }
}

/**
 * The scan control in the bar — the one thing up here that carries the slab,
 * because it is the one thing up here that opens a box. Everything else in
 * the bar is quiet glass.
 *
 * **It says the word.** It was a 30dp circle with a bracket glyph in it, the
 * same size and the same shape as the bell beside it, and nothing on the
 * screen said what it did. Scanning the cabinet is how a parcel is collected;
 * a person who has never used this app has to be able to find it without
 * pressing things to see what they are. The label costs 34dp of a bar that
 * has the room.
 */
@Composable
fun ScanButton(
    icon: ImageVector,
    label: String,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    val shape = RoundedCornerShape(999.dp)
    // A small control, so it takes a smaller press than the slab does.
    val press = rememberPress(target = 0.94f)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
            // Shadow first, or it is drawn over the fill and the icon.
            .shadow(
                elevation = 3.dp,
                shape = shape,
                ambientColor = t.shadow,
                spotColor = t.shadow,
            )
            .clip(shape)
            .background(t.accentDeep)
            // The visible label is what a screen reader reads; the longer
            // description goes on the tap, so it is not announced twice.
            .clickable(
                interactionSource = press.source,
                indication = null,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .padding(start = 11.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = t.onAccent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = t.onAccent,
        )
    }
}
