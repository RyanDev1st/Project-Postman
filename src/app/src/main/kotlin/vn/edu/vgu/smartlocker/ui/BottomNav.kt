package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/** A settings switch: on is a fill, off is a recess — the knob has slid back
 * into the well. Two states, two materials, no extra chrome. */
@Composable
fun LockerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    val knob = remember { Animatable(if (checked) 18f else 2f) }
    LaunchedEffect(checked) {
        knob.animateTo(if (checked) 18f else 2f, tween(220))
    }
    Box(
        modifier = modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (checked) Modifier.background(t.accentDeep)
                else Modifier
                    .background(t.field)
                    .background(
                        Brush.verticalGradient(
                            listOf(t.recessIn, Color.Transparent, Color.Transparent, t.recessLit),
                        ),
                        RoundedCornerShape(999.dp),
                    )
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knob.value.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else t.ink3),
        )
    }
}

/** The bottom nav — floating, and clearly above the page.
 *
 * The selected tab is a second piece of glass lifted off the first; two
 * materials stacked is what gives the bar depth. The pill's fill is a 24%
 * accent wash with a neutral offset shadow and a raised lip — edge first,
 * shadow second, fill last. */
@Composable
fun BottomNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    icons: List<androidx.compose.ui.graphics.vector.ImageVector>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    backdrop: BackdropState? = null,
) {
    val t = LocalLockerTokens.current
    GlassPane(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            icons.forEachIndexed { i, icon ->
                val label = labels[i]
                val isSelected = i == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) {
                                // Shadow first, then clip, then the two
                                // fills — edge, shadow, fill, as the pane
                                // above it does.
                                Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(999.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.34f),
                                        spotColor = Color.Black.copy(alpha = 0.34f),
                                    )
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(t.accent.copy(alpha = 0.24f))
                                    // The white wash stops at 62%, not at the
                                    // foot. Run to the foot it lightens the
                                    // whole pill evenly and the lift goes.
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.White.copy(alpha = 0.13f),
                                            0.62f to Color.Transparent,
                                            1.0f to Color.Transparent,
                                        )
                                    )
                                    .border(1.dp, t.accent.copy(alpha = 0.26f), RoundedCornerShape(999.dp))
                                    // `inset 0 1px 0 0 rgba(255,255,255,.42)`
                                    // and `inset 0 -1px 0 0 rgba(0,0,0,.14)`.
                                    // The lit top edge is what lifts the pill
                                    // off the bar; without it the 24% wash is
                                    // a flat coloured lozenge.
                                    .drawBehind {
                                        val px = 1.dp.toPx()
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.42f),
                                            size = Size(size.width, px),
                                        )
                                        drawRect(
                                            color = Color.Black.copy(alpha = 0.14f),
                                            topLeft = Offset(0f, size.height - px),
                                            size = Size(size.width, px),
                                        )
                                    }
                            } else Modifier.clip(RoundedCornerShape(999.dp)),
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 7.dp, horizontal = 3.dp),
                    // The tab is `flex: 1` with `align-items: center`, so its
                    // content sits in the middle of the tab. Without this the
                    // Column wraps its own width and lands against the left
                    // edge of each tab — three labels all pushed off-centre,
                    // which is what "positioned awkwardly" was.
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(17.dp)
                                // `translateY(-1px) scale(1.06)` on the
                                // selected tab. The icon rises out of the
                                // pill; it is the only thing that moves.
                                .then(
                                    if (isSelected) {
                                        Modifier.offset(y = (-1).dp).scale(1.06f)
                                    } else Modifier
                                ),
                            tint = if (isSelected) t.accent else t.ink3,
                        )
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                // 9.5px, not whatever labelSmall happens to
                                // be. The bar is measured in the design and
                                // a label a point too big crowds the icon.
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                letterSpacing = 0.07.em,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = if (isSelected) t.accent else t.ink3,
                        )
                    }
                }
            }
        }
    }
}
