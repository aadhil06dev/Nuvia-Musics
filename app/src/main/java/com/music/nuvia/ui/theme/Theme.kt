package com.music.nuvia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.nuvia.R

/**
 * NUViA Brand Primary Accent.
 * Dynamic accents from artwork override this during playback.
 */
val NUViAAccent = NUViABrand.Primary
@Deprecated("Use NUViAColors.primary instead", ReplaceWith("LocalNUViAColors.current.primary"))
val AccentGreen = NUViABrand.Primary

/** NUViA Typography: Sora for geometric display and Manrope for crystal-clear readable copy. */
val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.W400),
    Font(R.font.sora_semibold, FontWeight.W600),
    Font(R.font.sora_bold, FontWeight.W700),
    Font(R.font.sora_extrabold, FontWeight.W800),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.W400),
    Font(R.font.manrope_medium, FontWeight.W500),
    Font(R.font.manrope_semibold, FontWeight.W600),
    Font(R.font.manrope_bold, FontWeight.W700),
)

val NUViATypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.W800,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.W800,
        fontSize = 28.sp,
        letterSpacing = (-0.6).sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    ),
)

/**
 * NUViA Liquid-Glass Theme.
 * Provides the OLED-black design foundation and dynamic artwork color palette to all child composables.
 */
@Composable
fun NUViATheme(
    colors: NUViAColors? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val nuviaColors = colors ?: if (darkTheme) defaultNUViADarkColors else defaultNUViALightColors
    val materialScheme = nuviaColors.toMaterialColorScheme()

    CompositionLocalProvider(
        LocalNUViAColors provides nuviaColors,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = NUViATypography,
            content = content,
        )
    }
}




/**
 * Draws the status and navigation bar glyphs dark or light.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}

