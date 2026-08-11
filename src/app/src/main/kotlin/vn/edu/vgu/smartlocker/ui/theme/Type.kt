package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import vn.edu.vgu.smartlocker.R

/**
 * The type scale, ported from the mock-up's roles (`screens.css`).
 *
 * **Geist and Geist Mono, bundled** — DESIGN.md names both, and they are the
 * face the whole mock-up was drawn in. An earlier version of this file used
 * Roboto and `FontFamily.Monospace` instead, on the argument that character
 * could come from scale, weight and tracking alone. It cannot: Android's
 * stock monospace is Droid Sans Mono, a wide typewriter face, and it went on
 * every number in the app — box numbers, times, the phone number, the code
 * cells. That is what made the screens read as unfinished.
 *
 * Both files are variable, one `wght` axis from 100 to 900, so one 124 kB
 * file covers every weight instead of four static cuts. Below API 26 Android
 * ignores variation settings and draws the 400 instance; the shape is still
 * Geist, which is the part that matters.
 *
 * Roles, in the mock-up's own words:
 *   display 40/650   the sign-in headline
 *   number  76/600   the claim ticket's door number
 *   bignum  122/700  the Opened screen, read across a corridor
 *   headline 25/600  a screen's one heading
 *   body    14/400   reading text
 *   label   12.5/550 a card's second line
 *   caption 10/600   a section label, uppercase
 */

// FontVariation is still marked experimental. It is the only way to ask a
// variable font for a weight, and the alternative — four static cuts — is
// four more files for the same result.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun geist(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun family(resId: Int) = FontFamily(
    geist(resId, 400),
    geist(resId, 500),
    geist(resId, 600),
    geist(resId, 700),
)

/**
 * The face for everything that is a number or a time. Geist Mono, because
 * these have to line up and change in place without the row reflowing.
 */
val NumberFace = family(R.font.geist_mono)

private val Face = family(R.font.geist)

val LockerType = Typography(

    // The number on the Opened screen. The one thing read from a distance.
    displayLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Bold,
        fontSize = 122.sp,
        lineHeight = 122.sp * 0.84f,
        letterSpacing = (-0.06).em,
    ),

    // The claim ticket's number, and the cabinet panel's.
    displayMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 76.sp,
        lineHeight = 76.sp * 0.88f,
        letterSpacing = (-0.055).em,
    ),

    // The sign-in headline, three lines of large tight type.
    displaySmall = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 40.sp * 1.02f,
        letterSpacing = (-0.045).em,
    ),

    // The code screens' heading (the display at 34sp).
    headlineLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 34.sp * 1.04f,
        letterSpacing = (-0.04).em,
    ),

    // A screen's one heading.
    headlineMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 25.sp * 1.14f,
        letterSpacing = (-0.032).em,
    ),

    // The brand in the app bar, and the user's name.
    titleLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.012).em,
    ),

    // Reading text, and the one-liners that name controls.
    bodyLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // A card's second line, and the quiet text under a primary control.
    labelLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    ),

    // A section label, a field label, the stub heading on a ticket —
    // small, spaced, quiet. Never lighter than 600.
    labelMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.15.em,
    ),

    // The smallest rank: the stub's own labels, the divider's "or".
    labelSmall = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.12.em,
    ),
)
