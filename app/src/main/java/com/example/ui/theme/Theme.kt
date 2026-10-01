package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkUiBlue,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = DarkGreen,
    onSecondary = Color(0xFF0A3818),
    secondaryContainer = Color(0xFF233B5E),
    onSecondaryContainer = Color(0xFFD3E3FD),
    tertiary = DarkYellow,
    onTertiary = Color(0xFF3B2F00),
    error = DarkRed,
    onError = Color(0xFF601410),
    errorContainer = DarkErrorBg,
    onErrorContainer = Color(0xFFF9DEDC),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkUiBlue,
    outline = Color(0xFF8E939C),
    outlineVariant = DarkOutline,
    surfaceDim = DarkBackground,
    surfaceBright = Color(0xFF33373E),
    surfaceContainerLowest = Color(0xFF0A0C0E),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF31353C),
    inverseSurface = DarkOnSurface,
    inverseOnSurface = Color(0xFF2E3136),
    inversePrimary = LightUiBlue,
    scrim = Color.Black,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightUiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = LightGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF041E49),
    tertiary = LightYellow,
    onTertiary = Color(0xFF3B2F00),
    error = LightRed,
    onError = Color.White,
    errorContainer = LightErrorBg,
    onErrorContainer = Color(0xFF410E0B),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightUiBlue,
    outline = Color(0xFF80868B),
    outlineVariant = LightOutline,
    surfaceDim = Color(0xFFDDE3EA),
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFE1E7F0),
    inverseSurface = Color(0xFF2E3136),
    inverseOnSurface = Color(0xFFF1F3F4),
    inversePrimary = DarkUiBlue,
    scrim = Color.Black,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Brand colors by default; users can opt into Material You dynamic color in Settings.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val base = if (darkTheme) DarkGoogleColors else LightGoogleColors
  // With dynamic color the neutral and primary roles follow the wallpaper palette; the
  // semantic status colors (green/yellow/red) stay fixed so their meaning is preserved.
  val googleColors =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      base.copy(
        uiBlue = colorScheme.primary,
        white = colorScheme.onPrimary,
        darkGray = colorScheme.onSurface,
        mediumGray = colorScheme.onSurfaceVariant,
        borderGray = colorScheme.outlineVariant,
        lightGray = colorScheme.surfaceContainerHigh,
        infoBg = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.45f else 0.6f),
      )
    } else {
      base
    }

  CompositionLocalProvider(LocalGoogleColors provides googleColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
