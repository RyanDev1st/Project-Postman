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
 * Screen 1. Ask for the phone number, ask the server for a code.
 *
 * Skeleton: nothing is sent. Endpoint 1 arrives with task P2-01.
 */
@Composable
fun PhoneNumberScreen(version: String, onSend: () -> Unit) {
    var number by remember { mutableStateOf("") }

    ScreenFrame {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text(stringResource(R.string.your_phone_number)) },
            singleLine = true,
            // The number pad opens by itself. Nobody should have to go
            // looking for it while holding a parcel.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onSend, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.send_me_a_code))
        }

        Spacer(Modifier.height(12.dp))

        // Say why a code is coming before it arrives. An unexplained code
        // reads as somebody trying to break into your account.
        Text(
            text = stringResource(R.string.why_a_code),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "v$version",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneNumberPreview() {
    MaterialTheme { PhoneNumberScreen(version = "0.1.0", onSend = {}) }
}
