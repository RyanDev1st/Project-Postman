package vn.edu.vgu.smartlocker.parcels.map

import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.geojson.Point

/**
 * The walk, baked.
 *
 * A real pedestrian route from valhalla1.openstreetmap.de over OSM data
 * (ODbL), fetched 2026-08-11 and kept in `docs/designs/mockup/route.json`.
 * 543 m, 6 min 32 s. Baked because the gate does not move, and because a
 * routing call is the one part of a map that nobody serves for free.
 *
 * In its own file because it is data, not drawing: [LiveMap] renders it and
 * the Home card captions it, and neither of those needs to be reopened to
 * correct a coordinate.
 */
internal object Route {
    /** GeoJSON is longitude first. Reading route.json's lat,lon pairs in
     * order here would put the walk in the South China Sea. */
    val LINE: List<Point> = listOf(
        11.105934 to 106.614255,
        11.105837 to 106.613982,
        11.105716 to 106.613638,
        11.105885 to 106.613576,
        11.106110 to 106.613492,
        11.106231 to 106.613447,
        11.106769 to 106.613244,
        11.108100 to 106.612743,
        11.107839 to 106.612073,
        11.107516 to 106.611253,
        11.107471 to 106.611139,
    ).map { (lat, lon) -> Point.fromLngLat(lon, lat) }

    /** OSM node 12093474313, barrier=gate, open 06:00–23:00. */
    val GATE_POINT: Point = Point.fromLngLat(106.611139, 11.107471)

    /**
     * What the camera is asked to fit.
     *
     * Built from the walk itself rather than written down, so correcting a
     * coordinate cannot leave the framing pointing at the old one. There was a
     * hand-computed centre and zoom here before, worked out for a card assumed
     * to be 326dp by 112dp; the map knows its own size and the bounds let it
     * use that.
     */
    val BOUNDS: LatLngBounds = LatLngBounds.Builder()
        .includes(LINE.map { LatLng(it.latitude(), it.longitude()) })
        .build()

    /**
     * How far the walk is, **measured off the line above** rather than written
     * down beside it.
     *
     * The card used to carry "7 min walk · 540 m" as a default argument on a
     * composable, three metres and half a minute away from the route actually
     * drawn under it. Two numbers for one walk, and the one on the picture was
     * the one nobody could correct. Measuring means moving a coordinate cannot
     * leave the caption describing the old path.
     *
     * Comes out at 542.5 m against the router's own 543, which is the check
     * that the line here is the line that was fetched.
     */
    val METRES: Int = kotlin.math.round(
        LINE.zipWithNext().sumOf { (a, b) ->
            haversine(a.latitude(), a.longitude(), b.latitude(), b.longitude())
        }
    ).toInt()

    /**
     * 6 min 32 s, from the same valhalla1.openstreetmap.de response as the
     * line (2026-08-11, `docs/designs/mockup/route.json`).
     *
     * This one is a constant because it cannot be derived: a duration is the
     * router's judgement about gates, kerbs and crossings, not a length
     * divided by an assumed pace.
     */
    const val SECONDS: Int = 392
}

/**
 * Great-circle metres between two WGS-84 points.
 *
 * The walk is 543 m across eleven segments, so the flat-earth shortcut would
 * be accurate here — but it is wrong by a factor that grows with latitude, and
 * this is the kind of helper that gets reused for something longer later.
 */
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}
