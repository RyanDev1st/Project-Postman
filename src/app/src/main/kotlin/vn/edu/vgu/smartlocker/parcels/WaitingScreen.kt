package vn.edu.vgu.smartlocker.parcels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame

/** One waiting parcel. Made up, until endpoint 5 exists (task P4-04). */
data class WaitingParcel(val cabinet: String, val box: String, val arrived: String)

/**
 * Screen 3, the home screen.
 *
 * **One parcel fills the screen.** At the cabinet the receiver wants one
 * button, not a list to read. A list appears only with two or more.
 * Decided with the team on 2026-08-06.
 */
@Composable
fun WaitingScreen(
    onScan: () -> Unit,
    onTypeCode: () -> Unit,
    onHistory: () -> Unit,
    parcels: List<WaitingParcel> = listOf(WaitingParcel("Cabinet A1", "04", "14:32")),
) {
    ScreenFrame(verticalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onHistory, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.history))
        }

        when {
            parcels.isEmpty() -> NothingWaiting()
            parcels.size == 1 -> OneParcel(parcels.first())
            else -> ParcelList(parcels)
        }

        // Everything tappable sits at the bottom, within thumb reach of a
        // hand that is also holding a parcel.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onScan,
                enabled = parcels.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scan_to_open))
            }

            // Quiet text, not a second big button. It is the back door and
            // it should look like one - see api-contract.md.
            TextButton(onClick = onTypeCode, enabled = parcels.isNotEmpty()) {
                Text(stringResource(R.string.cant_scan))
            }
        }
    }
}

@Composable
private fun OneParcel(parcel: WaitingParcel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.a_parcel_is_waiting),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(parcel.cabinet, style = MaterialTheme.typography.titleMedium)

        // The box number is what the receiver looks for on the cabinet, so
        // it is the biggest thing on the screen.
        Text(
            text = stringResource(R.string.box_n, parcel.box),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = stringResource(R.string.arrived_at, parcel.arrived),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParcelList(parcels: List<WaitingParcel>) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.n_parcels_waiting, parcels.size),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))
        parcels.forEach { parcel ->
            Text(
                text = "${parcel.cabinet} — ${stringResource(R.string.box_n, parcel.box)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.arrived_at, parcel.arrived),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Its own state, not an empty list. An empty list looks broken. */
@Composable
private fun NothingWaiting() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.nothing_waiting),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.we_will_tell_you),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WaitingOnePreview() {
    MaterialTheme { WaitingScreen({}, {}, {}) }
}

@Preview(showBackground = true, name = "two parcels")
@Composable
private fun WaitingManyPreview() {
    MaterialTheme {
        WaitingScreen({}, {}, {}, parcels = listOf(
            WaitingParcel("Cabinet A1", "04", "14:32"),
            WaitingParcel("Cabinet A1", "11", "Monday"),
        ))
    }
}

@Preview(showBackground = true, name = "nothing waiting")
@Composable
private fun WaitingNonePreview() {
    MaterialTheme { WaitingScreen({}, {}, {}, parcels = emptyList()) }
}
