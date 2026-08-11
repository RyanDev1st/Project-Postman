package vn.edu.vgu.smartlocker.parcels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import vn.edu.vgu.smartlocker.ui.GlassPanel
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/** One waiting parcel. Made up, until endpoint 5 exists (task P4-04). */
data class WaitingParcel(val cabinet: String, val box: String, val arrived: String)

/**
 * Screen 3, the home screen, and the first viewport of the whole app.
 *
 * **One parcel fills the screen.** At the cabinet the receiver wants one
 * button, not a list to read. A list appears only at two or more. Decided
 * with the team on 2026-08-06.
 *
 * The box number sits on the app's only glass panel, lifted off the cabinet
 * ground. Everything else on the screen is deliberately quiet so that the
 * number and the button are the only two things the eye lands on.
 */
@Composable
fun WaitingScreen(
    onScan: () -> Unit,
    onTypeCode: () -> Unit,
    onHistory: () -> Unit,
    parcels: List<WaitingParcel> = listOf(WaitingParcel("Cabinet A1", "04", "14:32")),
) {
    ScreenFrame(verticalArrangement = Arrangement.Top) {
        TextButton(onClick = onHistory, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.collected), style = MaterialTheme.typography.labelLarge)
        }

        // The card sits high, where the eye lands first. Centring it left a
        // dead third at the top of the screen and pushed the number away
        // from the thumb.
        Spacer(Modifier.height(24.dp))

        when {
            parcels.isEmpty() -> NothingWaiting()
            parcels.size == 1 -> OneParcel(parcels.first())
            else -> ParcelList(parcels)
        }

        Spacer(Modifier.weight(1f))

        // Everything tappable sits at the bottom, in reach of a thumb on a
        // hand that is also holding a parcel.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onScan,
                enabled = parcels.isNotEmpty(),
                shape = MaterialTheme.shapes.large,
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
            ) {
                Text(
                    text = stringResource(R.string.scan_to_open),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // Quiet text, not a second big button. It is the back door and
            // it should look like one - see api-contract.md.
            TextButton(onClick = onTypeCode, enabled = parcels.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cant_scan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OneParcel(parcel: WaitingParcel) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.one_ready),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        // The number the receiver hunts for along a row of identical doors.
        // Nothing else in the app is set at this size.
        Text(
            text = parcel.box,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.box_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = parcel.cabinet,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.ready_at, parcel.arrived),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParcelList(parcels: List<WaitingParcel>) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.n_ready, parcels.size),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        parcels.forEach { parcel ->
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = parcel.box,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${parcel.cabinet} · ${stringResource(R.string.ready_at, parcel.arrived)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Its own state, not an empty list. An empty list reads as broken. */
@Composable
private fun NothingWaiting() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.nothing_waiting),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.we_will_tell_you),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WaitingOnePreview() {
    PreviewTheme { WaitingScreen({}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun WaitingDarkPreview() {
    PreviewTheme(dark = true) { WaitingScreen({}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "nothing waiting")
@Composable
private fun WaitingNonePreview() {
    PreviewTheme { WaitingScreen({}, {}, {}, parcels = emptyList()) }
}
