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
 * **The text sizes sit on one ratio, 1.2, anchored at the body's 15sp.**
 * They did not before, and that is what made the screens read as unfinished.
 * The ported roles ran 9.5, 10, 12.5, 14, 14.5, 15 — four of them inside two
 * and a half points, at ratios of 1.034 to 1.120. Half a point is below the
 * size difference a reader can see, so those four did not read as four ranks.
 * They read as one rank rendered inconsistently, and then the scale leapt
 * 1.667 to reach the heading with nothing in between.
 *
 * Worse, **`titleLarge` was 14.5sp while `bodyLarge` was 15sp**: the brand and
 * the user's name were set smaller than reading text. No screen can recover a
 * hierarchy the scale does not have.
 *
 * A constant ratio is Bringhurst's rule (*The Elements of Typographic Style*)
 * and Tim Brown's for screens ("More Meaningful Typography", 2011): every step
 * must be a step the reader can see.
 *
 *   caption  10.5/600  a section label, uppercase, and the nav's own labels
 *   label    12.5/500  a card's second line
 *   body     15/400    reading text
 *   title    18/600    the brand in the bar, the user's name
 *   headline 26/600    a screen's one heading
 *
 * The **display sizes are not on the ratio and should not be**. 34, 40, 76 and
 * 122 are sized for the job — 122sp is set to be read across a corridor, not
 * to fit a progression — and the ratio governs text, not signage.
 *
 * The **leading curve is unchanged**, because it was already right: tight at
 * display (0.84) opening to loose at body (1.40) is what the books ask for.
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
        fontSize = 26.sp,
        lineHeight = 26.sp * 1.14f,
        letterSpacing = (-0.032).em,
    ),

    // The brand in the app bar, and the user's name. One step above body, so
    // it outranks it — at 14.5 against a 15sp body it did not. The bar does
    // not grow: its height comes from the 30dp beads, and an 18sp line box is
    // 24dp.
    titleLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.018).em,
    ),

    // Reading text, and the one-liners that name controls.
    bodyLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    // One rank with bodyLarge, not a rank below it. The two were 14 and 15,
    // which is not a difference a reader can see. What separates them where
    // both are used is weight: this slot is the one that gets SemiBold.
    bodyMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
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
        fontSize = 10.5.sp,
        lineHeight = 14.5.sp,
        letterSpacing = 0.15.em,
    ),

    // The stub's own labels, the divider's "or", and the nav bar's tab names.
    //
    // Identical to labelMedium, deliberately. It was 9.5 against that slot's
    // 10 — half a point, which is no rank at all, so the app had one caption
    // rank pretending to be two. The slot stays so the call sites that name it
    // keep working; it is an alias, not a size.
    labelSmall = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.5.sp,
        letterSpacing = 0.15.em,
    ),
)
