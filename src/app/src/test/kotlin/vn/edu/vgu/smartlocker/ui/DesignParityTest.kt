package vn.edu.vgu.smartlocker.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme
import java.io.File

/**
 * The port half of the design-parity loop — see
 * `docs/roadmap/phase-1.5-design-parity.md`.
 *
 * This draws a composable to a PNG on this machine. No phone, no emulator, no
 * install, no waiting for Ryan to tell us it is wrong. The other half is
 * `docs/designs/mockup/reference-harness.html`, which draws the same component
 * from the real stylesheets. A block is done when the two pictures agree.
 *
 * **These tests do not fail.** They are a camera, not an assertion. Nothing
 * here can decide whether a shadow is the right shadow, and a test that
 * pretended to would be worse than none — it would tick the board while the
 * screen stayed wrong. What they produce is one PNG per test under
 * `build/parity/`, to be looked at beside the reference.
 *
 * **What this cannot draw.** `RenderEffect` and AGSL want a GPU, so the
 * backdrop blur and the rim refraction come out as plain fills here. The
 * glass' own geometry — the bloom, the lip, the rim, where the pane sits —
 * does draw, and that is most of what has been wrong. The blur stays
 * device-checked.
 *
 * `qualifiers` are the mock-up's own frame: 390dp wide at xhdpi is density 2,
 * which is the `--force-device-scale-factor=2` the reference is shot at, so a
 * dp here is a CSS pixel there and the two PNGs can be laid on top of one
 * another.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h860dp-xhdpi")
class DesignParityTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** `.card` — glass.css 671. 18dp is the radius `.ticket.card` uses. */
    @Test
    fun card() = shoot("card") {
        CardMaterial(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Minh Nguyễn",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = LocalLockerTokens.current.ink,
                )
                Text(
                    text = "0912 345 678",
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalLockerTokens.current.ink2,
                )
            }
        }
    }

    /** `.ledger .no` — 34dp square, radius 10 — and a field beside it. */
    @Test
    fun recess() = shoot("recess") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("04", "17").forEach { n ->
                Recess(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = n,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalLockerTokens.current.ink2,
                    )
                }
            }
            Recess(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(14.dp),
            )
        }
    }

    /**
     * Both ends of the switch, in one picture, which is how the reference
     * shows it. BUG-002 lives here: the four white rings round the knob are
     * the design's own, and the pill is supposed to clip them.
     */
    @Test
    fun themeSwitch() = shoot("theme-switch") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ThemeSwitch(checked = false, onCheckedChange = {})
            ThemeSwitch(checked = true, onCheckedChange = {})
        }
    }

    /**
     * The bar, over text, with the middle tab selected — the same three tabs
     * and the same word underneath as the reference harness.
     *
     * The blur behind it will not appear here. Its geometry will: where the
     * items sit, how big the labels are, and whether the selected pill reads
     * as a second pane or as a coloured lozenge.
     */
    @Test
    fun bottomNav() = shoot("bottom-nav") {
        Box {
            Text(
                text = "COLLECTED",
                modifier = Modifier.align(Alignment.TopStart),
                style = MaterialTheme.typography.headlineLarge,
                color = LocalLockerTokens.current.ink,
            )
            BottomNav(
                selected = 1,
                onSelect = {},
                icons = listOf(AppIcons.Home, AppIcons.Cabinet, AppIcons.Settings),
                labels = listOf("Home", "Cabinet", "Settings"),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    /**
     * Put the content on the app's own ground, settle every animation, and
     * write the pixels out.
     *
     * The padding is the reference harness' 17dp gutter. Without it a shadow
     * or a ring that escapes its component is clipped away by the edge of the
     * bitmap and the picture looks correct — which is the exact fault being
     * hunted.
     */
    private fun shoot(
        name: String,
        dark: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        // Drive the frame clock by hand. Left to advance itself it never
        // reports idle under Robolectric and `waitForIdle` times out, even on
        // a card that has no animation at all.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PreviewTheme(dark = dark) {
                Box(
                    modifier = Modifier
                        .background(LocalLockerTokens.current.ground)
                        .padding(horizontal = 17.dp, vertical = 22.dp),
                ) {
                    content()
                }
            }
        }
        // A reference is a still. Every state here is set at composition, so
        // any animation still running is a frame caught half way. A second is
        // twice the longest one in the app - the theme switch's 500 ms.
        compose.mainClock.advanceTimeBy(1_000L)

        val png = File("build/parity").apply { mkdirs() }.resolve("$name.png")
        val bmp = compose.activity.findViewById<View>(android.R.id.content).toBitmap()
        png.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("parity: ${png.absolutePath} (${png.length()} bytes)")
    }

    /**
     * The pixels, straight off the view.
     *
     * Not `captureToImage()`. That reads the window back through `PixelCopy`,
     * and under Robolectric there is no window and no surface to read: it
     * waits for a redraw that never arrives and times out after two seconds.
     * Drawing the view into a bitmap goes through the same Skia that
     * `GraphicsMode.NATIVE` provides, and it returns.
     */
    private fun View.toBitmap(): Bitmap {
        if (width == 0 || height == 0) {
            measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .also { draw(Canvas(it)) }
    }
}
