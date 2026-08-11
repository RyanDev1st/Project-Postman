package otp.google

import java.io.IOException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Google's real signing keys, from the address in its OpenID discovery
 * document. Plain HttpURLConnection, the same reasoning as SpeedSms.kt: one
 * call every so often does not justify a client library.
 *
 * Only RSA keys are kept. Google publishes RSA today; if it ever adds
 * another kind, an unknown key type is skipped rather than guessed at.
 */
class GoogleCerts(
    private val url: String = "https://www.googleapis.com/oauth2/v3/certs",
) : Jwks {

    override fun keys(): Map<String, RSAPublicKey> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
        }
        val status = conn.responseCode
        if (status !in 200..299) {
            conn.disconnect()
            throw IOException("Google certs answered $status")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val factory = KeyFactory.getInstance("RSA")
        val decoder = Base64.getUrlDecoder()
        return buildMap {
            Json.parseToJsonElement(body).jsonObject["keys"]?.jsonArray.orEmpty()
                .forEach { entry ->
                    val jwk = entry.jsonObject
                    fun text(name: String) = jwk[name]?.jsonPrimitive?.contentOrNull
                    if (text("kty") != "RSA") return@forEach
                    val kid = text("kid") ?: return@forEach
                    val modulus = text("n") ?: return@forEach
                    val exponent = text("e") ?: return@forEach
                    val key = factory.generatePublic(
                        RSAPublicKeySpec(
                            BigInteger(1, decoder.decode(modulus)),
                            BigInteger(1, decoder.decode(exponent)),
                        ),
                    )
                    put(kid, key as RSAPublicKey)
                }
        }
    }
}
