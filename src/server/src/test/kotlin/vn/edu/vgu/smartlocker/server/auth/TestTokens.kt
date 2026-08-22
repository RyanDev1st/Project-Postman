package vn.edu.vgu.smartlocker.server.auth

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * Google ID tokens, minted here, for tests only.
 *
 * Nothing in this file reaches Google. [google] is a keypair standing in for
 * Google's signing key; [attacker] stands in for somebody who can sign
 * whatever they like but does not hold Google's key.
 */
object TestTokens {
    const val CLIENT = "111222333.apps.googleusercontent.com"
    const val KID = "test-key"
    const val SUB = "108476213905551212121"
    const val NOW = 1_786_000_000_000L

    val google: KeyPair = rsa()
    val attacker: KeyPair = rsa()

    /** The key set a verifier should see: Google's, under its key id. */
    fun published(): Map<String, RSAPublicKey> = mapOf(KID to google.public as RSAPublicKey)

    /** A verifier wired to [published], on a clock stopped at [NOW]. */
    fun verifier(): GoogleTokens = GoogleTokens(CLIENT, Jwks { published() }) { NOW }

    /**
     * A token. Every argument is a check in GoogleTokens, so a test bends
     * exactly one of them. [tamperedSub] signs the honest claims and then
     * sends different ones, which is what editing a token after the fact
     * looks like on the wire.
     */
    fun mint(
        alg: String = "RS256",
        kid: String = KID,
        iss: String = "https://accounts.google.com",
        aud: String = CLIENT,
        sub: String = SUB,
        expSec: Long = NOW / 1000 + 3600,
        signWith: KeyPair = google,
        tamperedSub: String? = null,
    ): String {
        val header = b64("""{"alg":"$alg","kid":"$kid","typ":"JWT"}""")
        val honest = b64(claims(iss, aud, sub, expSec))
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(signWith.private)
            update("$header.$honest".toByteArray(Charsets.US_ASCII))
            b64(sign())
        }
        val sent = tamperedSub?.let { b64(claims(iss, aud, it, expSec)) } ?: honest
        return "$header.$sent.$signature"
    }

    fun claims(iss: String, aud: String, sub: String, expSec: Long) =
        """{"iss":"$iss","aud":"$aud","sub":"$sub","exp":$expSec,"email_verified":true}"""

    fun b64(text: String): String = b64(text.toByteArray(Charsets.UTF_8))

    fun b64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun rsa(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
}
