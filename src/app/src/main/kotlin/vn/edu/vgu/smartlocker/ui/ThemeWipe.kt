package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.launch

/**
 * The theme change, with a direction.
 *
 * A theme that swaps in one frame is a flicker — nothing tells you the two
 * schemes are the same room under a different light. So the outgoing frame
 * is held still and the new one is uncovered behind it: **going dark, the
 * dark sweeps down from the top; coming back to light, the light rises from
 * the bottom.** Down for night, up for morning, which is the way the sky
 * does it and the only mnemonic anybody needs.
 *
 * How it works: the whole app is recorded into a graphics layer every frame.
 * On a toggle the last recorded frame is lifted out as a still image, the
 * theme flips underneath it, and the still is clipped back a line at a time.
 * Nothing is animated twice — the app below has already finished changing,
 * and what moves is one rectangle.
 *
 * That is deliberate. The mock-up's first attempt put a transition on every
 * element on the page, roughly fourteen thousand animations for one tap,
 * and it stuttered so badly the toggle's own knob refused to move
 * (DESIGN.md, "Two earlier versions and what each got wrong").
 */
@Composable
fun ThemeWipe(
    dark: Boolean,
    onDarkChanged: (Boolean) -> Unit,
    content: @Composable (requestToggle: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val sweep = remember { Animatable(0f) }

    var outgoing by remember { mutableStateOf<ImageBitmap?>(null) }
    var downward by remember { mutableStateOf(true) }

    val requestToggle: () -> Unit = {
        // One at a time. A second tap mid-sweep would capture a half-wiped
        // frame and wipe that, which looks like a fault rather than a change.
        if (outgoing == null) {
            scope.launch {
                val still = runCatching { layer.toImageBitmap() }.getOrNull()
                downward = !dark
                onDarkChanged(!dark)
                if (still == null) return@launch
                outgoing = still
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(DURATION_MS, easing = FastOutSlowInEasing))
                outgoing = null
            }
        }
    }

    // One root, so the still is explicitly on top of the app rather than
    // relying on whatever the host happens to do with two siblings.
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            content(requestToggle)
        }

        outgoing?.let { still ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val h = size.height
                        // Keep the part of the old frame the sweep has not
                        // reached yet: below the line going down, above it
                        // coming back up.
                        val top = if (downward) sweep.value * h else 0f
                        val bottom = if (downward) h else (1f - sweep.value) * h
                        if (bottom > top) {
                            clipRect(0f, top, size.width, bottom) {
                                drawImage(still)
                            }
                        }
                    },
            ) {}
        }
    }
}

private const val DURATION_MS = 520
