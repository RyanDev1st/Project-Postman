package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The cabinet is the mark: cropped and bleeding off three edges, masked out
 * at the bottom. The crop is chosen, not centred — the top-right quadrant
 * carries doors 03, 04, 07, 08, 11 and 12 and the right-hand side panel,
 * which is the part that shows the three-quarter depth.
 *
 * The window shows image fractions x 0.46–1.03, y 0.07–0.43: the image is
 * 175% of the box's width, shifted left 80% of the box and up 19% of its
 * height. Past the right edge the render is transparent, so the cabinet ends
 * and the ground shows through — which is what stops it reading as wallpaper.
 */
@Composable
internal fun AuthHero() {
    val t = LocalLockerTokens.current
    val cabinet = ImageBitmap.imageResource(R.drawable.cabinet)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            // The render is deliberately wider than its box, so the box has
            // to clip or it paints over the form below it.
            .clipToBounds(),
    ) {
        // The crop is drawn, not laid out. BUG-004.
        //
        // It used to be an oversized child: `requiredSize(boxW * 1.75f)` and
        // then an offset back. The numbers were right - reproduced against
        // the bitmap they give the picture the comment above describes - and
        // on a real GPU the screen did not. What arrived was the far right
        // edge of the cabinet at roughly twice the intended zoom, ending in a
        // hard vertical cut a third of the way across, on hardware GL as well
        // as on the software rasteriser, which is what ruled out the
        // rasteriser. Three modifiers deciding one rectangle between them is
        // one too many to reason about, so the rectangle is now stated.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.width * 1.75f
            drawImage(
                image = cabinet,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(cabinet.width, cabinet.height),
                dstOffset = IntOffset(
                    (size.width * -0.80f).roundToInt(),
                    (size.height * -0.19f).roundToInt(),
                ),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            )
        }
        // Masked out at the bottom: the ground comes up through the cabinet.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, t.ground),
                    )
                ),
        )
    }
}
