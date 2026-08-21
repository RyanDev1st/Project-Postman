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
 * **Type 4, the shared brandname `Notify`.** Their own SDKs name it:
 * `TYPE_BRANDNAME_NOTIFY = 4  // Gửi sms sử dụng brandname Notify`. The message
 * arrives from `Notify` rather than from a phone number, and it is SpeedSMS's
 * brandname, not ours - so it needs no business licence and no registration
 * from us.
 *
 * It was type 2, the customer-care long code, chosen on the belief that any
 * brandname meant a term of paperwork. That is true of **your own** brandname
 * - 200,000 VND to create and 200,000 VND a month to keep, on their price list
 * - and not of theirs. That one wrong belief was the main reason this project
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

        // **Both spellings of the type field, on purpose.**
        //
        // Their own SDKs disagree, and we cannot test which is right without
        // an account. Read from the official downloads on 2026-08-21:
        // SpeedSMSAPI.js and SpeedSMSAPI_PHP_2020 send `sms_type`;
        // SpeedSMSAPI-CSharp_2020 sends `type`. Sending both costs nothing and
        // removes the guess. The wrong one is an unknown field, which their
        // API ignores; sending only the wrong one silently falls back to
        // sms_type 2, the customer-care long code - a message that looks sent,
        // arrives from a random number, and is the shape carriers filter.
        //
        // `sender` is empty because type 4 does not use it. Only types 3, 5, 7
        // and 8 require one, and every SDK sends the field regardless.
        val body =
            """{"to":"$to","content":${quote(text)},"sms_type":4,"type":4,"sender":""}"""

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

            val sent = speedSmsAccepted(code, answer)
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

/**
 * Did SpeedSMS say it sent the message?
 *
 * Its own function because it is the line that decides whether somebody's code
 * is real, and because every other way of checking it costs 350 VND. A unit
 * test can ask it a hundred times for nothing.
 *
 * **A whitelist, and it stays one.** Only a body that says success in a
 * spelling we know counts. Anything else - an error, a maintenance page, a
 * captive portal, an empty body, a shape they change next year - is a code
 * that may never have arrived, and a code nobody received must never open an
 * account. Two spellings because their documentation shows both and this team
 * has never seen a real success.
 *
 * The refusal below is not from documentation. It is what the live endpoint
 * answered a bogus token on 2026-08-21:
 *
 *     {"name":"Unauthorized","message":"...","code":0,"status":401}
 *
 * Note `"status":401` - their errors put an HTTP code in the same field a
 * success uses, so "the field is present" would have been the wrong test.
 *
 * If a real success turns out to be a third spelling, this returns false and
 * the failure is loud and safe: the SMS arrives, the user is told it did not,
 * and `SpeedSMS refused` appears in the log with the exact body. Fix it then,
 * with the body in hand, rather than guessing wider now.
 */
internal fun speedSmsAccepted(httpCode: Int, body: String): Boolean =
    httpCode in 200..299 && SPEEDSMS_SUCCESS.containsMatchIn(body)

/**
 * `"status": 1`, `"status": "1"` or `"status": "success"`.
 *
 * The bare `1` refuses to be followed by another digit. Written `"?1"?` first,
 * which matched the `1` at the front of `"status":12` and `"status":100` and
 * called both a successful send - and 1xx is exactly what an error carries.
 * Caught by the test beside this, not by reading it.
 */
private val SPEEDSMS_SUCCESS = Regex("\"status\"\\s*:\\s*(\"1\"|\"success\"|1(?![0-9]))")
