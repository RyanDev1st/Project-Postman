package vn.edu.vgu.smartlocker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
                // White knob on a white track is no knob at all: the slab
                // is near-white in the dark scheme, so the knob takes the
                // colour that is defined as sitting on it.
                .background(if (checked) t.onAccent else t.ink3),
        )
    }
}

/** The bottom nav — floating, and clearly above the page.
 *
 * The selected tab is a second piece of glass lifted off the first; two
 * materials stacked is what gives the bar depth. The selected tab is a plate
 * lifted out of that glass: one shadow, one surface fill, and the ink stepping
 * from `ink3` to `ink`. Nothing else marks it. */
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
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            // The plate is drawn once and moved, rather than drawn inside
            // whichever tab is selected. Three tabs, `weight(1f)` each, with a
            // 2dp gap between them: the arithmetic has to be done here because
            // a sibling laid out behind the row cannot ask the row how wide a
            // third of it is.
            val gaps = 2.dp * (icons.size - 1)
            val tab = (maxWidth - gaps) / icons.size
            val slide by animateDpAsState(
                targetValue = (tab + 2.dp) * selected,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "tab",
            )
            // An outer box sized to the row, so the plate can take the row's
            // full height without being the thing that decides it.
            Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .offset(x = slide)
                    .width(tab)
                    .fillMaxHeight()
                    // A plate, raised out of the glass. One shadow, one fill,
                    // one edge, and the ink does the rest.
                    //
                    // It was six materials: a 34% black shadow, a 24% accent
                    // wash, a white gradient over that, a 26% accent hairline
                    // round it, a lit top edge and a dark foot. That reads as
                    // depth one material at a time and as sludge all together.
                    // It was also tuned when the accent was a colour — a 24%
                    // wash of a colour is a tint, a 24% wash of near-black is
                    // a smear.
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(999.dp),
                        ambientColor = t.shadow,
                        spotColor = t.shadow,
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(t.surface)
                    // The one hairline that earns itself. The pane under the
                    // plate has already sampled the ground and darkened it, so
                    // the four points that separate a card from the ground are
                    // not there to separate a plate from glass, and in the
                    // dark scheme the plate all but vanished.
                    .border(1.dp, t.lip, RoundedCornerShape(999.dp)),
            )
            }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            icons.forEachIndexed { i, icon ->
                val label = labels[i]
                val isSelected = i == selected
                // The ink crosses over with the plate. Snapped, the label went
                // dark a whole plate-length before the plate arrived under it.
                val inkFor = @Composable { on: Boolean ->
                    animateColorAsState(
                        targetValue = if (on) t.ink else t.ink3,
                        animationSpec = tween(durationMillis = 260),
                        label = "tabInk",
                    ).value
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .pressable(onClick = { onSelect(i) }, target = 0.94f)
                        .padding(vertical = 8.dp, horizontal = 2.dp),
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
                            tint = inkFor(isSelected),
                        )
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                // 9.5px, not whatever labelSmall happens to
                                // be. The bar is measured in the design and
                                // a label a point too big crowds the icon.
                                fontSize = 9.5.sp,
                                // The label's line box, measured off the
                                // reference: 14.2px, which is `normal` line
                                // height on 9.5px. Set to 11 the whole bar
                                // came out 3px short — 8 + 17 + 2 + line + 8
                                // is the only thing that sets its height.
                                //
                                // Those 8s were the reference's 7s until the
                                // spacing grid took them, and the 5dp pane
                                // padding became 4 in the same pass. They
                                // cancel: 5 + 47.25 + 5 and 4 + 49.25 + 4 are
                                // both 57.25, so the bar is exactly as tall
                                // as the design's, by a different route.
                                lineHeight = 14.25.sp,
                                letterSpacing = 0.07.em,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = inkFor(isSelected),
                        )
                    }
                }
            }
        }
        }
    }
}
