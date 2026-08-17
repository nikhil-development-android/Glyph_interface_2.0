package com.glyphinterface

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class AccentColorOption(
    val name: String,
    val color: Color,
    val isMonet: Boolean = false
)

data class WallpaperOption(
    val name: String,
    val description: String
)

object AppSettings {
    private const val PREFS_NAME = "glyph_app_settings"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_GLASS_EFFECT = "key_glass_effect"
    private const val KEY_ACCENT_INDEX = "key_accent_index"
    private const val KEY_WALLPAPER_INDEX = "key_wallpaper_index"

    private var prefs: SharedPreferences? = null

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var isGlassEffect by mutableStateOf(true)
    var accentIndex by mutableIntStateOf(0)
    var wallpaperIndex by mutableIntStateOf(1) // Default to Dot Matrix

    val accentColors = listOf(
        AccentColorOption("Glyph Red", Color(0xFFFF3B30)),
        AccentColorOption("Cyber Cyan", Color(0xFF00E5FF)),
        AccentColorOption("Matrix Green", Color(0xFF00FF41)),
        AccentColorOption("Electric Violet", Color(0xFFA855F7)),
        AccentColorOption("Sunset Amber", Color(0xFFFF9500)),
        AccentColorOption("Monochrome", Color(0xFFFFFFFF)),
        AccentColorOption("Dynamic Monet", Color(0xFF6750A4), isMonet = true)
    )

    val wallpapers = listOf(
        WallpaperOption("Pitch Black", "Pure AMOLED minimal dark"),
        WallpaperOption("Dot Matrix", "Classic Nothing OS grid"),
        WallpaperOption("Cyber Glow", "Radial accent ambient glow"),
        WallpaperOption("Frosted Mesh", "Smooth gradient dark mesh"),
        WallpaperOption("Modern Grid", "Subtle technical wireframe")
    )

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedModeOrdinal = prefs?.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal) ?: ThemeMode.SYSTEM.ordinal
            themeMode = ThemeMode.entries.getOrElse(savedModeOrdinal) { ThemeMode.SYSTEM }
            isGlassEffect = prefs?.getBoolean(KEY_GLASS_EFFECT, true) ?: true
            accentIndex = prefs?.getInt(KEY_ACCENT_INDEX, 0) ?: 0
            wallpaperIndex = prefs?.getInt(KEY_WALLPAPER_INDEX, 1) ?: 1
        }
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs?.edit()?.putInt(KEY_THEME_MODE, mode.ordinal)?.apply()
    }

    fun setGlass(enabled: Boolean) {
        isGlassEffect = enabled
        prefs?.edit()?.putBoolean(KEY_GLASS_EFFECT, enabled)?.apply()
    }

    fun setAccent(index: Int) {
        accentIndex = index.coerceIn(0, accentColors.lastIndex)
        prefs?.edit()?.putInt(KEY_ACCENT_INDEX, accentIndex)?.apply()
    }

    fun setWallpaper(index: Int) {
        wallpaperIndex = index.coerceIn(0, wallpapers.lastIndex)
        prefs?.edit()?.putInt(KEY_WALLPAPER_INDEX, wallpaperIndex)?.apply()
    }

    val currentAccentColor: Color
        get() = accentColors.getOrElse(accentIndex) { accentColors[0] }.color
}
