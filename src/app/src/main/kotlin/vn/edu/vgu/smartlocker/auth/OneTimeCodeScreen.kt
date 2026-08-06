package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardOptions
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/**
 * Screen 2. Type back the code that arrived by message.
 *
 * Skeleton: any six digits are accepted. Endpoint 2 arrives with P2-03.
 */
@Composable
fun OneTimeCodeScreen(onDone: () -> Unit) {
    var code by remember { mutableStateOf("") }

    ScreenFrame {
        Text(
            text = stringResource(R.string.enter_the_code),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        // The number is masked even though it is the user's own. Somebody
        // is always able to see this screen over a shoulder.
        Text(
            text = stringResource(R.string.we_sent_it_to, "09xx xxx 89"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            onClick = onDone,
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.check_the_code))
        }

        Spacer(Modifier.height(16.dp))

        // A countdown, not a button. A resend that can be hammered is a way
        // to spend the project's SMS money for us.
        Text(
            text = stringResource(R.string.send_again_in, "0:42"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneTimeCodePreview() {
    MaterialTheme { OneTimeCodeScreen(onDone = {}) }
}
