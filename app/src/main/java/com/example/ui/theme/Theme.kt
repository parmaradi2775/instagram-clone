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

private val HighDensityLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),        // M3 Deep Purple Accent
    secondary = Color(0xFF625B71),      // Soft Secondary Gray/Purple
    tertiary = Color(0xFFB3261E),       // Engagement Red Accent
    background = Color(0xFFFEF7FF),     // Elegant Soft Light Background
    surface = Color(0xFFFEF7FF),        // Surface Background color
    surfaceVariant = Color(0xFFF3EDF7), // Bottom Nav and secondary containers
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1D1B20),   // High density dark typography
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val HighDensityDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFF2B8B5),
    background = Color(0xFF141218),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onTertiary = Color(0xFF601410),
    onBackground = Color(0xFFE6E1E4),
    onSurface = Color(0xFFE6E1E4),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disordered dynamic color by default for faithful theme accuracy
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> HighDensityDarkColorScheme
      else -> HighDensityLightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

