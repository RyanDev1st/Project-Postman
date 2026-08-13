package vn.edu.vgu.smartlocker.parcels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import androidx.compose.ui.unit.sp
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.Pill
import vn.edu.vgu.smartlocker.ui.PillKind
import vn.edu.vgu.smartlocker.ui.theme.DoorLight
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace

/** One waiting parcel, as the claim ticket carries it. */
data class Claim(
    val cabinet: String,
    val box: String,
    val dropped: String,
    val collectBy: String,
    val left: String,
    val pct: Float,
    val soon: Boolean,
)

/** The small ticket — the same object, one row high. */
data class SmallClaim(
    val cabinet: String,
    val box: String,
    val detail: String,
    val left: String,
    val pct: Float,
    val soon: Boolean,
)

/**
 * The claim ticket. A cloakroom tag, a luggage stub, a lottery slip: a big
 * number, a seam, and a time, and everyone alive can read one without being
 * taught. The number is the hero because the number is the thing you carry
 * to the cabinet and repeat to yourself on the walk over.
 *
 * The seam is a real cut, not a divider rule: two circular bites taken out
 * of the card edges with the ground showing through them, and a dashed rule
 * between. The hairline is an inset ring, because a circle placed on the
 * card edge has to cover the edge the way a punch does.
 */
@Composable
fun ClaimTicket(
    claim: Claim,
    modifier: Modifier = Modifier,
    isFree: Boolean = false,
    freeNote: String? = null,
) {
    val t = LocalLockerTokens.current
    CardMaterial(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = claim.cabinet.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = t.ink2,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = stringResource(
                            if (isFree) R.string.ticket_boxes_free else R.string.ticket_door,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = t.ink3,
                    )
                    Text(
                        text = claim.box,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = NumberFace,
                            color = if (isFree) t.free else t.ink,
                        ),
                    )
                }
                Spacer(Modifier.weight(1f))
                Pill(
                    text = claim.left,
                    kind = when {
                        isFree -> PillKind.FREE
                        claim.soon -> PillKind.SOON
                        else -> PillKind.NEUTRAL
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // The seam: two bites out of the card edges, and a dashed rule.
            Seam()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
            ) {
                if (isFree) {
                    Text(
                        text = freeNote ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = t.ink2,
                    )
                } else {
                    StubCell(stringResource(R.string.ticket_dropped), claim.dropped)
                    StubCell(stringResource(R.string.ticket_collect_by), claim.collectBy)
                }
            }

            // Time left, as a length rather than a number. It drains to its
            // real value on entry: two tickets, one nearly empty and one
            // nearly full, and you know which to walk to first.
            DrainMeter(pct = claim.pct, soon = claim.soon)
        }
    }
}

@Composable
private fun StubCell(label: String, value: String) {
    val t = LocalLockerTokens.current
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = t.ink3,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = NumberFace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).sp,
            ),
            color = t.ink,
        )
    }
}

/** The two circular bites and the dashed rule between them. */
@Composable
private fun Seam() {
    val t = LocalLockerTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp)
            .height(1.5.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            drawLine(
                color = t.ink.copy(alpha = 0.17f),
                start = Offset(8.5.dp.toPx(), 0f),
                end = Offset(size.width - 8.5.dp.toPx(), 0f),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
        }
        // The bites, cut out to the ground. Placed on the card edge (the
        // card is padded 16dp, so the bites sit at the padding edges).
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(17.dp)
                .height(17.dp)
                .offset(x = (-8.5).dp)
                .background(t.ground, androidx.compose.foundation.shape.CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(17.dp)
                .height(17.dp)
                .offset(x = 8.5.dp)
                .background(t.ground, androidx.compose.foundation.shape.CircleShape),
        )
    }
}

/** How much time is left, as a length. Drains in on entry. */
@Composable
fun DrainMeter(
    pct: Float,
    soon: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    val drain = remember { Animatable(0f) }
    LaunchedEffect(pct) {
        drain.animateTo(pct, tween(700))
    }
    val track = t.ink.copy(alpha = 0.13f)
    val fill = if (soon) DoorLight else t.ink.copy(alpha = 0.42f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            drawRoundRect(
                color = track,
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth()) {
            drawRoundRect(
                color = fill,
                size = Size(size.width * drain.value, size.height),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/**
 * The same ticket, one row high — the parcel that is not the urgent one, and
 * the cabinet panel's ticket. Same card, same number face, same drain rule,
 * a quarter of the height.
 */
@Composable
fun SmallTicket(
    claim: SmallClaim,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val t = LocalLockerTokens.current
    CardMaterial(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = claim.box,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = NumberFace,
                        fontSize = 30.sp,
                        lineHeight = 30.sp,
                        letterSpacing = (-0.05).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = t.ink,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = claim.cabinet.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = t.ink2,
                    )
                    Text(
                        text = claim.detail,
                        style = MaterialTheme.typography.labelLarge,
                        color = t.ink2,
                    )
                }
                Text(
                    text = claim.left,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = NumberFace,
                    ),
                    color = t.ink2,
                )
            }
            DrainMeter(
                pct = claim.pct,
                soon = claim.soon,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
