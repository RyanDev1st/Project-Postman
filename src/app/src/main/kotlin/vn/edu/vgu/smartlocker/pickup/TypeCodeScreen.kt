package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.OtpCells
import vn.edu.vgu.smartlocker.ui.QuietButton
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * The typed backup code - and where it is actually typed.
 *
 * **This screen cannot open a door, and it used to pretend it could.** Its
 * button called straight through to the "opened" screen: no request, no
 * server, no box number, amber and the words IS OPEN over a door that had not
 * moved. That is the one failure this project's rules name outright - never
 * show a success that was not confirmed.
 *
 * It cannot be fixed by wiring the button up, either. Endpoint 13
 * (`/cabinet/collect-by-code`) is answered only for a caller holding a cabinet
 * key, and a phone must never hold one. The code is typed on the **cabinet's
 * own screen** - which is the point of it: it is the way in when the phone is
 * flat, and a flat phone cannot type anything.
 *
 * So this screen says where the code goes. See P5-08, whose own Verify reads
 * "typed on the real cabinet".
 */
@Composable
fun TypeCodeScreen(
    /** The box this is about. It was the literal string "04". */
    box: String,
    onScan: () -> Unit,
    onBack: () -> Unit = {},
    error: Boolean = false,
) {
    val t = LocalLockerTokens.current
    val code = ""

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            brand = stringResource(R.string.box_label, box),
            leading = {
                AppBarBead(
                    icon = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
            },
        )

        Column {
            Text(
                text = stringResource(R.string.type_title),
                style = MaterialTheme.typography.headlineMedium,
                color = t.ink,
            )
            Text(
                text = stringResource(R.string.type_sub),
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            OtpCells(code = code, caretIndex = -1)
            if (error) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(
                            t.refuse.copy(alpha = 0.14f),
                            RoundedCornerShape(16.dp),
                        )
                        .border(1.dp, t.refuse.copy(alpha = 0.34f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.Warn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = t.refuse,
                    )
                    Text(
                        text = stringResource(R.string.type_wrong),
                        style = MaterialTheme.typography.labelLarge,
                        color = t.refuse,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // No "open the box" button. There is nothing this screen could
            // call that would open one, and a button that navigates to a
            // success screen without asking anybody is worse than no button.
            Text(
                text = stringResource(R.string.type_at_cabinet),
                style = MaterialTheme.typography.bodyLarge,
                color = t.ink2,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            GoButton(text = stringResource(R.string.type_scan_instead), onClick = onScan)
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun TypeCodePreview() {
    PreviewTheme { TypeCodeScreen(box = "04", onScan = {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun TypeCodeDarkPreview() {
    PreviewTheme(dark = true) { TypeCodeScreen(box = "04", onScan = {}) }
}
