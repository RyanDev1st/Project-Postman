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

/** The cabinet's four states, as the bench's mode buttons choose them. */
enum class CabinetMode { TWO, ONE, EMPTY, FULL }

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
     * Free doors, when anybody knows. **Usually nothing.**
     *
     * A receiver's side of the contract has no endpoint that reports free
     * boxes - only the cabinet screen may ask, with a cabinet key - so the
     * phone genuinely does not know. It used to list six invented ones.
     */
    freeDoors: List<String> = emptyList(),
    mode: CabinetMode = when {
        yours.isEmpty() -> CabinetMode.EMPTY
        yours.size == 1 -> CabinetMode.ONE
        else -> CabinetMode.TWO
    },
) {
    val t = LocalLockerTokens.current
    var selected by remember(yours) { mutableStateOf(yours.firstOrNull()?.n.orEmpty()) }
    var framed by remember { mutableStateOf<String?>(null) }

    val mine = if (mode == CabinetMode.EMPTY || mode == CabinetMode.FULL) emptyList() else yours
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
        CabinetMode.TWO -> stringResource(R.string.cab_two_title) to freeLine
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
        Recess(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
            if (mode == CabinetMode.EMPTY || mode == CabinetMode.FULL) {
                StatusCard(mode)
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
                CabinetMode.EMPTY -> {}
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

/** Nothing waiting, or nowhere to put anything. Same height as the ticket. */
@Composable
private fun StatusCard(mode: CabinetMode) {
    val t = LocalLockerTokens.current
    val free = mode == CabinetMode.EMPTY
    val icon = if (free) AppIcons.Check else AppIcons.NoEntry
    val colour = if (free) t.free else t.refuse
    CardMaterial(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colour.copy(alpha = if (free) 0.15f else 0.16f)),
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
                        if (free) R.string.cab_room_title else R.string.cab_inuse_title,
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = t.ink,
                )
                Text(
                    text = stringResource(
                        if (free) R.string.cab_room_sub else R.string.cab_inuse_sub,
                    ),
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
