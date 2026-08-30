package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace

/**
 * A Vietnamese mobile number, without the country code.
 *
 * `+84` is drawn beside the field rather than typed into it, so the number
 * held here is the local part only: nine digits beginning 3, 5, 7, 8 or 9.
 * The same rule the OTP server applies in `Phone.normalize` — a landline
 * (`024…`, `028…`) is not a mobile and no code can reach it.
 *
 * Kept as its own type so a screen cannot forget which half of the number it
 * is holding, which is the mistake that makes `+8484…` numbers.
 */
object VnMobile {

    /** Digits only, and never longer than a Vietnamese mobile's local part. */
    fun clean(raw: String): String = raw.filter { it.isDigit() }.take(9)

    /** Whether [local] is a number a code could actually be sent to. */
    fun isComplete(local: String): Boolean =
        local.length == 9 && local.first() in "35789"

    /** `912 345 678` — grouped the way the design draws it. */
    fun spaced(local: String): String = buildString {
        local.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append(' ')
            append(c)
        }
    }

    /** `+84912345678`, the form the server takes. Only for a complete number. */
    fun e164(local: String): String = "+84$local"

    /**
     * `+84 912 345 678` - the account's own number, as Settings draws it.
     *
     * Endpoint 24 answers in the form the server stores, which is E.164. Put
     * on the screen unchanged it was a fourteen-digit run with no grouping,
     * beside a field two screens away that groups the same number.
     * Anything that does not parse is returned as it came: a number nobody
     * can read beats a number that is wrong.
     */
    fun display(e164: String): String {
        val local = clean(e164.removePrefix("+84").removePrefix("84"))
        return if (isComplete(local)) "+84 " + spaced(local) else e164
    }
}

/**
 * Draws the grouping without putting it in the value.
 *
 * **Not by formatting the text as it is typed.** That was the first attempt
 * and it silently reordered the number: typing `912345678` produced
 * `912 456 783`, because inserting a space moves every later character and
 * the caret is left pointing at the wrong one, so the next digit lands in the
 * wrong place. Seen on the emulator, not reasoned about.
 *
 * A `VisualTransformation` is the way round it: the value stays nine plain
 * digits and only the drawing gains spaces, with [OffsetMapping] telling the
 * caret where each digit went.
 */
private object GroupDigits : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        AnnotatedString(VnMobile.spaced(text.text)),
        object : OffsetMapping {
            // Two spaces go in, before the 4th digit and before the 7th.
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 6 -> offset + 1
                else -> offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset - 1
                else -> offset - 2
            }
        },
    )
}

/**
 * The phone field — a recess, with the label above it, because a label inside
 * a recess is a second depth inside the first.
 *
 * **This is a real input.** It was a `Text` drawing a fixed string until
 * 2026-08-13, which meant tapping it raised no keyboard: the sign-in screen
 * looked finished and could not be used. The only way on was the button, and
 * the only way back was the system back gesture, which from the sign-in screen
 * closes the app — so the flow read as "it just exits".
 *
 * The caret is the accent, matching [vn.edu.vgu.smartlocker.ui.OtpCells].
 * Digits are grouped as they are typed, so what is on screen is what the
 * design draws, while what is held is nine plain digits.
 */
@Composable
fun PhoneField(
    number: String,
    onChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val t = LocalLockerTokens.current
    Recess(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "+84",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NumberFace,
                    color = t.ink2,
                ),
                modifier = Modifier.padding(end = 8.dp),
            )
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp)
                    .background(t.hair),
            )
            // The value is nine plain digits; the grouping is drawn on top by
            // [GroupDigits], so a space can be neither typed nor deleted.
            BasicTextField(
                value = number,
                onValueChange = { onChange(VnMobile.clean(it)) },
                visualTransformation = GroupDigits,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NumberFace,
                    color = t.ink,
                ),
                singleLine = true,
                cursorBrush = SolidColor(t.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { field ->
                    if (number.isEmpty()) {
                        // Half weight, because it is set in the same face
                        // and size as a real number beside a real +84 prefix.
                        // At full ink3 it reads as a number already typed in.
                        Text(
                            text = VnMobile.spaced("912345678"),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NumberFace,
                                color = t.ink3.copy(alpha = 0.5f),
                            ),
                        )
                    }
                    field()
                },
            )
        }
    }
}
