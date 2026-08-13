package vn.edu.vgu.smartlocker.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * The thinking orb, in its `globe` structure — latitude rings whose dot
 * count follows |cos(latitude)|, so they crowd at the equator and converge
 * at the poles, spun on a yaw. This screen is captioned *Checking your
 * parcels*, which is a search, not a thought.
 */
@Composable
fun LoadingScreen(label: String = "CHECKING YOUR PARCELS") {
    val t = LocalLockerTokens.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Orb(modifier = Modifier.size(132.dp))
        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "VGU Locker",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = (-0.01).sp,
                ),
                color = t.ink,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.14.em,
                    fontSize = 11.sp,
                ),
                color = t.ink3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The globe, drawn honestly: every point is a dot on a sphere, normalised
 * and projected per frame, so the bands bulge and gather toward the rim the
 * way lines of longitude do. Dot size and opacity fall with depth; the
 * whole sphere turns on a slow yaw.
 */
@Composable
private fun Orb(modifier: Modifier = Modifier) {
    val t = LocalLockerTokens.current
    val yaw by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val latRings = 11
    val lonDensity = 29

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Dot radius ~ (size/300)^.6, clamped — the dots are small.
        val baseR = (size.minDimension / 300f).pow(0.6f).coerceIn(0.9f, 2.5f)

        for (i in 0 until latRings) {
            // Latitude from -90..90, gathered at the poles.
            val lat = -PI / 2 + PI * (i + 0.5f) / latRings
            val ringR = cos(lat).toFloat()          // radius of this ring
            if (ringR < 0.06f) continue
            val count = (lonDensity * abs(ringR)).toInt().coerceAtLeast(2)

            for (j in 0 until count) {
                val lon = 2 * PI * j / count + yaw
                // Project to the screen: sphere of radius r, yaw around Y.
                val x = cos(lat).toFloat() * cos(lon).toFloat()
                val z = sin(lon).toFloat()          // -1 far, +1 near
                val y = sin(lat).toFloat()
                val px = cx + x * ringR * r
                val py = cy + y * r

                // Ink fades with depth; the front half is brighter.
                val depth = (z + 1f) / 2f
                val alpha = (0.62f + 0.54f * depth).coerceIn(0f, 1f)
                val dotR = (baseR * (0.6f + 1.7f * depth)).coerceAtLeast(0.3f)
                drawCircle(
                    color = t.ink.copy(alpha = alpha * 0.85f),
                    radius = dotR,
                    center = Offset(px, py),
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun LoadingPreview() {
    PreviewTheme { LoadingScreen() }
}
