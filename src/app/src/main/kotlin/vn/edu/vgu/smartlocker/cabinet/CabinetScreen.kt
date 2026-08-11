package vn.edu.vgu.smartlocker.cabinet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.parcels.SmallClaim
import vn.edu.vgu.smartlocker.parcels.SmallTicket
import vn.edu.vgu.smartlocker.ui.AltButton
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.NavClearance
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.QuietButton
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace

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
    onTypeCode: () -> Unit,
    mode: CabinetMode = CabinetMode.TWO,
) {
    val t = LocalLockerTokens.current
    var selected by remember { mutableStateOf("04") }
    var framed by remember { mutableStateOf<String?>(null) }

    val allMine = listOf(
        YourDoor("04", "08:14 today", "6h left", 0.12f, soon = true),
        YourDoor("07", "21:40 yesterday", "31h left", 0.65f, soon = false),
    )
    val freeDoors = listOf("02", "05", "11", "14", "17", "20")
    val mine = when (mode) {
        CabinetMode.TWO -> allMine
        CabinetMode.ONE -> listOf(allMine.first())
        CabinetMode.EMPTY, CabinetMode.FULL -> emptyList()
    }
    val effective = if (mode == CabinetMode.ONE) allMine.first().n else selected

    // Heading carries the constants; per-door facts live in the panel.
    val (title, sub) = when (mode) {
        CabinetMode.FULL -> "Cabinet full" to "Nothing waiting, and no free box"
        CabinetMode.EMPTY -> "${freeDoors.size} boxes free" to "Nothing waiting for you"
        CabinetMode.TWO -> "Two boxes are yours" to "${freeDoors.size} of 20 doors free"
        CabinetMode.ONE -> "One box is yours" to "${freeDoors.size} of 20 doors free"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = t.ink,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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

        // One panel, always. Built from the same list the doors are lit
        // from, so the panel and a lit door cannot disagree.
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                mode == CabinetMode.EMPTY || mode == CabinetMode.FULL ->
                    StatusCard(mode)

                mine.size > 1 -> DoorSwitch(
                    doors = mine,
                    selected = effective,
                    onSelect = { selected = it; framed = it },
                )
            }
            if (mode == CabinetMode.TWO || mode == CabinetMode.ONE) {
                val d = mine.first { it.n == effective }
                SmallTicket(
                    claim = SmallClaim(
                        cabinet = "Back gate",
                        box = d.n,
                        detail = "Dropped ${d.at}",
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
            modifier = Modifier.padding(top = 10.dp, bottom = NavClearance),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            when (mode) {
                CabinetMode.EMPTY -> {}
                CabinetMode.FULL -> AltButton(
                    text = "Show the Library cabinet",
                    onClick = {},
                )
                else -> {
                    GoButton(
                        text = "Scan to open $effective",
                        onClick = { onScan(effective) },
                    )
                    QuietButton(
                        text = "Use a code",
                        onClick = onTypeCode,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** The segmented switch — which of your boxes. */
@Composable
private fun DoorSwitch(
    doors: List<YourDoor>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val t = LocalLockerTokens.current
    val shape = RoundedCornerShape(15.dp)
    Recess(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            doors.forEach { door ->
                val isSelected = door.n == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(t.surface)
                                    .border(1.dp, t.hair, RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(t.lip.copy(alpha = 0.6f), Color.Transparent),
                                        ),
                                        RoundedCornerShape(11.dp),
                                    )
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(11.dp),
                                        ambientColor = t.shadow,
                                        spotColor = t.shadow,
                                    )
                            } else Modifier,
                        )
                        .clickable { onSelect(door.n) }
                        .padding(vertical = 8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = door.n,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = NumberFace,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = if (isSelected) t.ink else t.ink2,
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (door.soon) DoorLight else t.ink.copy(alpha = 0.32f)),
                        )
                        Spacer(Modifier.weight(1f))
                    }
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
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
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
                    text = if (free) "Room for a drop" else "All 20 boxes in use",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = t.ink,
                )
                Text(
                    text = if (free) "Give a courier your number and one is yours"
                    else "A courier cannot leave one here",
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
            .padding(horizontal = 19.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.NoEntry,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = t.refuse,
        )
        Text(
            text = "No free box",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = t.ink,
        )
    }
}
