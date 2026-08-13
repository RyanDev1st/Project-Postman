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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    /**
     * Pass this and the row becomes a real input. Left out, it stays what it
     * was — a picture — which is what the previews and the design-parity
     * renders want.
     *
     * It was only ever a picture until 2026-08-13, so the code screen could
     * not be typed into: tapping the cells raised no keyboard, and the way
     * out was the back gesture, which from sign-in closes the app.
     */
    onChange: ((String) -> Unit)? = null,
) {
    if (onChange == null) {
        Cells(code = code, caretIndex = caretIndex, modifier = modifier)
        return
    }

    val focus = remember { FocusRequester() }
    // The keyboard, without a tap. Arriving here there is exactly one thing
    // to do, and asking for a tap first is asking for nothing.
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(modifier = modifier) {
        Cells(code = code, caretIndex = caretIndex)
        // Over the cells, not inside them: the field carries the focus, the
        // keyboard and the taps, while the cells stay a drawing. Its own text
        // and caret are invisible — the cells below are the visible ones.
        BasicTextField(
            value = code,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(6)) },
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focus),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        )
    }
}

@Composable
private fun Cells(
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
                                .padding(vertical = 12.dp),
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
                            // A recess is a Box and its content lands top-left
                            // unless it says otherwise — which the caret above
                            // does for itself and this did not. It never
                            // showed, because until the field could be typed
                            // into there was never a digit to draw.
                            modifier = Modifier.align(Alignment.Center),
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
                            // Shadow first: after the border it is drawn on
                            // top of the ring instead of glowing behind it.
                            .shadow(
                                elevation = 4.dp,
                                shape = MaterialTheme.shapes.small,
                                ambientColor = t.accent.copy(alpha = 0.2f),
                                spotColor = t.accent.copy(alpha = 0.2f),
                            )
                            .border(1.5.dp, t.accent, MaterialTheme.shapes.small),
                    )
                }
            }
        }
    }
}
