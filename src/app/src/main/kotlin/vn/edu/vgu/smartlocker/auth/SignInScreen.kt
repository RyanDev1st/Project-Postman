package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.GoButton
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.SocialButton
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
    onSendCode: () -> Unit,
    onGoogle: () -> Unit,
    onVgu: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    Column(modifier = Modifier.fillMaxSize()) {
        AuthHero()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Nhận hàng\nkhông cần\n")
                    withStyle(SpanStyle(color = t.accent)) { append("chìa khoá") }
                },
                style = MaterialTheme.typography.displaySmall,
                color = t.ink,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "Any VGU locker, from your phone.",
                style = MaterialTheme.typography.labelLarge,
                color = t.ink2,
            )
        }

        // The form: three ranks, three intervals — 8px inside a field group,
        // 12px from a field to the button that submits it, 22px between ranks.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PHONE NUMBER",
                        style = MaterialTheme.typography.labelMedium,
                        color = t.ink2,
                        modifier = Modifier.padding(start = 3.dp),
                    )
                    PhoneField(number = "912 345 678")
                }
                GoButton(text = "Send code", onClick = onSendCode)
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SocialButton(
                    label = "Google",
                    onClick = onGoogle,
                    modifier = Modifier.weight(1f),
                ) { GoogleMark() }
                SocialButton(
                    label = "VGU account",
                    onClick = onVgu,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = AppIcons.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = t.ink,
                    )
                }
            }
        }

        // Terms are not a control, and they are not in the box with the
        // buttons either.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "PRIVACY",
                style = MaterialTheme.typography.labelSmall,
                color = t.ink3,
            )
            Text(
                text = "TERMS",
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
            .height(214.dp),
    ) {
        val boxW = maxWidth
        val boxH = 214.dp
        Image(
            painter = painterResource(R.drawable.cabinet),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth(1.75f)
                .aspectRatio(1f)
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

/** The phone field — a recess, with the label above it, because a
 * placeholder leaves exactly when the field is in use. */
@Composable
fun PhoneField(
    number: String,
    onChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    focused: Boolean = true,
) {
    val t = LocalLockerTokens.current
    Recess(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text(
                text = "+84",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NumberFace,
                    color = t.ink2,
                ),
                modifier = Modifier.padding(end = 9.dp),
            )
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp)
                    .background(t.hair),
            )
            Text(
                text = number,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NumberFace,
                    color = if (number.isEmpty()) t.ink3 else t.ink,
                ),
            )
        }
    }
}

@Composable
private fun Divider() {
    val t = LocalLockerTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(t.hair))
        Text(
            text = "OR",
            style = MaterialTheme.typography.labelSmall,
            color = t.ink3,
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(t.hair))
    }
}

/** The Google G, drawn from its own brand paths. */
@Composable
private fun GoogleMark() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = size.width / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawPath(parse("M23 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.2a5.3 5.3 0 0 1-2.3 3.5v2.9h3.7c2.2-2 3.4-5 3.4-8.6z"), Color(0xFF4285F4))
            drawPath(parse("M12 24c3.1 0 5.7-1 7.6-2.8l-3.7-2.9c-1 .7-2.3 1.1-3.9 1.1-3 0-5.5-2-6.4-4.7H1.8v3C3.7 21.4 7.6 24 12 24z"), Color(0xFF34A853))
            drawPath(parse("M5.6 14.7a7.2 7.2 0 0 1 0-4.6v-3H1.8a12 12 0 0 0 0 10.6l3.8-3z"), Color(0xFFFBBC05))
            drawPath(parse("M12 4.8c1.7 0 3.2.6 4.4 1.7l3.3-3.3C17.7 1.2 15.1 0 12 0 7.6 0 3.7 2.6 1.8 6.1l3.8 3c.9-2.7 3.4-4.3 6.4-4.3z"), Color(0xFFEA4335))
        }
    }
}

private fun parse(d: String) = androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath()

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SignInPreview() {
    PreviewTheme { SignInScreen({}, {}) }
}

@Preview(showBackground = true, heightDp = 780, name = "dark")
@Composable
private fun SignInDarkPreview() {
    PreviewTheme(dark = true) { SignInScreen({}, {}) }
}
