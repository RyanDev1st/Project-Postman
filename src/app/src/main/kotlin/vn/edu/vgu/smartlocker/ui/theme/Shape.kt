package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Four radii, with gaps you can see: **10, 16, 22, 28**. Plus the pill.
 *
 * | shape | dp | carries |
 * | --- | --- | --- |
 * | extraSmall | 10 | icon wells, the ledger's number chips |
 * | small | 16 | OTP cells |
 * | medium | 16 | settings rows, the segmented switch |
 * | large | 22 | the primary button, the field, a card |
 * | extraLarge | 28 | the claim ticket, the cabinet recess, the scan frame |
 *
 * It used to be 10, 13, 15, 17, 20 — and twelve more values were set inline at
 * the call sites, which the old version of this comment recorded rather than
 * fixed: *"the hero (22), the profile card (19) and the avatar (14) are set in
 * place where they are drawn"*. A scale that documents its own bypass is not
 * a scale.
 *
 * The five old values were also too close to be five ranks. 13, 15 and 17 are
 * two points apart; nobody holding a phone can tell them apart, so they bought
 * inconsistency and paid nothing for it. A radius set should be short with
 * visible gaps — Refactoring UI's rule, and the reason `small` and `medium`
 * are now the same 16: they were 13 and 15, which was always one rank.
 *
 * Sizes are assigned by how big the thing is. A 32dp well takes 10; a control
 * or a cell takes 16; a card takes 22; a full-width container takes 28.
 */
val LockerShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(16.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
