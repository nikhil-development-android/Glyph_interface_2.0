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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF3B30),
    secondary = Color(0xFF888888),
    tertiary = Color(0xFF00FF41),
    background = Color(0xFF000000),
    surface = Color(0xFF151515)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF3B30),
    secondary = Color(0xFF6C6C70),
    tertiary = Color(0xFF00FF41),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun GlyphInterfaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
