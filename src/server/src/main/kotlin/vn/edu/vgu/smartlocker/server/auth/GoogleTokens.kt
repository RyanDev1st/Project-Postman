package vn.edu.vgu.smartlocker.server.auth

import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Google's published signing keys, by `kid`. Injected so tests mint their own. */
fun interface Jwks {
    fun keys(): Map<String, RSAPublicKey>
}

/**
 * Checks a Google ID token without asking Google.
 *
 * An ID token is a JWT that Google signed. Checking it here rather than
 * calling Google's tokeninfo endpoint keeps the network off the login path
 * and out of a rate limit. The cost is that every check has to be written
 * down. They are all in [subjectOf], and each one has a test that attacks it.
 *
 * The four classic ways a JWT check is broken, and what closes each:
 *
 *  - `"alg": "none"`, or a swap to HMAC that uses the public key as the
 *    shared secret. RS256 is fixed in the code below. The token's own `alg`
 *    is never used to pick an algorithm - only compared to the one we allow.
 *  - A token signed by somebody else. The key is chosen from Google's
 *    published set by `kid`, never taken from the token.
 *  - A real Google token minted for a different app, replayed at us. `aud`
 *    must equal our own client id.
 *  - A token that has expired. `exp` is checked against the clock.
 *
 * The account is keyed by `sub`, never by email. A person can change their
 * Gmail address; the `sub` stays the same for as long as the account lives.
 *
 * Every failure returns null. Which check failed is never told to the
 * caller, for the same reason a wrong one-time code says only "wrong".
 */
class GoogleTokens(
    private val clientId: String,
    private val jwks: Jwks,
    /**
     * The hosted domains this server accepts, as `hd` carries them.
     *
     * A domain matches itself or anything under it: `vgu.edu.vn` admits staff,
     * and `student.vgu.edu.vn` matches through the dot. **The dot is the
     * check** - see [allows].
     */
    private val allowedDomains: List<String>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var cached: Map<String, RSAPublicKey> = emptyMap()
    @Volatile private var fetchedAtMs = 0L

    /**
     * What a token turned out to be.
     *
     * [WrongDomain] is told apart from [Invalid] on purpose, and it is the one
     * exception to this file's "every failure looks the same" rule. A student
     * tapping the button with their personal Gmail has made an ordinary
     * mistake and has to be told which account to use; that this system only
     * serves VGU is written on the cabinet, so saying so reveals nothing.
     * Every *other* failure stays a single answer, because telling a caller
     * which check failed helps only somebody probing it.
     */
    sealed interface Check {
        data class Ok(val sub: String) : Check
        data object Invalid : Check
        data object WrongDomain : Check
    }

    /** The Google account id behind a token, or null if anything is wrong. */
    fun subjectOf(idToken: String): String? = (check(idToken) as? Check.Ok)?.sub

    /** Every check, with the domain refusal kept separate. */
    fun check(idToken: String): Check {
        val parts = idToken.split(".")
        if (parts.size != 3) return Check.Invalid

        val header = decodeJson(parts[0]) ?: return Check.Invalid
        if (header.text("alg") != "RS256") return Check.Invalid
        val key = keyFor(header.text("kid") ?: return Check.Invalid) ?: return Check.Invalid

        val signature = decodeBytes(parts[2]) ?: return Check.Invalid
        val signed = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
        val genuine = try {
            Signature.getInstance("SHA256withRSA").run {
                initVerify(key)
                update(signed)
                verify(signature)
            }
        } catch (e: GeneralSecurityException) {
            false
        }
        if (!genuine) return Check.Invalid

        val claims = decodeJson(parts[1]) ?: return Check.Invalid
        if (claims.text("iss") !in ISSUERS) return Check.Invalid
        if (claims.text("aud") != clientId) return Check.Invalid
        val expiresAtSec = claims["exp"]?.jsonPrimitive?.longOrNull ?: return Check.Invalid
        if (now() >= expiresAtSec * 1000) return Check.Invalid

        // The domain, and it is checked after the signature on purpose: an
        // unsigned token's `hd` is whatever the sender typed.
        val hd = claims.text("hd")
        if (hd.isNullOrBlank() || !allows(hd)) return Check.WrongDomain

        val sub = claims.text("sub")?.takeIf { it.isNotBlank() } ?: return Check.Invalid
        return Check.Ok(sub)
    }

    /**
     * Whether a hosted domain is one of ours.
     *
     * **The leading dot is the whole check.** `hd.endsWith("vgu.edu.vn")`
     * alone also accepts `notvgu.edu.vn`, which anybody can register for a few
     * dollars and which would then hold accounts on this locker.
     */
    private fun allows(hd: String): Boolean {
        val seen = hd.lowercase()
        return allowedDomains.any { raw ->
            val allowed = raw.lowercase().removePrefix(".")
            seen == allowed || seen.endsWith(".$allowed")
        }
    }

    /**
     * The key with this id. An unknown id means Google may have rotated, so
     * the set is fetched again - but at most once a minute, or an attacker
     * could make us hammer Google by sending made-up key ids.
     */
    private fun keyFor(kid: String): RSAPublicKey? {
        cached[kid]?.let { return it }
        if (now() - fetchedAtMs < REFETCH_GAP_MS) return null
        fetchedAtMs = now()
        cached = try {
            jwks.keys()
        } catch (e: IOException) {
            emptyMap()
        }
        return cached[kid]
    }

    private fun JsonObject.text(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun decodeBytes(part: String): ByteArray? = try {
        Base64.getUrlDecoder().decode(part)
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun decodeJson(part: String): JsonObject? {
        val bytes = decodeBytes(part) ?: return null
        return try {
            Json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        val ISSUERS = setOf("accounts.google.com", "https://accounts.google.com")
        const val REFETCH_GAP_MS = 60_000L
    }
}
