package vn.edu.vgu.smartlocker.net

import android.content.Context
import org.json.JSONObject

/**
 * The numbers we guessed, and how a correction reaches a phone.
 *
 * Every value here is a guess with a default - see the settings table in
 * `docs/reference/architecture.md`. They ship inside the app in
 * `res/raw/settings.json`, which is a copy of `config/settings.json` at the
 * repo root, and endpoint 15 replaces them at runtime when the server has a
 * newer set. That is task **P1-08**: until it is proved on a real phone,
 * changing a number still means a release.
 *
 * Three rules from `api-contract.md`, all of them enforced in [apply]:
 *
 * 1. **The higher `settings_version` wins.** Not the newest arrival - an old
 *    answer arriving late must not undo a correction.
 * 2. **A failed fetch keeps the last good copy.** Never fall back to nothing.
 * 3. **`server_base_url` is never taken from the server.** The app has to know
 *    the address before it can ask, so a server-set address is circular - and
 *    it would hand whoever answers that call the power to point this app
 *    somewhere else entirely.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val shipped: JSONObject = readShipped(context)

    /**
     * Where the server is.
     *
     * **Read only from the file that shipped**, never from [prefs], so no
     * network answer can move it. Blank until the Server team gives us an
     * address - P0-04 - and [Http] refuses a blank one rather than guessing.
     */
    val serverBaseUrl: String get() = shipped.optString(SERVER_URL)

    val settingsVersion: Int get() = number(VERSION, 1)

    val qrSessionSeconds: Int get() = number("qr_session_seconds", 60)
    val qrRefreshSeconds: Int get() = number("qr_refresh_seconds", 30)
    val receiverTokenDays: Int get() = number("receiver_token_days", 30)
    val pickupCodeDigits: Int get() = number("pickup_code_digits", 6)
    val pickupCodeHours: Int get() = number("pickup_code_hours", 48)
    val wrongTriesBeforeLock: Int get() = number("wrong_tries_before_lock", 5)
    val boxLockMinutes: Int get() = number("box_lock_minutes", 15)
    val uncollectedChaseDays: Int get() = number("uncollected_chase_days", 3)

    /**
     * Take a newer set of numbers from endpoint 15.
     *
     * Returns whether anything was taken, so a caller can say so in a log
     * without this class deciding to log anything itself.
     */
    fun apply(fromServer: JSONObject): Boolean {
        val incoming = fromServer.optInt(VERSION, 0)
        if (incoming <= settingsVersion) return false

        val editor = prefs.edit()
        for (key in fromServer.keys()) {
            // The one key the server may not set. Skipped rather than
            // rejected: an otherwise good settings answer should still apply.
            if (key == SERVER_URL) continue
            fromServer.opt(key)?.let { value ->
                when (value) {
                    is Int -> editor.putInt(key, value)
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    else -> Unit          // anything else is not a setting
                }
            }
        }
        editor.apply()
        return true
    }

    /** An int from the server's copy if it is newer, else the shipped one. */
    private fun number(key: String, fallback: Int): Int =
        if (prefs.contains(key)) prefs.getInt(key, fallback)
        else shipped.optInt(key, fallback)

    private fun readShipped(context: Context): JSONObject =
        try {
            val id = context.resources.getIdentifier(
                "settings", "raw", context.packageName,
            )
            if (id == 0) JSONObject()
            else context.resources.openRawResource(id)
                .bufferedReader().use { JSONObject(it.readText()) }
        } catch (e: Exception) {
            // Every getter above carries its own default, so an unreadable
            // file costs the shipped values and nothing else. The app still
            // runs, which is what matters at the point this is read.
            JSONObject()
        }

    private companion object {
        const val FILE = "vn.edu.vgu.smartlocker.settings"
        const val VERSION = "settings_version"
        const val SERVER_URL = "server_base_url"
    }
}
