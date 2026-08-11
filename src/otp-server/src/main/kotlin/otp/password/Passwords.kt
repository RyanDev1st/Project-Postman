package otp.password

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Argon2id hashing, and the rules about what a password may be.
 *
 * Argon2id is OWASP's first choice, and the reason is memory: a graphics
 * card can try billions of SHA-256 guesses a second because each one is
 * tiny, but every Argon2id guess has to hold 19 MB at once, which is what
 * takes the advantage away. The parameters below are OWASP's minimum for
 * Argon2id, written here rather than left to a default, because a default
 * that changes under us would silently weaken every new password.
 *
 * A stored hash carries its own parameters, so raising them later does not
 * strand the passwords hashed before the change.
 *
 * Format: `argon2id$<m>$<t>$<p>$<salt b64>$<hash b64>`.
 */
object Passwords {

    /** The shortest password we accept, in characters. */
    const val MIN_LENGTH = 8

    /** The longest, so nobody can post a megabyte and make us hash it. */
    const val MAX_LENGTH = 200

    private const val MEMORY_KB = 19 * 1024
    private const val ITERATIONS = 2
    private const val PARALLELISM = 1
    private const val SALT_BYTES = 16
    private const val HASH_BYTES = 32

    private val random = SecureRandom()

    /**
     * Why this password cannot be used, or null when it can.
     *
     * Deliberately short. Length is the rule that carries most of the
     * weight; a pile of "must contain a symbol" rules mostly teaches people
     * to write Password1! and reuse it everywhere.
     */
    fun refuse(password: String): String? = when {
        password.length < MIN_LENGTH -> "PASSWORD_TOO_SHORT"
        password.length > MAX_LENGTH -> "PASSWORD_TOO_LONG"
        password.isBlank() -> "PASSWORD_TOO_SHORT"
        else -> null
    }

    /** Hash a password with a fresh random salt. */
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(password, salt, MEMORY_KB, ITERATIONS, PARALLELISM)
        return listOf(
            "argon2id",
            MEMORY_KB.toString(),
            ITERATIONS.toString(),
            PARALLELISM.toString(),
            b64(salt),
            b64(hash),
        ).joinToString("$")
    }

    /**
     * Does this password match that stored hash?
     *
     * The comparison is constant time. A byte-by-byte one that stops at the
     * first difference leaks, through how long it took, how much of the
     * hash a guess got right.
     */
    fun matches(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 6 || parts[0] != "argon2id") return false
        val memoryKb = parts[1].toIntOrNull() ?: return false
        val iterations = parts[2].toIntOrNull() ?: return false
        val parallelism = parts[3].toIntOrNull() ?: return false
        val salt = unB64(parts[4]) ?: return false
        val expected = unB64(parts[5]) ?: return false
        val actual = derive(password, salt, memoryKb, iterations, parallelism, expected.size)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(
        password: String,
        salt: ByteArray,
        memoryKb: Int,
        iterations: Int,
        parallelism: Int,
        length: Int = HASH_BYTES,
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(memoryKb)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()
        val out = ByteArray(length)
        Argon2BytesGenerator().apply { init(params) }
            .generateBytes(password.toByteArray(Charsets.UTF_8), out)
        return out
    }

    private fun b64(bytes: ByteArray): String =
        Base64.getEncoder().withoutPadding().encodeToString(bytes)

    private fun unB64(text: String): ByteArray? = try {
        Base64.getDecoder().decode(text)
    } catch (e: IllegalArgumentException) {
        null
    }
}
