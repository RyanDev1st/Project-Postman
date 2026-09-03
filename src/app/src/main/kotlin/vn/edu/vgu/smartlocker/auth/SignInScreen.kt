package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    /**
     * Endpoint 19, and **the way in** - ADR 0026. A VGU Google account is
     * what makes an account here; the number below is what is left of the
     * old way and P2-14 removes it once this is proven on a real phone.
     */
    onGoogle: () -> Unit = {},
    /** True while the account picker is open or the token is being posted. */
    googleBusy: Boolean = false,
    /** What went wrong with the last tap on the Google button, if anything. */
    googleNote: Int? = null,
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
            // First, because it is the way in. ADR 0026 made the Google
            // account what creates an account here, and the number below is
            // a delivery address rather than proof of anybody's identity -
            // it is asked for once, later, when a box is booked.
            GoogleRow(onClick = onGoogle, busy = googleBusy, note = googleNote)

            // A word, not a rule across the screen. The two ways in are not
            // equals and drawing a divider between them would say they were.
            Text(
                text = stringResource(R.string.signin_or),
                style = MaterialTheme.typography.labelSmall,
                color = t.ink3,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally),
            )

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

            // The Google button that used to sit here went to a screen that
            // took a phone number, threw it away and went to Home - no code,
            // no token, no server call at all - so the app opened with
            // whichever account was already in the secure store. BUG-011, on
            // Ryan's phone, 2026-08-17. It was removed rather than patched.
            //
            // It is back, at the top of the form, and it now posts a token
            // Google signed to endpoint 19. Nothing about who is signing in
            // is decided on this phone - see GoogleSignIn.
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
