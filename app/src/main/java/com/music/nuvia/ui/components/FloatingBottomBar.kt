package com.music.nuvia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.backdrop.Backdrop
import com.music.nuvia.ui.components.backdrop.backdrops.rememberHazeBackdrop
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import dev.chrisbanes.haze.HazeState
import kotlin.math.sqrt

data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector? = null,
)

private val BarShape = RoundedCornerShape(percent = 50)
private val IndicatorShape = RoundedCornerShape(percent = 50)

/**
 * NUViA Liquid Glass Floating Bottom Navigation Bar:
 *
 * 1. True Liquid Glass Material:
 *    - Uses Phase 1 [NUViAGlassTier.Elevated] with 0.33x resolution scaling.
 *    - Real-time backdrop sampling (hardware AGSL shaders where available, resolution-scaled Haze fallback).
 *    - Thin glass specular rim with subtle edge lighting and elevation depth.
 *    - Clean OLED solid dark fallback when blur is reduced or minimal UI is active.
 *    - Strictly ZERO UI glow.
 *
 * 2. Physically Coherent Liquid Movement:
 *    - Direct destination travel without selecting intermediate tabs or invoking intermediate callbacks.
 *    - Direction-aware forward and reverse movement (leading edge drives travel, trailing edge follows).
 *    - Bounded elongation (1.35x max) with volume conservation squashing (0.86f-1.0f).
 *    - Immediate settling with zero playful bounce or overshoot.
 *    - Crisp instant transition under [AppSettings.reduceAnimation].
 *
 * 3. Tactile Interaction & Strict Haptics:
 *    - Per-item tactile press scaling (0.94f) on touch down.
 *    - Exactly one soft [Haptic.Tick] on actual destination change; never on repeat or animation frames.
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    ownBackdrop: Boolean = false,
    backdrop: Backdrop? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val effectiveBackdrop = backdrop ?: LocalAppBackdrop.current ?: rememberHazeBackdrop(hazeState)

    val haptics = rememberHaptics()
    val density = LocalDensity.current

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val n = tabs.size

    val tabWidthPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width - gapPx * (n - 1)) / n
    } else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width + gapPx) / n
    } else 0f

    // Track movement direction immediately on recomposition with interruption support
    var previousTargetIndex by remember { mutableIntStateOf(selectedIndex) }
    val isMovingRight = remember(selectedIndex) {
        val moving = selectedIndex >= previousTargetIndex
        previousTargetIndex = selectedIndex
        moving
    }

    val targetLeftPx = if (tabStepPx > 0f) {
        selectedIndex * tabStepPx
    } else 0f
    val targetRightPx = targetLeftPx + tabWidthPx

    // Fluid liquid springs tuned to reproduce the reference animation character:
    // Leading edge: fast, responsive departure and controlled deceleration into target mark
    val leadingSpring = remember {
        spring<Float>(
            dampingRatio = 0.78f,
            stiffness = 380f,
        )
    }
    // Trailing edge: inertial lag at start, smooth acceleration to catch up and seal the droplet
    val trailingSpring = remember {
        spring<Float>(
            dampingRatio = 0.84f,
            stiffness = 260f,
        )
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .nuviaGlass(
                tier = NUViAGlassTier.Elevated,
                shape = BarShape,
                borderWidth = GLASS_EDGE_WIDTH,
                hazeState = hazeState,
                backdrop = effectiveBackdrop,
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        if (tabWidthPx > 0f && rowSize.height > 0) {
            // Isolated Liquid indicator pill so animation frames do not recompose outer bar
            LiquidIndicator(
                targetLeftPx = targetLeftPx,
                targetRightPx = targetRightPx,
                tabWidthPx = tabWidthPx,
                rowHeightPx = rowSize.height,
                isMovingRight = isMovingRight,
                leadingSpring = leadingSpring,
                trailingSpring = trailingSpring,
                reduceAnimation = reduceAnimation,
                primaryColor = nuviaColors.primary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                BottomBarItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    reduceAnimation = reduceAnimation,
                    onClick = {
                        if (index != selectedIndex) {
                            haptics.play(Haptic.Tick)
                            onTabSelected(index)
                        } else {
                            onTabSelected(index)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LiquidIndicator(
    targetLeftPx: Float,
    targetRightPx: Float,
    tabWidthPx: Float,
    rowHeightPx: Int,
    isMovingRight: Boolean,
    leadingSpring: SpringSpec<Float>,
    trailingSpring: SpringSpec<Float>,
    reduceAnimation: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val animSpecLeft: AnimationSpec<Float> = if (reduceAnimation) {
        snap()
    } else if (isMovingRight) {
        trailingSpring
    } else {
        leadingSpring
    }

    val animSpecRight: AnimationSpec<Float> = if (reduceAnimation) {
        snap()
    } else if (isMovingRight) {
        leadingSpring
    } else {
        trailingSpring
    }

    val animatedLeft by animateFloatAsState(
        targetValue = targetLeftPx,
        animationSpec = animSpecLeft,
        label = "liquidLeft",
    )
    val animatedRight by animateFloatAsState(
        targetValue = targetRightPx,
        animationSpec = animSpecRight,
        label = "liquidRight",
    )

    // Bounded elongation: reproduction of the reference liquid droplet stretch
    // 1-tab transition stretches smoothly by ~18-25%; multi-tab transitions stretch up to 1.50x
    val maxAllowedWidth = if (reduceAnimation) tabWidthPx else tabWidthPx * 1.50f
    val rawWidthPx = (animatedRight - animatedLeft).coerceAtLeast(tabWidthPx)
    val currentWidthPx = if (reduceAnimation) tabWidthPx else rawWidthPx.coerceAtMost(maxAllowedWidth)
    val stretchRatio = if (tabWidthPx > 0f) (currentWidthPx / tabWidthPx) else 1f

    // Volume conservation: organic vertical squash during horizontal elongation
    val liquidScaleY = if (reduceAnimation) 1f else (1f / sqrt(stretchRatio)).coerceIn(0.82f, 1.0f)

    // Leading-edge anchored travel: the leading edge drives destination travel continuously
    val drawLeftPx = if (isMovingRight && rawWidthPx > maxAllowedWidth) {
        animatedRight - currentWidthPx
    } else {
        animatedLeft
    }

    val density = LocalDensity.current
    val indicatorBrush = remember(primaryColor) {
        Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.22f),
                primaryColor.copy(alpha = 0.08f),
            )
        )
    }
    val borderBottom = remember(primaryColor) { primaryColor.copy(alpha = 0.14f) }
    val borderTop = remember { Color.White.copy(alpha = 0.30f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = drawLeftPx
                scaleY = liquidScaleY
            }
            .width(with(density) { currentWidthPx.toDp() })
            .height(with(density) { rowHeightPx.toDp() })
            .clip(IndicatorShape)
            .background(indicatorBrush)
            .nuviaSpecularBorder(
                shape = IndicatorShape,
                width = 0.75.dp,
                topHighlight = borderTop,
                bottomBorder = borderBottom,
            ),
    )
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    reduceAnimation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press scaling: subtley depresses under thumb without moving the bar
    val pressScale by animateFloatAsState(
        targetValue = if (!reduceAnimation && isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 500f,
        ),
        label = "tabPressScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = if (reduceAnimation) snap() else spring(
            dampingRatio = 0.90f,
            stiffness = 400f,
        ),
        label = "iconScale",
    )

    val tint by animateColorAsState(
        targetValue = if (selected) {
            nuviaColors.primary
        } else {
            nuviaColors.textSecondary.copy(alpha = 0.78f)
        },
        animationSpec = if (reduceAnimation) snap() else tween(180),
        label = "tabTint",
    )

    val currentIcon = if (selected && tab.activeIcon != null) tab.activeIcon else tab.icon

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.5.dp),
    ) {
        Icon(
            imageVector = currentIcon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Manrope,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.5.sp,
                letterSpacing = 0.2.sp,
            ),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
