package vn.edu.vgu.smartlocker.auth

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * Set a password, from an account that is already signed in. Endpoint 20.
 *
 * **It is only reachable from a live session, and that is the design.** ADR
 * 0012: forgetting a password is the one-time code, then setting a new one
 * here. So this screen is both the set path and the reset path, there is no
 * reset screen, and the product never has to collect an email address to
 * send one to.
 *
 * The second field is not security theatre. A password that is never shown
 * and never sent by email is a password nobody can recover, so a typo in the
 * one field would lock somebody out of the way in that exists precisely for
 * when the other way in has failed. The two are compared here rather than on
 * the server, because the server has no business knowing there were two.
 */
@Composable
fun SetPasswordScreen(
    onSave: (password: String) -> Unit,
    onBack: () -> Unit = {},
    /** What the server said - too short, too long, or a token that has gone.
     *  Also carries the "saved" sentence, which is the one good outcome. */
    note: Int? = null,
    busy: Boolean = false,
) {
    val t = LocalLockerTokens.current
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }

    // Only once both are typed. Saying "the two do not match" against a
    // second field somebody has not started is a screen telling them off for
    // not having finished.
    val mismatch = again.isNotEmpty() && password != again
    val ready = password.length >= MIN && password == again && !busy

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
                    append(stringResource(R.string.pw_set_title_lead) + "\n")
                    // Lead recedes, payoff keeps full ink - see SignInScreen.
                    withStyle(SpanStyle(color = t.ink)) {
                        append(stringResource(R.string.pw_set_title_accent))
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = t.ink2,
            )
            Text(
                text = stringResource(R.string.pw_set_sub),
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
                FieldLabel(R.string.pw_label)
                PasswordField(
                    password = password,
                    onChange = { password = it },
                    shown = shown,
                    onToggleShown = { shown = !shown },
                    imeAction = ImeAction.Next,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(R.string.pw_again_label)
                PasswordField(
                    password = again,
                    onChange = { again = it },
                    // One toggle for both fields: revealing one and not the
                    // other is what makes a mismatch impossible to see.
                    shown = shown,
                )
            }
            GoButton(
                text = stringResource(if (busy) R.string.working else R.string.pw_save),
                onClick = { onSave(password) },
                enabled = ready,
            )
            // The screen's own complaint comes first: it is about what is on
            // screen now, while the server's note is about the last attempt.
            (R.string.pw_mismatch.takeIf { mismatch } ?: note)?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.ink3,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}

/** The small caps label above a field. The same one on both screens here. */
@Composable
internal fun FieldLabel(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelMedium,
        color = LocalLockerTokens.current.ink2,
        modifier = Modifier.padding(start = 2.dp),
    )
}

/**
 * The server's minimum, repeated here only to keep the button off until it
 * could be accepted. `Passwords.MIN_LENGTH` is the one that counts.
 */
private const val MIN = 8
