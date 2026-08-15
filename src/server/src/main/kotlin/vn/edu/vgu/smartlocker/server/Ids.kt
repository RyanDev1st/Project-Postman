package vn.edu.vgu.smartlocker.server

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Random values, and the one-way function everything secret goes through
 * before it is written down.
 *
 * Three rules live here, and each is one line that is easy to get wrong
 * somewhere else:
 *
 * 1. **Randomness is [SecureRandom], never [kotlin.random.Random].** A token
 *    from a predictable generator is not a token.
 * 2. **A credential is hashed before it is stored.** Tokens, one-time codes,
 *    cabinet keys and typed pickup codes are all compared by hash, so the
 *    database file is a list of useless digests rather than a list of working
 *    logins. Passwords are the exception and use Argon2id - see `Passwords.kt`.
 * 3. **A comparison of two secrets is constant time.** `==` on a String stops
 *    at the first differing character, and the time it took says how much of
 *    the guess was right.
 */
object Ids {

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    /** An identifier for a row. Not a secret, just unique. */
    fun id(): String = bytes(12).let(encoder::encode).toString(Charsets.US_ASCII)

    /**
     * A secret handed to a caller: a token, a cabinet key, a session code.
     *
     * 32 bytes. The QR session code is one of these, which is why it cannot be
     * invented - see the first rule under "The scanned code" in the contract.
     */
    fun secret(): String = bytes(32).let(encoder::encode).toString(Charsets.US_ASCII)

    /**
     * A code a person has to read off a screen and type.
     *
     * Uniform over the whole range. Taking `nextInt() % 1_000_000` would skew
     * the low codes very slightly, and there is no reason to accept even that.
     */
    fun digits(count: Int): String {
        val bound = generateSequence(1) { it * 10 }.elementAt(count)
        return random.nextInt(bound).toString().padStart(count, '0')
    }

    /** What gets written down instead of the secret itself. */
    fun hash(secret: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray(Charsets.UTF_8))
            .let(encoder::encode)
            .toString(Charsets.US_ASCII)

    /**
     * Compare two hashes without leaking how far the match got.
     *
     * Both sides are already digests of a fixed length here, so this is belt
     * and braces - but the day somebody passes a raw value in, it still holds.
     */
    fun same(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun bytes(n: Int) = ByteArray(n).also(random::nextBytes)
}

/** Now, as the rest of the server counts it: epoch milliseconds, UTC. */
fun now(): Long = System.currentTimeMillis()

/**
 * A time on the wire.
 *
 * ISO 8601 in UTC, per the contract's ground rules. The app parses it as a
 * string and shows it, so the shape matters more than the precision.
 */
fun Long.asIso(): String =
    java.time.Instant.ofEpochMilli(this)
        .atOffset(java.time.ZoneOffset.UTC)
        .format(java.time.format.DateTimeFormatter.ISO_INSTANT)
