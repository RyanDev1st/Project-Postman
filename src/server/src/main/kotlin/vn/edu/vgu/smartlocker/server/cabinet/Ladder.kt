package vn.edu.vgu.smartlocker.server.cabinet

import java.text.Normalizer
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.bookings.Bookings
import vn.edu.vgu.smartlocker.server.str

/**
 * How sure the server is about who a typed number belongs to, and why.
 *
 * [EXACT] and [BOOKING] proceed on the number alone. [NEAR] and [NAMED]
 * proceed too, and **the receiver is told afterwards that the digits did not
 * match** - a mismatch resolved is still a mismatch, and the person who
 * mistyped their own number has no other way to find out. Task P4-06.
 */
enum class Confidence { EXACT, BOOKING, NEAR, NAMED }

/**
 * Who the shipper is delivering to, once the server is sure enough to say.
 *
 * [boxNumber] is set only when a live booking at **this** cabinet is holding a
 * door. Endpoint 11 then opens that door and no other.
 */
data class Match(
    val receiverId: String,
    val maskedName: String,
    val confidence: Confidence,
    val boxNumber: String? = null,
    /** The number the account really holds, for the notice. Never on screen. */
    val bookedNumber: String? = null,
)

/**
 * The ladder the shipper walks at the cabinet. ADR 0026, tasks P3-08 to P3-11.
 *
 * The number he types came off a parcel label, which came from whatever the
 * receiver typed into a shop days earlier. **A miss is far more often a typo
 * than a stranger**, and the person who made it is not standing there to be
 * asked - so the server tries harder than an exact match before it gives up,
 * and it is careful about exactly how much harder.
 *
 * ```
 *   A  exact, and a live booking here   -> that booking's door opens
 *   B  exact, no booking                -> any free door, as before
 *   C  within two digits of exactly one -> "did you mean ...?"
 *   D  within two of several, or none   -> type the name off the label
 *   E  the name matches none or several -> nobody. Contact them, go to ABO
 * ```
 *
 * **More than one candidate means no candidate, at any distance.** That rule,
 * not the tolerance, is what makes this safe. The tolerance was measured
 * against uniformly drawn digits and real numbers do not arrive that way -
 * students buy SIMs in batches, so two numbers a digit apart genuinely sit in
 * one cabinet. The screen therefore never offers a choice between two people;
 * it asks for the name instead.
 */
class Ladder(private val db: Db, private val bookings: Bookings) {

    /**
     * Rungs A to C: what a typed number resolves to, or null for rung D.
     *
     * Rung D is null and not a refusal of its own, because the caller's next
     * move is the same either way - ask for the name - and a second code
     * would only tell somebody probing which of the two happened.
     */
    fun byNumber(cabinetId: String, phone: String): Match? {
        exactly(cabinetId, phone)?.let { return it }

        // Rung C. Only live bookings at this cabinet are candidates: a near
        // miss is a guess, and a guess is only worth making about somebody
        // who is actually expecting a parcel here.
        val close = bookingsHere(cabinetId).filter { within(TOLERANCE, phone, it.phone) }
        val only = close.singleOrNull() ?: return null
        return only.toMatch(Confidence.NEAR)
    }

    /**
     * Rung D: the name off the label, checked against everybody expecting a
     * parcel at this cabinet. One match or nothing.
     *
     * **Matched against every live booking here, not only the near misses.**
     * Rung D covers a number that was within two digits of *nothing at all*,
     * and there would be no candidates to check a name against if it did not.
     * The bar that keeps this honest is elsewhere: the whole name has to
     * match, and exactly one person may match it.
     *
     * Accents and case come off both sides, so `NGUYEN VAN PHONG`,
     * `Nguyen Van Phong` and `Nguyễn Văn Phong` are one name - a courier
     * reading a label typed by a shop cannot be asked for tone marks.
     * `Nguyễn Văn Phúc` is a different name and stays one.
     */
    fun byName(cabinetId: String, typed: String): Match? {
        val wanted = plain(typed)
        if (wanted.isEmpty()) return null
        val hit = bookingsHere(cabinetId).filter { plain(it.fullName) == wanted }
        return hit.singleOrNull()?.toMatch(Confidence.NAMED)
    }

    /** Rung A or B, on an exact number. Null when nobody holds that number. */
    private fun exactly(cabinetId: String, phone: String): Match? {
        val row = db.row(
            "SELECT id, full_name FROM receivers WHERE phone = ?", phone,
        ) { it.str("id") to it.str("full_name") } ?: return null

        val (id, name) = row
        val held = bookings.at(cabinetId, id)
        return Match(
            receiverId = id,
            maskedName = mask(name),
            confidence = if (held != null) Confidence.BOOKING else Confidence.EXACT,
            boxNumber = held?.boxNumber,
            bookedNumber = phone,
        )
    }

    /** Everybody with a live booking at this cabinet, and their number. */
    private fun bookingsHere(cabinetId: String): List<Candidate> {
        bookings.sweep()
        return db.rows(
            """SELECT r.id, r.full_name, r.phone, b.box_number
                 FROM bookings b JOIN receivers r ON r.id = b.receiver_id
                WHERE b.cabinet_id = ? AND r.phone IS NOT NULL""",
            cabinetId,
        ) {
            Candidate(it.str("id"), it.str("full_name"), it.str("phone"), it.str("box_number"))
        }
    }

    private data class Candidate(
        val id: String,
        val fullName: String,
        val phone: String,
        val boxNumber: String,
    )

    private fun Candidate.toMatch(confidence: Confidence) = Match(
        receiverId = id,
        maskedName = mask(fullName),
        confidence = confidence,
        boxNumber = boxNumber,
        bookedNumber = phone,
    )

    private fun mask(name: String) =
        vn.edu.vgu.smartlocker.server.maskName(name).ifBlank { "***" }

    companion object {

        /**
         * How many digits may differ. **Measured, not guessed.**
         *
         * Simulated against twenty live bookings with real Vietnamese
         * operator prefixes: at two, a receiver who mistyped one digit is
         * found 100% of the time, one who mistyped two is found 100% of the
         * time, and a parcel for somebody with no booking at all is wrongly
         * offered a student 0.02% of the time. At one, the two-digit typo is
         * found only 15% of the time. At three the false offer rises to 0.35%
         * and keeps climbing.
         */
        const val TOLERANCE = 2

        /**
         * Whether two numbers are within [limit] edits of each other.
         *
         * Damerau-Levenshtein over the **nine national digits**, so `+84` and
         * a leading zero cannot pay for a typo somewhere real. A
         * substitution, an insertion, a deletion and a transposition each
         * cost one - the transposition is the reason this is not plain
         * Levenshtein, because `...328` typed as `...382` is the single most
         * common way a person mistypes a number and plain Levenshtein calls
         * that two edits.
         */
        fun within(limit: Int, a: String, b: String): Boolean =
            distance(national(a), national(b)) <= limit

        /** The nine subscriber digits, whatever shape the number arrived in. */
        internal fun national(number: String): String =
            number.filter(Char::isDigit).let {
                when {
                    it.length == 11 && it.startsWith("84") -> it.substring(2)
                    it.length == 10 && it.startsWith("0") -> it.substring(1)
                    else -> it
                }
            }

        /** Damerau-Levenshtein, the unrestricted-transposition-free form. */
        internal fun distance(a: String, b: String): Int {
            if (a == b) return 0
            if (a.isEmpty()) return b.length
            if (b.isEmpty()) return a.length

            // Three rows are enough: the transposition case looks back two.
            var twoAgo = IntArray(b.length + 1)
            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)

            for (i in 1..a.length) {
                current[0] = i
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var best = minOf(
                        current[j - 1] + 1,      // insert
                        previous[j] + 1,         // delete
                        previous[j - 1] + cost,  // substitute
                    )
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                        best = minOf(best, twoAgo[j - 2] + 1) // transpose
                    }
                    current[j] = best
                }
                val spare = twoAgo
                twoAgo = previous
                previous = current
                current = spare
            }
            return previous[b.length]
        }

        /**
         * A name with the accents and the case taken off, and its spacing
         * tidied. `Nguyễn Văn Phong` and `NGUYEN  VAN  PHONG` are one string
         * here; `Nguyễn Văn Phúc` is not.
         *
         * `đ` is handled on its own because it is a letter in Vietnamese
         * rather than a `d` with a mark, so stripping combining marks leaves
         * it alone - and `Đặng` typed as `Dang` would otherwise never match.
         */
        internal fun plain(name: String): String = Normalizer
            .normalize(name.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace(Regex("\\s+"), " ")
    }
}
