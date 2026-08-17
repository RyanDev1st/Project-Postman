package vn.edu.vgu.smartlocker.net

import org.json.JSONArray
import org.json.JSONObject
import vn.edu.vgu.smartlocker.net.Http.Answer

/**
 * The calls the app is allowed to make. One function per endpoint.
 *
 * These are endpoints 1 to 8 and 15 in `docs/reference/api-contract.md`.
 * **Endpoints 9 to 14 are the cabinet's and are deliberately absent** - the
 * cabinet signs in as a device with a key this app never holds, and a call
 * here that could look up a stranger's name would be a hole in the product,
 * not a missing feature. The contract's "two callers, not one" is enforced by
 * this file having no such function to call.
 *
 * Nothing here opens a connection. Everything goes through [Http], so the
 * HTTPS rule, the timeouts and the refusal vocabulary are decided once.
 *
 * Every function is blocking and must be called off the main thread. There is
 * no coroutine scope, no dispatcher and no callback here on purpose: this
 * layer's job is one request and one answer, and the screen that wants it
 * decides where that waiting happens - task P2-01 onward.
 */
class Api(private val settings: Settings, private val tokens: TokenStore) {

    private fun post(path: String, body: JSONObject, withToken: Boolean = true) =
        Http.call(
            baseUrl = settings.serverBaseUrl,
            path = path,
            method = Http.Method.POST,
            body = body,
            token = if (withToken) tokens.read() else null,
        )

    private fun get(path: String) =
        Http.call(
            baseUrl = settings.serverBaseUrl,
            path = path,
            method = Http.Method.GET,
            token = tokens.read(),
        )

    // --- Register and identity - endpoints 1 to 4 --------------------------

    /** 1. Ask for a one-time code by SMS. No token yet: this is the way in. */
    fun requestCode(phoneNumber: String): Answer<Unit> =
        post("/auth/request-code", JSONObject().put("phone_number", phoneNumber), withToken = false)
            .map { }

    /**
     * 2. Send the code back and get a token.
     *
     * The token is written to the secure store here rather than handed to the
     * caller, so no screen ever holds one and none can log it by accident.
     */
    fun verifyCode(phoneNumber: String, code: String, fullName: String = ""): Answer<Unit> {
        val body = JSONObject()
            .put("phone_number", phoneNumber)
            .put("code", code)
            // Taken only when the account has no name yet. Without a name the
            // cabinet shows the shipper `***`, which confirms nothing.
            .put("full_name", fullName)
        return when (val answer = post("/auth/verify-code", body, withToken = false)) {
            is Answer.Ok -> {
                tokens.write(Session.from(answer.value).token)
                Answer.Ok(Unit)
            }
            is Answer.Refused -> answer
            is Answer.Unclear -> answer
        }
    }

    /** 3. Swap a tiring token for a fresh one. */
    fun refresh(): Answer<Unit> =
        when (val answer = post("/auth/refresh", JSONObject())) {
            is Answer.Ok -> {
                tokens.write(Session.from(answer.value).token)
                Answer.Ok(Unit)
            }
            is Answer.Refused -> answer
            is Answer.Unclear -> answer
        }

    /**
     * 4. Log out.
     *
     * The local token is cleared **whatever the server says**. If the call
     * fails, the person who tapped Log out has still logged out on this phone;
     * leaving the token behind because the network was down would be the one
     * outcome nobody expects.
     */
    fun logout(): Answer<Unit> {
        val answer = post("/auth/logout", JSONObject())
        tokens.clear()
        return answer.map { }
    }

    // --- Parcels and pickup - endpoints 5 to 8 -----------------------------

    /** 5. What is waiting for me, and where. */
    fun waitingParcels(): Answer<List<Parcel>> =
        get("/parcels").map { json ->
            json.optJSONArray("parcels").each { Parcel.from(it) }
        }

    /**
     * 6. **The main path.** Scanned the cabinet's QR; open my box.
     *
     * This is the call that moves metal, so two rules apply and both are the
     * caller's to keep:
     *
     * - **Only from a real scan.** Never from a timer, a retry or a screen
     *   coming back into view. Rule 3 in architecture.md.
     * - **Never retried.** [Http] does not retry, and this must not be wrapped
     *   in anything that does. An [Answer.Unclear] here means *go and look at
     *   the cabinet*, not *ask again*.
     */
    fun collectByScan(sessionCode: String): Answer<Opened> =
        post("/parcels/collect", JSONObject().put("session_code", sessionCode))
            .map { Opened.from(it) }

    /** 7. What happened before. */
    fun history(): Answer<List<Event>> =
        get("/parcels/history").map { json ->
            json.optJSONArray("events").each { Event.from(it) }
        }

    /** 8. Tell the server where to send notifications for this phone. */
    fun registerDevice(deviceId: String): Answer<Unit> =
        post("/devices", JSONObject().put("device_id", deviceId)).map { }

    // --- Both callers - endpoint 15 ----------------------------------------

    /**
     * 15. Fetch the numbers we guessed, in case any have been corrected.
     *
     * The answer is applied by [Settings], which keeps the higher
     * `settings_version` and ignores `server_base_url` entirely. Task P1-08.
     */
    fun fetchSettings(): Answer<Unit> =
        get("/settings").map { settings.apply(it) }
}

/** Turn one answer into another without repeating the three-branch match. */
private inline fun <T, R> Answer<T>.map(f: (T) -> R): Answer<R> = when (this) {
    is Answer.Ok -> Answer.Ok(f(value))
    is Answer.Refused -> this
    is Answer.Unclear -> this
}

/**
 * Read a JSON array into a list, skipping anything that will not parse.
 *
 * A single malformed parcel should cost the user that parcel, not the whole
 * screen. Null array - the field was missing - reads as empty.
 */
private inline fun <T> JSONArray?.each(f: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        optJSONObject(i)?.let { out.add(f(it)) }
    }
    return out
}
