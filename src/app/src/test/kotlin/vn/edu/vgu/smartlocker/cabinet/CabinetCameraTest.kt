package vn.edu.vgu.smartlocker.cabinet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
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
 * P1.5-09, the cabinet camera — see `docs/roadmap/phase-1.5-design-parity.md`.
 *
 * **What this proves, and what it cannot.**
 *
 * It proves the camera arrives: framed on door 04, the picture is a different
 * picture from the wide shot, by a third of its pixels. That is worth having —
 * the camera has twice shipped doing nothing at all, once because the effect
 * that moved it only ran while no door was framed, and once because the pan
 * was measured in fractions of a pixel instead of fractions of the render.
 * Either fault would show here as two identical pictures.
 *
 * It **cannot** prove the other half of P1.5-09's Verify — that pulling out
 * lands back on the wide shot, and that the next push-in starts where the last
 * one ended. That needs one camera driven through wide → framed → wide →
 * framed, and this harness will not do it: a state written from the test after
 * `setContent` never reaches the render. Measured, not assumed — the four
 * frames of such a run came out byte-identical, 347,319 bytes each, while a
 * cabinet composed framed from birth came out 297,624. Advancing the clock in
 * one jump, advancing it frame by frame, and writing the state through
 * `runOnUiThread` all gave the same four identical frames. So the return path
 * stays on Ryan's phone, and P1.5-09 stays open.
 *
 * Hence both cabinets are composed at once, side by side, and the two halves
 * of the one picture are compared. No state changes, so nothing to propagate.
 *
 * `qualifiers` matches `DesignParityTest`: 390dp at density 2, the mock-up's
 * own frame.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h860dp-xhdpi")
class CabinetCameraTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theCameraFramesTheDoor() {
        // The clock by hand, as in DesignParityTest: left to itself it never
        // reports idle under Robolectric.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PreviewTheme(dark = true) {
                Column(modifier = Modifier.background(LocalLockerTokens.current.ground)) {
                    // Two doors are yours, so `yours.singleOrNull()` is null
                    // and this one frames nothing unasked. The wide shot.
                    CabinetArt(
                        yours = listOf(door("04"), door("17")),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // The same cabinet, asked for door 04.
                    CabinetArt(
                        yours = listOf(door("04"), door("17")),
                        framedDoor = "04",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        // Past everything in the timeline: a 720 ms push-in behind a 600 ms
        // wait, and a 450 ms arrival.
        compose.mainClock.advanceTimeBy(2_000L)

        val shot = compose.activity.findViewById<View>(android.R.id.content).toBitmap()
        File("build/parity").apply { mkdirs() }.resolve("cabinet-camera.png")
            .outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // Each cabinet is `fillMaxWidth` with `aspectRatio(1f)`, so each is a
        // square the width of the screen and the pair stack exactly.
        val side = shot.width
        val moved = difference(shot, side)
        println("cabinet camera: framed differs from wide by $moved% of pixels")
        assertTrue(
            "the camera did not frame the door — the framed cabinet differs " +
                "from the wide one by only $moved% of its pixels",
            moved > 5.0,
        )
    }

    /** A parcel in a box. Only `n` matters here — the camera frames by number. */
    private fun door(n: String) =
        YourDoor(n = n, at = "07 Aug 14:20", left = "2 days", pct = 0.5f, soon = false)

    /**
     * How much the lower square differs from the upper one, as a percentage.
     *
     * Not exact equality. The render is scaled by a float and resampled, so
     * edges differ in the bottom bit of a channel. A pixel counts only if a
     * channel is out by more than 8 of 255.
     */
    private fun difference(shot: Bitmap, side: Int): Double {
        val rows = minOf(side, shot.height - side)
        if (rows <= 0) return 0.0
        val top = IntArray(side * rows)
            .also { shot.getPixels(it, 0, side, 0, 0, side, rows) }
        val bottom = IntArray(side * rows)
            .also { shot.getPixels(it, 0, side, 0, side, side, rows) }
        var off = 0
        for (i in top.indices) {
            val x = top[i]
            val y = bottom[i]
            if (x == y) continue
            val dr = Math.abs((x shr 16 and 0xFF) - (y shr 16 and 0xFF))
            val dg = Math.abs((x shr 8 and 0xFF) - (y shr 8 and 0xFF))
            val db = Math.abs((x and 0xFF) - (y and 0xFF))
            if (dr > 8 || dg > 8 || db > 8) off++
        }
        return off * 100.0 / top.size
    }

    /** The pixels, straight off the view — see `DesignParityTest.toBitmap`. */
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
