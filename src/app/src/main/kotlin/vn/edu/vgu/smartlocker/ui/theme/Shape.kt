package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Thick corners.
 *
 * Glass has depth, and a thin radius reads as a cut edge rather than a
 * rounded one. The parcel card uses `extraLarge`, which is what makes it
 * look like a slab lifted off the ground instead of a rectangle drawn on it.
 */
val LockerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
