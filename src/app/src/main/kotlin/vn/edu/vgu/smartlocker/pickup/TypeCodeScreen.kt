package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/**
 * Screen 6. The back door: type the code from the notice.
 *
 * It exists so a flat battery, a broken camera or a dark corridor does not
 * trap a parcel.
 *
 * A wrong code must say the same thing whether it is wrong, already used,
 * or expired - CODE_REJECTED in api-contract.md. Anything else lets
 * somebody standing at the cabinet work out which codes are real. Skeleton:
 * nothing is checked here yet. Task P5-08.
 */
@Composable
fun TypeCodeScreen(onAccepted: () -> Unit) {
    var code by remember { mutableStateOf("") }

    ScreenFrame {
        // Held in from the screen edge so the heading breaks into two lines
        // instead of filling the full width to both margins.
        Text(
            text = stringResource(R.string.type_the_code),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.code_is_in_the_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onAccepted,
            enabled = code.length == 6,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
        ) {
            Text(
                text = stringResource(R.string.open_my_box),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TypeCodePreview() {
    MaterialTheme { TypeCodeScreen({}) }
}
