package vn.edu.vgu.smartlocker.parcels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.parcels.map.LiveMap
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The one element on Home that is not about a parcel: which gate, which side
 * of campus, how far — and it hands off to the Maps app for the walk itself.
 *
 * The picture is a real map now ([LiveMap]). It used to be a plan drawn from
 * OSM data baked at build time, which meant the same scene wherever the phone
 * was; that plan and its projected geometry were deleted in the same change
 * that added this, and are in the history at cedc3b2 if they are ever wanted.
 */
@Composable
fun MapCard(
    cabinet: String = "Back gate",
    walk: String = "7 min walk · 540 m",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    CardMaterial(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier) {
            LiveMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                onClick = onClick,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cabinet,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = t.ink,
                    )
                    Text(
                        text = walk,
                        style = MaterialTheme.typography.labelLarge,
                        color = t.ink2,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(t.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Navigate,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = t.accentInk,
                    )
                }
            }
        }
    }
}
