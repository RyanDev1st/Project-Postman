package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/**
 * Screen 5. A door opened.
 *
 * **This screen is only ever shown when the server said the door opened.**
 * If the answer was unclear, the app says it was unclear - task P5-06. A
 * false success sends somebody away from their own parcel.
 */
@Composable
fun OpenedScreen(box: String = "04", onDone: () -> Unit) {
    ScreenFrame(verticalArrangement = Arrangement.SpaceBetween) {
        Spacer(Modifier.height(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Twenty doors look alike. The number is the biggest thing here.
            Text(
                text = stringResource(R.string.box_n, box),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(R.string.is_open),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.take_and_close),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.done))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OpenedPreview() {
    MaterialTheme { OpenedScreen(onDone = {}) }
}
