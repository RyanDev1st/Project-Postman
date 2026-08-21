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
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import vn.edu.vgu.smartlocker.server.auth.LogSms
import vn.edu.vgu.smartlocker.server.auth.Otp
import vn.edu.vgu.smartlocker.server.auth.SpeedSms
import vn.edu.vgu.smartlocker.server.auth.Tokens
import vn.edu.vgu.smartlocker.server.auth.authRoutes
import vn.edu.vgu.smartlocker.server.cabinet.Boxes
import vn.edu.vgu.smartlocker.server.cabinet.CABINET_KEY_HEADER
import vn.edu.vgu.smartlocker.server.cabinet.Cabinets
import vn.edu.vgu.smartlocker.server.cabinet.Commands
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
    val sms = config.speedSmsToken?.let(::SpeedSms) ?: LogSms()
    val otp = Otp(db, sms, digits = config.pickupCodeDigits, maxPerHour = config.otpMaxPerHour)
    val sessions = Sessions(db, config.qrSessionSeconds)
    val commands = Commands(db)
    val boxes = Boxes(db, config.wrongTriesBeforeLock, config.boxLockMinutes)
    val collect = Collect(db, sessions, commands, config.openTimeoutSeconds)

    tokens.sweep()
    otp.sweep()

    routing {
        authRoutes(db, otp, tokens)
        parcelRoutes(db, tokens, collect)
        cabinetRoutes(db, sessions, boxes, commands, config.pickupCodeHours)
        doorRoutes(db, boxes, commands)

        /**
         * 15. The numbers, for a phone that is already installed.
         *
         * Open to both callers and to neither credential. It carries no
         * personal data and no rules - only the numbers already shipped inside
         * both front-ends - and demanding a token here would mean a phone
         * could not correct a wrong timeout until after it had logged in.
         */
        get("/settings") { call.respond(config.settingsForClients()) }

        /** Is it up. Says nothing about what is on it. */
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
