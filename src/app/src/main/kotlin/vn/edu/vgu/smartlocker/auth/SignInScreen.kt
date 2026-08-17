package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.Recess
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
    /** What the server said, if anything went wrong asking for a code. Also
     * how this screen says the build has no server address at all. */
    note: Int? = null,
    /** True while the code is being asked for. */
    busy: Boolean = false,
) {
    val t = LocalLockerTokens.current
    Column(modifier = Modifier.fillMaxSize()) {
        AuthHero()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.signin_headline_lead))
                    append("\n")
                    withStyle(SpanStyle(color = t.accent)) {
                        append(stringResource(R.string.signin_headline_accent))
                    }
                },
                style = MaterialTheme.typography.displaySmall,
                color = t.ink,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.signin_sub),
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            // The render is deliberately wider than its box, so the box has
            // to clip or it paints over the form below it.
            .clipToBounds(),
    ) {
        val boxW = maxWidth
        val boxH = 214.dp
        Image(
            painter = painterResource(R.drawable.cabinet),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                // requiredSize, not fillMaxWidth(1.75f): that fraction is
                // declared 0..1 and is coerced back to the parent's width,
                // so the zoom silently never happened and the -80% offset
                // then dragged the un-zoomed render off the left edge. All
                // that was left was a sliver. requiredSize ignores the
                // parent's maximum, which is the whole point here.
                .requiredSize(boxW * 1.75f)
                .offset(
                    x = boxW * -0.80f,
                    y = boxH * -0.19f,
                ),
        )
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
