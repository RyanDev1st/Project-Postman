package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/**
 * Screen 4. Point the phone at the QR on the cabinet screen.
 *
 * **The phone scans the cabinet, never the reverse** - ADR 0003. The QR
 * only says which cabinet and when; the token proves who is holding the
 * phone. A photograph of the cabinet screen opens nothing.
 *
 * Skeleton: no camera. Tapping the frame stands in for a successful scan.
 * The camera, and the permission ask, arrive with task P5-02 - asked for
 * here, where the need is obvious, never on first open.
 */
@Composable
fun ScanScreen(onScanned: () -> Unit, onTypeCode: () -> Unit) {
    ScreenFrame(verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.point_at_the_screen),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )

        OutlinedCard(
            onClick = onScanned,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().height(280.dp),
        ) {
            Text(
                text = stringResource(R.string.camera_goes_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 128.dp),
            )
        }

        // Refusing the camera is not a dead end. It drops here.
        TextButton(onClick = onTypeCode) {
            Text(stringResource(R.string.cant_scan))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanPreview() {
    MaterialTheme { ScanScreen({}, {}) }
}
