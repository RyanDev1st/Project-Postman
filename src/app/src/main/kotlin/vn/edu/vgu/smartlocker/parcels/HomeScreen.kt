package vn.edu.vgu.smartlocker.parcels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import vn.edu.vgu.smartlocker.NavClearance
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.Pill
import vn.edu.vgu.smartlocker.ui.PillKind
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/** One line of the ledger: what you already collected. */
data class LedgerEntry(val box: String, val cabinet: String, val when_: String)

private val SAMPLE = Claim(
    cabinet = "Back gate",
    box = "04",
    dropped = "08:14",
    collectBy = "14:20  today",
    left = "6h left",
    pct = 0.12f,
    soon = true,
)

private val SAMPLE_SECOND = SmallClaim(
    cabinet = "Back gate",
    box = "07",
    detail = "Dropped 21:40 yesterday",
    left = "31h",
    pct = 0.65f,
    soon = false,
)

private val SAMPLE_LEDGER = listOf(
    LedgerEntry("02", "Library", "2 Aug"),
    LedgerEntry("17", "Back gate", "28 Jul"),
    LedgerEntry("09", "Library", "21 Jul"),
)

/**
 * Home — a claim ticket, not a photo album.
 *
 * You have a claim on a thing in a box. A cloakroom tag, a luggage stub, a
 * lottery slip: a big number, a seam, and a time, and everyone alive can
 * read one without being taught. The number is the thing you carry to the
 * cabinet, so the number is the screen. The wall of doors lives on the
 * Cabinet tab, where it answers a question; Home answers only *what is
 * mine, and when do I need to be there*.
 *
 * Ranks, and nothing on this screen shares a size with anything else:
 * the ticket, the one-row second parcel, the map, the hairline ledger.
 */
@Composable
fun HomeScreen(
    onOpen: (String) -> Unit = {},
    onOpenSecond: (String) -> Unit = {},
    onMap: () -> Unit = {},
    parcels: List<Claim> = listOf(SAMPLE),
    second: List<SmallClaim> = listOf(SAMPLE_SECOND),
    ledger: List<LedgerEntry> = SAMPLE_LEDGER,
    freeCount: Int = 6,
) {
    val t = LocalLockerTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        if (parcels.isEmpty()) {
            // The empty state is the same ticket with a different number on
            // it: *nothing waiting* and *room to send you something* are
            // different facts, and only the second is useful.
            ClaimTicket(
                claim = Claim(
                    cabinet = "Back gate",
                    box = "%02d".format(freeCount),
                    dropped = "",
                    collectBy = "",
                    left = "Nearest to you",
                    pct = 0f,
                    soon = false,
                ),
                isFree = true,
                freeNote = "Nothing waiting. Give a courier your number and any of the six is yours within the hour.",
            )
        } else {
            ClaimTicket(claim = parcels.first())
            GoButton(
                text = "Open door ${parcels.first().box}",
                onClick = { onOpen(parcels.first().box) },
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        if (second.isNotEmpty()) {
            SectionLabel("Also waiting")
            second.forEach { s ->
                SmallTicket(
                    claim = s,
                    onClick = { onOpenSecond(s.box) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        SectionLabel("Where to walk")
        MapCard(onClick = onMap)

        SectionLabel("Collected")
        Ledger(entries = ledger)

        // Room to scroll the last row out from under the floating nav.
        Spacer(Modifier.height(NavClearance))
    }
}

/** More space above a label than below it — the gap belongs to the group the
 * label opens, not to the one it just closed. */
@Composable
fun SectionLabel(text: String, action: String? = null) {
    val t = LocalLockerTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 9.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = t.ink2,
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.08.em),
                color = t.accentInk,
            )
        }
    }
}

/** A ledger, not a list of cards. History is a thing you glance at; it is
 * the quietest rank on the screen and it should look like it — no fill, no
 * border, no radius, a hairline between entries and a recessed number chip,
 * which is the only piece of it anyone ever actually reads. */
@Composable
fun Ledger(entries: List<LedgerEntry>, modifier: Modifier = Modifier) {
    val t = LocalLockerTokens.current
    Column(modifier = modifier.fillMaxWidth()) {
        entries.forEachIndexed { i, e ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(t.hair),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Recess(
                    modifier = Modifier
                        .size(34.dp)
                        .padding(0.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = e.box,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = NumberFace,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = t.ink2,
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = e.cabinet,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = t.ink,
                )
                Text(
                    text = e.when_,
                    style = MaterialTheme.typography.labelLarge,
                    color = t.ink2,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomePreview() {
    PreviewTheme { HomeScreen() }
}

@Preview(showBackground = true, heightDp = 900, name = "dark")
@Composable
private fun HomeDarkPreview() {
    PreviewTheme(dark = true) { HomeScreen() }
}

@Preview(showBackground = true, heightDp = 900, name = "empty")
@Composable
private fun HomeEmptyPreview() {
    PreviewTheme { HomeScreen(parcels = emptyList(), second = emptyList()) }
}
