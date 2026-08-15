package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace
import vn.edu.vgu.smartlocker.ui.theme.OnDoorLight
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * Opened — the one saturated screen.
 *
 * Amber fills the whole screen and it is identical in both schemes: a door
 * being open is a fact about the world, not about the phone's settings, and
 * the corridor it is read in is dark either way. The number at 122px is
 * readable across a corridor without reading a word.
 */
@Composable
fun OpenedScreen(
    box: String,
    onDone: () -> Unit,
    onBack: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Background first, insets after. The amber must reach all four
            // edges - it is the signal you read from across a corridor - so
            // the inset is applied to what is written on it, never to it.
            .background(DoorLight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        AppBar(
            brand = stringResource(R.string.cabinet_back_gate),
            leading = {
                AppBarBead(
                    icon = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Blank here is not possible any more, and it used to be: this
            // took a default of "04", and the typed-code path arrived without
            // ever setting a number, so the screen announced an open door and
            // named none. The default is gone and the caller must say which.
            Text(
                text = box,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = NumberFace,
                    fontWeight = FontWeight.Bold,
                    color = OnDoorLight,
                ),
            )
            Text(
                text = stringResource(R.string.opened_is_open),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.2.em,
                    fontSize = 11.sp,
                ),
                color = OnDoorLight.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.opened_close_when_done),
                style = MaterialTheme.typography.bodyLarge,
                color = OnDoorLight,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        // The one dark control on the amber ground — it is the way out, and
        // it has to read as a key you press, not a panel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(MaterialTheme.shapes.large)
                .background(OnDoorLight)
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.opened_done),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DoorLight,
                ),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OpenedPreview() {
    PreviewTheme { OpenedScreen(box = "04", onDone = {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun OpenedDarkPreview() {
    PreviewTheme(dark = true) { OpenedScreen(box = "04", onDone = {}) }
}
