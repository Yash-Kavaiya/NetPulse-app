package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---- Light palette -------------------------------------------------------------------------
internal val LightBlue = Color(0xFF4285F4)
internal val LightRed = Color(0xFFD93025)
internal val LightYellow = Color(0xFFF9AB00)
internal val LightGreen = Color(0xFF1E8E3E)
internal val LightUiBlue = Color(0xFF1A73E8)

internal val LightOnSurface = Color(0xFF202124)
internal val LightOnSurfaceVariant = Color(0xFF5F6368)
internal val LightOutline = Color(0xFFDADCE0)
internal val LightBackground = Color(0xFFF6F8FC)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceContainer = Color(0xFFF0F4F9)
internal val LightSurfaceContainerHigh = Color(0xFFE9EEF6)

internal val LightInfoBg = Color(0xFFE8F0FE)
internal val LightSuccessBg = Color(0xFFE6F4EA)
internal val LightWarningBg = Color(0xFFFEF7E0)
internal val LightErrorBg = Color(0xFFFCE8E6)

// ---- Dark palette --------------------------------------------------------------------------
// Dark surfaces step up in lightness with elevation instead of using shadows, and accents are
// desaturated tonal variants so they keep contrast without glowing on dark backgrounds.
internal val DarkBlue = Color(0xFF8AB4F8)
internal val DarkRed = Color(0xFFF28B82)
internal val DarkYellow = Color(0xFFFDD663)
internal val DarkGreen = Color(0xFF81C995)
internal val DarkUiBlue = Color(0xFFA8C7FA)

internal val DarkOnSurface = Color(0xFFE3E5EA)
internal val DarkOnSurfaceVariant = Color(0xFFA9AEB8)
internal val DarkOutline = Color(0xFF3A3F47)
internal val DarkBackground = Color(0xFF0E1013)
internal val DarkSurface = Color(0xFF181B20)
internal val DarkSurfaceContainer = Color(0xFF1E2228)
internal val DarkSurfaceContainerHigh = Color(0xFF272B32)

internal val DarkInfoBg = Color(0xFF1B2A41)
internal val DarkSuccessBg = Color(0xFF17301F)
internal val DarkWarningBg = Color(0xFF382D12)
internal val DarkErrorBg = Color(0xFF3C1F1D)

data class GoogleThemeColors(
    val blue: Color = LightBlue,
    val red: Color = LightRed,
    val yellow: Color = LightYellow,
    val green: Color = LightGreen,
    val uiBlue: Color = LightUiBlue,
    val darkGray: Color = LightOnSurface,
    val mediumGray: Color = LightOnSurfaceVariant,
    val borderGray: Color = LightOutline,
    val lightGray: Color = LightSurfaceContainer,
    /** Content color for use on top of [uiBlue]. */
    val white: Color = Color.White,
    val infoBg: Color = LightInfoBg,
    val successBg: Color = LightSuccessBg,
    val warningBg: Color = LightWarningBg,
    val errorBg: Color = LightErrorBg,
    /** Gradient stops for hero cards. */
    val heroStart: Color = Color(0xFF1A73E8),
    val heroEnd: Color = Color(0xFF6C4DE6),
    val isDark: Boolean = false
)

internal val LightGoogleColors = GoogleThemeColors()

internal val DarkGoogleColors = GoogleThemeColors(
    blue = DarkBlue,
    red = DarkRed,
    yellow = DarkYellow,
    green = DarkGreen,
    uiBlue = DarkUiBlue,
    darkGray = DarkOnSurface,
    mediumGray = DarkOnSurfaceVariant,
    borderGray = DarkOutline,
    lightGray = DarkSurfaceContainerHigh,
    white = Color(0xFF062E6F),
    infoBg = DarkInfoBg,
    successBg = DarkSuccessBg,
    warningBg = DarkWarningBg,
    errorBg = DarkErrorBg,
    heroStart = Color(0xFF1B3A6B),
    heroEnd = Color(0xFF3B2A7A),
    isDark = true
)

val LocalGoogleColors = staticCompositionLocalOf { GoogleThemeColors() }

// Theme-aware accessors: these resolve to the light or dark variant of the active theme, so
// components can keep referring to brand colors by name.
val GoogleBlue: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.blue
val GoogleRed: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.red
val GoogleYellow: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.yellow
val GoogleGreen: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.green
val GoogleUIBlue: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.uiBlue
val BorderGray: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.borderGray
val White: Color @Composable @ReadOnlyComposable get() = LocalGoogleColors.current.white
