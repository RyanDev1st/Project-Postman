package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import vn.edu.vgu.smartlocker.ui.theme.DarkTokens
import vn.edu.vgu.smartlocker.ui.theme.LightTokens

/**
 * The curtain's state, read by [lockerGround] — every copy of the ground
 * draws it, and they must agree.
 *
 * [fromTop] says which edge the outgoing colour is anchored to, so [fraction]
 * shrinking to zero reveals the new page from the other end.
 */
data class ThemeCurtain(
    val color: Color = Color.Transparent,
    val fraction: Float = 0f,
    val fromTop: Boolean = true,
)

val LocalThemeCurtain = compositionLocalOf { ThemeCurtain() }

/**
 * The theme change: a curtain **behind** the content.
 *
 * The curtain is filled with the **outgoing** ground colour and held at full
 * height, covering the ground. The theme swaps underneath it, hidden. The
 * curtain then shrinks, and the new ground is revealed behind its moving edge.
 *
 * **Which way it moves depends on which way you are going.** Going dark, the
 * curtain is anchored at the top and the new dark page rises from the floor.
 * Going light, it is anchored at the bottom and the new light page comes down
 * from above. Dark rises off the ground; light falls from the sky. A wipe that
 * runs the same way in both directions makes one of the two feel backwards,
 * and there is no reading of it that makes light climb up out of the floor.
 *
 * Behind the content, and that single fact is the whole design:
 *
 *  - Every card, rule and letter stays exactly where it is, on top, in full
 *    view, and changes its own colours as the new ground lands underneath.
 *    Nothing is ever hidden.
 *  - The mock-up first put the curtain **over** everything instead. For half
 *    a second the screen was a blank rectangle: nothing transitioned, the
 *    content was covered and then uncovered, already changed. A snapshot of
 *    the outgoing frame laid on top has the same fault in a nicer costume,
 *    and this file used to do exactly that.
 *
 * One pass, upward, 480 ms. The component Ryan supplied runs two — fall,
 * swap, rise — which is 1100 ms end to end with a dead beat in the middle,
 * and `duration` is a documented prop on it precisely so the caller can say
 * otherwise. Easing is the component's own `cubic-bezier(.76, 0, .24, 1)`.
 */
@Composable
fun ThemeWipe(
    dark: Boolean,
    onDarkChanged: (Boolean) -> Unit,
    content: @Composable (requestToggle: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val fraction = remember { Animatable(0f) }
    var outgoing by remember { mutableStateOf(Color.Transparent) }
    var fromTop by remember { mutableStateOf(true) }

    val requestToggle: () -> Unit = {
        if (!fraction.isRunning) {
            scope.launch {
                // The ground being left. Read before the swap: after it the
                // tokens already describe the scheme arriving.
                outgoing = (if (dark) DarkTokens else LightTokens).ground
                // Going dark: hold the old page at the top so the dark one
                // rises. Going light: hold it at the bottom so the light one
                // comes down.
                fromTop = !dark
                fraction.snapTo(1f)
                onDarkChanged(!dark)
                fraction.animateTo(0f, tween(480, easing = CURTAIN))
                outgoing = Color.Transparent
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeCurtain provides ThemeCurtain(outgoing, fraction.value, fromTop),
    ) {
        content(requestToggle)
    }
}

private val CURTAIN = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)
