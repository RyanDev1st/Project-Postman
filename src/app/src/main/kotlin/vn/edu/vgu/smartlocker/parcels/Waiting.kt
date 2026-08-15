package vn.edu.vgu.smartlocker.parcels

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import vn.edu.vgu.smartlocker.net.Event
import vn.edu.vgu.smartlocker.net.Parcel

/**
 * Turning what the server sends into what the ticket draws.
 *
 * **Endpoint 5 sends three facts and no deadline** - cabinet name, box number,
 * and the time it arrived. The claim ticket wants six. The rest is arithmetic
 * on one number the server also serves, `pickup_code_hours`, so both ends
 * count the same window without adding a field to the contract.
 *
 * Kept out of `HomeScreen` on purpose. A screen cannot be run without a
 * device; these can, and every rule below is checked in `WaitingTest`.
 */

/** How the ticket's numbers are worked out. Both come from the server. */
data class Window(
    /** How long a parcel may sit before it stops being collectable. */
    val hours: Int,
    /** Under this many hours left, the ticket says hurry. */
    val soonHours: Int,
)

/**
 * The parcel to put on the big ticket, and the rest.
 *
 * The server sends them newest first. The ticket wants the **most urgent**,
 * which is the oldest, so the order is reversed here rather than asking the
 * server to sort two ways for two callers.
 */
fun waiting(
    parcels: List<Parcel>,
    window: Window,
    now: Instant,
    zone: ZoneId,
    say: Phrases,
): Pair<Claim?, List<SmallClaim>> {
    val oldestFirst = parcels.sortedBy { arrival(it) ?: Instant.MAX }
    val first = oldestFirst.firstOrNull() ?: return null to emptyList()
    return claim(first, window, now, zone, say) to
        oldestFirst.drop(1).map { small(it, window, now, zone, say) }
}

/**
 * The sentences the ticket needs, handed in rather than looked up.
 *
 * They are `stringResource` calls at the call site, and `stringResource` is a
 * composable - it cannot be called from a plain function, and a plain function
 * is what makes this testable. So the screen reads the strings and passes them
 * as lambdas, and the rules stay in one place with no Android in them.
 */
data class Phrases(
    /**
     * "14:20 today" / "09:00 tomorrow" / "14:20 on 18 Aug".
     *
     * `date` is filled in for every case and only read for [Day.OTHER]. The
     * word and the date cannot be joined here, because the order of the two
     * is a property of the language, not of the clock.
     */
    val at: (time: String, day: Day, date: String) -> String,
    /** "6 hours left", or the last-hours wording when it is under one. */
    val left: (hours: Long) -> String,
    /** "dropped 21:40 yesterday", for the one-row ticket. */
    val dropped: (time: String, day: Day, date: String) -> String,
)

/** Which day a time falls on, relative to now. The words are the caller's. */
enum class Day { TODAY, TOMORROW, YESTERDAY, OTHER }

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "18 Aug", in whatever language the phone is set to. */
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

fun claim(
    parcel: Parcel,
    window: Window,
    now: Instant,
    zone: ZoneId,
    say: Phrases,
): Claim {
    val arrived = arrival(parcel)
    val deadline = arrived?.plus(window.hours.toLong(), ChronoUnit.HOURS)
    val hoursLeft = deadline?.let { hoursBetween(now, it) }
    return Claim(
        cabinet = parcel.cabinetName,
        box = parcel.boxNumber,
        dropped = arrived?.let { clock(it, zone) }.orEmpty(),
        collectBy = deadline
            ?.let { say.at(clock(it, zone), dayOf(it, now, zone), date(it, zone)) }
            .orEmpty(),
        left = hoursLeft?.let(say.left).orEmpty(),
        pct = fraction(hoursLeft, window.hours),
        soon = hoursLeft != null && hoursLeft <= window.soonHours,
    )
}

fun small(
    parcel: Parcel,
    window: Window,
    now: Instant,
    zone: ZoneId,
    say: Phrases,
): SmallClaim {
    val full = claim(parcel, window, now, zone, say)
    val arrived = arrival(parcel)
    return SmallClaim(
        cabinet = full.cabinet,
        box = full.box,
        detail = arrived
            ?.let { say.dropped(clock(it, zone), dayOf(it, now, zone), date(it, zone)) }
            .orEmpty(),
        left = full.left,
        pct = full.pct,
        soon = full.soon,
    )
}

/**
 * The collected ledger.
 *
 * Only a collection belongs on it. The history holds every event this person
 * touched - drops, opens, faults - and a ledger headed *collected* that lists
 * a drop is a ledger that lies. The two actions below are the two ways a
 * parcel is actually picked up: by scanning, and by typing the backup code.
 */
fun ledger(events: List<Event>, zone: ZoneId): List<LedgerEntry> =
    events
        .filter { it.action in COLLECTIONS }
        .mapNotNull { event ->
            val at = parse(event.at) ?: return@mapNotNull null
            LedgerEntry(box = event.boxNumber, cabinet = event.cabinetName, when_ = date(at, zone))
        }

private val COLLECTIONS = setOf("collect-opened", "collect-by-code")

/**
 * How full the bar is: the share of the window **still left**, not used up.
 *
 * The design's own two samples fix this - 6 hours left draws 0.12, and 31
 * draws 0.65, which are 6/48 and 31/48. Reading it the other way round would
 * fill the bar as the parcel got more urgent, which is backwards.
 */
private fun fraction(hoursLeft: Long?, window: Int): Float {
    if (hoursLeft == null || window <= 0) return 0f
    return (hoursLeft.toFloat() / window).coerceIn(0f, 1f)
}

/** Rounded up, so 90 minutes left says 2 hours and never says 1. */
private fun hoursBetween(from: Instant, to: Instant): Long {
    val minutes = ChronoUnit.MINUTES.between(from, to)
    if (minutes <= 0) return 0
    return (minutes + 59) / 60
}

private fun dayOf(instant: Instant, now: Instant, zone: ZoneId): Day {
    val days = ChronoUnit.DAYS.between(
        now.atZone(zone).toLocalDate(),
        instant.atZone(zone).toLocalDate(),
    )
    return when (days) {
        0L -> Day.TODAY
        1L -> Day.TOMORROW
        -1L -> Day.YESTERDAY
        else -> Day.OTHER
    }
}

private fun clock(instant: Instant, zone: ZoneId): String =
    CLOCK.format(instant.atZone(zone))

private fun date(instant: Instant, zone: ZoneId): String =
    DATE.format(instant.atZone(zone))

private fun arrival(parcel: Parcel): Instant? = parse(parcel.arrivedAt)

/**
 * Lenient, like every other reader in `Models.kt`. A time the app cannot read
 * leaves the ticket's times blank rather than throwing: the box number is the
 * part that gets somebody to their parcel, and it is still right.
 */
private fun parse(text: String): Instant? =
    if (text.isEmpty()) null
    else try {
        Instant.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }
