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
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var cached: Map<String, RSAPublicKey> = emptyMap()
    @Volatile private var fetchedAtMs = 0L

    /** The Google account id behind a token, or null if anything is wrong. */
    fun subjectOf(idToken: String): String? {
        val parts = idToken.split(".")
        if (parts.size != 3) return null

        val header = decodeJson(parts[0]) ?: return null
        if (header.text("alg") != "RS256") return null
        val key = keyFor(header.text("kid") ?: return null) ?: return null

        val signature = decodeBytes(parts[2]) ?: return null
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
        if (!genuine) return null

        val claims = decodeJson(parts[1]) ?: return null
        if (claims.text("iss") !in ISSUERS) return null
        if (claims.text("aud") != clientId) return null
        val expiresAtSec = claims["exp"]?.jsonPrimitive?.longOrNull ?: return null
        if (now() >= expiresAtSec * 1000) return null

        return claims.text("sub")?.takeIf { it.isNotBlank() }
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
