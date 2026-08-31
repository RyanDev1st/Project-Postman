package vn.edu.vgu.smartlocker.parcels.map

import org.json.JSONObject
import org.maplibre.geojson.Point
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The walk to the cabinet — the one the person holding the phone would take.
 *
 * Three answers, because there are three real situations and the card used to
 * draw the first one in all of them:
 *
 * - [Baked] — we do not know where you are, so the line is the last stretch to
 *   the gate, fetched once and kept. Honest: it says where the gate is, and
 *   claims nothing about where you started.
 * - [FromYou] — you are close enough to walk, so this is a real route from
 *   where you stand.
 * - [TooFar] — you are kilometres away. **Not a walk, and no line at all.**
 *   It carries the real distance, and where you are, so the map can show you
 *   and the cabinet together.
 * - [Unrouted] — we know exactly where you are and you are close enough to
 *   walk, but the router did not answer. Same picture as [TooFar]: your real
 *   distance, and no line.
 */
internal sealed interface Walk {

    data object Baked : Walk

    data class FromYou(
        val line: List<Point>,
        val metres: Int,
        val seconds: Int,
    ) : Walk

    /**
     * The cabinet is kilometres away.
     *
     * [you] is here because of what the first version of this got wrong. It
     * held only the distance, so the caption said "40 km away · too far to
     * walk" over the **baked walking line around the campus** — the map drew
     * the same picture at forty kilometres as at forty metres. Ryan, holding
     * it: *"doesnt show the right location it shows exactly this always"*.
     *
     * A red line on a map means "this is your way". Drawing one that is not
     * yours is the same lie as a caption that is not true, and the harder one
     * to catch, because a picture is not read as a claim.
     *
     * So at this distance the map draws no line. It shows two points — where
     * you are and where the cabinet is — and the gap between them is the
     * fact. The earlier reasoning against this, that framing both gives "a
     * view of a province with two invisible dots in it", was simply wrong:
     * both markers are drawn at a fixed size on the screen, so they are
     * exactly as visible at forty kilometres as at four hundred metres.
     */
    data class TooFar(val metres: Int, val you: Point) : Walk

    /**
     * We know where you are. We could not get you a route.
     *
     * This exists because of what [Baked] was being made to cover. Baked is
     * the honest answer to *we do not know where you are* — it draws the last
     * stretch to the gate and claims nothing about a starting point. It was
     * also the fallback when the routing call failed, and there the same
     * picture is a lie: the caption reads "7 min walk · 543 m" over a line
     * around the campus, to somebody standing in another city, and it is
     * stated with the same confidence as a real route.
     *
     * Found on 2026-08-31 with the emulator's DNS broken, which is a good
     * model of a phone on bad campus wifi — the exact condition a receiver
     * walking to a locker is most likely to be in.
     *
     * So: the straight-line distance from where you actually are, and no
     * line. Less than a route, and true.
     */
    data class Unrouted(val metres: Int, val you: Point) : Walk
}

/**
 * How far away the gate has to be before "walk" stops being the right word.
 *
 * 3 km is about forty minutes on foot. Past it the honest answer is a
 * distance, not a route — and the routing call is skipped entirely, which also
 * keeps a public service from being asked to plan a walk across a province.
 */
internal const val WALKABLE_METRES = 3_000

/**
 * A pedestrian route from [fromLat], [fromLon] to the gate.
 *
 * **Blocking. Call it off the main thread.**
 *
 * Valhalla on valhalla1.openstreetmap.de, over OSM data (ODbL) — the same
 * service that produced the baked line in [Route], so the live route and the
 * fallback come from one router and cannot disagree about where the path
 * runs. No key, no account, no card, which is the rule this project holds
 * services to.
 *
 * **Its own connection, deliberately not the app's `Http`.** That door adds
 * the receiver's bearer token to every request it sends. This is a third
 * party, and it has no business holding a token that opens lockers.
 *
 * Returns null on anything at all going wrong — no network, a refusal, a shape
 * that will not parse. The caller keeps the baked line, and the card still
 * shows where the gate is.
 */
internal fun routeToGate(fromLat: Double, fromLon: Double): Walk.FromYou? = try {
    val ask = JSONObject().apply {
        put(
            "locations",
            org.json.JSONArray().apply {
                put(JSONObject().apply { put("lat", fromLat); put("lon", fromLon) })
                put(
                    JSONObject().apply {
                        put("lat", Route.GATE_POINT.latitude())
                        put("lon", Route.GATE_POINT.longitude())
                    }
                )
            },
        )
        put("costing", "pedestrian")
    }
    // https only. A route request carries where a person is standing, and that
    // is not going over a plain socket.
    val url = URL(
        "https://valhalla1.openstreetmap.de/route?json=" +
            URLEncoder.encode(ask.toString(), "UTF-8")
    )
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8_000
        readTimeout = 12_000
        setRequestProperty("Accept", "application/json")
        // Named, as the service's fair-use policy asks of anything automated.
        setRequestProperty("User-Agent", "VGUSmartLocker-Android (student project)")
    }
    val status = conn.responseCode
    val body = (if (status in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()?.use { it.readText() }.orEmpty()
    conn.disconnect()

    if (status !in 200..299) {
        null
    } else {
        val leg = JSONObject(body).getJSONObject("trip").getJSONArray("legs").getJSONObject(0)
        val line = decodePolyline6(leg.getString("shape"))
        val summary = leg.getJSONObject("summary")
        if (line.size < 2) {
            null
        } else {
            Walk.FromYou(
                line = line,
                // Valhalla answers in kilometres unless told otherwise.
                metres = kotlin.math.round(summary.getDouble("length") * 1000).toInt(),
                seconds = kotlin.math.round(summary.getDouble("time")).toInt(),
            )
        }
    }
} catch (e: IOException) {
    null
} catch (e: org.json.JSONException) {
    null
}

/**
 * Valhalla's encoded polyline, at six decimal places.
 *
 * The same algorithm Google published for five, with the divisor moved: the
 * shape comes back at 1e6 and reading it at 1e5 puts the walk ten degrees
 * away, in the sea off Da Nang.
 */
private fun decodePolyline6(encoded: String): List<Point> {
    val out = mutableListOf<Point>()
    var i = 0
    var lat = 0
    var lon = 0
    while (i < encoded.length) {
        var shift = 0
        var result = 0
        var b: Int
        do {
            b = encoded[i++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20 && i < encoded.length)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        shift = 0
        result = 0
        do {
            b = encoded[i++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20 && i < encoded.length)
        lon += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        out += Point.fromLngLat(lon / 1e6, lat / 1e6)
    }
    return out
}
