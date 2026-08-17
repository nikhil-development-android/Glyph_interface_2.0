package com.glyphinterface.ui.theme

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
import com.glyphinterface.AppSettings
import com.glyphinterface.ThemeMode

@Composable
fun GlyphInterfaceTheme(
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (AppSettings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val selectedAccent = AppSettings.accentColors.getOrElse(AppSettings.accentIndex) { AppSettings.accentColors[0] }
    val context = LocalContext.current

    val colorScheme = if (selectedAccent.isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val accent = selectedAccent.color
        if (isDark) {
            darkColorScheme(
                primary = accent,
                secondary = Color(0xFF888888),
                tertiary = Color(0xFF00FF41),
                background = Color(0xFF000000),
                surface = Color(0xFF151515),
                surfaceVariant = Color(0xFF1C1C1E),
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = accent,
                secondary = Color(0xFF6C6C70),
                tertiary = Color(0xFF008730),
                background = Color(0xFFF2F2F7),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE5E5EA),
                onPrimary = Color.White,
                onBackground = Color(0xFF1C1C1E),
                onSurface = Color(0xFF1C1C1E)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
