package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * One press, everywhere.
 *
 * The glass in [GlassPane] has squashed under a finger since it was written —
 * `scale(0.96)` while held, no ripple. Nothing else in the app did. The
 * primary button, which is the control that opens a locker door, fell back to
 * Android's default ripple; so did every settings row, every text link and the
 * scan button. So the two smallest controls in the app answered a finger and
 * the largest one did not, which is the opposite of the truth about which
 * matters.
 *
 * The feel is deliberate and it is not symmetric. **Down is 90ms and up is
 * 200ms.** A control that returns as fast as it left reads as a rubber sheet;
 * one that snaps down and settles back reads as a key with a spring under it.
 * That difference is most of what separates a control that feels expensive
 * from one that merely animates.
 *
 * No ripple anywhere. A ripple is Android's answer to *did the tap land*, and
 * with the squash carrying that message a ripple on top is two answers to one
 * question — and the only part of the app that would still look like stock
 * Material.
 */
@Immutable
class Press internal constructor(
    val source: MutableInteractionSource,
    val scale: Float,
)

/**
 * For a control that draws its own shadow or blur. Those layers have to scale
 * with the face — a button that shrinks while its shadow stays put is a button
 * sliding out from under its own shadow — so the caller puts [Press.scale] in a
 * `graphicsLayer` at the very top of the chain, above the shadow, and hands
 * [Press.source] to `clickable` at the bottom.
 */
@Composable
fun rememberPress(target: Float = 0.97f): Press {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) target else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 200),
        label = "press",
    )
    return Press(source, scale)
}

/**
 * For everything else — a row, a link, a plain box. Nothing is drawn above the
 * scale, so the whole thing is one call.
 *
 * [target] is smaller for larger controls: three percent off a full-width
 * button is a clear movement, and three percent off a 30dp bead is a twitch.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    target: Float = 0.97f,
    onClickLabel: String? = null,
): Modifier {
    val press = rememberPress(target)
    return this
        .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
        .clickable(
            interactionSource = press.source,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}
