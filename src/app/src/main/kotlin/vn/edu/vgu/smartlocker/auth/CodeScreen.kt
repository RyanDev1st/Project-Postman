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
import vn.edu.vgu.smartlocker.BuildConfig
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
            // Lead recedes, payoff keeps full ink - see SignInScreen.
            withStyle(SpanStyle(color = t.ink)) { append(accent) }
        },
        style = MaterialTheme.typography.headlineLarge,
        color = t.ink2,
        modifier = modifier,
    )
}

/**
 * The one-time code — only on the phone path. Signing in with Google skips
 * it.
 */
@Composable
fun CodeScreen(
    /** The six digits and the name, handed up to whoever can check them.
     * This screen cannot: only the server knows what it sent, and only the
     * server knows whether this account already has a name. */
    onDone: (code: String, fullName: String) -> Unit,
    onBack: () -> Unit = {},
    /** The number a code was asked for, local part — see [VnMobile]. */
    number: String = "",
    resendAt: String = "0:42",
    /** What the server said, if it has said anything. A refusal, or a note
     * that this build has nowhere to ask. Null while nothing is wrong. */
    note: Int? = null,
    /** True while the server is being asked. The button stops rather than
     * sending the same code twice. */
    busy: Boolean = false,
) {
    val t = LocalLockerTokens.current
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

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
            AuthDisplay(
                first = stringResource(R.string.code_head_lead),
                accent = stringResource(R.string.code_head_accent),
            )
            Text(
                text = stringResource(
                    R.string.code_sent_to,
                    if (number.isEmpty()) "+84 ··· 678"
                    else "+84 " + VnMobile.spaced(number),
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = NumberFace),
                color = t.ink2,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The block above is bottom-aligned in the space it is given,
                // so without this its last line and this block's first sit
                // against each other. 24dp is the interval between ranks on
                // the sign-in screen, and these are two ranks.
                .padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.ink2,
                    modifier = Modifier.padding(start = 2.dp),
                )
                OtpCells(
                    code = code,
                    caretIndex = code.length.coerceAtMost(5),
                    onChange = { code = it },
                )
            }
            // The name, which nothing else in the app ever asks for. Left
            // empty it changes nothing: the server takes it only when the
            // account has no name yet, so signing in on a second phone does
            // not have to retype it. Without it the cabinet shows the shipper
            // `***` and there is nothing to confirm.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.name_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.ink2,
                    modifier = Modifier.padding(start = 2.dp),
                )
                NameField(name = name, onChange = { name = it })
                Text(
                    text = stringResource(R.string.name_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.ink3,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }

            // Six digits, and then the server says whether they are the right
            // six. Disabled while it is being asked, so a second tap cannot
            // spend the same code twice.
            GoButton(
                text = stringResource(if (busy) R.string.working else R.string.code_continue),
                onClick = { onDone(code, name) },
                enabled = code.length == 6 && !busy,
            )
            note?.let { Note(it) }
        }

        QuietButton(
            text = stringResource(R.string.code_resend_in, resendAt),
            onClick = {},
            enabled = false,
        )
    }
}

/**
 * A quiet line under the code box, for whatever the server just said.
 *
 * It replaced a debug-only note reading *"any 6 digits will let you in"*, which
 * was true while there was nothing to check a code against and became a lie
 * the day there was. The sentence is chosen by the caller, because the caller
 * is the one holding the answer.
 */
@Composable
private fun Note(@androidx.annotation.StringRes text: Int) {
    val t = LocalLockerTokens.current
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelSmall,
        color = t.ink3,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun CodePreview() {
    PreviewTheme { CodeScreen({ _, _ -> }) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun CodeDarkPreview() {
    PreviewTheme(dark = true) { CodeScreen({ _, _ -> }) }
}
