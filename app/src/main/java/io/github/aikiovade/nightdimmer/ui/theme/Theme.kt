package io.github.aikiovade.nightdimmer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    outline = AppOutline,
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
    outline = AppOutline,
  )

/**
 * The app is always dark: a bright UI would defeat the purpose of a dimmer that
 * is used in the dark.
 */
@Composable
fun MyApplicationTheme(
  deepBlackThemeEnabled: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (deepBlackThemeEnabled) DeepBlackColorScheme else DarkColorScheme,
    typography = Typography,
    content = content,
  )
}
