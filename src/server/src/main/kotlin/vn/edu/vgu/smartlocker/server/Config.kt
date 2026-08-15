package vn.edu.vgu.smartlocker.server

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Every number the server runs on, read from the file a person edits.
 *
 * **`config/settings.json` is the one copy.** The app ships with it, the
 * cabinet reads it, and this server serves it back at endpoint 15. Reading it
 * here rather than repeating the numbers in Kotlin is the difference between
 * *one place to change a timeout* and *three places, two of which get missed*.
 *
 * Anything not in that file - where to listen, where the database goes, which
 * SMS provider - is an environment variable with a default, so
 * `./gradlew :server:run` works on a clean clone with nothing set.
 */
class Config private constructor(
    val settings: JsonObject,
    val port: Int,
    val dbFile: File,
    val certDir: File,
    val speedSmsToken: String?,
    val seedDemo: Boolean,
) {

    /** A number from `settings.json`, or the fallback if somebody removed it. */
    fun num(key: String, fallback: Long): Long =
        (settings[key] as? JsonPrimitive)?.content?.toLongOrNull() ?: fallback

    val qrSessionSeconds get() = num("qr_session_seconds", 60)
    val receiverTokenDays get() = num("receiver_token_days", 30)
    val pickupCodeDigits get() = num("pickup_code_digits", 6).toInt()
    val pickupCodeHours get() = num("pickup_code_hours", 48)
    val wrongTriesBeforeLock get() = num("wrong_tries_before_lock", 5).toInt()
    val boxLockMinutes get() = num("box_lock_minutes", 15)

    /**
     * What endpoint 15 sends back.
     *
     * **`server_base_url` is removed.** The app has to know the address before
     * it can ask, so a server-set address is circular - and worse, it hands
     * anybody who answers this call the power to point the app somewhere else.
     * The contract marks it fixed for exactly that reason.
     */
    fun settingsForClients(): JsonObject = JsonObject(
        settings.filterKeys { it != "server_base_url" && !it.startsWith("_") },
    )

    companion object {

        fun load(root: File): Config {
            val file = File(root, "config/settings.json")
            val settings = if (file.exists()) {
                Json.parseToJsonElement(file.readText()).jsonObject
            } else {
                JsonObject(emptyMap())
            }

            return Config(
                settings = settings,
                port = env("PORT")?.toIntOrNull() ?: 8443,
                dbFile = File(env("LOCKER_DB") ?: File(root, "data/locker.db").path),
                certDir = File(env("LOCKER_CERT_DIR") ?: File(root, "config/dev-cert").path),
                // Unset means the demo provider, which prints the code to the
                // console. Never a silent fallback when a real send fails.
                speedSmsToken = env("SPEEDSMS_TOKEN"),
                seedDemo = env("LOCKER_SEED") == "1",
            )
        }

        private fun env(name: String) = System.getenv(name)?.trim()?.takeIf(String::isNotEmpty)
    }
}

/** Read a string out of a settings object, for the few that are not numbers. */
fun JsonObject.text(key: String, fallback: String): String =
    (this[key] as? JsonPrimitive)?.jsonPrimitive?.content ?: fallback
