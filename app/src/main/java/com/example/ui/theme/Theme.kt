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
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GoogleBlue,
    onPrimary = DarkBackground,
    primaryContainer = GoogleUIBlue,
    onPrimaryContainer = White,
    secondary = GoogleGreen,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = GoogleYellow,
    error = GoogleRed,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GoogleUIBlue,
    onPrimary = White,
    primaryContainer = InfoBackground,
    onPrimaryContainer = GoogleUIBlue,
    secondary = GoogleGreen,
    onSecondary = White,
    secondaryContainer = SuccessBackground,
    onSecondaryContainer = GoogleGreen,
    tertiary = GoogleYellow,
    error = GoogleRed,
    background = LightGray,
    onBackground = DarkGray,
    surface = White,
    onSurface = DarkGray,
    surfaceVariant = InfoBackground,
    onSurfaceVariant = MediumGray,
    outline = BorderGray,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // We prefer our explicit Google Brand colors over arbitrary OEM dynamic colors to match prompt guidelines
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

  val googleColors = GoogleThemeColors(
    infoBg = if (darkTheme) DarkSurfaceVariant else InfoBackground,
    successBg = if (darkTheme) DarkSurfaceVariant else SuccessBackground,
    warningBg = if (darkTheme) DarkSurfaceVariant else WarningBackground,
    errorBg = if (darkTheme) DarkSurfaceVariant else ErrorBackground,
  )

  CompositionLocalProvider(LocalGoogleColors provides googleColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

