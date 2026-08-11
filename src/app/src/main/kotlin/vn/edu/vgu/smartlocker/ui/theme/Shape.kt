package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radii from the mock-up (`screens.css`), one per surface family.
 *
 * | shape | dp | carries |
 * | --- | --- | --- |
 * | extraSmall | 10 | icon wells, the ledger's number chips |
 * | small | 13 | OTP cells |
 * | medium | 15 | settings rows, the segmented switch |
 * | large | 17 | the primary button, the field |
 * | extraLarge | 20 | the claim ticket |
 *
 * The hero (22), the profile card (19) and the avatar (14) are set in place
 * where they are drawn — they appear once each.
 */
val LockerShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(13.dp),
    medium     = RoundedCornerShape(15.dp),
    large      = RoundedCornerShape(17.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
