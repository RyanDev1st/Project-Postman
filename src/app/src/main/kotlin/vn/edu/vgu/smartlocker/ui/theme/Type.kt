package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The Material type scale, with one role pushed hard.
 *
 * Roboto is the system face and stays the system face - Android's own
 * guidance, and the right call for a screen somebody reads in two seconds
 * while walking. Character comes from scale, weight and tracking, not from a
 * novelty face the phone would have to load first.
 *
 * The display roles carry the box number and nothing else. They are set
 * enormous and tight, because that number is the only thing in this app that
 * is read from across a corridor.
 */

private val Face = FontFamily.Default   // Roboto on Android

val LockerType = Typography(

    // The box number.
    displayLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Bold,
        fontSize = 104.sp,
        lineHeight = 104.sp,
        letterSpacing = (-0.045).em,
    ),
    displayMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Bold,
        fontSize = 68.sp,
        lineHeight = 70.sp,
        letterSpacing = (-0.04).em,
    ),
    displaySmall = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em,
    ),

    headlineMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.015).em,
    ),

    titleLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),

    // Small type that has to survive corridor light: never lighter than
    // Medium, and given room by tracking rather than by size.
    labelLarge = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    labelMedium = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.09.em,
    ),
)
