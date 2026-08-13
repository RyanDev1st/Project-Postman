package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import vn.edu.vgu.smartlocker.ui.theme.NumberFace
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/** The heading of the code screens: 34px, tight, the last word in the
 * accent. Sitting just above the form — a question and its answer are one
 * thought, and centring put 180px of hole between them. */
@Composable
private fun AuthDisplay(
    first: String,
    accent: String,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Text(
        text = buildAnnotatedString {
            append("$first\n")
            withStyle(SpanStyle(color = t.accent)) { append(accent) }
        },
        style = MaterialTheme.typography.headlineLarge,
        color = t.ink,
        modifier = modifier,
    )
}

/**
 * The one-time code — only on the phone path. Signing in with Google skips
 * it.
 */
@Composable
fun CodeScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = {},
    resendAt: String = "0:42",
) {
    val t = LocalLockerTokens.current
    var code by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            brand = "VGU Locker",
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
            verticalArrangement = Arrangement.Bottom,
        ) {
            AuthDisplay(first = "Enter", accent = "the code")
            Text(
                text = stringResource(R.string.code_sent_to, "+84 ··· 678"),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = NumberFace),
                color = t.ink2,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.ink2,
                    modifier = Modifier.padding(start = 2.dp),
                )
                OtpCells(code = code, caretIndex = code.length.coerceAtMost(5))
            }
            GoButton(text = stringResource(R.string.code_continue), onClick = onDone)
        }

        QuietButton(
            text = stringResource(R.string.code_resend_in, resendAt),
            onClick = {},
            enabled = false,
        )
    }
}

/**
 * The "one more thing" after Google or VGU — the shipper finds a receiver
 * by typing a phone number on the cabinet, and social sign-in has an email,
 * not a number. The heading carries the reason, the label carries the ask,
 * and the consequence sits under Skip, where the decision is made.
 */
@Composable
fun AddPhoneScreen(
    onSaved: () -> Unit,
    onSkip: () -> Unit,
) {
    val t = LocalLockerTokens.current

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(brand = "VGU Locker")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            AuthDisplay(first = "Couriers find you", accent = "by number")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.phone_number),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.ink2,
                    modifier = Modifier.padding(start = 2.dp),
                )
                PhoneField(number = "")
            }
            GoButton(text = stringResource(R.string.code_save_number), onClick = onSaved)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuietButton(text = stringResource(R.string.code_skip), onClick = onSkip)
            Text(
                text = stringResource(R.string.code_need_number),
                style = MaterialTheme.typography.labelLarge,
                color = t.ink3,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun CodePreview() {
    PreviewTheme { CodeScreen({}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun CodeDarkPreview() {
    PreviewTheme(dark = true) { CodeScreen({}) }
}
