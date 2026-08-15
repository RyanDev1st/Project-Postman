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
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.net.Backend
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
    /** The last failure, for the caller to turn into a sentence. Null if none. */
    val trouble: Answer<*>? = null,
)

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
fun rememberHome(backend: Backend, reloadKey: Any?): HomeData {
    val say = phrases()
    val zone = remember { ZoneId.systemDefault() }
    var data by remember { mutableStateOf(HomeData(emptyList(), emptyList(), emptyList())) }

    LaunchedEffect(reloadKey) {
        val window = Window(
            hours = backend.pickupCodeHours,
            soonHours = backend.collectSoonHours,
        )
        val now = Instant.now()

        when (val answer = backend.parcels()) {
            is Answer.Ok -> {
                val (big, rest) = waiting(answer.value, window, now, zone, say)
                data = data.copy(
                    parcels = listOfNotNull(big),
                    second = rest,
                    doors = doors(answer.value, window, now, zone, say),
                    trouble = null,
                )
            }
            else -> data = data.copy(trouble = answer)
        }

        // The ledger is the quietest thing on the screen. Failing to fetch it
        // is not worth a sentence over a parcel list that did arrive, so it
        // only ever replaces itself.
        when (val answer = backend.history()) {
            is Answer.Ok -> data = data.copy(ledger = ledger(answer.value, zone))
            else -> Unit
        }
    }

    return data
}

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
