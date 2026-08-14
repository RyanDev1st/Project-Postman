package vn.edu.vgu.smartlocker.cabinet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
 * **The cabinet steps back so the doors that are yours come forward.**
 *
 * Ryan asked for it as transparency: *"I want to unhighlight in the sense of
 * changing the opacity and make it sort of transparent. It gives a more
 * intuitive (oh these are your boxes)"*. Two earlier attempts missed, and each
 * one was checked with a measure that could not see how it missed:
 *
 * - Alpha 0.55 with the colour drained. Too weak to read.
 * - Darkening instead. Judged on the **luminance ratio** of lit doors to unlit
 *   ones, which improved from 0.69 to 1.39 and looked like a fix. It was not.
 *   That ratio compares the doors to each other and never asks whether the
 *   whole cabinet got louder — which it had. Against the card behind it, the
 *   unlit doors were then as prominent as the lit ones.
 *
 * So the thing measured here is **distance from the card background**. A door
 * that is not yours should sit on the background; a door that is yours should
 * sit far off it. That question has one answer for a ghosted cabinet and a
 * darkened one alike, which is exactly why the earlier measure was the wrong
 * one to trust.
 *
 * Numbers from the emulator, three builds, as a ratio of "yours" to "the
 * rest": 4.2x faded, 1.1x darkened, 8.2x ghosted.
 *
 * Both cabinets are composed at once and the halves compared, because a state
 * written after `setContent` never reaches the render in this harness — see
 * [CabinetCameraTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h860dp-xhdpi")
class CabinetGhostTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * A cabinet with two doors lit sits further back than one with none.
     *
     * The comparison is the same cabinet with and without a parcel in it, so
     * the only difference is the ghost.
     */
    @Test
    fun `the cabinet steps back when doors are lit`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PreviewTheme(dark = false) {
                Column(modifier = Modifier.background(LocalLockerTokens.current.ground)) {
                    // Nothing is yours: the cabinet is drawn at full strength.
                    CabinetArt(yours = emptyList(), modifier = Modifier.fillMaxWidth())
                    // Two are yours: everything but those two steps back.
                    CabinetArt(
                        yours = listOf(door("04"), door("07")),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000L)
        val shot = capture("cabinet-ghost.png")
        val side = shot.width

        // Door 01, top left, is nobody's in either cabinet. If the ghost works
        // it is close to the card in the lower half and far from it in the
        // upper one. Read at a tenth-width inset from the render's left edge,
        // clear of the frame.
        val solid = doorReading(shot, side, yOffset = 0)
        val ghost = doorReading(shot, side, yOffset = side)
        println("door 01 off the card: solid $solid, ghosted $ghost")

        assertTrue(
            "the cabinet did not step back: an unlit door reads $ghost from the " +
                "card with lit doors present, against $solid without them",
            ghost < solid * 0.6,
        )
    }

    /**
     * Every door taken, and the cabinet steps back there too.
     *
     * Ryan asked for this case by name — *"it works much better when there are
     * no available boxes too"*. There is no route to it in the running app, so
     * it is only ever seen here or in a mock-up.
     */
    @Test
    fun `a full cabinet steps back as well`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PreviewTheme(dark = false) {
                Column(modifier = Modifier.background(LocalLockerTokens.current.ground)) {
                    CabinetArt(yours = emptyList(), modifier = Modifier.fillMaxWidth())
                    CabinetArt(
                        yours = emptyList(),
                        full = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000L)
        val shot = capture("cabinet-ghost-full.png")
        val side = shot.width

        val solid = doorReading(shot, side, yOffset = 0)
        val ghost = doorReading(shot, side, yOffset = side)
        println("full cabinet, door 01 off the card: solid $solid, ghosted $ghost")

        assertTrue(
            "a full cabinet did not step back: it reads $ghost from the card, " +
                "against $solid at full strength",
            ghost < solid * 0.7,
        )
    }

    /**
     * How far a patch of door sits from the card behind it, as a distance in
     * RGB. The patch is inside door 01 — upper left of the render, well clear
     * of the doors that are ever lit in these two cases.
     */
    private fun doorReading(shot: Bitmap, side: Int, yOffset: Int): Int {
        val card = meanOf(shot, side / 40, yOffset + side / 40, side / 20, side / 20)
        val door = meanOf(shot, side / 5, yOffset + side / 3, side / 12, side / 12)
        return kotlin.math.sqrt(
            (0..2).sumOf { i ->
                val d = (door[i] - card[i]).toDouble()
                d * d
            }
        ).toInt()
    }

    /** Mean r, g, b over a box. */
    private fun meanOf(shot: Bitmap, x: Int, y: Int, w: Int, h: Int): IntArray {
        val px = IntArray(w * h).also { shot.getPixels(it, 0, w, x, y, w, h) }
        val sum = LongArray(3)
        px.forEach {
            sum[0] += (it shr 16 and 0xFF).toLong()
            sum[1] += (it shr 8 and 0xFF).toLong()
            sum[2] += (it and 0xFF).toLong()
        }
        return IntArray(3) { (sum[it] / px.size).toInt() }
    }

    private fun capture(name: String): Bitmap {
        val shot = compose.activity.findViewById<View>(android.R.id.content).toBitmap()
        File("build/parity").apply { mkdirs() }.resolve(name)
            .outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return shot
    }

    private fun door(n: String) =
        YourDoor(n = n, at = "07 Aug 14:20", left = "2 days", pct = 0.5f, soon = false)

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
