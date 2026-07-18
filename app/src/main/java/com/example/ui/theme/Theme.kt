package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.data.SchoolEntity

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.trim().replace("#", "")
        if (cleaned.isEmpty()) return fallback
        if (cleaned.length == 6) {
            Color(android.graphics.Color.parseColor("#FF$cleaned"))
        } else {
            Color(android.graphics.Color.parseColor("#$cleaned"))
        }
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun MyApplicationTheme(
  school: SchoolEntity? = null,
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  // Determine if dark theme is active based on school setting or system default
  val isDark = when (school?.themeMode) {
      "LIGHT" -> false
      "DARK" -> true
      else -> darkTheme
  }

  val colorScheme = if (school != null) {
      val prim = parseHexColor(school.primaryColorHex, if (isDark) Purple80 else Purple40)
      val sec = parseHexColor(school.secondaryColorHex, if (isDark) PurpleGrey80 else PurpleGrey40)
      val tert = parseHexColor(school.accentColorHex, if (isDark) Pink80 else Pink40)
      
      if (isDark) {
          darkColorScheme(
              primary = prim,
              secondary = sec,
              tertiary = tert,
              background = Color(0xFF121212),
              surface = Color(0xFF1E1E1E),
              onPrimary = Color.Black,
              onSecondary = Color.Black,
              onBackground = Color(0xFFE2E2E2),
              onSurface = Color(0xFFE2E2E2),
              primaryContainer = prim.copy(alpha = 0.2f),
              secondaryContainer = sec.copy(alpha = 0.2f)
          )
      } else {
          lightColorScheme(
              primary = prim,
              secondary = sec,
              tertiary = tert,
              background = Color(0xFFF9F9FF),
              surface = Color.White,
              onPrimary = Color.White,
              onSecondary = Color.White,
              onBackground = Color(0xFF1A1A1C),
              onSurface = Color(0xFF1A1A1C),
              primaryContainer = prim.copy(alpha = 0.12f),
              secondaryContainer = sec.copy(alpha = 0.12f)
          )
      }
  } else {
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
      }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
