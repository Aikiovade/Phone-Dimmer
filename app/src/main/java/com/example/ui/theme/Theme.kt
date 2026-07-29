package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    background = AppBackground,
    onBackground = AppOnBackground,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline
  )

private val DeepBlackColorScheme =
  darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    background = Color(0xFF000000),
    onBackground = AppOnBackground,
    surface = Color(0xFF121212),
    onSurface = AppOnSurface,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline
  )

private val LightColorScheme = DarkColorScheme // Force dark theme layout for this design

@Composable
fun MyApplicationTheme(
  deepBlackThemeEnabled: Boolean = false,
  darkTheme: Boolean = true, // Always dark as per design
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce the theme
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      deepBlackThemeEnabled -> DeepBlackColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
