package vn.edu.vgu.smartlocker.pickup

/**
 * What the cabinet screen puts in its QR, and what the app is willing to
 * believe about it.
 *
 *     VGU1|<cabinet-id>|<unix-seconds>|<random>
 *
 * **Issued by the server**, at endpoint 9, and drawn by `src/cabinet/qr.js`.
 * The server's `Sessions.format` writes it; this reads it; the cabinet only
 * carries it. All three are one contract and none may change shape alone.
 *
 * The first three fields are here so the app can answer *before it has any
 * signal*: standing in a corridor it can say "that one is old, scan again" or
 * "not one of ours" with no round trip. **Those answers decide nothing.** The
 * fourth field is 32 random bytes and is the only part that proves anything,
 * and only the server can check it. Without that field the code would be a
 * cabinet id and a clock reading, which anybody can type.
 *
 * **This is not a key and reading it opens nothing.** Identity comes from the
 * token in the app, so whoever photographs the screen still has to be signed
 * in as the person the parcel belongs to - P0-07, architecture.md section 6.
 * That is the property the whole design rests on, and it is why the checks
 * below are about *plausibility*, never about secrecy.
 *
 * Kept apart from the camera on purpose. A rule about the shape of a string
 * needs no lens to check, and every case below is a unit test rather than a
 * person standing in a corridor holding a phone.
 *
 * Public, unlike everything else in this file, because `ScanScreen` hands one
 * of these to whoever placed the screen - and every screen in this app is
 * public. The reader is the thing that stays internal.
 */
sealed interface Scanned {

    /** A code from one of our cabinets, recent enough to act on. */
    data class Ours(val cabinet: String, val at: Long) : Scanned

    /**
     * Ours, and too old.
     *
     * Not a failure and never shown as one. The screen refreshes on a timer,
     * so a person who walks up mid-cycle catches an expired one as a matter
     * of course. P5-05: the message says "scan again", with no alarm.
     */
    data class Stale(val cabinet: String, val secondsOld: Long) : Scanned

    /**
     * Some other QR entirely - a poster, a payment code, a wifi code.
     *
     * The camera reads whatever is in front of it, and a person waving a
     * phone around a lobby will catch things. Silence is the right answer:
     * the app keeps looking rather than accusing anybody of anything.
     */
    data object NotOurs : Scanned
}

/** The format marker. Its whole job is to let the app say "not ours" fast,
 * and to let the format change one day without a scanner that guesses. */
private const val FORMAT = "VGU1"

/**
 * How far out of date a code may be before it is refused.
 *
 * The cabinet gives each code 60 seconds and draws a new one every 30
 * (`qr_session_seconds`, `qr_refresh_seconds`). This is the same 60 with
 * room for the two clocks disagreeing - a phone and a lobby terminal are
 * both roughly right and neither is exact, and a person refused at a locker
 * because two clocks differ by four seconds would have no idea why.
 *
 * Being generous costs nothing here. The code is a place and a clock
 * reading, so the only thing this window stops is somebody scanning a
 * screenshot from last Tuesday.
 */
internal const val CODE_GOOD_FOR_SECONDS = 60L
private const val CLOCK_SLACK_SECONDS = 15L

/**
 * Read a scanned string, and say which of the three things it is.
 *
 * [now] is passed in rather than read here so the staleness rule can be
 * tested at any moment, on any machine, without waiting.
 */
internal fun readSessionCode(raw: String?, now: Long): Scanned {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return Scanned.NotOurs

    val parts = text.split('|')
    if (parts.size != 4) return Scanned.NotOurs
    if (parts[0] != FORMAT) return Scanned.NotOurs

    // The random field. Its contents mean nothing here - only the server can
    // say whether it issued this one - but a code without it is a code
    // somebody typed, and there is no reason to carry that to the server.
    if (parts[3].isEmpty()) return Scanned.NotOurs

    val cabinet = parts[1]
    if (cabinet.isEmpty()) return Scanned.NotOurs

    // A cabinet id reaches the screen from our own setup, but this string
    // arrived from a camera pointed at the world. Anything that is not a
    // plain id is not ours, whoever printed it.
    if (!cabinet.all { it.isLetterOrDigit() || it == '-' }) return Scanned.NotOurs
    if (cabinet.length > 64) return Scanned.NotOurs

    val at = parts[2].toLongOrNull() ?: return Scanned.NotOurs
    if (at <= 0) return Scanned.NotOurs

    val age = now - at

    // Ahead of us by more than the clocks could plausibly differ. Not stale -
    // that would tell somebody to scan again, and a second scan gives the
    // same answer. Whatever this is, it is not a code this cabinet issued a
    // moment ago.
    if (age < -CLOCK_SLACK_SECONDS) return Scanned.NotOurs

    if (age > CODE_GOOD_FOR_SECONDS + CLOCK_SLACK_SECONDS) {
        return Scanned.Stale(cabinet, age)
    }
    return Scanned.Ours(cabinet, at)
}

/**
 * What the app does about a code it has read.
 *
 * Reading the code and acting on it are two questions and this is the second
 * one. It is kept apart because the answer is about *you* - which parcels are
 * yours, and where - while [readSessionCode] is only about the string.
 */
internal sealed interface Pickup {

    /**
     * Worth asking the server about. **Not** a door opening.
     *
     * This is as far as the app's own judgement goes, and the change is the
     * point: until P5-03 this branch decided *which box opens*, from a list of
     * parcels the app was carrying. It cannot decide that, and it never could
     * - the list was a copy, the copy was stale the moment it was made, and an
     * attacker running their own build would simply write a different one.
     * Which door moves is the server's answer, in `Collect.byScan`.
     */
    data class Ask(val code: String) : Pickup

    /**
     * Ours, and expired. Normal, not a failure: the cabinet redraws every 30
     * seconds and anybody who walks up mid-cycle catches an old one.
     *
     * Answered here rather than by asking, only because it saves a round trip
     * standing in a corridor. The server refuses the same code with
     * `SESSION_EXPIRED` if it is ever sent, so nothing rests on this.
     */
    data object ScanAgain : Pickup

    /** Somebody else's QR. Say nothing and keep looking. */
    data object KeepLooking : Pickup
}

/**
 * Whether a scanned string is worth sending to the server.
 *
 * Three answers, and none of them opens anything. "Nothing here for you" is
 * missing on purpose: only the server knows what is in a cabinet, and the app
 * guessing at it was the last made-up thing in the pickup flow.
 *
 * [raw] is carried through rather than rebuilt from [read], because the server
 * compares the **whole string** against what it issued. A code reassembled
 * from the parts this file understood would differ by whatever this file did
 * not, and would be refused.
 */
internal fun whatToDo(read: Scanned, raw: String): Pickup =
    when (read) {
        is Scanned.Stale -> Pickup.ScanAgain
        Scanned.NotOurs -> Pickup.KeepLooking
        is Scanned.Ours -> Pickup.Ask(raw)
    }
