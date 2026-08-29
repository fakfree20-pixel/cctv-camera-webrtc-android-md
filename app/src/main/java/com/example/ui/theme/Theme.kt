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
    primary = CctvIceBlue,
    onPrimary = CctvNavyDark,
    primaryContainer = CctvNavyPrimary,
    onPrimaryContainer = CctvIceBlue,
    secondary = CctvNavyHover,
    onSecondary = Color.White,
    secondaryContainer = CctvCardBgSecondary,
    onSecondaryContainer = CctvIceBlue,
    tertiary = CctvSuccessGreen,
    background = CctvDarkBg,
    onBackground = CctvTextPrimary,
    surface = CctvCardBg,
    onSurface = CctvTextPrimary,
    surfaceVariant = CctvCardBgSecondary,
    onSurfaceVariant = CctvTextSecondary,
    outline = CctvCardBorder,
    error = CctvAlertRed,
    onError = Color.White
  )

private val LightColorScheme = DarkColorScheme // Default to high-contrast dark CCTV styling

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else -> DarkColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

