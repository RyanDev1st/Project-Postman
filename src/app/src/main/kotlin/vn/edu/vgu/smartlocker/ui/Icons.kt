package vn.edu.vgu.smartlocker.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The app's icons, **verbatim from the mock-up**.
 *
 * Every one of these is the `d=` string out of `docs/designs/mockup/body.html`,
 * parsed rather than transcribed. Half the set used to be `Icons.Filled.*`
 * from Material, which is a different drawing in a different weight: the
 * design's are 2px open strokes with round caps, and Settings is a **row of
 * faders**, not a gear. On a 17dp tab icon that is not a detail — the filled
 * shapes read as a heavier, blunter app.
 *
 * Transcribing arcs into `PathBuilder` calls by hand is where this goes wrong
 * quietly, so nothing is transcribed. `<circle>` and `<rect rx>` become path
 * strings because SVG has no other way to say them here, and those two
 * conversions are the only hand-written geometry in the file.
 */
object AppIcons {

    // ---- tab bar ---------------------------------------------------------

    /** A roof and two walls. Open, not the filled Material house. */
    val Home: ImageVector by lazy {
        stroke("home", "M3 10.5 12 3l9 7.5", "M5 9.5V21h14V9.5")
    }

    /** A wall of doors: the frame, then the joints between them. */
    val Cabinet: ImageVector by lazy {
        stroke("cabinet", rect(3f, 3f, 18f, 18f, 2f), "M3 9h18M3 15h18M9 3v18")
    }

    /**
     * Three faders, each with its handle at a different place.
     *
     * This was a gear. A gear is the icon for a machine's settings; the
     * design's is the icon for *preferences you slide*, and the three handles
     * at three positions are the whole point of it.
     */
    val Settings: ImageVector by lazy {
        stroke(
            "settings",
            "M4 7h9M17.5 7H20M4 12h3M11.5 12H20M4 17h9M17.5 17H20",
            circle(15f, 7f, 2.1f),
            circle(9f, 12f, 2.1f),
            circle(15f, 17f, 2.1f),
        )
    }

    // ---- app bar ---------------------------------------------------------

    /** Back — a chevron alone, no shaft. */
    val Back: ImageVector by lazy { stroke("back", "M15 18l-6-6 6-6") }

    /** A bell with its clapper. */
    val Bell: ImageVector by lazy {
        stroke(
            "bell",
            "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9",
            "M13.7 21a2 2 0 0 1-3.4 0",
        )
    }

    /** The menu: three rules, the last one short. */
    val Menu: ImageVector by lazy { stroke("menu", "M4 7h16M4 12h16M4 17h10") }

    /** A viewfinder: four corners and a scanning line. */
    val Scan: ImageVector by lazy {
        stroke(
            "scan",
            "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2" +
                "M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2",
            "M7 12h10",
        )
    }

    /** The same viewfinder — the primary button's mark is the scan mark. */
    val OpenDoor: ImageVector get() = Scan

    // ---- rows and lists --------------------------------------------------

    /** The chevron at the end of a settings row. */
    val ChevronRight: ImageVector by lazy { stroke("chevronRight", "M9 6l6 6-6 6") }

    /** An arrow with a shaft — a step forward, not a row to open. */
    val ArrowRight: ImageVector by lazy { stroke("arrowRight", "M5 12h13M13 6l6 6-6 6") }

    /** PIN code — a padlock, shackle drawn over the body. */
    val Lock: ImageVector by lazy {
        stroke("lock", rect(4f, 10f, 16f, 11f, 2f), "M8 10V7a4 4 0 0 1 8 0v3")
    }

    /** Change password — a shield. */
    val Shield: ImageVector by lazy {
        stroke("shield", "M12 2 4 6v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V6z")
    }

    /** SMS backup — an envelope, flap first. */
    val Mail: ImageVector by lazy {
        stroke("mail", rect(3f, 5f, 18f, 14f, 2f), "m3 7 9 6 9-6")
    }

    /** Language — a globe with one meridian, not a crosshair. */
    val Globe: ImageVector by lazy {
        stroke("globe", circle(12f, 12f, 9f), "M3 12h18M12 3a15 15 0 0 1 0 18a15 15 0 0 1 0-18")
    }

    /** Dark mode — a crescent. */
    val Moon: ImageVector by lazy { stroke("moon", "M12 3a9 9 0 1 0 9 9c-4 1-8-3-9-9") }

    /** The VGU account mark — a shield with a tick inside it. */
    val Badge: ImageVector by lazy {
        stroke("badge", "M12 3 3 7v5c0 5 3.8 8.4 9 10 5.2-1.6 9-5 9-10V7z", "M9 12l2 2 4-4")
    }

    /** A tick on its own. */
    /** A door with an arrow leaving it. Log out, not the locker's Lock. */
    val LogOut: ImageVector by lazy {
        stroke("logOut", "M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4", "M15 12H4", "M8 8l-4 4 4 4")
    }

    val Check: ImageVector by lazy { stroke("check", "M5 12.5l5 5 9-11") }

    // ---- refusals --------------------------------------------------------

    /** A wrong code — the refuse pill's mark. */
    val Warn: ImageVector by lazy {
        stroke("warn", circle(12f, 12f, 9f), "M12 8v5M12 16.5v.01")
    }

    /** No free box — a circle with a cut through it. */
    val NoEntry: ImageVector by lazy {
        stroke("noEntry", circle(12f, 12f, 9f), "M5.6 5.6l12.8 12.8")
    }

    // ---- filled ----------------------------------------------------------

    /** The navigation arrow on the map card — the one symbol everybody reads
     * as "take me there". Filled, and the only stroke-free mark here. */
    val Navigate: ImageVector by lazy {
        filled(
            "navigate",
            "M21.3 3.3a1 1 0 0 0-1.1-.2L3.4 10.4a1 1 0 0 0 .1 1.9l7.1 2.1 " +
                "2.1 7.1a1 1 0 0 0 1.9.1l7.3-16.8a1 1 0 0 0-.6-1.5z",
        )
    }

    /**
     * Google's G, as four filled paths.
     *
     * Their brand mark, and it is drawn in their four colours by the caller —
     * a single-tint G is not a thing Google's guidelines allow.
     */
    val GoogleG: List<String> = listOf(
        "M23 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.2a5.3 5.3 0 0 1-2.3 3.5v2.9h3.7c2.2-2 3.4-5 3.4-8.6z",
        "M12 24c3.1 0 5.7-1 7.6-2.8l-3.7-2.9c-1 .7-2.3 1.1-3.9 1.1-3 0-5.5-2-6.4-4.7H1.8v3C3.7 21.4 7.6 24 12 24z",
        "M5.6 14.7a7.2 7.2 0 0 1 0-4.6v-3H1.8a12 12 0 0 0 0 10.6l3.8-3z",
        "M12 4.8c1.7 0 3.2.6 4.4 1.7l3.3-3.3C17.7 1.2 15.1 0 12 0 7.6 0 3.7 2.6 1.8 6.1l3.8 3c.9-2.7 3.4-4.3 6.4-4.3z",
    )

    // ---- the two builders ------------------------------------------------

    /** Open strokes, 2px, round caps and joins — the mock-up's own numbers. */
    private fun stroke(name: String, vararg d: String): ImageVector {
        val b = builder(name)
        d.forEach {
            b.addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun filled(name: String, vararg d: String): ImageVector {
        val b = builder(name)
        d.forEach {
            b.addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }
        return b.build()
    }

    private fun builder(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    /** `<circle>` as a path — two half arcs, which is the only way to say a
     * circle in path data. */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} $cy a$r $r 0 1 0 ${r * 2} 0 a$r $r 0 1 0 ${-r * 2} 0"

    /** `<rect rx>` as a path — sides, then a quarter arc at each corner. */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val sx = w - r * 2
        val sy = h - r * 2
        return "M${x + r} ${y} h$sx a$r $r 0 0 1 $r $r v$sy " +
            "a$r $r 0 0 1 ${-r} $r h${-sx} a$r $r 0 0 1 ${-r} ${-r} " +
            "v${-sy} a$r $r 0 0 1 $r ${-r} z"
    }
}
