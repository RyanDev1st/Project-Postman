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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
 *
 * **It scans now.** Until P5-02 this screen was a drawing of a scanner: a
 * dark box, four animated corners, a travelling sweep, and no camera behind
 * any of it. `onScanned` existed and was never called. The corners and the
 * sweep are kept exactly as they were and now sit over a live preview - the
 * design was right, it just had nothing underneath it.
 *
 * The camera is asked for here, on a tap, never on first open. By the time
 * a person reaches this screen they are standing at a locker pointing a
 * phone at a code, and the reason is obvious. Refuse it and the screen says
 * so and offers the typed code instead, which is the other way in (P5-08).
 */
@Composable
fun ScanScreen(
    onScanned: () -> Unit,
    onTypeCode: () -> Unit,
    onBack: () -> Unit = {},
    /** What a read code turns out to be. Held by the caller because what
     * happens next - a door, a message, a different screen - is not this
     * screen's decision. */
    onRead: (Scanned) -> Unit = {},
    /** A sentence to show instead of "Scan the cabinet": the code was expired,
     * or nothing in that cabinet is yours. The caller decides which, because
     * only the caller knows what is waiting for you. Null the rest of the
     * time, which is nearly always. */
    note: Int? = null,
) {
    val t = LocalLockerTokens.current
    val ctx = LocalContext.current

    var allowed by remember { mutableStateOf(cameraAllowed(ctx)) }
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed = it }

    // Asked once, when the screen appears, because the screen exists only to
    // point a camera at something. Refused, nothing is asked again - the
    // system would not show the dialog twice anyway, and a screen that
    // re-asks on every visit is a screen people learn to back out of.
    LaunchedEffect(Unit) {
        if (!allowed) ask.launch(android.Manifest.permission.CAMERA)
    }

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
            Aperture(modifier = Modifier.fillMaxWidth(), live = allowed, onRead = onRead)
            Text(
                text = stringResource(
                    note ?: if (allowed) R.string.scan_title else R.string.scan_no_camera,
                ),
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

/**
 * The viewfinder: a dark well, four accent corners, and a sweep that keeps
 * travelling — the scan is a thing the phone is doing, not a state.
 *
 * The camera goes underneath, and everything above it is unchanged. The dark
 * well is still drawn: it is what the corners sit on when there is no
 * permission, and it stops the moment between the screen appearing and the
 * first frame arriving from being a white flash.
 *
 * Each code is acted on once and the rest are dropped. ML Kit reads the same
 * code out of thirty frames a second, and the door behind this must open
 * once — rule 6 in `working-rules.md`, and P5-07's Verify counts the requests
 * in the server log. When P5-03 puts a real request behind this, it goes on
 * this same one-per-code path and nowhere else.
 *
 * It is the *code* that is remembered, not the fact of having read one. A
 * refused code — expired, or a cabinet with nothing of yours in it — leaves a
 * sentence on the screen and the camera still running, because the cabinet
 * draws a new code every 30 seconds and the next one is the answer. Latching
 * after the first read would mean "scan again" was advice the app itself made
 * impossible to follow.
 */
@Composable
fun Aperture(
    modifier: Modifier = Modifier,
    live: Boolean = false,
    onRead: (Scanned) -> Unit = {},
) {
    val t = LocalLockerTokens.current
    var acted by remember { mutableStateOf<String?>(null) }
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
        if (live) {
            QrCamera(
                modifier = Modifier.fillMaxSize(),
                onCode = { raw ->
                    // Every frame carrying the same code lands here. The first
                    // one is acted on and the rest are dropped, so whatever
                    // happens next happens once per code.
                    if (raw == acted) return@QrCamera
                    val read = readSessionCode(raw, System.currentTimeMillis() / 1000)
                    // A stranger's QR is not an event. Someone waving a phone
                    // around a lobby catches posters and payment codes, and
                    // the right answer is to keep looking.
                    if (read is Scanned.NotOurs) return@QrCamera
                    acted = raw
                    onRead(read)
                },
            )
        }

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
