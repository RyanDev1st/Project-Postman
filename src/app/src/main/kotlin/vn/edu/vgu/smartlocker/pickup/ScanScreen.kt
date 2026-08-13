package vn.edu.vgu.smartlocker.pickup

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.QuietButton
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * Scan — three words. The aperture already says what to do; it used to be
 * two sentences, and the sentences were doing nothing.
 */
@Composable
fun ScanScreen(
    onScanned: () -> Unit,
    onTypeCode: () -> Unit,
    onBack: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            brand = stringResource(R.string.box_label, "04"),
            leading = {
                AppBarBead(
                    icon = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Aperture(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.scan_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = t.ink,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        QuietButton(
            text = stringResource(R.string.cab_use_code),
            onClick = onTypeCode,
        )
    }
}

/** The viewfinder: a dark well, four accent corners, and a sweep that keeps
 * travelling — the scan is a thing the phone is doing, not a state. */
@Composable
fun Aperture(modifier: Modifier = Modifier) {
    val t = LocalLockerTokens.current
    val sweep by rememberInfiniteTransition().animateFloat(
        initialValue = 0.2f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF05070A)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = 28.dp.toPx()
            val inset = 16.dp.toPx()
            val stroke = 3.dp.toPx()
            val accent = t.accent
            // Top-left corner
            drawLine(accent, Offset(inset, inset + corner), Offset(inset, inset), stroke)
            drawLine(accent, Offset(inset, inset), Offset(inset + corner, inset), stroke)
            // Top-right
            drawLine(accent, Offset(size.width - inset, inset), Offset(size.width - inset - corner, inset), stroke)
            drawLine(accent, Offset(size.width - inset, inset), Offset(size.width - inset, inset + corner), stroke)
            // Bottom-left
            drawLine(accent, Offset(inset, size.height - inset), Offset(inset + corner, size.height - inset), stroke)
            drawLine(accent, Offset(inset, size.height - inset), Offset(inset, size.height - inset - corner), stroke)
            // Bottom-right
            drawLine(accent, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - corner, size.height - inset), stroke)
            drawLine(accent, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - corner), stroke)

            // The sweep.
            val y = size.height * sweep
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, accent, Color.Transparent),
                    startX = size.width * 0.12f,
                    endX = size.width * 0.88f,
                ),
                start = Offset(size.width * 0.12f, y),
                end = Offset(size.width * 0.88f, y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ScanPreview() {
    PreviewTheme { ScanScreen({}, {}) }
}
