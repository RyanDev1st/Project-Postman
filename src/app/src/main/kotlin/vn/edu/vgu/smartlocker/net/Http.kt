package vn.edu.vgu.smartlocker.net

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * **The one door.** Every network call the app makes goes through here.
 *
 * Rule 1 in architecture.md, and task P1-04. Nothing else in the app opens a
 * connection, and a search of the source proves it - `scripts/checknet.py`.
 * The reason is not tidiness: it is that HTTPS, timeouts, the token header,
 * and the refusal codes are decided once instead of being re-decided, badly,
 * in whichever screen needed a call that afternoon.
 *
 * Three things this file will not do, each of them load-bearing:
 *
 * 1. **It never speaks plain HTTP.** Not in test, not behind a flag. A URL
 *    that is not `https://` throws before a socket is opened.
 * 2. **It never retries.** An open-door call moves real metal, and a retry
 *    can open a door nobody is standing at. Retrying is the caller's
 *    decision, made per call, and for [Api.collectByScan] the answer is no.
 * 3. **It never logs a token, a key, or a body.** There is no debug flag that
 *    turns that on, because a flag like that is always on somewhere.
 *
 * No HTTP library. Android carries `HttpURLConnection` and `org.json`, and
 * fifteen small JSON endpoints do not justify a dependency.
 */
object Http {

    /** How long to wait to reach the server, and then for it to answer. */
    private const val CONNECT_MS = 10_000
    private const val READ_MS = 20_000

    /** What the server calls us. Handy in its logs when something is odd. */
    private const val USER_AGENT = "VGUSmartLocker-Android"

    /**
     * The answer to a request. Three outcomes, not two.
     *
     * [Unclear] is the one that matters and the one usually left out. If the
     * network drops after the request goes out, the app does not know whether
     * the door opened. Saying "failed" would be a lie, and saying "worked"
     * would send somebody away from their own parcel. Rule 4 in
     * architecture.md: unclear is not success, and the screen has to say so.
     */
    sealed interface Answer<out T> {
        data class Ok<T>(val value: T) : Answer<T>

        /** The server said no, in a way it planned for. */
        data class Refused(val reason: Refusal) : Answer<Nothing>

        /** We do not know what happened. Never show this as success. */
        data class Unclear(val why: String) : Answer<Nothing>
    }

    /** Which credential rides on the request. The app only ever holds a token. */
    enum class Method { GET, POST }

    /**
     * Make one request and parse one JSON object back.
     *
     * @param token the receiver's token, or null before they have one. It is
     *   passed in rather than read from storage here, so that this file has no
     *   opinion about where the token lives and cannot accidentally log it.
     */
    fun call(
        baseUrl: String,
        path: String,
        method: Method,
        body: JSONObject? = null,
        token: String? = null,
    ): Answer<JSONObject> {
        val url = try {
            require(baseUrl.isNotBlank()) { "no server address is set" }
            URL(baseUrl.trimEnd('/') + path)
        } catch (e: Exception) {
            return Answer.Unclear("bad address: ${e.message}")
        }

        // HTTPS or nothing. Checked on the built URL rather than on the
        // string, so "HTTPS://", a redirect target, and anything else the URL
        // parser normalises are all covered by the same test.
        if (!url.protocol.equals("https", ignoreCase = true)) {
            return Answer.Unclear("refused to send over ${url.protocol}")
        }

        var conn: HttpsURLConnection? = null
        return try {
            conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = method.name
                connectTimeout = CONNECT_MS
                readTimeout = READ_MS
                // Redirects are off. A redirect can move an https request to
                // http, or to another host, and neither is something this app
                // should follow without being asked.
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
            }

            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val code = conn.responseCode
            val text = readBody(conn, code)
            interpret(code, text)
        } catch (e: SocketTimeoutException) {
            // The request may well have arrived. We simply do not know.
            Answer.Unclear("the server did not answer in time")
        } catch (e: IOException) {
            Answer.Unclear("could not reach the server")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Read the body from whichever stream the response put it on.
     *
     * `inputStream` throws for any 4xx or 5xx, and the refusal code we need is
     * in the body of exactly those responses.
     */
    private fun readBody(conn: HttpURLConnection, code: Int): String =
        try {
            val stream = if (code in 200..399) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } catch (e: IOException) {
            ""
        }

    /**
     * Turn a status code and a body into an [Answer].
     *
     * A refusal is read from the body's `code` field, never guessed from the
     * status. The server owns that vocabulary - the table in api-contract.md -
     * and mapping 404 to some invented meaning here is how two teams end up
     * disagreeing about what happened.
     */
    private fun interpret(status: Int, text: String): Answer<JSONObject> {
        val json = try {
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (e: Exception) {
            // A 200 we cannot read is not a success. It is an unknown.
            return Answer.Unclear("the server's answer could not be read")
        }

        if (status in 200..299) return Answer.Ok(json)

        val named = json.optString("code").takeIf { it.isNotBlank() }
        if (named != null) return Answer.Refused(Refusal.of(named))

        // A failure the contract does not describe. Not a refusal we can put
        // words to, so it is unclear, and the screen says something honest.
        return Answer.Unclear("the server answered $status")
    }
}
