package vn.edu.vgu.smartlocker.parcels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.net.Backend
import vn.edu.vgu.smartlocker.net.Cabinet
import vn.edu.vgu.smartlocker.net.Http.Answer

/**
 * What Home shows, fetched from the server rather than made up.
 *
 * Until this file existed, `HomeScreen` defaulted to a sample parcel: Back
 * Gate, box 04, six hours left. It was drawn on a real tester's phone for six
 * builds and it was never anybody's parcel. A screen that invents its contents
 * is worse than an empty one, because an empty one is honest.
 */
data class HomeData(
    val parcels: List<Claim>,
    val second: List<SmallClaim>,
    val ledger: List<LedgerEntry>,
    /** Every waiting parcel as a door row, for the Cabinet tab. */
    val doors: List<SmallClaim> = emptyList(),
    /** How much room is free, or **null until the server has said**. */
    val free: FreeBoxes? = null,
    /** The last failure, for the caller to turn into a sentence. Null if none. */
    val trouble: Answer<*>? = null,
)

/**
 * How many doors stand empty, and at which cabinet.
 *
 * [cabinet] is filled in **only when there is exactly one cabinet**. With two,
 * this app cannot say which one somebody is standing near - it asks for no
 * location and holds none - so it names neither and the ticket draws no name.
 * The count stays true either way: it is every free door at VGU.
 */
data class FreeBoxes(val cabinet: String, val count: Int)

/**
 * Load the parcel list and the history, and turn them into ticket shapes.
 *
 * **Reloads when [reloadKey] changes** - arriving at Home, or coming back from
 * a collect - and at no other time. It is deliberately not on a timer: a
 * parcel list that refreshes itself is harmless, but the habit is not, and the
 * one call next door to this one opens a door.
 *
 * A failure leaves the last good lists on screen and reports the failure
 * beside them. Blanking a parcel somebody can see because one request timed
 * out would tell them their parcel had gone.
 */
@Composable
fun rememberHome(backend: Backend, reloadKey: Any?, targetId: String? = null): HomeData {
    val say = phrases()
    val zone = remember { ZoneId.systemDefault() }
    var data by remember { mutableStateOf(HomeData(emptyList(), emptyList(), emptyList())) }

    LaunchedEffect(reloadKey) {
        val window = Window(
            hours = backend.pickupCodeHours,
            soonHours = backend.collectSoonHours,
        )
        val now = Instant.now()

        // **All three at once.** None of them needs an answer from another,
        // and asking in turn cost Home the sum of three round trips - which
        // on a phone is the ticket appearing, then the count, then the
        // history, each arriving as its own visible step.
        //
        // Safe to run together: `Http.call` holds no state between calls and
        // opens a fresh connection for each one on purpose, and the token is
        // only ever read. Nothing here retries, and none of these opens a
        // door.
        val (waitingFor, past, room) = coroutineScope {
            val parcels = async { backend.parcels() }
            val history = async { backend.history() }
            val cabinets = async { backend.cabinets() }
            Triple(parcels.await(), history.await(), cabinets.await())
        }

        var next = when (waitingFor) {
            is Answer.Ok -> {
                val (big, rest) = waiting(waitingFor.value, window, now, zone, say, targetId)
                data.copy(
                    parcels = listOfNotNull(big),
                    second = rest,
                    doors = doors(waitingFor.value, window, now, zone, say),
                    trouble = null,
                )
            }
            else -> data.copy(trouble = waitingFor)
        }

        // The ledger is the quietest thing on the screen. Failing to fetch it
        // is not worth a sentence over a parcel list that did arrive, so it
        // only ever replaces itself.
        if (past is Answer.Ok) next = next.copy(ledger = ledger(past.value, zone))

        // Same rule for the count, and the same reason: a screen that says
        // *nothing is waiting for you* has already answered the question
        // somebody opened the app to ask, and a missing count is not worth
        // taking that back for.
        if (room is Answer.Ok) next = next.copy(free = freeBoxes(room.value))

        // Written once, not three times. Three assignments drew the screen
        // three times and let the reader watch it assemble.
        data = next
    }

    return data
}

/**
 * Turn the cabinet list into the one line Home draws.
 *
 * An empty list is `null` and not zero. *No cabinet answered* and *every box
 * is taken* are opposite facts, and the second one is the one that makes
 * somebody stop giving couriers their number.
 */
private fun freeBoxes(cabinets: List<Cabinet>): FreeBoxes? =
    if (cabinets.isEmpty()) null
    else FreeBoxes(
        cabinet = cabinets.singleOrNull()?.name.orEmpty(),
        count = cabinets.sumOf { it.free },
    )

/**
 * The sentences, read from resources here so [waiting] can stay a plain
 * function with no Android in it and be checked without a device.
 */
@Composable
private fun phrases(): Phrases {
    val today = stringResource(R.string.time_at_today)
    val tomorrow = stringResource(R.string.time_at_tomorrow)
    val yesterday = stringResource(R.string.time_at_yesterday)
    val on = stringResource(R.string.time_at_on)
    val hours = stringResource(R.string.time_hours_left)
    val dropped = stringResource(R.string.cab_dropped)

    fun when_(time: String, day: Day, date: String): String = when (day) {
        Day.TODAY -> today.format(time)
        Day.TOMORROW -> tomorrow.format(time)
        Day.YESTERDAY -> yesterday.format(time)
        Day.OTHER -> on.format(time, date)
    }

    return Phrases(
        at = ::when_,
        left = { hoursLeft -> hours.format(hoursLeft.toString()) },
        dropped = { time, day, date -> dropped.format(when_(time, day, date)) },
    )
}
