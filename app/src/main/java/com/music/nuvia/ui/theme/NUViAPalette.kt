package com.music.nuvia.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.nuvia.data.model.CARD_ART_PX
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Permanent NUViA Brand Palette:
 * Used for app launch, splash identity, and fallback states when no artwork is present.
 * High-craft deep orange and warm metallic tones with pure AMOLED black foundation (#000000).
 */
object NUViABrand {
    val DeepOrange = Color(0xFFB83D00)        // Deep Rich Orange / Atmospheric Glow
    val PrimaryOrange = Color(0xFFE85D04)     // Primary Brand Orange
    val HighlightOrange = Color(0xFFFF7A1A)   // Vibrant Accent / Highlight
    val Primary = PrimaryOrange               // Primary Brand Accent
    val Accent = HighlightOrange              // Radiant Highlight
    val DarkBase = Color(0xFF000000)          // Pure AMOLED Black Foundation
    val SurfaceDark = Color(0xFF0C0D10)       // Deep Glass Surface
    val SurfaceElevated = Color(0xFF16171D)   // Elevated Glass Surface
    val TextPrimary = Color(0xFFFFFFFF)       // Pure / Warm White Text
    val TextSecondary = Color(0xFF94A3B8)     // Soft Neutral Slate Gray
    val TextMuted = Color(0xFF64748B)         // Muted Metadata
    val GlassBorder = Color(0x24FFFFFF)
    val GlassHighlight = Color(0x38FFFFFF)

    val BrandGradient = Brush.linearGradient(
        colors = listOf(PrimaryOrange, HighlightOrange)
    )

    val LaunchGradient = Brush.radialGradient(
        colors = listOf(
            DeepOrange.copy(alpha = 0.22f),
            PrimaryOrange.copy(alpha = 0.08f),
            DarkBase.copy(alpha = 0.0f)
        )
    )
}

/**
 * The complete NUViA Liquid-Glass Design System color tokens.
 * Adapts dynamically to the currently playing song's artwork while maintaining
 * strict OLED-black foundation and WCAG AA/AAA legibility standards.
 */
@Immutable
data class NUViAColors(
    /** Primary vibrant accent for interactive elements, play buttons, active tabs */
    val primary: Color,
    /** Secondary complementary tone for active chips, progress tails */
    val secondary: Color,
    /** Deep harmonic tone for subtle accents and tag fills */
    val tertiary: Color,
    /** Atmospheric ambient light color for top/corner glow effects */
    val ambientGlow: Color,
    /** Specular rim highlight color for glass edges */
    val glassHighlight: Color,
    /** Specular subtle hairline border color */
    val glassBorder: Color,
    /** Translucent liquid-glass container background */
    val glassSurface: Color,
    /** Elevated translucent liquid-glass container background */
    val glassSurfaceElevated: Color,
    /** Pure OLED background canvas (#000000 / #040407) */
    val surfaceOled: Color,
    /** Dark surface for non-blurred fallback or secondary cards */
    val surfaceCard: Color,
    /** Crisp, high-contrast text on glass and OLED surfaces */
    val textPrimary: Color,
    /** Softened secondary text with guaranteed contrast against dark backgrounds */
    val textSecondary: Color,
    /** Muted metadata text (durations, track counts, subtle labels) */
    val textMuted: Color,
    /** Contrasting icon/text color drawn on top of [primary] */
    val onPrimary: Color,
    /** Hairline divider color for separators and borders */
    val divider: Color,
    /** Whether this palette is currently in dark (OLED) mode */
    val isDark: Boolean,
)

val LocalNUViAColors: ProvidableCompositionLocal<NUViAColors> = compositionLocalOf {
    defaultNUViADarkColors
}

val defaultNUViADarkColors = NUViAColors(
    primary = NUViABrand.Primary,
    secondary = NUViABrand.Accent,
    tertiary = NUViABrand.DeepOrange,
    ambientGlow = Color.Transparent,
    glassHighlight = Color(0x20FFFFFF),
    glassBorder = Color(0x16FFFFFF),
    glassSurface = Color(0x99101116),
    glassSurfaceElevated = Color(0xCC181A22),
    surfaceOled = NUViABrand.DarkBase,
    surfaceCard = NUViABrand.SurfaceDark,
    textPrimary = NUViABrand.TextPrimary,
    textSecondary = NUViABrand.TextSecondary,
    textMuted = NUViABrand.TextMuted,
    onPrimary = Color(0xFF000000),
    divider = Color(0x14FFFFFF),
    isDark = true,
)

val defaultNUViALightColors = NUViAColors(
    primary = NUViABrand.PrimaryOrange,
    secondary = NUViABrand.HighlightOrange,
    tertiary = NUViABrand.DeepOrange,
    ambientGlow = Color.Transparent,
    glassHighlight = Color(0x60FFFFFF),
    glassBorder = Color(0x18000000),
    glassSurface = Color(0xEBFFFFFF),
    glassSurfaceElevated = Color(0xF8F8F9FA),
    surfaceOled = Color(0xFFF8F9FA),
    surfaceCard = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    onPrimary = Color.White,
    divider = Color(0x14000000),
    isDark = false,
)

/**
 * True Minimalist Dark Palette (AMOLED pure black, neutral grays, high-contrast white accents, NO orange).
 */
val minimalNUViADarkColors = NUViAColors(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFF94A3B8),
    tertiary = Color(0xFF64748B),
    ambientGlow = Color.Transparent,
    glassHighlight = Color(0x18FFFFFF),
    glassBorder = Color(0x1AFFFFFF),
    glassSurface = Color(0xFF141519),
    glassSurfaceElevated = Color(0xFF1B1C22),
    surfaceOled = Color(0xFF000000),
    surfaceCard = Color(0xFF111215),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    onPrimary = Color(0xFF000000),
    divider = Color(0x1AFFFFFF),
    isDark = true,
)

/**
 * True Minimalist Light Palette (Crisp off-white canvas, clean slate accents, pure neutral contrast, NO orange).
 */
val minimalNUViALightColors = NUViAColors(
    primary = Color(0xFF0F172A),
    secondary = Color(0xFF475569),
    tertiary = Color(0xFF64748B),
    ambientGlow = Color.Transparent,
    glassHighlight = Color.Transparent,
    glassBorder = Color(0x14000000),
    glassSurface = Color(0xFFFFFFFF),
    glassSurfaceElevated = Color(0xFFF1F5F9),
    surfaceOled = Color(0xFFF8F9FA),
    surfaceCard = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    onPrimary = Color(0xFFFFFFFF),
    divider = Color(0x14000000),
    isDark = false,
)

/**
 * Extracts and animates a dynamic [NUViAColors] palette from the provided artwork URL.
 * Automatically respects OLED black standards, ensures WCAG contrast, and performs
 * ultra-smooth interpolation between tracks.
 */
@Composable
fun rememberNUViADynamicPalette(
    imageUrl: String?,
    dark: Boolean = true,
    artPx: Int = CARD_ART_PX,
): NUViAColors {
    val context = LocalContext.current
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val dynamicArtworkColors by AppSettings.dynamicArtworkColors.collectAsStateWithLifecycle()
    val ambientLighting by AppSettings.ambientLighting.collectAsStateWithLifecycle()

    val baseDefault = if (dark) defaultNUViADarkColors else defaultNUViALightColors
    val baseMinimal = if (dark) minimalNUViADarkColors else minimalNUViALightColors

    val shouldExtract = !minimalUi && dynamicArtworkColors

    var seed by remember(imageUrl) {
        mutableStateOf(ArtPaletteCache.get(imageUrl))
    }
    val knownUpFront = remember(imageUrl) { seed != null }

    LaunchedEffect(imageUrl, artPx, shouldExtract) {
        if (!shouldExtract || imageUrl == null) return@LaunchedEffect
        val cached = ArtPaletteCache.get(imageUrl)
        if (cached != null) {
            seed = cached
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl.artworkAt(artPx))
            .size(128)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return@LaunchedEffect

        val extracted = withContext(Dispatchers.Default) { extractArtPaletteSeed(bitmap) } ?: return@LaunchedEffect
        ArtPaletteCache.put(imageUrl, extracted)
        seed = extracted
    }

    val customThemeColorInt by AppSettings.customThemeColor.collectAsStateWithLifecycle()
    val customColors = remember(customThemeColorInt, dark) {
        buildCustomNUViAColors(Color(customThemeColorInt), dark)
    }

    val targetColors = remember(seed, dark, minimalUi, dynamicArtworkColors, ambientLighting, customColors) {
        if (minimalUi) {
            baseMinimal
        } else if (!dynamicArtworkColors) {
            if (!ambientLighting) customColors.copy(ambientGlow = Color.Transparent) else customColors
        } else {
            val dynamic = seed?.let { buildNUViAColors(it, dark) } ?: customColors
            if (!ambientLighting) dynamic.copy(ambientGlow = Color.Transparent) else dynamic
        }
    }

    val animSpec: AnimationSpec<Color> = if (reduceAnimation || knownUpFront) {
        snap()
    } else {
        tween(durationMillis = 650, easing = FastOutSlowInEasing)
    }

    // Only animate the dynamic artwork-derived tones; theme constants are bound directly to save per-frame allocations
    val animPrimary by animateColorAsState(targetColors.primary, animSpec, label = "nuvia_primary")
    val animSecondary by animateColorAsState(targetColors.secondary, animSpec, label = "nuvia_secondary")
    val animTertiary by animateColorAsState(targetColors.tertiary, animSpec, label = "nuvia_tertiary")
    val animAmbientGlow by animateColorAsState(targetColors.ambientGlow, animSpec, label = "nuvia_glow")
    val animGlassSurface by animateColorAsState(targetColors.glassSurface, animSpec, label = "nuvia_gsurface")
    val animGlassSurfaceElevated by animateColorAsState(targetColors.glassSurfaceElevated, animSpec, label = "nuvia_gsurface_elev")
    val animSurfaceCard by animateColorAsState(targetColors.surfaceCard, animSpec, label = "nuvia_scard")
    val animOnPrimary by animateColorAsState(targetColors.onPrimary, animSpec, label = "nuvia_onp")

    return NUViAColors(
        primary = animPrimary,
        secondary = animSecondary,
        tertiary = animTertiary,
        ambientGlow = animAmbientGlow,
        glassHighlight = targetColors.glassHighlight,
        glassBorder = targetColors.glassBorder,
        glassSurface = animGlassSurface,
        glassSurfaceElevated = animGlassSurfaceElevated,
        surfaceOled = targetColors.surfaceOled,
        surfaceCard = animSurfaceCard,
        textPrimary = targetColors.textPrimary,
        textSecondary = targetColors.textSecondary,
        textMuted = targetColors.textMuted,
        onPrimary = animOnPrimary,
        divider = targetColors.divider,
        isDark = dark,
    )
}

internal fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val l = (max + min) / 2f
    var h = 0f
    var s = 0f

    if (delta != 0f) {
        s = if (l > 0.5f) delta / (2f - max - min) else delta / (max + min)
        h = when (max) {
            r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
            g -> ((b - r) / delta + 2f) * 60f
            else -> ((r - g) / delta + 4f) * 60f
        }
    }
    return floatArrayOf(h, s, l)
}

internal fun hslToRgb(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (rPrime, gPrime, bPrime) = when (((h / 60f).toInt() % 6 + 6) % 6) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (rPrime + m).coerceIn(0f, 1f),
        green = (gPrime + m).coerceIn(0f, 1f),
        blue = (bPrime + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

internal fun calculateLuminance(r: Float, g: Float, b: Float): Float {
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/**
 * Derives a full [NUViAColors] palette from a single user-chosen custom accent color.
 */
fun buildCustomNUViAColors(customColor: Color, isDark: Boolean): NUViAColors {
    val hsl = rgbToHsl(customColor.red, customColor.green, customColor.blue)
    val maxChannelDiff = maxOf(customColor.red, customColor.green, customColor.blue) - minOf(customColor.red, customColor.green, customColor.blue)
    val isNeutral = hsl[1] < 0.08f || maxChannelDiff < 0.08f

    return if (isDark) {
        val vibrantAccent = if (isNeutral) {
            customColor
        } else {
            customColor.adjustHsl(
                saturation = { it.coerceIn(0.30f, 1.0f) },
                lightness = { it.coerceIn(0.40f, 0.75f) },
            )
        }
        val secondaryAccent = if (isNeutral) {
            customColor.copy(alpha = 0.82f)
        } else {
            customColor.adjustHsl(
                saturation = { (it * 0.9f).coerceIn(0.25f, 0.85f) },
                lightness = { (it + 0.10f).coerceIn(0.50f, 0.80f) },
            )
        }
        val tertiaryAccent = if (isNeutral) {
            customColor.copy(alpha = 0.65f)
        } else {
            customColor.adjustHsl(
                saturation = { (it * 0.8f).coerceIn(0.20f, 0.70f) },
                lightness = { (it - 0.15f).coerceIn(0.30f, 0.60f) },
            )
        }
        val onPrimary = if (calculateLuminance(vibrantAccent.red, vibrantAccent.green, vibrantAccent.blue) > 0.45f) {
            Color(0xFF040407)
        } else {
            Color(0xFFFFFFFF)
        }
        defaultNUViADarkColors.copy(
            primary = vibrantAccent,
            secondary = secondaryAccent,
            tertiary = tertiaryAccent,
            ambientGlow = vibrantAccent.copy(alpha = 0.28f),
            onPrimary = onPrimary,
        )
    } else {
        val vibrantAccent = if (isNeutral) {
            customColor
        } else {
            customColor.adjustHsl(
                saturation = { it.coerceIn(0.30f, 1.0f) },
                lightness = { it.coerceIn(0.25f, 0.55f) },
            )
        }
        val secondaryAccent = if (isNeutral) {
            customColor.copy(alpha = 0.82f)
        } else {
            customColor.adjustHsl(
                saturation = { (it * 0.9f).coerceIn(0.25f, 0.85f) },
                lightness = { (it + 0.10f).coerceIn(0.35f, 0.60f) },
            )
        }
        val tertiaryAccent = if (isNeutral) {
            customColor.copy(alpha = 0.65f)
        } else {
            customColor.adjustHsl(
                saturation = { (it * 0.8f).coerceIn(0.20f, 0.75f) },
                lightness = { (it - 0.10f).coerceIn(0.25f, 0.45f) },
            )
        }
        val onPrimary = if (calculateLuminance(vibrantAccent.red, vibrantAccent.green, vibrantAccent.blue) > 0.45f) {
            Color(0xFF040407)
        } else {
            Color(0xFFFFFFFF)
        }
        defaultNUViALightColors.copy(
            primary = vibrantAccent,
            secondary = secondaryAccent,
            tertiary = tertiaryAccent,
            ambientGlow = vibrantAccent.copy(alpha = 0.28f),
            onPrimary = onPrimary,
        )
    }
}

/**
 * Transforms raw swatches into refined NUViA design tokens with strict legibility guarantees.
 */
private fun buildNUViAColors(seed: ArtPaletteSeed, isDark: Boolean): NUViAColors {
    return if (isDark) {
        // High-luminance accent for dark backgrounds: ensures high readability without blowing out
        val vibrantAccent = seed.vibrant.adjustHsl(
            saturation = { it.coerceIn(0.65f, 0.98f) },
            lightness = { it.coerceIn(0.52f, 0.72f) }
        )

        val secondaryAccent = seed.dominant.adjustHsl(
            saturation = { it.coerceIn(0.40f, 0.85f) },
            lightness = { it.coerceIn(0.44f, 0.62f) }
        )

        val tertiaryAccent = seed.muted.adjustHsl(
            saturation = { it.coerceIn(0.30f, 0.70f) },
            lightness = { it.coerceIn(0.35f, 0.52f) }
        )

        // Tint the glass surface slightly with the dominant hue while keeping OLED depth
        val glassSurface = seed.dominant.adjustHsl(
            saturation = { (it * 0.35f).coerceIn(0.08f, 0.30f) },
            lightness = { 0.07f }
        ).copy(alpha = 0.75f)

        val glassSurfaceElevated = seed.dominant.adjustHsl(
            saturation = { (it * 0.40f).coerceIn(0.10f, 0.35f) },
            lightness = { 0.11f }
        ).copy(alpha = 0.88f)

        val surfaceCard = seed.dominant.adjustHsl(
            saturation = { (it * 0.25f).coerceIn(0.04f, 0.20f) },
            lightness = { 0.05f }
        )

        val ambientGlow = vibrantAccent.copy(alpha = 0.28f)

        // Calculate contrast-safe onPrimary (dark text on bright primary, white on dark primary)
        val onPrimary = if (calculateLuminance(vibrantAccent.red, vibrantAccent.green, vibrantAccent.blue) > 0.45f) {
            Color(0xFF040407)
        } else {
            Color(0xFFFFFFFF)
        }

        NUViAColors(
            primary = vibrantAccent,
            secondary = secondaryAccent,
            tertiary = tertiaryAccent,
            ambientGlow = ambientGlow,
            glassHighlight = Color(0x33FFFFFF),
            glassBorder = Color(0x1FFFFFFF),
            glassSurface = glassSurface,
            glassSurfaceElevated = glassSurfaceElevated,
            surfaceOled = NUViABrand.DarkBase,
            surfaceCard = surfaceCard,
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB0B5C9),
            textMuted = Color(0xFF6B728D),
            onPrimary = onPrimary,
            divider = Color(0x1FFFFFFF),
            isDark = true,
        )
    } else {
        val vibrantAccent = seed.vibrant.adjustHsl(
            saturation = { it.coerceIn(0.65f, 1.0f) },
            lightness = { it.coerceIn(0.30f, 0.45f) }
        )

        val secondaryAccent = seed.dominant.adjustHsl(
            saturation = { it.coerceIn(0.45f, 0.85f) },
            lightness = { it.coerceIn(0.25f, 0.42f) }
        )

        val tertiaryAccent = seed.muted.adjustHsl(
            saturation = { it.coerceIn(0.35f, 0.75f) },
            lightness = { it.coerceIn(0.35f, 0.50f) }
        )

        NUViAColors(
            primary = vibrantAccent,
            secondary = secondaryAccent,
            tertiary = tertiaryAccent,
            ambientGlow = vibrantAccent.copy(alpha = 0.14f),
            glassHighlight = Color(0x80FFFFFF),
            glassBorder = Color(0x1F000000),
            glassSurface = Color(0xCCFFFFFF),
            glassSurfaceElevated = Color(0xF2F6F8FD),
            surfaceOled = Color(0xFFF4F6FB),
            surfaceCard = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF0A0C14),
            textSecondary = Color(0xFF4A5168),
            textMuted = Color(0xFF868FA6),
            onPrimary = Color.White,
            divider = Color(0x14000000),
            isDark = false,
        )
    }
}

private fun Color.adjustHsl(
    saturation: (Float) -> Float = { it },
    lightness: (Float) -> Float = { it },
): Color {
    val hsl = rgbToHsl(red, green, blue)
    val newS = saturation(hsl[1]).coerceIn(0f, 1f)
    val newL = lightness(hsl[2]).coerceIn(0f, 1f)
    return hslToRgb(hsl[0], newS, newL)
}

/**
 * Generates an Android Material 3 [ColorScheme] matching the active [NUViAColors].
 */
fun NUViAColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.20f),
            onPrimaryContainer = textPrimary,
            secondary = secondary,
            onSecondary = onPrimary,
            secondaryContainer = secondary.copy(alpha = 0.18f),
            onSecondaryContainer = textPrimary,
            tertiary = tertiary,
            onTertiary = onPrimary,
            background = surfaceOled,
            onBackground = textPrimary,
            surface = surfaceCard,
            onSurface = textPrimary,
            surfaceVariant = glassSurfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = glassBorder,
            outlineVariant = divider,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = textPrimary,
            secondary = secondary,
            onSecondary = onPrimary,
            secondaryContainer = secondary.copy(alpha = 0.12f),
            onSecondaryContainer = textPrimary,
            tertiary = tertiary,
            onTertiary = onPrimary,
            background = surfaceOled,
            onBackground = textPrimary,
            surface = surfaceCard,
            onSurface = textPrimary,
            surfaceVariant = glassSurfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = glassBorder,
            outlineVariant = divider,
        )
    }
}
