package vn.edu.vgu.smartlocker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's icons. The core Material set (which material3 already carries)
 * supplies the common strokes; the rest are drawn from the mock-up's own
 * SVGs (`body.html`) so the port does not depend on a second source.
 * Everything is tinted by the caller.
 */
object AppIcons {

    val Check = Icons.Filled.Check
    val Home = Icons.Filled.Home
    val Settings = Icons.Filled.Settings
    val Back = Icons.Filled.ArrowBack
    val Bell = Icons.Filled.Notifications
    val Menu = Icons.Filled.Menu
    val ChevronRight = Icons.Filled.KeyboardArrowRight
    val Lock = Icons.Filled.Lock
    val Mail = Icons.Filled.MailOutline
    /** Language — the globe the settings row uses. */
    val Globe: ImageVector by lazy {
        stroke("globe") {
            circle(12f, 12f, 9f)
            moveTo(3f, 12f)
            lineTo(21f, 12f)
            moveTo(12f, 3f)
            lineTo(12f, 21f)
            moveTo(12f, 3f)
            lineTo(12f, 21f)
        }
    }

    /** A circle, as two half arcs — the smallest shape the mock-up strokes. */
    private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        close()
    }

    private fun stroke(
        name: String,
        width: Float = 2f,
        build: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
        pathBuilder = build,
    ).build()

    /** The cabinet tab: a wall of doors. */
    val Cabinet: ImageVector by lazy {
        stroke("cabinet") {
            moveTo(3f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(3f, 9f)
            lineTo(21f, 9f)
            moveTo(3f, 15f)
            lineTo(21f, 15f)
            moveTo(9f, 3f)
            lineTo(9f, 21f)
        }
    }

    /** The scan control — a viewfinder. */
    val Scan: ImageVector by lazy {
        stroke("scan") {
            moveTo(3f, 7f)
            lineTo(3f, 5f)
            lineTo(4f, 4f)
            lineTo(7f, 4f)
            moveTo(17f, 4f)
            lineTo(19f, 4f)
            lineTo(20f, 5f)
            lineTo(20f, 7f)
            moveTo(20f, 17f)
            lineTo(20f, 19f)
            lineTo(19f, 20f)
            lineTo(17f, 20f)
            moveTo(7f, 20f)
            lineTo(5f, 20f)
            lineTo(4f, 19f)
            lineTo(4f, 17f)
            moveTo(7f, 12f)
            lineTo(17f, 12f)
        }
    }

    /** The navigation arrow on the map card — the one symbol everybody reads
     * as "take me there". */
    val Navigate: ImageVector by lazy {
        ImageVector.Builder(
            name = "navigate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
            moveTo(21.3f, 3.3f)
            lineTo(3.4f, 10.4f)
            lineTo(10.6f, 14.4f)
            lineTo(12.7f, 21.5f)
            lineTo(21.9f, 4.8f)
            close()
        }.build()
    }

    /** Change password — a shield. */
    val Shield: ImageVector by lazy {
        stroke("shield") {
            moveTo(12f, 2f)
            lineTo(4f, 6f)
            lineTo(4f, 12f)
            lineTo(12f, 22f)
            lineTo(20f, 12f)
            lineTo(20f, 6f)
            close()
        }
    }

    /** Dark mode — a crescent. */
    val Moon: ImageVector by lazy {
        stroke("moon") {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 21f, 12f)
            curveToRelative(-4f, 1f, -8f, -3f, -9f, -9f)
            close()
        }
    }

    /** A wrong code — the refuse pill's mark. */
    val Warn: ImageVector by lazy {
        stroke("warn") {
            circle(12f, 12f, 9f)
            moveTo(12f, 8f)
            lineTo(12f, 13f)
            moveTo(12f, 16.5f)
            lineTo(12f, 16.51f)
        }
    }

    /** No free box — a circle with a cut through it. */
    val NoEntry: ImageVector by lazy {
        stroke("noEntry") {
            circle(12f, 12f, 9f)
            moveTo(5.6f, 5.6f)
            lineTo(18.4f, 18.4f)
        }
    }

    /** The primary button's mark — a door to scan. */
    val OpenDoor: ImageVector by lazy {
        stroke("openDoor") {
            moveTo(3f, 7f)
            lineTo(3f, 5f)
            lineTo(4f, 4f)
            lineTo(7f, 4f)
            moveTo(17f, 4f)
            lineTo(19f, 4f)
            lineTo(20f, 5f)
            lineTo(20f, 7f)
            moveTo(20f, 17f)
            lineTo(20f, 19f)
            lineTo(19f, 20f)
            lineTo(17f, 20f)
            moveTo(7f, 20f)
            lineTo(5f, 20f)
            lineTo(4f, 19f)
            lineTo(4f, 17f)
            moveTo(7f, 12f)
            lineTo(17f, 12f)
        }
    }
}
