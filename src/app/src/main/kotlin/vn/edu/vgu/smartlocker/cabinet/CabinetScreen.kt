package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.parcels.SmallClaim
import vn.edu.vgu.smartlocker.parcels.SmallTicket
import vn.edu.vgu.smartlocker.ui.AltButton
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.NavClearance
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.QuietButton
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The cabinet's states, as the bench's mode buttons choose them.
 *
 * [UNKNOWN] is the one that was missing, and its absence was BUG-024: with no
 * way to say *the server did not answer*, a failed fetch arrived as an empty
 * list and the tab drew [EMPTY] - a green tick and "Room for a drop" - to a
 * receiver whose parcel was thirty centimetres away behind a locked door.
 */
enum class CabinetMode { TWO, ONE, EMPTY, FULL, UNKNOWN }

/**
 * The cabinet tab.
 *
 * Every door that is yours lights, and keeps its own stencilled number.
 * Under the render there is **one panel in every state**: the ticket for the
 * box the button will open, a switch above it when more than one is yours,
 * and a status card when none is.
 *
 * `selected` is which box the panel and the button are about — in any state
 * where you have a parcel here it is never null. `framed` is where the
 * camera is, and null means the wide shot. They are separate variables
 * because they are separate questions.
 */
@Composable
fun CabinetScreen(
    onScan: (String) -> Unit,
    onTypeCode: (String) -> Unit,
    /**
     * The doors that are actually yours, from the server.
     *
     * These were two invented parcels - box 04 and box 07 - built in this
     * function and drawn on every phone that opened this tab. The tab named a
     * door nobody had been given, which is why it never agreed with Home.
     */
    yours: List<YourDoor>,
    /**
     * What went wrong the last time this was fetched, already a sentence.
     * Null when the server answered.
     *
     * The screen may not draw a state that claims to know anything about the
     * cabinet while this is set and nothing is cached. BUG-024, and rule 4 in
     * architecture.md: an unclear result is never shown as a success.
     */
    note: Int? = null,
    /**
     * Free doors, when anybody knows. **Usually nothing.**
     *
     * A receiver's side of the contract has no endpoint that reports free
     * boxes - only the cabinet screen may ask, with a cabinet key - so the
     * phone genuinely does not know. It used to list six invented ones.
     */
    freeDoors: List<String> = emptyList(),
    mode: CabinetMode = when {
        // Cached doors still stand: a fetch that failed is not news that a
        // parcel has gone, and blanking one somebody can see would say it
        // had. The note goes underneath them instead.
        yours.isNotEmpty() -> if (yours.size == 1) CabinetMode.ONE else CabinetMode.TWO
        note != null -> CabinetMode.UNKNOWN
        else -> CabinetMode.EMPTY
    },
) {
    val t = LocalLockerTokens.current
    var selected by remember(yours) { mutableStateOf(yours.firstOrNull()?.n.orEmpty()) }
    var framed by remember { mutableStateOf<String?>(null) }

    val told = mode == CabinetMode.EMPTY || mode == CabinetMode.FULL ||
        mode == CabinetMode.UNKNOWN
    val mine = if (told) emptyList() else yours
    val effective = selected.ifEmpty { mine.firstOrNull()?.n.orEmpty() }

    // Heading carries the constants; per-door facts live in the panel.
    // "0 of 20 doors free" is what an unknown count printed, and it is a lie
    // in the other direction - the cabinet was nearly empty. Nothing on the
    // receiver's side of the contract reports free boxes, so when nobody has
    // said, the line is not drawn at all.
    val freeLine = if (freeDoors.isEmpty()) "" else stringResource(R.string.cab_doors_free, freeDoors.size)

    val (title, sub) = when (mode) {
        CabinetMode.FULL ->
            stringResource(R.string.cab_full_title) to stringResource(R.string.cab_full_sub)
        CabinetMode.EMPTY ->
            stringResource(R.string.cab_room_title) to stringResource(R.string.cab_room_sub)
        // The card under the render carries the detail. Saying it here
        // too puts the same two sentences on the screen twice.
        CabinetMode.UNKNOWN -> stringResource(R.string.cab_unknown_title) to ""
        // The heading counts, and the line under it says how to reach the one
        // the button is not about. "Two boxes are yours" over a single ticket
        // was a screen disagreeing with itself, and the way to the second one
        // was written down nowhere.
        CabinetMode.TWO ->
            stringResource(R.string.cab_two_title) to stringResource(R.string.cab_tap_a_door)
        CabinetMode.ONE -> stringResource(R.string.cab_one_title) to freeLine
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = t.ink,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))

        // The render sits in a well. It is a thing you look INTO — a wall of
        // boxes at the back gate — and a recess is the one material here that
        // says "behind the surface" rather than "on top of it". It is also
        // where the screen's depth went when the switch below it was removed:
        // that switch was the only recessed thing on the tab.
        // Sized to the render, not to whatever is left over. The render is
        // square and the well was taking the whole remaining column, so on a
        // tall phone about 300px of empty well sat under the cabinet - inside
        // the one material on this tab whose job is to read as depth. The
        // slack goes below instead, between the render and the panel, where
        // it reads as air rather than as a hole.
        Spacer(Modifier.weight(0.55f))
        Recess(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
        ) {
            CabinetArt(
                yours = mine,
                free = if (mode == CabinetMode.EMPTY) freeDoors else emptyList(),
                full = mode == CabinetMode.FULL,
                framedDoor = framed,
                onDoorTapped = { n ->
                    if (mine.any { it.n == n }) {
                        selected = n
                        framed = if (framed == n) null else n
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (mode == CabinetMode.FULL) {
                NoFreeBadge(modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.weight(1f))

        // One panel, and only one.
        //
        // There used to be a segmented switch above this naming your boxes.
        // It did nothing the render does not already do: its handler and the
        // door's handler were the same two lines. Two controls for one job,
        // stacked on top of each other, and the one being duplicated was the
        // better of the two — the doors are lit, numbered and in the place the
        // receiver is actually looking. The switch was a second, smaller,
        // abstract copy of the cabinet directly beneath a picture of it.
        //
        // What is left says which box the button will open and when it has to
        // be collected, which is the only thing the switch was really for.
        Column(modifier = Modifier.padding(top = 12.dp)) {
            if (told) {
                StatusCard(mode, note)
            } else {
                val d = mine.firstOrNull { it.n == effective } ?: mine.first()
                SmallTicket(
                    claim = SmallClaim(
                        cabinet = stringResource(R.string.cabinet_back_gate),
                        box = d.n,
                        detail = stringResource(R.string.cab_dropped, d.at),
                        left = d.left,
                        pct = d.pct,
                        soon = d.soon,
                    ),
                )
                // Cached doors with a failed refresh behind them. The doors
                // stay, and the screen says it could not check.
                note?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = t.ink3,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp),
                    )
                }
            }
        }

        // The action row. With nothing waiting there genuinely is no action.
        // The foot clears the floating nav: the glass is allowed over a
        // render, never over a button. Without this "Scan to open" sat half
        // under the bar and "Use a code" was hidden by it completely.
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = NavClearance),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            when (mode) {
                // Nothing to open, and nothing to pretend about.
                CabinetMode.EMPTY, CabinetMode.UNKNOWN -> {}
                CabinetMode.FULL -> AltButton(
                    text = stringResource(R.string.cab_show_library),
                    onClick = {},
                )
                else -> {
                    GoButton(
                        text = stringResource(R.string.cab_scan_to_open, effective),
                        onClick = { onScan(effective) },
                    )
                    QuietButton(
                        text = stringResource(R.string.cab_use_code),
                        onClick = { onTypeCode(effective) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Nothing waiting, nowhere to put anything, or no answer at all. Same height
 * as the ticket.
 *
 * The tick is only drawn for something the server actually said. A tick is
 * the mark for *checked, and fine*, and spending it on a guess is how this
 * screen came to confirm a free box to somebody holding a parcel. BUG-024.
 */
@Composable
private fun StatusCard(mode: CabinetMode, note: Int?) {
    val t = LocalLockerTokens.current
    val free = mode == CabinetMode.EMPTY
    val unknown = mode == CabinetMode.UNKNOWN
    val icon = when {
        free -> AppIcons.Check
        unknown -> AppIcons.Warn
        else -> AppIcons.NoEntry
    }
    // Not knowing is not a fact about a box, so it gets no colour.
    val colour = when {
        free -> t.free
        unknown -> t.ink2
        else -> t.refuse
    }
    CardMaterial(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colour.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colour,
                )
            }
            Column {
                Text(
                    text = stringResource(
                        when {
                            free -> R.string.cab_room_title
                            unknown -> R.string.cab_unknown_title
                            else -> R.string.cab_inuse_title
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = t.ink,
                )
                Text(
                    text = when {
                        free -> stringResource(R.string.cab_room_sub)
                        unknown -> note?.let { stringResource(it) }.orEmpty()
                        else -> stringResource(R.string.cab_inuse_sub)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = t.ink2,
                )
            }
        }
    }
}

/** The "no free box" badge, on the drained render. */
@Composable
private fun NoFreeBadge(modifier: Modifier = Modifier) {
    val t = LocalLockerTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(t.surface.copy(alpha = 0.92f))
            .border(1.dp, t.hair, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.NoEntry,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = t.refuse,
        )
        Text(
            text = stringResource(R.string.cab_no_free_box),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = t.ink,
        )
    }
}
