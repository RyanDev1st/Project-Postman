package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * Sign in with a number and a password. Endpoint 21.
 *
 * The second way in, and today the only one that works at all - no Vietnamese
 * one-time code has ever been delivered, BUG-016.
 *
 * **The screen never says whether the number has an account.** Every way this
 * can fail - a number the server has never seen, an account that never set a
 * password, the right password five wrong tries into a lockout - comes back
 * as the same refusal, and the sentence is written to keep that promise. A
 * screen that said "no account with that number" would answer, for free, the
 * one question an attacker with a list of numbers is asking.
 *
 * The number is handed in and handed back out so that walking between here
 * and the code screen never loses what has already been typed.
 */
@Composable
fun PasswordSignInScreen(
    /** The local part of the number, nine digits - see [VnMobile]. */
    number: String,
    onNumberChange: (String) -> Unit,
    onSignIn: (password: String) -> Unit,
    /** Back to the one-time code way in, keeping the number. */
    onUseCode: () -> Unit = {},
    onBack: () -> Unit = {},
    note: Int? = null,
    busy: Boolean = false,
) {
    val t = LocalLockerTokens.current
    var password by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }

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
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.pw_signin_title_lead) + "\n")
                    withStyle(SpanStyle(color = t.accent)) {
                        append(stringResource(R.string.pw_signin_title_accent))
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = t.ink,
            )
            Text(
                text = stringResource(R.string.pw_signin_sub),
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(R.string.phone_number)
                PhoneField(number = number, onChange = onNumberChange)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(R.string.pw_label)
                PasswordField(
                    password = password,
                    onChange = { password = it },
                    shown = shown,
                    onToggleShown = { shown = !shown },
                )
            }
            // Off until both could be accepted. It is never sent twice for
            // one tap: `busy` is set before the call and cleared after it.
            GoButton(
                text = stringResource(if (busy) R.string.working else R.string.pw_signin_go),
                onClick = { onSignIn(password) },
                enabled = VnMobile.isComplete(number) && password.isNotEmpty() && !busy,
            )
            note?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.ink3,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            Text(
                text = stringResource(R.string.pw_use_code),
                style = MaterialTheme.typography.labelMedium,
                color = t.accent,
                modifier = Modifier.clickable(onClick = onUseCode).padding(start = 2.dp, top = 4.dp),
            )
        }
    }
}
