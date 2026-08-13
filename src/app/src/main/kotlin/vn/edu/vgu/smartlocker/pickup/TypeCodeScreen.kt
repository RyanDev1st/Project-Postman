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
 * The typed backup code — the back door, refusing.
 *
 * The refusal wording is fixed by api-contract.md and shared with the
 * cabinet, so it is a contract change rather than a screen edit; this screen
 * shows the shortened form.
 */
@Composable
fun TypeCodeScreen(
    onAccepted: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit = {},
    error: Boolean = true,
) {
    val t = LocalLockerTokens.current
    var code by remember { mutableStateOf("82") }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            brand = stringResource(R.string.box_label, "04"),
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
            OtpCells(code = code, caretIndex = 2)
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
            GoButton(text = stringResource(R.string.type_open_box), onClick = onAccepted)
            QuietButton(text = stringResource(R.string.type_scan_instead), onClick = onScan)
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun TypeCodePreview() {
    PreviewTheme { TypeCodeScreen({}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun TypeCodeDarkPreview() {
    PreviewTheme(dark = true) { TypeCodeScreen({}, {}) }
}
