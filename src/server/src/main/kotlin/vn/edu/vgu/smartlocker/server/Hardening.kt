package vn.edu.vgu.smartlocker.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlin.time.Duration.Companion.seconds
import org.slf4j.LoggerFactory

/**
 * What the server does about being on a public network.
 *
 * Separate from [locker] because it is a different kind of thing: nothing here
 * implements a rule about parcels, and all of it exists because port 8443 has
 * been reached from addresses nobody on this team owns.
 *
 * **What this can and cannot do.** These are host-level defences. They stop one
 * machine, or a few, from occupying the server or draining the SMS balance -
 * which is the attack this project has actually seen. They do **not** stop a
 * real distributed denial of service: a botnet saturating the campus uplink is
 * decided upstream of any code here, by whoever runs the network, and the
 * honest answer is a service in front of the server rather than a plugin
 * inside it. Saying otherwise would be a lie told in a config file.
 *
 * The three limits, and why the numbers are what they are:
 *
 * - **Asking for a code is the expensive one.** Every one is money once a real
 *   provider is connected. It already has a per-number cooldown and an hourly
 *   cap across everybody (BUG-015); this adds a per-caller ceiling so one host
 *   cannot spend the hour's budget in ten seconds.
 * - **Everything else is cheap but not free.** A read still takes a database
 *   lock, and the lock is one connection wide.
 * - **The cabinet talks constantly and is trusted.** It polls for door
 *   commands, so its ceiling is high on purpose. It holds a key; a caller who
 *   does not is refused before any limit is reached.
 *
 * **The numbers are per caller, and a campus is one caller.** Behind one
 * university NAT, three hundred students share one address, so a tight
 * per-address limit would lock out the exact crowd this is built for. They are
 * set well above a real cohort and well below a flood: `stress.py` from one
 * laptop reached about 114,000 requests a minute, which is three orders of
 * magnitude past anything below.
 */
private val log = LoggerFactory.getLogger("guard")

/** Asking for a code, and spending one. The paid path. */
val AUTH_LIMIT = RateLimitName("auth")

/** Reading a parcel list, the settings, health. Token or nothing. */
val READ_LIMIT = RateLimitName("read")

/** The cabinet screen and the cabinet's own electronics. Key-holders. */
val CABINET_LIMIT = RateLimitName("cabinet")

fun Application.harden(config: Config) {

    /**
     * Headers a browser acts on, on every reply.
     *
     * Only the cabinet screen is a browser, and it is the one front-end served
     * from a page rather than an app - so it is the only caller any of this
     * protects. It costs nothing to send them everywhere.
     *
     * `Server` is removed rather than set. The default names Ktor and its
     * version, which tells a stranger which advisories to read.
     */
    install(DefaultHeaders) {
        /**
         * **Off until there is a real domain in front, and that is not
         * timidity.**
         *
         * HSTS is remembered per host, not per port. This server is
         * `127.0.0.1:8443` and the cabinet screen is served from
         * `127.0.0.1:8137` over plain HTTP - the same host. Send the header
         * here and the browser upgrades the screen's own address too, the
         * static server has no TLS to answer with, and the cabinet goes dead
         * with no message. The browser then remembers that for a year, on the
         * machine of whoever was testing.
         *
         * It is also worth nothing today: the certificate is self-signed, so
         * there is no downgrade for HSTS to prevent that trusting the
         * certificate has not already allowed.
         *
         * Turn it on in the same change that puts a real certificate and a
         * real domain in front of this server, and not before.
         */
        if (config.sendHsts) {
            header(HttpHeaders.StrictTransportSecurity, "max-age=31536000; includeSubDomains")
        }
        // Do not guess a content type. Everything here is JSON and says so.
        header("X-Content-Type-Options", "nosniff")
        // Nothing we serve is meant to be framed. Clickjacking, one line.
        header("X-Frame-Options", "DENY")
        // A JSON API needs no scripts, styles, images or frames of its own.
        header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        // Never send our address to a third party as a referrer.
        header("Referrer-Policy", "no-referrer")
        header(HttpHeaders.Server, "")
    }

    install(RateLimit) {
        register(AUTH_LIMIT) {
            rateLimiter(limit = config.authPerMinute, refillPeriod = 60.seconds)
            requestKey { it.caller() }
        }
        register(READ_LIMIT) {
            rateLimiter(limit = config.readPerMinute, refillPeriod = 60.seconds)
            requestKey { it.signedInCaller() }
        }
        register(CABINET_LIMIT) {
            rateLimiter(limit = config.cabinetPerMinute, refillPeriod = 60.seconds)
            requestKey { it.cabinetCaller() }
        }
    }

    install(BodySizeLimit(config.maxRequestBytes))
}

/**
 * Who to count this request against.
 *
 * The address the socket came from, and nothing a caller can set. An
 * `X-Forwarded-For` header would be read from the request, which means an
 * attacker sets a new one per request and the limit stops existing. If this
 * server ever moves behind a proxy, the fix is Ktor's `XForwardedHeaders`
 * plugin plus a list of proxies to trust - never a bare header read.
 */
private fun io.ktor.server.application.ApplicationCall.caller(): String =
    request.origin.remoteAddress

/**
 * Count a signed-in caller against **their own** allowance, not their wifi's.
 *
 * A university NAT presents three hundred students as one address. Keying a
 * read limit by address there means the students rate-limit each other, and
 * the busiest lecture theatre looks exactly like an attack - the failure lands
 * on the crowd this is built for, at the moment it is most used.
 *
 * The token is the right identity: it is per person, the server issued it, and
 * a caller cannot mint one. Hashed before use so live credentials are not what
 * sits in the limiter's key map.
 *
 * No token means no allowance of one's own, so it falls back to the address -
 * an unauthenticated caller on this path is either about to be refused a 401
 * anyway or is scanning.
 */
private fun io.ktor.server.application.ApplicationCall.signedInCaller(): String =
    request.headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")?.trim()?.takeIf(String::isNotEmpty)
        ?.let { "t:" + Ids.hash(it) }
        ?: caller()

/**
 * Likewise for a cabinet: its key is its identity.
 *
 * One cabinet polls for door commands constantly and legitimately. Two
 * cabinets behind one campus address must not share an allowance, or adding a
 * second cabinet halves the first one's.
 */
private fun io.ktor.server.application.ApplicationCall.cabinetCaller(): String =
    request.headers[vn.edu.vgu.smartlocker.server.cabinet.CABINET_KEY_HEADER]
        ?.trim()?.takeIf(String::isNotEmpty)
        ?.let { "c:" + Ids.hash(it) }
        ?: caller()

/**
 * Refuse a body bigger than we could ever mean.
 *
 * Every request this API takes is a handful of short fields - a phone number,
 * a six-digit code, a box number. The largest honest one is a few hundred
 * bytes. Without a ceiling, a caller can announce a gigabyte and the server
 * will sit reading it, holding a connection and heap the whole time, and no
 * rate limit helps because it is one request.
 *
 * Checked on the declared length, before the body is read. A request that
 * lies about its length is a chunked upload, and Netty's own frame limits
 * apply there.
 */
private fun BodySizeLimit(maxBytes: Long) =
    createApplicationPlugin("BodySizeLimit") {
        onCall { call ->
            val declared = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            if (declared != null && declared > maxBytes) {
                log.warn(
                    "refused {} {} - body declares {} bytes, ceiling is {}",
                    call.request.httpMethod.value, call.request.path(), declared, maxBytes,
                )
                call.respond(HttpStatusCode.PayloadTooLarge, RefusalBody("BODY_TOO_LARGE"))
            }
        }
    }
