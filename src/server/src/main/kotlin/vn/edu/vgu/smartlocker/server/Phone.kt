package vn.edu.vgu.smartlocker.server

/**
 * One phone number, written one way.
 *
 * The shipper types a number on the cabinet screen and the receiver typed one
 * into the app, days earlier, on a different keyboard. `0908 619 328`,
 * `+84908619328` and `84908619328` are the same person, and if the two sides
 * store them differently the parcel never reaches anybody.
 *
 * So there is one stored form - **E.164, `+84…`** - and everything is put into
 * it on the way in. This is the whole reason `receivers.phone` can be `UNIQUE`.
 */
object Phone {

    /**
     * Turn what a person typed into the stored form, or null if it is not a
     * Vietnamese mobile number.
     *
     * Vietnamese mobiles are `0` followed by 9 digits, so E.164 is `+84` and
     * those 9 digits: 12 characters in all.
     */
    fun normalise(typed: String): String? {
        val digits = typed.filter(Char::isDigit)

        val national = when {
            // +84908619328 or 84908619328
            digits.startsWith("84") && digits.length == 11 -> digits.substring(2)
            // 0908619328
            digits.startsWith("0") && digits.length == 10 -> digits.substring(1)
            // 908619328, typed without the trunk zero
            digits.length == 9 -> digits
            else -> return null
        }

        // A Vietnamese mobile prefix never starts with 0 or 1 after the trunk
        // zero is removed - those are landline and service ranges. Checked
        // here rather than in the route, so the cabinet and the app cannot
        // disagree about what a valid number is.
        if (national.first() in "01") return null

        return "+84$national"
    }

    /**
     * What the app shows, and the only form that may leave the server for a
     * public screen.
     *
     * The cabinet is a public terminal - rule 6 in `architecture.md` - so it
     * never receives a full number. This is not used for lookup, only for
     * showing a person which number they are looking at.
     */
    fun masked(e164: String): String =
        if (e164.length < 5) "***" else e164.take(3) + "*".repeat(e164.length - 5) + e164.takeLast(2)
}

/**
 * A name the cabinet screen is allowed to show.
 *
 * `Nguyễn Văn An` becomes `Nguyễn V. A***`. The shipper already knows who he is
 * delivering to; he only needs to confirm he has the right person. Without
 * masking, anybody can stand at the cabinet, type phone numbers, and collect
 * names. Contract endpoint 10, task P0-08.
 */
fun maskName(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    if (parts.isEmpty()) return "***"
    if (parts.size == 1) return parts[0].take(1) + "***"

    val family = parts.first()
    val middles = parts.subList(1, parts.size - 1).map { it.take(1) + "." }
    val given = parts.last().take(1) + "***"
    return (listOf(family) + middles + given).joinToString(" ")
}
