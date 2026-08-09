package otp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The real provider: SpeedSMS (api.speedsms.vn), the de-facto VN standard.
 *
 * Auth is Basic with the API access token and an empty password. `type 2` is
 * long-code SMS, which needs no brandname registration for testing. The
 * endpoint is plain Java HttpURLConnection, the same philosophy as the app's
 * Http.kt: fifteen small calls do not justify a client library.
 */
class SpeedSms(
    private val token: String,
    private val baseUrl: String = "https://api.speedsms.vn",
) : SmsProvider {

    override fun send(toE164: String, content: String): Boolean = try {
        val conn = (URL(baseUrl.trimEnd('/') + "/index.php/sms/send").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty(
                "Authorization",
                "Basic " + Base64.getEncoder()
                    .encodeToString("$token:x".toByteArray(Charsets.UTF_8)),
            )
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = Json.encodeToString(
            buildJsonObject {
                put("to", toE164.removePrefix("+"))
                put("content", content)
                put("type", 2)
            },
        )
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val status = conn.responseCode
        val text = if (status in 200..299) conn.inputStream else conn.errorStream
        val answer = text?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()

        // SpeedSMS says delivered only when status is exactly 1.
        status in 200..299 &&
            Json.parseToJsonElement(answer).jsonObject["status"]?.jsonPrimitive?.intOrNull == 1
    } catch (e: IOException) {
        false
    }
}
