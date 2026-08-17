package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The name the shipper is shown at the cabinet, masked.
 *
 * **Nothing else in this app ever asks for one.** An account is made by
 * proving a phone number and nothing more, so until this field existed every
 * real account had an empty name and endpoint 10 answered `***` - and the
 * shipper was asked to confirm a person the screen could not name.
 *
 * It is taken only when the account has no name yet. Somebody signing in on a
 * second phone types their code and leaves this empty; the server keeps the
 * name already stored. See `Receivers.findOrCreate`.
 *
 * Capped at 60 characters here as well as on the server. The server's cap is
 * the one that counts - this one only stops the field growing on screen.
 */
@Composable
fun NameField(
    name: String,
    onChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Recess(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        BasicTextField(
            value = name,
            onValueChange = { onChange(it.take(60)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = t.ink),
            singleLine = true,
            cursorBrush = SolidColor(t.accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                // A name is a name, not a sentence - and the cabinet draws it
                // as typed.
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { field ->
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.name_placeholder),
                        style = MaterialTheme.typography.bodyLarge.copy(color = t.ink3),
                    )
                }
                field()
            },
        )
    }
}
