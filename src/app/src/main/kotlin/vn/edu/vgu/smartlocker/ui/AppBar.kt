package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            .padding(start = 1.dp, end = 1.dp, top = 5.dp, bottom = 9.dp),
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

/** The scan control in the bar — the one thing up here that is tinted and
 * lit. Everything else in the bar is quiet glass. */
@Composable
fun ScanButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Box(
        modifier = modifier
            .size(30.dp)
            // Shadow first, or it is drawn over the fill and the icon.
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = t.accent.copy(alpha = 0.22f),
                spotColor = t.accent.copy(alpha = 0.22f),
            )
            .clip(CircleShape)
            .background(t.accent.copy(alpha = 0.20f))
            .border(1.dp, t.accent.copy(alpha = 0.34f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(15.dp),
            tint = t.accent,
        )
    }
}
