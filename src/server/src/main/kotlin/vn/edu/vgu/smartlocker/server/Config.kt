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
    val speedSmsSender: String,
    val seedDemo: Boolean,
    val cabinetOrigins: List<String>,
) {

    /** A number from `settings.json`, or the fallback if somebody removed it. */
    fun num(key: String, fallback: Long): Long =
        (settings[key] as? JsonPrimitive)?.content?.toLongOrNull() ?: fallback

    /**
     * A true/false from `settings.json`, or the fallback.
     *
     * Only exactly `true` is true. Anything else - absent, misspelled, a
     * number, a string somebody meant as yes - reads as the fallback, and
     * every flag here defaults to the safe side.
     */
    fun flag(key: String, fallback: Boolean): Boolean =
        (settings[key] as? JsonPrimitive)?.content?.let { it == "true" } ?: fallback

    val qrSessionSeconds get() = num("qr_session_seconds", 60)
    val receiverTokenDays get() = num("receiver_token_days", 30)
    val pickupCodeDigits get() = num("pickup_code_digits", 6).toInt()
    val pickupCodeHours get() = num("pickup_code_hours", 48)
    val wrongTriesBeforeLock get() = num("wrong_tries_before_lock", 5).toInt()

    /**
     * Codes sent an hour, across everybody. A guard on the bill, not on a
     * person - the per-number cooldown never sees a caller that changes the
     * number every time, and each code is a real message somebody pays for.
     */
    val otpMaxPerHour get() = num("otp_max_per_hour", 1000).toInt()

    /** Per-caller ceilings. Argued in `Hardening.kt`, not here. */
    val authPerMinute get() = num("auth_per_minute", 300).toInt()
    val readPerMinute get() = num("read_per_minute", 120).toInt()
    val cabinetPerMinute get() = num("cabinet_per_minute", 600).toInt()
    val maxRequestBytes get() = num("max_request_bytes", 16384)

    /** See the note in `Hardening.kt`. Off until a real domain exists. */
    val sendHsts get() = flag("send_hsts", false)
    val boxLockMinutes get() = num("box_lock_minutes", 15)

    /**
     * How long an open nobody confirmed is believed before the parcel is
     * given back. There is no sensor, so a door-closed report may never come.
     */
    val openTimeoutSeconds get() = num("open_timeout_seconds", 120)

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

            // Before anything else. Everything that writes a credential down
            // goes through Ids.hash, and Ids.hash refuses to run without a
            // key - so this has to happen ahead of the first database write,
            // not lazily on the first login. Both the server and the admin
            // commands come through here.
            Pepper.load(File(env("LOCKER_PEPPER_FILE") ?: File(root, "config/pepper.key").path))

            return Config(
                settings = settings,
                port = env("PORT")?.toIntOrNull() ?: 8443,
                dbFile = File(env("LOCKER_DB") ?: File(root, "data/locker.db").path),
                certDir = File(env("LOCKER_CERT_DIR") ?: File(root, "config/dev-cert").path),
                // Unset means the demo provider, which prints the code to the
                // console. Never a silent fallback when a real send fails.
                speedSmsToken = env("SPEEDSMS_TOKEN"),
                // The brandname the message arrives from. Required: the live
                // API answers `sender not found` without one, for every
                // sms_type including 4. Registered in the SpeedSMS dashboard,
                // not here. Empty until somebody has.
                speedSmsSender = env("SPEEDSMS_SENDER").orEmpty(),
                seedDemo = env("LOCKER_SEED") == "1",
                // Where the cabinet screen is served from. It is a page on the
                // Pi calling a server somewhere else, so every call it makes
                // is cross-origin and a browser blocks it unless the server
                // names the origin back.
                //
                // Not in settings.json on purpose: that file is shipped to
                // the app and served at endpoint 15, and which machines run a
                // cabinet screen is not the app's business.
                //
                // **Include the port.** An origin is scheme, host AND port,
                // so `127.0.0.1` does not match `http://127.0.0.1:8137` and
                // the browser is refused with no useful message. Found by
                // getting a 403 where a 200 was expected.
                //
                //     LOCKER_CABINET_ORIGINS=cabinet.vgu.edu.vn,localhost:8137
                //
                // The default is localhost only, which is what a Pi serving
                // its own screen looks like. A deployment that serves the
                // screen from anywhere else has to say so out loud.
                cabinetOrigins = (env("LOCKER_CABINET_ORIGINS") ?: DEFAULT_ORIGINS)
                    .split(",").map(String::trim).filter(String::isNotEmpty),
            )
        }

        private fun env(name: String) = System.getenv(name)?.trim()?.takeIf(String::isNotEmpty)

        /** A Pi serving its own screen, on the ports a static server picks. */
        private const val DEFAULT_ORIGINS = "localhost,127.0.0.1"
    }
}

/** Read a string out of a settings object, for the few that are not numbers. */
fun JsonObject.text(key: String, fallback: String): String =
    (this[key] as? JsonPrimitive)?.jsonPrimitive?.content ?: fallback
