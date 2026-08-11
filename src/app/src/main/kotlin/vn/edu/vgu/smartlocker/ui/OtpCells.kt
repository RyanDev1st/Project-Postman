package vn.edu.vgu.smartlocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace

/**
 * A code box — one cell of the OTP row.
 *
 * A filled digit is not recessed any more — it has something in it, so it
 * comes back up to the surface, and an accent ring replaces the border the
 * recess gave up (a border on a box whose depth is carried by inset shadows
 * fights them). The caret is the accent ring plus a soft halo.
 */
@Composable
fun OtpCells(
    code: String,
    caretIndex: Int,
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (i in 0 until 6) {
            val isOn = code.length > i
            val isCaret = i == caretIndex && !isOn
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f / 1.24f),
                contentAlignment = Alignment.Center,
            ) {
                Recess(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    if (isCaret) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 1.5.dp, height = 22.dp)
                                    .background(t.accent),
                            )
                        }
                    } else if (isOn) {
                        Text(
                            text = code[i].toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NumberFace,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = t.ink,
                        )
                    }
                }
                if (isOn) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(1.dp, t.accent.copy(alpha = 0.62f), MaterialTheme.shapes.small),
                    )
                }
                if (isCaret) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(1.5.dp, t.accent, MaterialTheme.shapes.small)
                            .shadow(
                                elevation = 4.dp,
                                shape = MaterialTheme.shapes.small,
                                ambientColor = t.accent.copy(alpha = 0.2f),
                                spotColor = t.accent.copy(alpha = 0.2f),
                            ),
                    )
                }
            }
        }
    }
}
