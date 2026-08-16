package com.glyphinterface.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.glyphinterface.R

// High Density Glyph Theme Colors mapped to res/values/colors.xml for easy MT Manager editing
val GlyphRed: Color
    @Composable get() = colorResource(id = R.color.glyph_red)

val GlyphDarkBg: Color
    @Composable get() = colorResource(id = R.color.glyph_bg_black)

val GlyphSurfaceDark: Color
    @Composable get() = colorResource(id = R.color.glyph_surface_dark)

val GlyphCardDark: Color
    @Composable get() = colorResource(id = R.color.glyph_card_dark)

val GlyphCardSubtle: Color
    @Composable get() = colorResource(id = R.color.glyph_card_subtle)

val GlyphCardBorderDark: Color
    @Composable get() = colorResource(id = R.color.glyph_card_border)

val GlyphCardBorderSubtle: Color
    @Composable get() = colorResource(id = R.color.glyph_card_border_subtle)

val GlyphTextPrimaryDark: Color
    @Composable get() = colorResource(id = R.color.glyph_text_primary)

val GlyphTextSecondaryDark: Color
    @Composable get() = colorResource(id = R.color.glyph_text_secondary)

val GlyphTextMuted: Color
    @Composable get() = colorResource(id = R.color.glyph_text_muted)

val GlyphStatusGreen: Color
    @Composable get() = colorResource(id = R.color.glyph_status_green)

val GlyphSliderBg: Color
    @Composable get() = colorResource(id = R.color.glyph_slider_bg)

val GlyphPhoneFrame: Color
    @Composable get() = colorResource(id = R.color.glyph_phone_frame)

val GlyphPhoneBorder: Color
    @Composable get() = colorResource(id = R.color.glyph_phone_border)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val GlyphLightBg = Color(0xFFF2F2F7)
val GlyphCardLight = Color(0xFFFFFFFF)
val GlyphCardBorderLight = Color(0x1F000000)
val GlyphTextPrimaryLight = Color(0xFF1C1C1E)
val GlyphTextSecondaryLight = Color(0xFF6C6C70)
