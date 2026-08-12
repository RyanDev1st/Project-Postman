package vn.edu.vgu.smartlocker.parcels

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.LockerTokens

/**
 * The campus plan, drawn from the projected OSM data.
 *
 * The window is sized from the ROUTE, not from the campus — a map of a place
 * is not a map of a journey, and the walk you would actually take fills the
 * card. The line is a real pedestrian route (543 m, 6 min 32 s — see
 * `docs/designs/mockup/route.json`) baked in at build time, because a
 * published artifact has no network at runtime.
 *
 * A dot and a pin, not two dots: *you are here, that is where you are going*.
 */
@Composable
fun CampusMap(modifier: Modifier = Modifier) {
    val t = LocalLockerTokens.current
    val paper = lerp(t.ground2, t.ink, 0.07f)
    Canvas(modifier = modifier) {
        val scale = size.width / CampusMapData.W
        val h = CampusMapData.H * scale
        val top = (size.height - h) / 2f
        withTransform({
            translate(left = 0f, top = top)
            // Pivot on the origin, not on the canvas centre. DrawTransform's
            // scale() defaults its pivot to the middle of the canvas, and
            // the plan's coordinates start at 0,0 - so the default sent the
            // whole map about 1550px off the left edge and the card drew
            // empty. Nothing else was wrong with it.
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawRect(paper)
            drawCampus(t, paper)
            drawRoute(t)
            drawMarkers(t, paper)
            drawNorth(t, paper)
        }
    }
}

private fun DrawScope.drawCampus(t: LockerTokens, paper: Color) {
    fun fill(paths: List<String>, level: Int) {
        val c = lerp(paper, t.ink, level / 100f)
        paths.forEach { d -> drawPath(PathParser.parse(d), c) }
    }
    fun stroke(paths: List<String>, level: Int, width: Float) {
        val c = lerp(paper, t.ink, level / 100f)
        paths.forEach { d -> drawPath(PathParser.parse(d), c, style = Stroke(width)) }
    }

    fill(CampusMapData.FILL_25, CampusMapData.FILL_25_LEVEL)
    fill(CampusMapData.FILL_19, CampusMapData.FILL_19_LEVEL)
    stroke(CampusMapData.STROKE_17, CampusMapData.STROKE_17_LEVEL, 0.6f)
    stroke(CampusMapData.STROKE_23, CampusMapData.STROKE_23_LEVEL, 0.9f)
    stroke(CampusMapData.STROKE_31, CampusMapData.STROKE_31_LEVEL, 1.4f)
    fill(CampusMapData.FILL_37, CampusMapData.FILL_37_LEVEL)
    stroke(CampusMapData.STROKE_44, CampusMapData.STROKE_44_LEVEL, 1.4f)
}

/** The route: a wide ground-2 underlay, then the accent line. They carry the
 * same fact, so they are the same colour. */
private fun DrawScope.drawRoute(t: LockerTokens) {
    drawPath(PathParser.parse(CampusMapData.ROUTE_UNDERLAY), t.ground2.copy(alpha = 0.9f), style = Stroke(7.5f))
    drawPath(PathParser.parse(CampusMapData.ROUTE), t.accent, style = Stroke(3.4f))
}

private fun DrawScope.drawMarkers(t: LockerTokens, paper: Color) {
    // The start: a solid neutral dot with a light collar.
    val (dx, dy) = CampusMapData.START_DOT
    val dotCol = lerp(paper, t.ink, CampusMapData.START_DOT_LEVEL / 100f)
    drawCircle(dotCol, radius = CampusMapData.START_DOT_R, center = Offset(dx, dy))
    drawCircle(paper, radius = CampusMapData.START_DOT_R, center = Offset(dx, dy), style = Stroke(2.8f))

    // The destination: a teardrop whose tip sits on the gate.
    val (gx, gy) = CampusMapData.GATE
    drawOval(
        color = t.accent.copy(alpha = CampusMapData.GATE_GLOW_ALPHA),
        topLeft = Offset(gx - CampusMapData.GATE_GLOW_RX, gy - CampusMapData.GATE_GLOW_RY),
        size = Size(CampusMapData.GATE_GLOW_RX * 2, CampusMapData.GATE_GLOW_RY * 2),
    )
    val (ox, oy) = CampusMapData.PIN_ORIGIN
    withTransform({ translate(left = ox, top = oy) }) {
        drawPath(PathParser.parse(CampusMapData.PIN_PATH), t.accent)
        drawPath(PathParser.parse(CampusMapData.PIN_PATH), paper, style = Stroke(2f))
        drawCircle(paper, radius = 3.1f, center = Offset(0f, -14.2f))
    }
}

private fun DrawScope.drawNorth(t: LockerTokens, paper: Color) {
    val c = lerp(paper, t.ink, 0.44f)
    drawPath(PathParser.parse(CampusMapData.NORTH_ARROW), c, style = Stroke(1.4f))
    val (tx, ty) = CampusMapData.NORTH_AT
    val p = android.graphics.Paint().apply {
        color = c.toArgb()
        textSize = 7.5f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawContext.canvas.nativeCanvas.drawText(CampusMapData.NORTH_LABEL, tx, ty + 4f, p)
}

/**
 * The one element on Home that is not about a parcel: which gate, which side
 * of campus, how far — and it hands off to the Maps app for the walk itself.
 *
 * The picture is [LiveMap] when this build has a Maps key: real tiles, and
 * the phone's own position on them. Without one it is [CampusMap], the plan
 * baked from the same OSM data — which is honest about the route but shows
 * the same scene wherever the phone is. That was the whole map until now.
 */
@Composable
fun MapCard(
    cabinet: String = "Back gate",
    walk: String = "7 min walk · 540 m",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    CardMaterial(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier) {
            val picture = Modifier
                .fillMaxWidth()
                .height(112.dp)
            if (hasMapsKey) LiveMap(modifier = picture, onClick = onClick)
            else CampusMap(modifier = picture)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cabinet,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = t.ink,
                    )
                    Text(
                        text = walk,
                        style = MaterialTheme.typography.labelLarge,
                        color = t.ink2,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(t.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Navigate,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = t.accentInk,
                    )
                }
            }
        }
    }
}

/** A tiny SVG path parser — the plan's paths are M/L/C/A/Z segments. */
internal object PathParser {
    private val TOKEN = Regex("[MmLlHhVvCcSsQqTtAaZz]|[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?")

    fun parse(d: String): Path {
        val path = Path()
        val tokens = TOKEN.findAll(d).map { it.value }.toList()
        var i = 0
        var cmd = ' '
        var cx = 0f
        var cy = 0f

        fun next(): Float = tokens[i++].toFloat()

        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok[0].isLetter()) {
                cmd = tok[0]
                i++
            }
            when (cmd) {
                'M' -> { cx = next(); cy = next(); path.moveTo(cx, cy); cmd = 'L' }
                'm' -> { cx += next(); cy += next(); path.moveTo(cx, cy); cmd = 'l' }
                'L' -> { cx = next(); cy = next(); path.lineTo(cx, cy) }
                'l' -> { cx += next(); cy += next(); path.lineTo(cx, cy) }
                'H' -> { cx = next(); path.lineTo(cx, cy) }
                'h' -> { cx += next(); path.lineTo(cx, cy) }
                'V' -> { cy = next(); path.lineTo(cx, cy) }
                'v' -> { cy += next(); path.lineTo(cx, cy) }
                'C' -> {
                    val x1 = next(); val y1 = next(); val x2 = next(); val y2 = next()
                    cx = next(); cy = next()
                    path.cubicTo(x1, y1, x2, y2, cx, cy)
                }
                'c' -> {
                    val x1 = cx + next(); val y1 = cy + next()
                    val x2 = cx + next(); val y2 = cy + next()
                    cx += next(); cy += next()
                    path.cubicTo(x1, y1, x2, y2, cx, cy)
                }
                'A', 'a' -> {
                    // The plan's only arcs are the teardrop's two corners;
                    // a line approximation is invisible at this scale.
                    repeat(5) { next() }
                    if (cmd == 'A') { cx = next(); cy = next() } else { cx += next(); cy += next() }
                    path.lineTo(cx, cy)
                }
                'Z', 'z' -> path.close()
            }
        }
        return path
    }
}
