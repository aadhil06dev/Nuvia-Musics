package com.music.nuvia.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.nuvia.R
import com.music.nuvia.ui.theme.NUViABrand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Minimal, cinematic NUViA Launch Experience.
 *
 * Launches into a pure AMOLED black canvas (#000000) with a sophisticated deep orange atmospheric glow.
 * The official transparent orange NUViA brand logo is displayed prominently (192dp) with a smooth
 * optical fade/scale reveal, holds with calm presence, and transitions continuously into the Home environment:
 *
 * Sequence:
 * BLACK -> large NUViA logo appears -> logo gently scales/fades -> orange atmospheric light expands outward
 * -> Home environment materializes -> normal HomeScreen.
 */
@Composable
fun NUViALaunchOverlay(
    visible: Boolean,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val containerAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.92f) }
    val glowIntensity = remember { Animatable(0f) }
    val glowRadiusScale = remember { Animatable(0.70f) }

    val smoothEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f) }
    val entranceEasing = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f) }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(4000L) {
            try {
                // Stage 1: Cinematic emergence in parallel (550ms)
                coroutineScope {
                    launch { logoAlpha.animateTo(1f, animationSpec = tween(550, easing = smoothEasing)) }
                    launch { logoScale.animateTo(1.0f, animationSpec = tween(650, easing = smoothEasing)) }
                    launch { glowIntensity.animateTo(1f, animationSpec = tween(550, easing = smoothEasing)) }
                }

                // Stage 2: Calm hold allowing branding recognition (550ms)
                delay(550)

                // Stage 3: Continuous Cinematic Home Entrance transition (~950ms)
                coroutineScope {
                    launch {
                        glowRadiusScale.animateTo(1.55f, animationSpec = tween(950, easing = entranceEasing))
                    }
                    launch {
                        logoScale.animateTo(1.05f, animationSpec = tween(400, easing = entranceEasing))
                        logoAlpha.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
                    }
                    launch {
                        delay(120)
                        containerAlpha.animateTo(0f, animationSpec = tween(800, easing = entranceEasing))
                    }
                }
            } catch (_: Throwable) {
                // Guaranteed safety fallback
            }
        }
        onDismissed()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(containerAlpha.value.coerceIn(0f, 1f))
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val currentGlowAlpha = (0.20f * glowIntensity.value * containerAlpha.value).coerceIn(0f, 1f)
                val currentRadius = size.width * glowRadiusScale.value
                if (currentGlowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NUViABrand.DeepOrange.copy(alpha = currentGlowAlpha),
                                NUViABrand.PrimaryOrange.copy(alpha = currentGlowAlpha * 0.35f),
                                Color.Transparent,
                            ),
                            center = centerOffset,
                            radius = currentRadius,
                        ),
                        center = centerOffset,
                        radius = currentRadius,
                    )
                }
            }
            .testTag("nuvia_launch_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        if (logoAlpha.value > 0f) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(192.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value.coerceIn(0f, 1f)
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .testTag("nuvia_launch_logo"),
            )
        }
    }
}

/**
 * Survey-to-Home Cinematic Entrance Transition.
 *
 * Triggered when a new user completes the onboarding preference survey:
 * AMOLED black background -> orange atmospheric expansion -> NUViA logo brief gentle pulse
 * -> Home environment softly materializes into full interactivity.
 */
@Composable
fun NUViAHomeEntranceOverlay(
    visible: Boolean,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val containerAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.95f) }
    val glowIntensity = remember { Animatable(0f) }
    val glowRadiusScale = remember { Animatable(0.60f) }

    val entranceEasing = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f) }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(3000L) {
            try {
                coroutineScope {
                    launch {
                        logoAlpha.animateTo(0.9f, animationSpec = tween(300, easing = entranceEasing))
                        delay(180)
                        logoScale.animateTo(1.04f, animationSpec = tween(400, easing = entranceEasing))
                        logoAlpha.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
                    }
                    launch {
                        glowIntensity.animateTo(1f, animationSpec = tween(350, easing = entranceEasing))
                        glowRadiusScale.animateTo(1.5f, animationSpec = tween(900, easing = entranceEasing))
                    }
                    launch {
                        delay(220)
                        containerAlpha.animateTo(0f, animationSpec = tween(700, easing = entranceEasing))
                    }
                }
            } catch (_: Throwable) {
                // Safety fallback
            }
        }
        onDismissed()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(containerAlpha.value.coerceIn(0f, 1f))
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val currentGlowAlpha = (0.22f * glowIntensity.value * containerAlpha.value).coerceIn(0f, 1f)
                val currentRadius = size.width * glowRadiusScale.value
                if (currentGlowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NUViABrand.DeepOrange.copy(alpha = currentGlowAlpha),
                                NUViABrand.PrimaryOrange.copy(alpha = currentGlowAlpha * 0.40f),
                                Color.Transparent,
                            ),
                            center = centerOffset,
                            radius = currentRadius,
                        ),
                        center = centerOffset,
                        radius = currentRadius,
                    )
                }
            }
            .testTag("nuvia_home_entrance_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        if (logoAlpha.value > 0f) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value.coerceIn(0f, 1f)
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .testTag("nuvia_home_entrance_logo"),
            )
        }
    }
}


