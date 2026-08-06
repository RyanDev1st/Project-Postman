/*
 * THESIS: this app exists to walk one person to one door. The box number is
 * the product, so it is set at 104sp and everything else gets out of its
 * way. It refuses the parcel-tracking dashboard - no timeline, no map, no
 * status chips, no card grid.
 *
 * OWN-WORLD: powder-coated locker steel. Cool grey-blue grounds, near-black
 * ink, generous 28dp radii, and one glass plane - the parcel card, lifted
 * off the ground on a real offset shadow. Exactly one saturated colour,
 * amber #FFB200, and it appears on exactly one screen: the door is open.
 * That screen is drenched in it and readable across a corridor.
 *
 * STORY: a notice arrives. The receiver opens the app, sees one box number
 * filling the screen, walks to the cabinet, and presses one button.
 *
 * FIRST VIEWPORT: the waiting screen. Cabinet name small, box number huge and
 * centred on a glass plane, arrival time beneath. The primary action is a
 * full-width filled button in the bottom third, within a thumb's reach of a
 * hand that is also holding a parcel.
 *
 * FORM: pinned by the user on 2026-08-06 - depth, liquid glass, modern, a 3D
 * feel, minimal, UX-led. A pinned direction beats the roll, so concept-seed
 * was not run; disclosed rather than skipped quietly. Rendered through
 * Material 3, because the platform is Android and iOS controls here would be
 * a costume.
 *
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the
 * finish review, the verdict, and DESIGN.md.
 */
package vn.edu.vgu.smartlocker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightSteel = lightColorScheme(
    primary            = SteelPrimary,
    onPrimary          = SteelOnPrimary,
    primaryContainer   = SteelSurfaceAlt,
    onPrimaryContainer = SteelInk,

    secondary          = SteelInkSoft,
    onSecondary        = SteelOnPrimary,

    // The door light is the accent role, and the app spends it once.
    tertiary           = DoorLight,
    onTertiary         = OnDoorLight,
    tertiaryContainer  = DoorLight,
    onTertiaryContainer = OnDoorLight,

    background         = SteelGround,
    onBackground       = SteelInk,
    surface            = SteelSurface,
    onSurface          = SteelInk,
    surfaceVariant     = SteelSurfaceAlt,
    onSurfaceVariant   = SteelInkSoft,
    outline            = SteelOutline,
    outlineVariant     = SteelSurfaceAlt,

    error              = RefusalLight,
    onError            = SteelOnPrimary,
)

private val DarkSteel = darkColorScheme(
    primary            = NightPrimary,
    onPrimary          = NightOnPrimary,
    primaryContainer   = NightSurfaceAlt,
    onPrimaryContainer = NightInk,

    secondary          = NightInkSoft,
    onSecondary        = NightOnPrimary,

    tertiary           = DoorLight,
    onTertiary         = OnDoorLight,
    tertiaryContainer  = DoorLight,
    onTertiaryContainer = OnDoorLight,

    background         = NightGround,
    onBackground       = NightInk,
    surface            = NightSurface,
    onSurface          = NightInk,
    surfaceVariant     = NightSurfaceAlt,
    onSurfaceVariant   = NightInkSoft,
    outline            = NightOutline,
    outlineVariant     = NightSurfaceAlt,

    error              = RefusalDark,
    onError            = NightOnPrimary,
)

/**
 * No Dynamic Colour.
 *
 * Material You would let the phone's wallpaper repaint this app, and the one
 * thing that must never be repainted is the amber that means a door is open.
 * A person learns that colour once and then trusts it. Handing it to a
 * wallpaper is a real cost for a decorative gain.
 */
@Composable
fun SmartLockerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (dark) DarkSteel else LightSteel
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge to edge: the app draws under the bars, and the bar icons
            // flip to match the ground behind them.
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = LockerType,
        shapes = LockerShapes,
        content = content,
    )
}

/** Kept so previews can force a scheme without an Activity behind them. */
@Composable
fun PreviewTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkSteel else LightSteel,
        typography = LockerType,
        shapes = LockerShapes,
        content = content,
    )
}
