package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.OnDoorLight

/** A small capsule. Three kinds:
 *
 * - neutral — a quiet tag
 * - [PillKind.SOON] — amber surface on its own dark ground. Amber is never
 *   ink: `#FFB200` is 1.81:1 on white. On a surface it is 11.5:1, identical
 *   in both schemes.
 * - [PillKind.FREE] — the free-box green, tinted and quiet.
 */
enum class PillKind { NEUTRAL, SOON, FREE }

@Composable
fun Pill(
    text: String,
    kind: PillKind = PillKind.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    val (bg, fg) = when (kind) {
        PillKind.SOON -> DoorLight to OnDoorLight
        PillKind.FREE -> t.free to t.free
        PillKind.NEUTRAL -> t.ink.copy(alpha = 0.09f) to t.ink2
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}
