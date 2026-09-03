package vn.edu.vgu.smartlocker.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.SocialButton

/**
 * The way in, as one button. Task P2-13, [ADR 0026].
 *
 * Lives beside the sign-in screen rather than in `ui/` because it is not a
 * shape somebody else will reuse - it is this product's front door, and the
 * rules about what it may say are the sign-in rules. `SignInScreen.kt` was
 * already at 285 of the 300 lines a file may have, which is the other reason.
 */

/** Google's four brand colours, in the order [AppIcons.GoogleG] draws them. */
private val BRAND = listOf(
    Color(0xFF4285F4), // the right bar
    Color(0xFF34A853), // the bottom sweep
    Color(0xFFFBBC05), // the left
    Color(0xFFEA4335), // the top
)

/**
 * Google's G, in Google's colours.
 *
 * Four filled paths on a 24-unit viewport, scaled to whatever size is asked
 * for. Tinting it a single colour is not something Google's brand guidelines
 * permit, which is why this is a canvas and not an `Icon`.
 */
@Composable
fun GoogleMark(size: Dp = 18.dp) {
    val paths = remember {
        AppIcons.GoogleG.map { PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier = Modifier.size(size)) {
        scale(this.size.minDimension / 24f, pivot = Offset.Zero) {
            paths.forEachIndexed { index, path -> drawPath(path, BRAND[index]) }
        }
    }
}

/**
 * The Google button, its one line of guidance, and anything that went wrong.
 *
 * The hint under it is not decoration. A student's phone is normally signed
 * in to a personal Gmail, and that account is the one the picker offers
 * first; the server will refuse it, correctly, but being told **before**
 * tapping is worth a line of small type.
 *
 * [note] is a sentence from the last attempt - the domain refusal, no account
 * on the phone, or a platform failure. It sits under the button rather than
 * over it so the button never moves as it appears.
 */
@Composable
fun GoogleRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    note: Int? = null,
) {
    val t = LocalLockerTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SocialButton(
            label = stringResource(if (busy) R.string.working else R.string.signin_google),
            // Tapping twice opens two pickers, and the second lands on top of
            // a system window that is already waiting for an answer.
            onClick = { if (!busy) onClick() },
            modifier = Modifier.fillMaxWidth(),
            leading = { GoogleMark() },
        )
        Text(
            text = stringResource(R.string.signin_google_hint),
            style = MaterialTheme.typography.labelSmall,
            color = t.ink3,
            modifier = Modifier.padding(start = 2.dp),
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
}
