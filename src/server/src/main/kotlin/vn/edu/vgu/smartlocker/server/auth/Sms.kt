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
 * **Nothing has ever been delivered by this class.** A real token was probed
 * against the live API on 2026-08-21 and found three faults in the request
 * below, all fixed here, none of them visible from their documentation. It
 * still cannot send: the account answers `sender not found` until a brandname
 * is registered in their dashboard, which is not a code change. See
 * docs/findings/2026-08-21-otp-channel-choice.md.
 *
 * @param sender the brandname to send from. Required by the API even for
 *   sms_type 4, whatever the SDKs imply.
 */
class SpeedSms(private val token: String, private val sender: String = "") : Sms {

    private val log = LoggerFactory.getLogger("sms.speedsms")

    override fun send(toE164: String, text: String): Boolean {
        // SpeedSMS wants the number without the plus.
        val to = toE164.removePrefix("+")

        // **Both spellings of the type field, on purpose.** Their own SDKs
        // disagree - SpeedSMSAPI.js and the 2020 PHP SDK send `sms_type`, the
        // 2020 C# SDK sends `type` - and the API accepts a body carrying both.
        // Sending one and guessing wrong is not a clean failure: an unknown
        // field is ignored, and the PHP SDK's default when `sms_type` is
        // absent is 2, the customer-care long code. The message would go out
        // from a random number, the shape carriers filter, looking sent.
        //
        // `to` may be a string or an array; both were tried against the live
        // API and neither is preferred, so it stays a string.
        //
        // **`sender` is required, whatever the SDKs imply.** They only demand
        // one for types 3, 5, 7 and 8, and type 4 is supposed to use SpeedSMS's
        // own `Notify`. The live API disagrees: every sms_type from 1 to 5,
        // with sender empty or set to Notify, Verify, SpeedSMS or VGU, answered
        // `sender not found` on an account with no brandname registered. So it
        // comes from configuration and there is nothing sensible to default it
        // to.
        val body = """{"to":"$to","content":${asciiJson(text)},""" +
            """"sms_type":4,"type":4,"sender":${asciiJson(sender)}}"""

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
 * **A whitelist, and it stays one.** Only a body that says success counts.
 * Anything else - an error, a maintenance page, a captive portal, an empty
 * body, a shape they change next year - is a code that may never have arrived,
 * and a code nobody received must never open an account.
 *
 * None of the shapes below are from documentation. All three were answered by
 * the live API on 2026-08-21:
 *
 *     success   {"status":"success","code":"00","data":{...}}
 *     bad token {"name":"Unauthorized","message":"...","code":0,"status":401}
 *     bad body  {"status":"error","code":"101","message":"Invalid or missing
 *                parameters"}         <- HTTP 200, so the code matters, not the
 *                                        status line
 *
 * The success is `"status":"success"`. This first accepted only `"status":1`,
 * read from a write-up, which every real send would have failed - the message
 * delivered, the user told it was not, nobody able to register at all. `1` is
 * still accepted because it costs nothing to.
 *
 * Their errors also put an HTTP code in the same `status` field a success uses,
 * so "the field is present" would have been the wrong test entirely.
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

/**
 * A JSON string with nothing in it above plain ASCII.
 *
 * **The one that stops a Vietnamese code being sent at all.** SpeedSMS refuses
 * a body with raw UTF-8 in it. Measured against the live API on 2026-08-21,
 * same account, same everything but the content:
 *
 *     "content":"Mã xác thực 123456"          -> {"status":"error","code":"101",
 *                                                 "message":"Invalid or missing
 *                                                 parameters"}
 *     "content":"M\u00e3 x\u00e1c th\u1ef1c"  -> past parameter validation
 *
 * Every code this server sends is Vietnamese - `Mã xác thực của bạn là ...` -
 * so raw UTF-8 meant every single send failed, and the reason came back as
 * "invalid parameters", which points at the phone number or the type and not
 * at the message. It cost nothing to find only because a refusal is free.
 *
 * This is what PHP's `json_encode` does by default, which is why their own SDK
 * never hit it and why nothing in their documentation mentions it.
 *
 * Escaped per UTF-16 code unit, so anything outside the basic plane comes out
 * as the surrogate pair JSON already expects.
 */
internal fun asciiJson(s: String): String =
    buildString {
        append('"')
        s.forEach {
            when {
                it == '"' -> append("\\\"")
                it == '\\' -> append("\\\\")
                it == '\n' -> append("\\n")
                it == '\r' -> append("\\r")
                it == '\t' -> append("\\t")
                it.code in 0x20..0x7E -> append(it)
                else -> append("\\u%04x".format(it.code))
            }
        }
        append('"')
    }
