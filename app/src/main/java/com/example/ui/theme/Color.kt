package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Google Primary Brand Colors
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

// Secondary / UI Accent Colors
val GoogleUIBlue = Color(0xFF1A73E8)

// Grayscale & Neutral Colors
val DarkGray = Color(0xFF202124)
val MediumGray = Color(0xFF5F6368)
val BorderGray = Color(0xFFDADCE0)
val LightGray = Color(0xFFF8F9FA)
val White = Color(0xFFFFFFFF)

// State Colors (Backgrounds)
val InfoBackground = Color(0xFFE8F0FE)
val SuccessBackground = Color(0xFFE6F4EA)
val WarningBackground = Color(0xFFFEF7E0)
val ErrorBackground = Color(0xFFFCE8E6)

// Dark Theme Companions
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1F21)
val DarkSurfaceVariant = Color(0xFF2D2E31)
val DarkOutline = Color(0xFF3C4043)
val DarkOnSurface = Color(0xFFE8EAED)
val DarkOnSurfaceVariant = Color(0xFF9AA0A6)

data class GoogleThemeColors(
    val blue: Color = GoogleBlue,
    val red: Color = GoogleRed,
    val yellow: Color = GoogleYellow,
    val green: Color = GoogleGreen,
    val uiBlue: Color = GoogleUIBlue,
    val darkGray: Color = DarkGray,
    val mediumGray: Color = MediumGray,
    val borderGray: Color = BorderGray,
    val lightGray: Color = LightGray,
    val white: Color = White,
    val infoBg: Color = InfoBackground,
    val successBg: Color = SuccessBackground,
    val warningBg: Color = WarningBackground,
    val errorBg: Color = ErrorBackground,
)

val LocalGoogleColors = staticCompositionLocalOf { GoogleThemeColors() }

