package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * A password, typed once. The same field sets one and signs in with one.
 *
 * Three things it deliberately does **not** do.
 *
 * It does not score the password, or demand a capital and a digit. The rule
 * the server enforces is a length, and inventing a second rule on the screen
 * would refuse passwords the server would have taken.
 *
 * It does not autocorrect or capitalise. `KeyboardCapitalization.None` and
 * `KeyboardType.Password` together are what stop the keyboard helpfully
 * changing the first letter of somebody's password, which is a bug that
 * only ever shows up as "the right password stopped working".
 *
 * It does not remember anything. The text lives in the caller's state for as
 * long as the screen is up and is never written down; only the token the
 * server mints is kept, and that goes to the secure store - see `TokenStore`.
 *
 * @param cap the server's own maximum, so a paste of a novel is cut here
 *   rather than making a request that can only be refused.
 */
@Composable
fun PasswordField(
    password: String,
    onChange: (String) -> Unit,
    /** Whether the characters are shown. The caller owns it, so a screen with
     *  two fields can reveal both with one tap. */
    shown: Boolean = false,
    onToggleShown: (() -> Unit)? = null,
    /** `Done` on the last field of a screen, `Next` on any before it. */
    imeAction: ImeAction = ImeAction.Done,
    cap: Int = 200,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Recess(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = password,
                onValueChange = { onChange(it.take(cap)) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = t.ink),
                singleLine = true,
                cursorBrush = SolidColor(t.accent),
                visualTransformation =
                    if (shown) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = imeAction,
                ),
                decorationBox = { field ->
                    if (password.isEmpty()) {
                        Text(
                            text = stringResource(R.string.pw_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(color = t.ink3),
                        )
                    }
                    field()
                },
            )
            // Only offered where the caller wants it. Somebody standing at a
            // cabinet with people behind them should not have to reveal a
            // password to check it, and somebody alone should not have to
            // guess at eight dots.
            onToggleShown?.let { toggle ->
                Text(
                    text = stringResource(if (shown) R.string.pw_hide else R.string.pw_show),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.accent,
                    modifier = Modifier
                        .clickable(onClick = toggle)
                        .padding(start = 12.dp),
                )
            }
        }
    }
}
