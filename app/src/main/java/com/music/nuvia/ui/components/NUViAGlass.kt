/*
 * Liquid Glass Foundation for NUViA
 *
 * Combines hardware-accelerated backdrop rendering (Kyant0/backdrop Apache-2.0,
 * vendored in [com.music.nuvia.ui.components.backdrop]) with AGSL runtime shaders
 * (lens refraction, dispersion, specular highlights, and inner shadows),
 * high-performance 0.33x resolution scaling, and backward-compatible Haze
 * fallback for API levels below 31.
 */
package com.music.nuvia.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.backdrop.Backdrop
import com.music.nuvia.ui.components.backdrop.backdrops.rememberHazeBackdrop
import com.music.nuvia.ui.components.backdrop.drawBackdrop
import com.music.nuvia.ui.components.backdrop.effects.blur
import com.music.nuvia.ui.components.backdrop.effects.colorControls
import com.music.nuvia.ui.components.backdrop.effects.lens
import com.music.nuvia.ui.components.backdrop.highlight.Highlight
import com.music.nuvia.ui.components.backdrop.highlight.HighlightStyle
import com.music.nuvia.ui.components.backdrop.isRenderEffectSupported
import com.music.nuvia.ui.components.backdrop.isRuntimeShaderSupported
import com.music.nuvia.ui.components.backdrop.shadow.InnerShadow
import com.music.nuvia.ui.components.backdrop.shadow.Shadow
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.concurrent.ConcurrentHashMap

// ============================================================================
// COMPOSITION LOCALS & CAPABILITY CHECKS
// ============================================================================

/** Whether liquid glass rendering is enabled globally. */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/** Whether liquid glass backdrop blur is enabled globally. Defaults to false (OFF). */
val LocalLiquidGlassBlurEnabled = staticCompositionLocalOf { false }

/** The app backdrop content that liquid glass surfaces sample from. */
val LocalAppBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Returns whether hardware-accelerated liquid glass shaders are supported on this device.
 * Requires [android.graphics.RenderEffect] (Android 12+, API 31+).
 */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

// ============================================================================
// PERFORMANCE & DESIGN CONSTANTS
// ============================================================================

/**
 * Resolution fraction the glass surface records and processes its backdrop at.
 * Set to 1f to maintain 1:1 pixel alignment and prevent translation/scaling desync in HazeBackdrop.
 */
const val GLASS_RESOLUTION_SCALE = 1f

/** Standard hairline border for glass surfaces and solid fallbacks. */
val GLASS_EDGE_WIDTH: Dp = 0.5.dp
val GLASS_EDGE_COLOR: Color = Color.White.copy(alpha = 0.12f)

// ============================================================================
// NUVIA MATERIAL TIER HIERARCHY
// ============================================================================

/**
 * Standardized NUViA Material Tiers:
 * Establishes consistent elevation, optical depth, surface translucency, and edge lighting.
 */
enum class NUViAGlassTier {
    /** Low elevation: chips, inline pills, tags, unselected controls. */
    Subtle,

    /** Medium elevation: cards, list items, floating buttons, mini player. */
    Elevated,

    /** High elevation: modal bottom sheets, alert dialogs, menus, floating panels. */
    Prominent,

    /** Full environment: player pane, floating navigation bar, ambient takeovers. */
    Immersive,
}

// ============================================================================
// STANDARDIZED GEOMETRY & SHAPE TOKENS
// ============================================================================

/**
 * Standardized corner radii and geometry tokens across the NUViA Liquid Glass design system.
 */
object NUViAGlassShapes {
    /** Micro elements: metadata tags, format badges (6dp). */
    val Badge: CornerBasedShape = RoundedCornerShape(6.dp)

    /** Small controls: compact chips, inline badges (8dp). */
    val Tag: CornerBasedShape = RoundedCornerShape(8.dp)

    /** Medium controls: standard buttons, action inputs, dropdowns (12dp). */
    val Control: CornerBasedShape = RoundedCornerShape(12.dp)

    /** Standard cards: content cards, media tiles, grid items (20dp). */
    val Card: CornerBasedShape = RoundedCornerShape(20.dp)

    /** Floating dialogs: alert dialogs, centered confirmation modals (24dp). */
    val Dialog: CornerBasedShape = RoundedCornerShape(24.dp)

    /** Bottom sheets: top-rounded modal sheets (28dp top corners). */
    val Sheet: CornerBasedShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    /** Fully circular / pill shapes: floating tab pills, icon buttons, toggle pills. */
    val Pill: CornerBasedShape = CircleShape
}

/**
 * High-contrast text/icon color for content sitting directly on a glass surface.
 */
@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

/**
 * Selected-tab indicator color for a glass surface.
 */
@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.White else Color.Black

// ============================================================================
// CORE LIQUID GLASS MODIFIERS
// ============================================================================

/**
 * Primary foundation modifier for all Liquid Glass in NUViA.
 *
 * Implements:
 * 1. Hardware AGSL lens refraction and chromatic aberration on Android 12+ (API 31+).
 * 2. Resolution-optimized Haze backdrop sampling fallback on API 26-30.
 * 3. High-contrast OLED solid fallback when Minimal UI is enabled or Liquid Glass is toggled off.
 * 4. Specular edge lighting, inner shadow depth, and ambient drop shadows per tier.
 * 5. Respects global Liquid Glass and Liquid Glass Blur settings.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
fun Modifier.nuviaGlass(
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
    shape: Shape = NUViAGlassShapes.Card,
    tintColor: Color? = null,
    borderWidth: Dp = GLASS_EDGE_WIDTH,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
): Modifier = composed {
    val nuviaColors = LocalNUViAColors.current
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val glassEffects by AppSettings.glassEffects.collectAsStateWithLifecycle()
    val liquidGlassBlur by AppSettings.liquidGlassBlur.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val localGlassEnabled = LocalLiquidGlassEnabled.current
    val localBlurEnabled = LocalLiquidGlassBlurEnabled.current

    val effectiveGlass = !minimalUi && glassEffects && localGlassEnabled && !reduceDynamicBlur
    val effectiveBlur = effectiveGlass && liquidGlassBlur && localBlurEnabled

    val hazeBackdrop = if (hazeState != null) rememberHazeBackdrop(hazeState) else null
    val effectiveBackdrop = backdrop ?: LocalAppBackdrop.current ?: hazeBackdrop

    val isDark = nuviaColors.isDark
    val density = LocalDensity.current

    // Solid High-Contrast Fallback (Liquid Glass Off, Minimal UI, or Reduced Blur)
    if (!effectiveGlass) {
        val solidSurface = tintColor ?: when (tier) {
            NUViAGlassTier.Subtle -> nuviaColors.surfaceCard
            NUViAGlassTier.Elevated -> nuviaColors.glassSurfaceElevated
            NUViAGlassTier.Prominent -> nuviaColors.surfaceCard
            NUViAGlassTier.Immersive -> nuviaColors.surfaceOled
        }
        return@composed this
            .clip(shape)
            .background(solidSurface, shape)
            .border(borderWidth, GLASS_EDGE_COLOR, shape)
    }

    // Determine Tier parameters
    val blurRadiusDp: Float
    val surfaceOpacity: Float
    val lensHeightRatio: Float
    val lensAmountRatio: Float
    val hasDispersion: Boolean
    val highlightAlpha: Float
    val innerShadowRadiusDp: Float
    val innerShadowAlpha: Float
    val shadowRadiusDp: Float
    val shadowAlpha: Float

    when (tier) {
        NUViAGlassTier.Subtle -> {
            blurRadiusDp = 8f
            surfaceOpacity = if (isDark) 0.28f else 0.50f
            lensHeightRatio = 0.15f
            lensAmountRatio = 0.15f
            hasDispersion = false
            highlightAlpha = 0.12f
            innerShadowRadiusDp = 0f
            innerShadowAlpha = 0f
            shadowRadiusDp = 0f
            shadowAlpha = 0f
        }
        NUViAGlassTier.Elevated -> {
            blurRadiusDp = 10f
            surfaceOpacity = if (isDark) 0.35f else 0.58f
            lensHeightRatio = 0.45f
            lensAmountRatio = 0.45f
            hasDispersion = true
            highlightAlpha = 0.24f
            innerShadowRadiusDp = 8f
            innerShadowAlpha = 0.14f
            shadowRadiusDp = 16f
            shadowAlpha = 0.18f
        }
        NUViAGlassTier.Prominent -> {
            blurRadiusDp = 24f
            surfaceOpacity = if (isDark) 0.65f else 0.88f
            lensHeightRatio = 0.50f
            lensAmountRatio = 0.50f
            hasDispersion = true
            highlightAlpha = 0.28f
            innerShadowRadiusDp = 20f
            innerShadowAlpha = 0.15f
            shadowRadiusDp = 24f
            shadowAlpha = 0.22f
        }
        NUViAGlassTier.Immersive -> {
            blurRadiusDp = 28f
            surfaceOpacity = if (isDark) 0.38f else 0.65f
            lensHeightRatio = 0.50f
            lensAmountRatio = 0.50f
            hasDispersion = true
            highlightAlpha = 0.30f
            innerShadowRadiusDp = 24f
            innerShadowAlpha = 0.18f
            shadowRadiusDp = 32f
            shadowAlpha = 0.25f
        }
    }

    val baseSurface = tintColor ?: if (isDark) {
        Color(0xFF0C0D12)
    } else {
        Color(0xFFFAFAFA)
    }

    // Path A: Hardware Backdrop + AGSL Shader Path (API 31+ with Backdrop)
    if (isGlassSupported() && effectiveBackdrop != null && effectiveGlass && shape is CornerBasedShape) {
        val blurPx = if (effectiveBlur) {
            with(density) { blurRadiusDp.dp.toPx() } * GLASS_RESOLUTION_SCALE
        } else 0f
        val maxLensDp = 48f
        val lensHeightPx = with(density) { (lensHeightRatio * maxLensDp).dp.toPx() } * GLASS_RESOLUTION_SCALE
        val lensAmountPx = with(density) { (lensAmountRatio * maxLensDp).dp.toPx() } * GLASS_RESOLUTION_SCALE

        val highlight = remember(highlightAlpha, borderWidth) {
            Highlight(
                width = borderWidth,
                blurRadius = (borderWidth / 2f).coerceAtLeast(0.5.dp),
                alpha = highlightAlpha,
                style = HighlightStyle.Default,
            )
        }

        val shadow = remember(shadowRadiusDp, shadowAlpha) {
            if (shadowRadiusDp > 0f) {
                Shadow(
                    radius = shadowRadiusDp.dp,
                    offset = DpOffset(0.dp, (shadowRadiusDp / 3f).dp),
                    color = Color.Black.copy(alpha = shadowAlpha),
                )
            } else null
        }

        val innerShadow = remember(innerShadowRadiusDp, innerShadowAlpha) {
            if (innerShadowRadiusDp > 0f) {
                InnerShadow(
                    radius = innerShadowRadiusDp.dp,
                    offset = DpOffset(0.dp, 2.dp),
                    color = Color.Black.copy(alpha = innerShadowAlpha),
                )
            } else null
        }

        return@composed this.drawBackdrop(
            backdrop = effectiveBackdrop,
            shape = { shape },
            effects = {
                val saturation = if (tier == NUViAGlassTier.Immersive) 1.30f else 1.25f
                colorControls(saturation = saturation)
                if (blurPx > 0f) {
                    blur(blurPx)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && lensHeightPx > 0f) {
                    lens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = tier == NUViAGlassTier.Immersive || tier == NUViAGlassTier.Elevated || tier == NUViAGlassTier.Prominent,
                        chromaticAberration = hasDispersion,
                    )
                }
            },
            highlight = { highlight },
            shadow = { shadow },
            innerShadow = { innerShadow },
            onDrawSurface = {
                drawRect(color = baseSurface.copy(alpha = surfaceOpacity), size = size)
            },
            backdropScale = GLASS_RESOLUTION_SCALE,
        )
    }

    // Path B: Haze Fallback (API 26-30 or Backdrop absent)
    if (hazeState != null) {
        val baseHazeStyle = if (tier == NUViAGlassTier.Subtle) {
            HazeMaterials.thin(baseSurface.copy(alpha = surfaceOpacity))
        } else {
            HazeMaterials.regular(baseSurface.copy(alpha = surfaceOpacity))
        }
        val hazeStyle = if (effectiveBlur) baseHazeStyle else baseHazeStyle.copy(blurRadius = 0.dp)

        return@composed this
            .clip(shape)
            .optimizedHazeEffect(state = hazeState, style = hazeStyle)
            .nuviaSpecularBorder(
                shape = shape,
                width = borderWidth,
                topHighlight = nuviaColors.glassHighlight.copy(alpha = highlightAlpha.coerceIn(0.1f, 0.4f)),
                bottomBorder = nuviaColors.glassBorder,
            )
    }

    // Path C: Solid Base Fallback
    this
        .clip(shape)
        .background(baseSurface.copy(alpha = surfaceOpacity), shape)
        .nuviaSpecularBorder(
            shape = shape,
            width = borderWidth,
            topHighlight = nuviaColors.glassHighlight,
            bottomBorder = nuviaColors.glassBorder,
        )
}

/**
 * Backward-compatible Liquid Glass modifier signature used across existing NUViA screens.
 * Delegates seamlessly to the unified [nuviaGlass] foundation.
 */
fun Modifier.nuviaLiquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = NUViAGlassShapes.Card,
    borderWidth: Dp = 0.75.dp,
    tintColor: Color? = null,
    elevated: Boolean = false,
    tier: NUViAGlassTier = if (elevated) NUViAGlassTier.Elevated else NUViAGlassTier.Subtle,
    backdrop: Backdrop? = null,
): Modifier = nuviaGlass(
    tier = tier,
    shape = shape,
    tintColor = tintColor,
    borderWidth = borderWidth,
    hazeState = hazeState,
    backdrop = backdrop,
)

// ============================================================================
// GLOBAL LIQUID GLASS COMPOSABLE SURFACES
// ============================================================================

/**
 * Standard Composable container for NUViA Liquid Glass.
 *
 * Wraps content in the unified [nuviaGlass] modifier with full support for:
 * - AGSL hardware lens refraction and dispersion (API 31+)
 * - Haze fallback blur (API 26-30)
 * - Solid high-contrast surface fallback (Minimal UI / Reduce Blur)
 * - Standardized [NUViAGlassTier] elevation and optical tokens
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
    shape: Shape = NUViAGlassShapes.Card,
    tintColor: Color? = null,
    borderWidth: Dp = GLASS_EDGE_WIDTH,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.nuviaGlass(
            tier = tier,
            shape = shape,
            tintColor = tintColor,
            borderWidth = borderWidth,
            hazeState = hazeState,
            backdrop = backdrop,
        ),
        content = content,
    )
}

/**
 * Standardized Liquid Glass container for bottom sheets.
 * Preconfigured with [NUViAGlassTier.Prominent] elevation, [NUViAGlassShapes.Sheet] top radii,
 * and high-contrast glass specular edge.
 */
@Composable
fun LiquidGlassSheetSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tintColor: Color? = null,
    borderWidth: Dp = GLASS_EDGE_WIDTH,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier,
        tier = NUViAGlassTier.Prominent,
        shape = NUViAGlassShapes.Sheet,
        tintColor = tintColor,
        borderWidth = borderWidth,
        hazeState = hazeState,
        backdrop = backdrop,
        content = content,
    )
}

/**
 * Standardized Liquid Glass container for centered modal dialogs.
 * Preconfigured with [NUViAGlassTier.Prominent] elevation, [NUViAGlassShapes.Dialog] radii,
 * and high-contrast glass specular edge.
 */
@Composable
fun LiquidGlassDialogSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tintColor: Color? = null,
    borderWidth: Dp = GLASS_EDGE_WIDTH,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier,
        tier = NUViAGlassTier.Prominent,
        shape = NUViAGlassShapes.Dialog,
        tintColor = tintColor,
        borderWidth = borderWidth,
        hazeState = hazeState,
        backdrop = backdrop,
        content = content,
    )
}

// ============================================================================
// SPECULAR BORDER & RIM LIGHTING
// ============================================================================

private val SpecularBrushCache = ConcurrentHashMap<Long, Brush>()

private fun getSpecularBrush(top: Color, bottom: Color): Brush {
    val key = (top.value.toLong() shl 32) xor (bottom.value.toLong() and 0xFFFFFFFFL)
    return SpecularBrushCache.computeIfAbsent(key) {
        Brush.verticalGradient(
            0.0f to top,
            0.4f to top.copy(alpha = top.alpha * 0.4f),
            1.0f to bottom,
        )
    }
}

/**
 * Draws a physical glass specular border that catches ambient light at the top edge
 * and smoothly falls off toward the bottom edge.
 */
fun Modifier.nuviaSpecularBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    width: Dp = 0.75.dp,
    topHighlight: Color? = null,
    bottomBorder: Color? = null,
): Modifier {
    if (topHighlight != null && bottomBorder != null) {
        return this.border(
            width = width,
            brush = getSpecularBrush(topHighlight, bottomBorder),
            shape = shape,
        )
    }
    return composed {
        val colors = LocalNUViAColors.current
        val top = topHighlight ?: colors.glassHighlight
        val bottom = bottomBorder ?: colors.glassBorder

        val specularBrush = remember(top, bottom) {
            getSpecularBrush(top, bottom)
        }

        this.border(
            width = width,
            brush = specularBrush,
            shape = shape,
        )
    }
}

// ============================================================================
// SPECIALIZED CONTAINERS & ATMOSPHERIC LIGHTING
// ============================================================================

/**
 * Liquid-glass pill container modifier (for floating tabs, search inputs, active chips).
 */
fun Modifier.nuviaGlassPill(
    hazeState: HazeState? = null,
    tintColor: Color? = null,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
    backdrop: Backdrop? = null,
): Modifier = nuviaGlass(
    tier = tier,
    shape = CircleShape,
    borderWidth = GLASS_EDGE_WIDTH,
    tintColor = tintColor,
    hazeState = hazeState,
    backdrop = backdrop,
)

/**
 * Renders a soft atmospheric glow behind a component using the current dynamic ambient color.
 */
fun Modifier.nuviaAmbientBacklight(
    color: Color? = null,
    radiusDp: Dp = 48.dp,
    alpha: Float = 0.35f,
): Modifier = composed {
    val colors = LocalNUViAColors.current
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val uiGlow by AppSettings.uiGlow.collectAsStateWithLifecycle()
    val glowEnabled = !minimalUi && uiGlow

    val baseGlow = color ?: colors.ambientGlow
    if (!glowEnabled || baseGlow == Color.Transparent || baseGlow.alpha <= 0.001f || alpha <= 0.001f) {
        return@composed Modifier
    }
    val glowColor = remember(baseGlow, alpha) {
        baseGlow.copy(alpha = (baseGlow.alpha * (alpha / 0.35f).coerceAtMost(1f)).coerceIn(0f, 1f))
    }
    val gradientColors = remember(glowColor) {
        listOf(glowColor, Color.Transparent)
    }

    val cache = remember {
        object {
            var lastRadius = -1f
            var lastCenter = Offset.Unspecified
            var lastColors: List<Color>? = null
            var brush: Brush? = null

            fun getBrush(colors: List<Color>, center: Offset, radius: Float): Brush {
                val b = brush
                if (b == null || lastRadius != radius || lastCenter != center || lastColors != colors) {
                    lastRadius = radius
                    lastCenter = center
                    lastColors = colors
                    val newBrush = Brush.radialGradient(colors = colors, center = center, radius = radius)
                    brush = newBrush
                    return newBrush
                }
                return b
            }
        }
    }

    this.drawBehind {
        val radius = size.maxDimension * 0.8f + radiusDp.toPx()
        drawCircle(
            brush = cache.getBrush(gradientColors, center, radius),
            radius = radius,
            center = center,
        )
    }
}

/**
 * Tactile spring scale interaction for premium glass buttons.
 * Slightly depresses on touch down and smoothly springs back on release.
 * Respects [AppSettings.reduceAnimation] for accessibility.
 */
fun Modifier.nuviaTactilePress(
    pressedScale: Float = 0.94f,
    haptic: Haptic? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val haptics = rememberHaptics()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val localInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    var pointerPressed by remember { mutableStateOf(false) }
    val isInteractionPressed by localInteractionSource.collectIsPressedAsState()
    val isPressed = isInteractionPressed || pointerPressed

    val targetScale = if (reduceAnimation || !isPressed) 1f else pressedScale

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 500f,
        ),
        label = "tactileScale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = localInteractionSource,
                    indication = null,
                    onClick = {
                        if (haptic != null) {
                            haptics.play(haptic)
                        }
                        onClick()
                    }
                )
            } else if (interactionSource == null) {
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pointerPressed = true
                        waitForUpOrCancellation()
                        pointerPressed = false
                    }
                }
            } else {
                Modifier
            }
        )
}
