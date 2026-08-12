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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.parcels.map.LiveMap
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.GlassBead
import vn.edu.vgu.smartlocker.ui.GlassPane
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
    var opened by remember { mutableStateOf(false) }

    if (opened) {
        MapSheet(cabinet = cabinet, walk = walk, onClose = { opened = false })
    }

    CardMaterial(
        modifier = modifier,
        onClick = { opened = true; onClick() },
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier) {
            LiveMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                onClick = { opened = true; onClick() },
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

/**
 * The map, opened out.
 *
 * The card is 112dp of picture — enough to say *which way*, not enough to
 * look at. Tapping it used to hand off to the Maps app, which is what the
 * mock-up specifies and which leaves the app entirely to answer a question
 * the app already has the data for. It opens here instead, and here the
 * gestures are live: pan, pinch, and the blue dot where you are.
 *
 * A dialog rather than a route, because there is no navigation graph yet and
 * a map that the back button closes is the behaviour either way.
 */
@Composable
private fun MapSheet(
    cabinet: String,
    walk: String,
    onClose: () -> Unit,
) {
    val t = LocalLockerTokens.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiveMap(
                modifier = Modifier.fillMaxSize(),
                interactive = true,
            )

            // The only chrome: what you are looking at, and the way out.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassBead(
                    modifier = Modifier.size(38.dp),
                    onClick = onClose,
                ) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "Close the map",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(17.dp),
                        tint = t.ink,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                GlassPane(
                    modifier = Modifier,
                    shape = RoundedCornerShape(999.dp),
                    pop = true,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text(
                            text = cabinet,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = t.ink,
                        )
                        Text(
                            text = walk,
                            style = MaterialTheme.typography.labelMedium,
                            color = t.ink2,
                        )
                    }
                }
            }

            Box(modifier = Modifier.navigationBarsPadding().align(Alignment.BottomCenter))
        }
    }
}
