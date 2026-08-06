package vn.edu.vgu.smartlocker.pickup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ScreenFrame
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * Screen 4. Point the phone at the QR on the cabinet screen.
 *
 * **The phone scans the cabinet, never the reverse** - ADR 0003. The QR only
 * says which cabinet and when; the login proves who is holding the phone. A
 * photograph of the cabinet screen opens nothing.
 *
 * The viewfinder is an aperture, and it is the exact inverse of the glass
 * panel on the waiting screen: that one is lit and lifted toward you, this
 * one is dark and cut into the surface. One says *here is your parcel*, the
 * other says *look through here*.
 *
 * Skeleton: no camera. Tapping the aperture stands in for a good scan. The
 * camera and its permission arrive with task P5-02 - asked for here, where
 * the need is obvious, never on first open.
 */
@Composable
fun ScanScreen(onScanned: () -> Unit, onTypeCode: () -> Unit) {
    ScreenFrame(verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.point_at_the_screen),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Aperture(onTap = onScanned)

        Spacer(Modifier.height(40.dp))

        // Refusing the camera is not a dead end. It drops here.
        TextButton(onClick = onTypeCode) {
            Text(
                text = stringResource(R.string.cant_scan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A hole cut into the cabinet face, with the corner marks a person already
 * reads as *aim here*. Square, because a QR is square - a tall rectangle
 * would invite the user to line the code up wrongly.
 */
@Composable
private fun Aperture(onTap: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge

    // The marks sit on a near-black aperture, so they are light in both
    // schemes. Tying them to onSurface painted dark ink on a dark hole and
    // they all but vanished.
    val mark = Color.White.copy(alpha = 0.70f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                // Recessed: darker at the top, where a lid would shade it.
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1014), Color(0xFF1B242B))
                ),
                shape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        CornerMarks(color = mark)

        Text(
            text = stringResource(R.string.camera_goes_here),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

/** Four L-shaped brackets, inset from the aperture edge. */
@Composable
private fun CornerMarks(color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(28.dp)
    ) {
        val arm = 34.dp.toPx()
        val weight = 3.dp.toPx()
        val w = size.width
        val h = size.height

        // x, y of each corner, and which way its arms run from there.
        listOf(
            Triple(0f, 0f, 1f to 1f),
            Triple(w, 0f, -1f to 1f),
            Triple(0f, h, 1f to -1f),
            Triple(w, h, -1f to -1f),
        ).forEach { (x, y, dir) ->
            val (dx, dy) = dir
            drawRect(
                color = color,
                topLeft = Offset(minOf(x, x + dx * arm), minOf(y, y + dy * weight)),
                size = Size(arm, weight),
            )
            drawRect(
                color = color,
                topLeft = Offset(minOf(x, x + dx * weight), minOf(y, y + dy * arm)),
                size = Size(weight, arm),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ScanPreview() {
    PreviewTheme { ScanScreen({}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun ScanDarkPreview() {
    PreviewTheme(dark = true) { ScanScreen({}, {}) }
}
