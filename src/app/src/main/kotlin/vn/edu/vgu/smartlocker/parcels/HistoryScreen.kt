package vn.edu.vgu.smartlocker.parcels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/** One past parcel. Made up, until endpoint 7 exists (task P7-01). */
data class PastParcel(val box: String, val what: String, val whenAt: String)

/**
 * Screen 7. What already happened.
 *
 * Read-only on purpose. **Nothing on this screen opens a door.**
 */
@Composable
fun HistoryScreen(
    parcels: List<PastParcel> = listOf(
        PastParcel("04", "collected", "Tue 14:35"),
        PastParcel("11", "collected", "Mon 09:12"),
    ),
) {
    ScreenFrame(verticalArrangement = Arrangement.Top) {
        Text(
            text = stringResource(R.string.my_parcels),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        parcels.forEach { parcel ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "${stringResource(R.string.box_n, parcel.box)} — ${parcel.what}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = parcel.whenAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    MaterialTheme { HistoryScreen() }
}
