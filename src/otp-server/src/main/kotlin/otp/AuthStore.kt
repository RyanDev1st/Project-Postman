package otp

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

    /**
     * Google account id -> the phone that account signed in with once.
     *
     * A Google account on its own cannot receive a parcel: the shipper finds
     * the receiver by phone number at the cabinet. So Google is a faster way
     * back into an account that a one-time code already proved, never a way
     * to make one.
     */
    private val googleLinks = ConcurrentHashMap<String, String>()
    private val random = SecureRandom()

    /**
     * Send a fresh code. Null on success; [RATE_LIMITED] within the cooldown;
     * [SEND_FAILED] when the provider did not confirm delivery (only the code
     * this call placed is removed in that case).
     */
    fun requestCode(phoneE164: String): String? {
        val value = "%06d".format(java.util.Locale.ROOT, random.nextInt(1_000_000))
        val content = "Mã xác thực của bạn là $value. Có hiệu lực trong 5 phút."
        val fresh = AtomicBoolean(false)
        val placed = AtomicReference<Code>()
        codes.compute(phoneE164) { _, existing ->
            if (existing != null && now() - existing.sentAtMs < COOLDOWN_MS) {
                existing
            } else {
                fresh.set(true)
                val t = now()
                Code(value, t, t + CODE_TTL_MS, MAX_TRIES).also { placed.set(it) }
            }
        }
        if (!fresh.get()) return "RATE_LIMITED"
        if (!provider.send(phoneE164, content)) {
            // Remove-if-equal: only delete the code this call placed. A later
            // send may have replaced it; that newer code must survive.
            placed.get() ?: return "SEND_FAILED"
            codes.remove(phoneE164, placed.get())
            return "SEND_FAILED"
        }
        return null
    }

    /**
     * Check a code. Null on success, [WRONG_CODE] for anything else — wrong,
     * expired, already used, or no tries left. The spec's wording rule: a
     * wrong answer says the same thing as a wrong-and-expired one.
     */
    fun verifyCode(phoneE164: String, code: String): String? {
        val outcome = AtomicReference<String?>()
        codes.compute(phoneE164) { _, entry ->
            when {
                entry == null -> {
                    outcome.set("WRONG_CODE")
                    null
                }
                now() >= entry.expiresAtMs -> {
                    outcome.set("WRONG_CODE")
                    null
                }
                MessageDigest.isEqual(
                    entry.value.toByteArray(Charsets.UTF_8),
                    code.toByteArray(Charsets.UTF_8),
                ) -> {
                    outcome.set(null)
                    null
                }
                else -> {
                    outcome.set("WRONG_CODE")
                    val left = entry.triesLeft - 1
                    if (left <= 0) null else entry.copy(triesLeft = left)
                }
            }
        }
        return outcome.get()
    }

    /** Mint a fresh token for a phone. */
    fun issueToken(phoneE164: String): Token {
        val value = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(random.generateSeed(32))
        val t = now()
        sessions[value] = Session(phoneE164, t + TOKEN_TTL_MS)
        return Token(value, t + TOKEN_TTL_MS)
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
        val phone = AtomicReference<String?>()
        sessions.compute(token) { _, session ->
            when {
                session == null -> null
                now() >= session.expiresAtMs -> null
                else -> {
                    phone.set(session.phone)
                    null
                }
            }
        }
        val p = phone.get() ?: return null
        return issueToken(p)
    }

    /** Forget a token. */
    fun revoke(token: String) {
        sessions.remove(token)
    }

    /** The phone behind a Google account, or null while it is unlinked. */
    fun phoneOfGoogle(sub: String): String? = googleLinks[sub]

    /**
     * Tie a Google account to a phone. Doing it again re-points it, which
     * needs both the Google account and a one-time code on the new phone -
     * and both of those belong to the same person.
     */
    fun linkGoogle(sub: String, phoneE164: String) {
        googleLinks[sub] = phoneE164
    }

    private companion object {
        const val CODE_TTL_MS = 5L * 60 * 1000
        const val COOLDOWN_MS = 60_000L
        const val MAX_TRIES = 5
        const val TOKEN_TTL_MS = 30L * 24 * 3600 * 1000
    }
}
