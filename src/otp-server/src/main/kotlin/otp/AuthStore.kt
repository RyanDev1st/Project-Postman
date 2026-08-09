package otp

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Codes and tokens, in memory, with every rule from the spec:
 * 6 digits, 5-minute TTL, 60-second resend cooldown, 5 tries, single use,
 * 30-day token TTL. Thread-safe maps because Ktor may serve concurrently.
 *
 * The clock is injected so tests can walk time. The provider is injected so
 * tests never touch the network.
 */
class AuthStore(
    private val provider: SmsProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** A token handed to a client. */
    data class Token(val value: String, val expiresAtMs: Long)

    private data class Code(
        val value: String,
        val sentAtMs: Long,
        val expiresAtMs: Long,
        var triesLeft: Int,
    )

    private data class Session(val phone: String, val expiresAtMs: Long)

    private val codes = ConcurrentHashMap<String, Code>()
    private val sessions = ConcurrentHashMap<String, Session>()
    private val random = SecureRandom()

    /**
     * Send a fresh code. Null on success; [RATE_LIMITED] within the cooldown;
     * [SEND_FAILED] when the provider did not confirm delivery (nothing is
     * stored in that case).
     */
    fun requestCode(phoneE164: String): String? {
        val existing = codes[phoneE164]
        if (existing != null && now() - existing.sentAtMs < COOLDOWN_MS) {
            return "RATE_LIMITED"
        }
        val value = "%06d".format(java.util.Locale.ROOT, random.nextInt(1_000_000))
        val content = "Mã xác thực của bạn là $value. Có hiệu lực trong 5 phút."
        if (!provider.send(phoneE164, content)) return "SEND_FAILED"
        codes[phoneE164] = Code(value, now(), now() + CODE_TTL_MS, MAX_TRIES)
        return null
    }

    /**
     * Check a code. Null on success, [WRONG_CODE] for anything else — wrong,
     * expired, already used, or no tries left. The spec's wording rule: a
     * wrong answer says the same thing as a wrong-and-expired one.
     */
    fun verifyCode(phoneE164: String, code: String): String? {
        val entry = codes[phoneE164] ?: return "WRONG_CODE"
        if (now() >= entry.expiresAtMs) {
            codes.remove(phoneE164)
            return "WRONG_CODE"
        }
        val matches = MessageDigest.isEqual(
            entry.value.toByteArray(Charsets.UTF_8),
            code.toByteArray(Charsets.UTF_8),
        )
        if (!matches) {
            entry.triesLeft -= 1
            if (entry.triesLeft <= 0) codes.remove(phoneE164)
            return "WRONG_CODE"
        }
        codes.remove(phoneE164)
        return null
    }

    /** Mint a fresh token for a phone. */
    fun issueToken(phoneE164: String): Token {
        val value = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(random.generateSeed(32))
        sessions[value] = Session(phoneE164, now() + TOKEN_TTL_MS)
        return Token(value, now() + TOKEN_TTL_MS)
    }

    /** The phone behind a token, or null when unknown or expired. */
    fun phoneOf(token: String): String? {
        val session = sessions[token] ?: return null
        if (now() >= session.expiresAtMs) {
            sessions.remove(token)
            return null
        }
        return session.phone
    }

    /** Swap a live token for a fresh one; null when the old one is dead. */
    fun refresh(token: String): Token? {
        val phone = phoneOf(token) ?: return null
        sessions.remove(token)
        return issueToken(phone)
    }

    /** Forget a token. */
    fun revoke(token: String) {
        sessions.remove(token)
    }

    private companion object {
        const val CODE_TTL_MS = 5L * 60 * 1000
        const val COOLDOWN_MS = 60_000L
        const val MAX_TRIES = 5
        const val TOKEN_TTL_MS = 30L * 24 * 3600 * 1000
    }
}
