/*
 * The design, ported from the mock-up (`docs/designs/mockup/`).
 *
 * Dark leads, following the drafts. Light is a second design — white on the
 * mid-steel ground, with the accent going burnt orange, because a cool steel
 * ground has nothing to push against from another blue. The one colour that
 * never changes is the amber door light: a door holding your parcel is a fact
 * about the world, not about the phone's settings.
 *
 * Tokens are exposed through [LocalLockerTokens] so a screen can read the
 * design's own names (ground, surface, hair, lip, recessIn…) rather than
 * squeezing them into Material roles.
 */
package vn.edu.vgu.smartlocker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** The design tokens for the current scheme. */
val LocalLockerTokens = staticCompositionLocalOf { LightTokens }

private val LightScheme = lightColorScheme(
    primary            = LightTokens.accentDeep,
    onPrimary          = LightTokens.onAccent,
    primaryContainer   = LightTokens.surface2,
    onPrimaryContainer = LightTokens.ink,

    secondary          = LightTokens.ink2,
    onSecondary        = LightTokens.surface,

    tertiary           = DoorLight,
    onTertiary         = OnDoorLight,
    tertiaryContainer  = DoorLight,
    onTertiaryContainer = OnDoorLight,

    background         = LightTokens.ground,
    onBackground       = LightTokens.ink,
    surface            = LightTokens.surface,
    onSurface          = LightTokens.ink,
    surfaceVariant     = LightTokens.surface2,
    onSurfaceVariant   = LightTokens.ink2,
    outline            = LightTokens.hair,
    outlineVariant     = LightTokens.surface2,

    error              = LightTokens.refuse,
    onError            = LightTokens.surface,
)

private val DarkScheme = darkColorScheme(
    primary            = DarkTokens.accentDeep,
    onPrimary          = DarkTokens.onAccent,
    primaryContainer   = DarkTokens.surface2,
    onPrimaryContainer = DarkTokens.ink,

    secondary          = DarkTokens.ink2,
    onSecondary        = DarkTokens.surface,

    tertiary           = DoorLight,
    onTertiary         = OnDoorLight,
    tertiaryContainer  = DoorLight,
    onTertiaryContainer = OnDoorLight,

    background         = DarkTokens.ground,
    onBackground       = DarkTokens.ink,
    surface            = DarkTokens.surface,
    onSurface          = DarkTokens.ink,
    surfaceVariant     = DarkTokens.surface2,
    onSurfaceVariant   = DarkTokens.ink2,
    outline            = DarkTokens.hair,
    outlineVariant     = DarkTokens.surface2,

    error              = DarkTokens.refuse,
    onError            = DarkTokens.surface,
)

/**
 * No Dynamic Colour.
 *
 * Material You would let the phone's wallpaper repaint this app, and the one
 * thing that must never be repainted is the amber that means *this door is
 * yours*. A person learns that colour once and then trusts it.
 */
@Composable
fun SmartLockerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (dark) DarkTokens else LightTokens
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(LocalLockerTokens provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LockerType,
            shapes = LockerShapes,
            content = content,
        )
    }
}

/** Kept so previews can force a scheme without an Activity behind them. */
@Composable
fun PreviewTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val tokens = if (dark) DarkTokens else LightTokens
    CompositionLocalProvider(LocalLockerTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = LockerType,
            shapes = LockerShapes,
            content = content,
        )
    }
}
