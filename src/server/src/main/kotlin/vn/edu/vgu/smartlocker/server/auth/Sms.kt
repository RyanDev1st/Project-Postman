package vn.edu.vgu.smartlocker.server.auth

import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import org.slf4j.LoggerFactory

/**
 * How a one-time code reaches a phone.
 *
 * One interface, two implementations, because the choice of provider is a
 * business decision that changes and the login flow is not. See
 * `docs/findings/2026-08-11-otp-delivery-cost.md` for why SpeedSMS and not
 * Telegram or Zalo: it is the only route that needs no app, no account and no
 * data on the receiver's phone, which is what a login must not get wrong.
 */
interface Sms {
    /** True only when the provider confirmed it sent. Never optimistic. */
    fun send(toE164: String, text: String): Boolean
}

/**
 * The demo provider. Prints the code to the server console.
 *
 * **This is a test fake and says so.** It ships in the module because
 * `./gradlew :server:run` on a clean clone has no SMS account behind it, and a
 * server nobody can register against is a server nobody will test. It is
 * chosen only when `SPEEDSMS_TOKEN` is unset - never silently as a fallback
 * when a real send fails.
 */
class LogSms : Sms {
    private val log = LoggerFactory.getLogger("sms.demo")

    override fun send(toE164: String, text: String): Boolean {
        log.warn("DEMO SMS - no message was sent. To {}: {}", toE164, text)
        return true
    }
}

/**
 * SpeedSMS, the real one.
 *
 * `POST https://api.speedsms.vn/index.php/sms/send`, basic auth with the
 * access token as the username.
 *
 * **Type 4, the shared brandname.** The message arrives from `Verify` rather
 * than from a phone number. It was type 2, the long code, chosen on the belief
 * that a brandname meant a term of paperwork - which is true of *your own*
 * brandname and not of SpeedSMS's shared ones. `Verify` and `Notify` are
 * theirs, already carrier-registered, and need no business licence and no
 * template approval from us. That belief was the main reason this project
 * thought it had no way to send a code (see the 2026-08-18 finding, now in
 * docs/legacy).
 *
 * It also matters for delivery, not only for looks: Vietnamese carriers treat
 * a code from a random long number as the thing spam looks like, and a
 * brandname is what a bank's code arrives as.
 *
 * **Never sent for real yet.** Nobody on this team has a SpeedSMS token, so
 * every line below is read from their documentation, not measured. The first
 * real send is the test - see docs/findings/2026-08-21-otp-channel-choice.md.
 */
class SpeedSms(private val token: String) : Sms {

    private val log = LoggerFactory.getLogger("sms.speedsms")

    override fun send(toE164: String, text: String): Boolean {
        // SpeedSMS wants the number without the plus.
        val to = toE164.removePrefix("+")
        val body = """{"to":"$to","content":${quote(text)},"type":4}"""

        return try {
            val conn = (URI(ENDPOINT).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Basic " + basic())
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val answer = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            // Only a confirmed send counts. A code the user never received
            // must not be usable, so anything unclear here is a failure.
            val sent = code in 200..299 && Regex("\"status\"\\s*:\\s*\"?1\"?").containsMatchIn(answer)
            if (!sent) log.warn("SpeedSMS refused: HTTP {} {}", code, answer.take(200))
            sent
        } catch (e: Exception) {
            // The number and the text are not logged. A server log is not the
            // place for a phone number, and the code is a live credential.
            log.warn("SpeedSMS could not be reached: {}", e.javaClass.simpleName)
            false
        }
    }

    private fun basic(): String =
        Base64.getEncoder().encodeToString("$token:x".toByteArray(Charsets.UTF_8))

    /** Minimal JSON string escaping. The text is ours, but it is Vietnamese. */
    private fun quote(s: String): String =
        buildString {
            append('"')
            s.forEach {
                when (it) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(it)
                }
            }
            append('"')
        }

    private companion object {
        const val ENDPOINT = "https://api.speedsms.vn/index.php/sms/send"
    }
}
