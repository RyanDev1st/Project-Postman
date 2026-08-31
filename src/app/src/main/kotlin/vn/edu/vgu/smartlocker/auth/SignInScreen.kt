package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.pressable
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace
import vn.edu.vgu.smartlocker.ui.theme.PreviewTheme

/**
 * The way in — and it is not a form, it is a screen.
 *
 * The cabinet, cropped and bleeding off three edges — the mark. A logo
 * placeholder on a product whose identity is a piece of metal in a corridor
 * costs nothing and says nothing. Then three lines of large tight type with
 * the last word in the accent. Then the form, on no panel at all: the
 * recessed field, the filled button and the outlined pair carry the three
 * ranks by themselves, because a container would flatten them into one.
 */
@Composable
fun SignInScreen(
    /** The local part of the number, nine digits — see [VnMobile]. */
    number: String,
    onNumberChange: (String) -> Unit,
    onSendCode: () -> Unit,
    /** The other way in - endpoint 21, task P2-09. It is offered rather than
     *  made the default because a password can only be set from an account
     *  that already exists, so a new receiver has to come this way first. */
    onUsePassword: () -> Unit = {},
    /** What the server said, if anything went wrong asking for a code. Also
     * how this screen says the build has no server address at all. */
    note: Int? = null,
    /** True while the code is being asked for. */
    busy: Boolean = false,
) {
    val t = LocalLockerTokens.current
    Column(modifier = Modifier.fillMaxSize()) {
        AuthHero()

        // The slack in this screen belongs above the headline, where the
        // hero is already fading into the ground, and not below it. Arranged
        // from the top it left a 250px hole between the sub-headline and the
        // phone field, which reads as a screen missing an element rather than
        // as a composed one.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.signin_headline_lead))
                    append("\n")
                    // The lead recedes and the payoff keeps full ink.
                    // Emphasis used to be a hue on the last line; with the
                    // accent at the end of the value scale that would be a
                    // near-white word among near-white words. Value carries
                    // it instead, which is what the type scale is for.
                    withStyle(SpanStyle(color = t.ink)) {
                        append(stringResource(R.string.signin_headline_accent))
                    }
                },
                style = MaterialTheme.typography.displaySmall,
                color = t.ink2,
            )
            Text(
                text = stringResource(R.string.signin_sub),
                style = MaterialTheme.typography.labelLarge,
                color = t.ink3,
                modifier = Modifier.padding(top = 12.dp, bottom = 22.dp),
            )
        }

        // The form: three ranks, three intervals — 8px inside a field group,
        // 12px from a field to the button that submits it, 22px between ranks.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.phone_number),
                        style = MaterialTheme.typography.labelMedium,
                        color = t.ink2,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                    PhoneField(number = number, onChange = onNumberChange)
                }
                // Off until the number could actually receive a code. The
                // button used to be live against a fixed string, so it went
                // on to the code screen whatever was — or was not — typed.
                GoButton(
                    text = stringResource(if (busy) R.string.working else R.string.send_code),
                    onClick = onSendCode,
                    enabled = VnMobile.isComplete(number) && !busy,
                )
                note?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = t.ink3,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }

            // The second way in. A quiet line rather than a button: it is
            // for somebody who already has an account, and the screen's job
            // is to get a first-time receiver to a code.
            //
            // It is not conditional on anything. The app cannot know whether
            // a number has a password without asking the server, and asking
            // would be an endpoint that answers "is this number registered".
            // Underlined rather than tinted. It is the only way in that
            // works while BUG-016 keeps one-time codes from arriving, so it
            // cannot look like the small print two rows below it - and with
            // the accent at the end of the value scale, a tint would make it
            // look like heavier body text rather than a link.
            Text(
                text = stringResource(R.string.pw_use_password),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
                color = t.ink,
                modifier = Modifier
                    .pressable(onClick = onUsePassword)
                    .padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
            )

            // The Google and VGU buttons were here. Both are removed until
            // there is something behind them.
            //
            // Google went to a screen that took a phone number, threw it away,
            // and went to Home - no code, no token, no server call at all. So
            // whatever number was typed, the app opened with whichever account
            // was already in the secure store. Found on Ryan's phone,
            // 2026-08-17: "It always logs me into one account despite that I
            // typed a different number." BUG-011.
            //
            // VGU was worse in a quieter way: `onVgu` defaulted to `{}` and
            // nothing ever passed one, so the button did nothing and looked
            // broken.
            //
            // Endpoint 19 in api-contract.md is the real Google path and it is
            // designed correctly - it never makes an account, because a Google
            // account has no phone number and a shipper finds people by
            // number. It is task P2-08, blocked on an OAuth client id. When
            // that exists, this row comes back wired to it.
        }

        // Terms are not a control, and they are not in the box with the
        // buttons either.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.privacy),
                style = MaterialTheme.typography.labelSmall,
                color = t.ink3,
            )
            Text(
                text = stringResource(R.string.terms),
                style = MaterialTheme.typography.labelSmall,
                color = t.ink3,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * The cabinet is the mark: cropped and bleeding off three edges, masked out
 * at the bottom. The crop is chosen, not centred — the top-right quadrant
 * carries doors 03, 04, 07, 08, 11 and 12 and the right-hand side panel,
 * which is the part that shows the three-quarter depth.
 *
 * The window shows image fractions x 0.46–1.03, y 0.07–0.43: the image is
 * 175% of the box's width, shifted left 80% of the box and up 19% of its
 * height. Past the right edge the render is transparent, so the cabinet ends
 * and the ground shows through — which is what stops it reading as wallpaper.
 */
@Composable
private fun AuthHero() {
    val t = LocalLockerTokens.current
    val cabinet = ImageBitmap.imageResource(R.drawable.cabinet)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            // The render is deliberately wider than its box, so the box has
            // to clip or it paints over the form below it.
            .clipToBounds(),
    ) {
        // The crop is drawn, not laid out. BUG-004.
        //
        // It used to be an oversized child: `requiredSize(boxW * 1.75f)` and
        // then an offset back. The numbers were right - reproduced against
        // the bitmap they give the picture the comment above describes - and
        // on a real GPU the screen did not. What arrived was the far right
        // edge of the cabinet at roughly twice the intended zoom, ending in a
        // hard vertical cut a third of the way across, on hardware GL as well
        // as on the software rasteriser, which is what ruled out the
        // rasteriser. Three modifiers deciding one rectangle between them is
        // one too many to reason about, so the rectangle is now stated.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.width * 1.75f
            drawImage(
                image = cabinet,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(cabinet.width, cabinet.height),
                dstOffset = IntOffset(
                    (size.width * -0.80f).roundToInt(),
                    (size.height * -0.19f).roundToInt(),
                ),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            )
        }
        // Masked out at the bottom: the ground comes up through the cabinet.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, t.ground),
                    )
                ),
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SignInPreview() {
    PreviewTheme { SignInScreen("912345678", {}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun SignInDarkPreview() {
    PreviewTheme(dark = true) { SignInScreen("912345678", {}, {}) }
}
