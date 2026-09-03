package vn.edu.vgu.smartlocker.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import vn.edu.vgu.smartlocker.server.auth.LogSms
import vn.edu.vgu.smartlocker.server.auth.Accounts
import vn.edu.vgu.smartlocker.server.auth.GoogleCerts
import vn.edu.vgu.smartlocker.server.auth.GoogleTokens
import vn.edu.vgu.smartlocker.server.auth.Otp
import vn.edu.vgu.smartlocker.server.auth.SpeedSms
import vn.edu.vgu.smartlocker.server.auth.Tokens
import vn.edu.vgu.smartlocker.server.auth.authRoutes
import vn.edu.vgu.smartlocker.server.bookings.Bookings
import vn.edu.vgu.smartlocker.server.bookings.bookingRoutes
import vn.edu.vgu.smartlocker.server.cabinet.Boxes
import vn.edu.vgu.smartlocker.server.cabinet.CABINET_KEY_HEADER
import vn.edu.vgu.smartlocker.server.cabinet.Cabinets
import vn.edu.vgu.smartlocker.server.cabinet.Commands
import vn.edu.vgu.smartlocker.server.cabinet.Ladder
import vn.edu.vgu.smartlocker.server.cabinet.Sessions
import vn.edu.vgu.smartlocker.server.cabinet.cabinetRoutes
import vn.edu.vgu.smartlocker.server.cabinet.doorRoutes
import vn.edu.vgu.smartlocker.server.parcels.Collect
import vn.edu.vgu.smartlocker.server.parcels.parcelRoutes

private val log = LoggerFactory.getLogger("locker")

/**
 * The server, and the handful of commands that set one up.
 *
 * ```
 *   ./gradlew :server:run                                  start it
 *   ./gradlew :server:run --args="cabinet list"            what exists
 *   ./gradlew :server:run --args="cabinet add ID NAME 20"  add one, print its key
 *   ./gradlew :server:run --args="cabinet rotate ID"       new key, old one dead
 * ```
 *
 * Adding a cabinet is the only time a cabinet key is ever printed. The server
 * stores its hash and cannot show it again - a stolen cabinet is answered with
 * `rotate`, not by looking the old key up.
 */
fun main(args: Array<String>) {
    val root = repoRoot()
    val config = Config.load(root)

    if (args.isNotEmpty()) {
        Db(config.dbFile).use { admin(it, args) }
        return
    }

    val db = Db(config.dbFile)
    if (config.seedDemo) Demo.seed(db)

    val keyStore = Tls.keyStore(config.certDir)
    announce(config)

    embeddedServer(
        Netty,
        configure = {
            // **Slow requests, which no rate limit catches.**
            //
            // A caller that opens a connection and then sends one byte a
            // minute costs almost nothing to make and holds a worker the whole
            // time. A thousand of them is one machine on a phone network, and
            // the request count stays near zero, so every per-caller ceiling
            // sees a quiet client. The defence is a clock, not a counter.
            //
            // Ten seconds to finish sending a request: the largest honest one
            // here is a few hundred bytes and the phones are on campus wifi.
            // Thirty to receive the answer, because a phone that walks behind
            // a wall mid-reply is a real person, not an attack.
            requestReadTimeoutSeconds = 10
            responseWriteTimeoutSeconds = 30

            sslConnector(
                keyStore = keyStore,
                keyAlias = Tls.ALIAS,
                keyStorePassword = { Tls.password() },
                privateKeyPassword = { Tls.password() },
            ) {
                port = config.port
                // Every interface, so a phone and an ESP32 on the same Wi-Fi
                // can reach it. The certificate names those addresses too.
                host = "0.0.0.0"
                keyStorePath = Tls.keyStoreFile(config.certDir)
            }
        },
        module = { locker(db, config) },
    ).start(wait = true)
}

/** Everything the server answers. */
fun Application.locker(db: Db, config: Config) {

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }

    /**
     * The cabinet screen is a web page, and a web page calling a server on a
     * different host is a cross-origin request. Without this the browser
     * refuses every call before it is sent, and the screen shows a cabinet
     * that cannot reach a server that is running perfectly well.
     *
     * **Named origins, never `anyHost()`.** The default is localhost, which
     * is a Pi serving its own screen; anything else is set on the machine
     * that runs the server, in `LOCKER_CABINET_ORIGINS`.
     *
     * This is not what keeps a stranger out - that is the cabinet key, and a
     * page on another origin cannot set `X-Cabinet-Key` any more than it
     * could guess it. CORS is about which pages the browser will hand a
     * reply to, and naming them costs nothing.
     *
     * The phone app is unaffected either way: it is not a browser and has no
     * origin.
     */
    install(CORS) {
        config.cabinetOrigins.forEach { allowHost(it, schemes = listOf("http", "https")) }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(CABINET_KEY_HEADER)
        allowMethod(HttpMethod.Post)
    }

    // Everything about being on a public network - headers, per-caller
    // ceilings, a body-size floor. Argued in Hardening.kt.
    harden(config)

    install(StatusPages) {
        // A refusal has already written its response. This only stops the
        // exception that ended the route from being logged as a fault.
        exception<Refused> { _, _ -> }

        exception<Throwable> { call, cause ->
            // The wire gets a code and nothing else. A stack trace on the
            // wire tells a stranger the shape of the server.
            log.error("unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, RefusalBody("SERVER_ERROR"))
        }
    }

    val tokens = Tokens(db, config.receiverTokenDays)
    val sms = config.speedSmsToken?.let { SpeedSms(it, config.speedSmsSender, config.speedSmsType) } ?: LogSms()
    val otp = Otp(db, sms, digits = config.pickupCodeDigits, maxPerHour = config.otpMaxPerHour)
    val sessions = Sessions(db, config.qrSessionSeconds)
    val commands = Commands(db)
    val boxes = Boxes(db, config.wrongTriesBeforeLock, config.boxLockMinutes)
    // The same two numbers a box lockout uses. One wrong-guess policy for the
    // whole product is easier to argue about than two that drift apart.
    val accounts = Accounts(db, config.wrongTriesBeforeLock, config.boxLockMinutes)

    // Endpoint 19 is off unless a client id is configured, and it says so -
    // `GOOGLE_OFF`, not a refusal that reads as "your Google account was
    // rejected". The id is an environment variable and not a setting,
    // because `config/settings.json` is served to both front-ends by
    // endpoint 15 and this does not belong in that answer.
    //
    // The allowed domains come the other way - out of `settings.json`, where
    // a person can add a subdomain without a release. They are not a secret
    // and not machine-specific: which university this locker serves is
    // written on the cabinet.
    val google = System.getenv("GOOGLE_CLIENT_ID")
        ?.takeIf { it.isNotBlank() }
        ?.let { GoogleTokens(it, GoogleCerts(), config.googleAllowedDomains) }
    if (google == null) {
        log.info("google    off - set GOOGLE_CLIENT_ID to turn endpoint 19 on")
    } else {
        log.info("google    on  - domains ${config.googleAllowedDomains.joinToString(", ")}")
    }
    val collect = Collect(db, sessions, commands, config.openTimeoutSeconds)
    val bookings = Bookings(db, boxes, config.bookingHours)
    val ladder = Ladder(db, bookings)

    tokens.sweep()
    otp.sweep()
    // A booking that ran out overnight holds a door until something sweeps
    // it. `Bookings` sweeps on every read and every write, so this only
    // matters for the first minutes after a restart - but a cabinet whose
    // doors are all held by yesterday is exactly what a restart follows.
    bookings.sweep()

    routing {
        // Grouped by what a flood of it would cost. `auth` is the paid path,
        // `cabinet` is a trusted device that polls constantly, `read` is
        // everything a signed-in phone does.
        rateLimit(AUTH_LIMIT) { authRoutes(db, otp, tokens, accounts, google) }
        rateLimit(READ_LIMIT) {
            parcelRoutes(db, tokens, collect)
            bookingRoutes(db, tokens, bookings, boxes)
        }
        rateLimit(CABINET_LIMIT) {
            cabinetRoutes(db, sessions, boxes, commands, bookings, ladder, config.pickupCodeHours)
            doorRoutes(db, boxes, commands)
        }

        /**
         * 15. The numbers, for a phone that is already installed.
         *
         * Open to both callers and to neither credential. It carries no
         * personal data and no rules - only the numbers already shipped inside
         * both front-ends - and demanding a token here would mean a phone
         * could not correct a wrong timeout until after it had logged in.
         */
        rateLimit(READ_LIMIT) {
            get("/settings") { call.respond(config.settingsForClients()) }
        }

        /**
         * Is it up. Says nothing about what is on it.
         *
         * Deliberately outside every limit. It touches no database and it is
         * what `checktest.py`, the cabinet and a person with curl all use to
         * ask whether the server is alive - a health check that answers 429 is
         * a health check that lies during exactly the incident it exists for.
         */
        get("/health") { call.respond(mapOf("ok" to true)) }
    }
}

/** The setup commands. Printing a cabinet key is the point of this. */
private fun admin(db: Db, args: Array<String>) {
    when {
        args.size >= 2 && args[0] == "cabinet" && args[1] == "list" ->
            Cabinets.all(db).forEach { (id, name) -> println("$id  $name") }

        args.size >= 4 && args[0] == "cabinet" && args[1] == "add" -> {
            val id = args[2]
            val name = args.getOrNull(3) ?: id
            val boxes = args.getOrNull(4)?.toIntOrNull() ?: 20
            if (Cabinets.find(db, id) != null) {
                println("$id already exists. Use `cabinet rotate $id` for a new key.")
                return
            }
            val key = Cabinets.create(db, id, name, boxes)
            println("added $id with $boxes boxes")
            println()
            println("  CABINET KEY: $key")
            println()
            println("Put it in src/cabinet/config.js as KEY, and in the ESP32 sketch.")
            println("It is not stored anywhere you can read it again.")
        }

        args.size >= 3 && args[0] == "cabinet" && args[1] == "rotate" -> {
            val key = Cabinets.rotate(db, args[2])
            if (key == null) println("no cabinet called ${args[2]}")
            else println("  NEW CABINET KEY for ${args[2]}: $key\n\nThe old key stopped working.")
        }

        else -> println("commands: cabinet list | cabinet add ID NAME [BOXES] | cabinet rotate ID")
    }
}

private fun announce(config: Config) {
    log.info("database  {}", config.dbFile.absolutePath)
    log.info("cert      {}", File(config.certDir, "locker.crt").absolutePath)
    log.info("sms       {}", if (config.speedSmsToken != null) "SpeedSMS" else "DEMO - codes print here")
    log.info("listening on https://localhost:{}", config.port)
    Tls.lanAddresses().forEach { log.info("        or https://{}:{}", it, config.port) }

    // The address is left blank in the repo on purpose - it is different on
    // every machine, and a LAN address is not something to commit. Printing
    // the line to paste is the difference between a two-minute setup and an
    // afternoon wondering why the phone cannot see anything.
    if (config.settings["server_base_url"]?.toString().orEmpty().trim('"').isBlank()) {
        val address = Tls.lanAddresses().firstOrNull() ?: "localhost"
        log.warn("")
        log.warn("config/settings.json has no server_base_url, so the app cannot reach this.")
        log.warn("  for a phone or the cabinet screen:  \"server_base_url\": \"https://{}:{}\"", address, config.port)
        log.warn("  for the Android emulator:           \"server_base_url\": \"https://10.0.2.2:{}\"", config.port)
        log.warn("")
    }
}

/**
 * The repo root, found by walking up until `config/settings.json` appears.
 *
 * Gradle runs the server from the module folder and a packaged run starts
 * wherever somebody happened to be, so neither can be assumed.
 */
private fun repoRoot(): File {
    var here: File? = File(".").absoluteFile
    while (here != null) {
        if (File(here, "config/settings.json").exists()) return here
        here = here.parentFile
    }
    return File(".").absoluteFile
}
