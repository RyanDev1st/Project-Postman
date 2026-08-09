package otp

/**
 * Vietnamese phone numbers, normalised to E.164.
 *
 * Accept what a user or the app types: 0908619328, 84908619328, +84908619328,
 * with or without spaces/dashes/dots. A Vietnamese mobile is `0` followed by
 * 9 or 10 digits (09x, 03x, 05x, 07x, 08x). A landline (028..., 024...) is
 * rejected: this server only ever sends to mobiles.
 */
object Phone {

    /** The E.164 form, or null when the number is not a Vietnamese mobile. */
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val local = when {
            digits.startsWith("84") && digits.length == 11 -> digits.drop(2)
            digits.startsWith("0") && digits.length == 10 -> digits.drop(1)
            else -> return null
        }
        // First digit after the leading 0: 3, 5, 7, 8 or 9 are mobile prefixes.
        return if (local.length == 9 && local.first() in "35789") "+84$local" else null
    }
}
