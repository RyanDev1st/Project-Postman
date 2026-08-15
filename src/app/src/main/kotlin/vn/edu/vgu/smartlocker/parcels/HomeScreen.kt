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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.Pill
import vn.edu.vgu.smartlocker.ui.PillKind
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/** One line of the ledger: what you already collected. */
data class LedgerEntry(val box: String, val cabinet: String, val when_: String)

// --- Preview fixtures ---------------------------------------------------
//
// **These are for @Preview only, and HomeScreen no longer defaults to them.**
// They were default arguments, which meant the shipping call site got a
// made-up parcel at Back Gate box 04 for free if it forgot to pass anything -
// and it did forget, for six builds. The parameters below are required now,
// so a screen with no data has to say so rather than inventing a parcel.
//
// They are composable functions rather than constants because a place name and
// the word "today" turn over with the language, and a top-level `val` is built
// once, in whatever language the process started in.

@Composable
private fun sample() = Claim(
    cabinet = stringResource(R.string.cabinet_back_gate),
    box = "04",
    dropped = "08:14",
    collectBy = stringResource(R.string.time_at_today, "14:20"),
    left = stringResource(R.string.time_hours_left, "6"),
    pct = 0.12f,
    soon = true,
)

@Composable
private fun sampleSecond() = SmallClaim(
    cabinet = stringResource(R.string.cabinet_back_gate),
    box = "07",
    detail = stringResource(
        R.string.cab_dropped,
        stringResource(R.string.time_at_yesterday, "21:40"),
    ),
    left = stringResource(R.string.time_hours_left, "31"),
    pct = 0.65f,
    soon = false,
)

@Composable
private fun sampleLedger() = listOf(
    LedgerEntry("02", stringResource(R.string.cabinet_library), stringResource(R.string.date_2_aug)),
    LedgerEntry("17", stringResource(R.string.cabinet_back_gate), stringResource(R.string.date_28_jul)),
    LedgerEntry("09", stringResource(R.string.cabinet_library), stringResource(R.string.date_21_jul)),
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
    parcels: List<Claim>,
    second: List<SmallClaim>,
    ledger: List<LedgerEntry>,
    onOpen: (String) -> Unit = {},
    onOpenSecond: (String) -> Unit = {},
    onMap: () -> Unit = {},
    /**
     * How many boxes are free, or **null when nobody has said**.
     *
     * It used to default to 6, and the empty ticket drew that 6 as large as a
     * real box number. Nothing on the receiver's side of the contract reports
     * free boxes, so that number was invented, and it looked exactly as solid
     * as a number that is not.
     */
    freeCount: Int? = null,
    /**
     * A sentence to show instead of "nothing waiting" - the same `note: Int?`
     * the sign-in and scan screens take.
     *
     * It matters most when it is null-shaped trouble: if the server cannot be
     * reached, an empty Home would quietly report that nobody has sent you
     * anything, which is a different fact from *we could not ask*.
     */
    note: Int? = null,
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
                    cabinet = stringResource(R.string.cabinet_back_gate),
                    box = freeCount?.let { "%02d".format(it) } ?: "\u2014",
                    dropped = "",
                    collectBy = "",
                    left = if (note == null) stringResource(R.string.home_nearest) else "",
                    pct = 0f,
                    soon = false,
                ),
                isFree = true,
                freeNote = stringResource(note ?: R.string.home_nothing_waiting),
            )
        } else {
            ClaimTicket(claim = parcels.first())
            GoButton(
                text = stringResource(R.string.home_open_door, parcels.first().box),
                onClick = { onOpen(parcels.first().box) },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (second.isNotEmpty()) {
            SectionLabel(stringResource(R.string.home_also_waiting))
            second.forEach { s ->
                SmallTicket(
                    claim = s,
                    onClick = { onOpenSecond(s.box) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        SectionLabel(stringResource(R.string.home_where_to_walk))
        MapCard(onClick = onMap)

        SectionLabel(stringResource(R.string.home_collected))
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
            .padding(top = 24.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
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
                    .padding(vertical = 8.dp, horizontal = 2.dp),
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
    PreviewTheme {
        HomeScreen(
            parcels = listOf(sample()),
            second = listOf(sampleSecond()),
            ledger = sampleLedger(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "dark")
@Composable
private fun HomeDarkPreview() {
    PreviewTheme(dark = true) {
        HomeScreen(
            parcels = listOf(sample()),
            second = listOf(sampleSecond()),
            ledger = sampleLedger(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "empty")
@Composable
private fun HomeEmptyPreview() {
    PreviewTheme {
        HomeScreen(parcels = emptyList(), second = emptyList(), ledger = sampleLedger())
    }
}
