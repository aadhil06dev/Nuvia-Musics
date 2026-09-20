package com.music.nuvia.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.R
import com.music.nuvia.ui.theme.NUViABrand
import kotlinx.coroutines.delay

/**
 * Phase 1 NUViA Welcome Screen.
 *
 * Cinematic, dark, sophisticated entry screen introducing the user to NUViA.
 * Features the official NUViA logo, subtle liquid-glass depth and atmospheric lighting,
 * and a tactile glass "Get Started" CTA with staggered layered reveal animations.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BackHandler {
        (context as? Activity)?.finish()
    }

    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var ctaVisible by remember { mutableStateOf(false) }

    val logoGlowAnim = remember { Animatable(0f) }
    val smoothEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f) }

    LaunchedEffect(Unit) {
        // Staggered sequence for layered reveal
        delay(100)
        logoVisible = true
        logoGlowAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = smoothEasing),
        )
        delay(80)
        titleVisible = true
        delay(100)
        subtitleVisible = true
        delay(120)
        ctaVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width / 2f, size.height * 0.36f)
                val glowAlpha = 0.20f * logoGlowAnim.value
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NUViABrand.DeepOrange.copy(alpha = glowAlpha),
                            NUViABrand.PrimaryOrange.copy(alpha = glowAlpha * 0.35f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.75f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.75f,
                )
            }
            .testTag("nuvia_welcome_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top/Center Section: Branding & Copy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Layer 1: Official NUViA Logo (112dp container, 100dp logo)
                AnimatedVisibility(
                    visible = logoVisible,
                    enter = fadeIn(tween(650, easing = smoothEasing)) +
                        slideInVertically(tween(650, easing = smoothEasing)) { it / 3 },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            NUViABrand.DeepOrange.copy(alpha = 0.20f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    radius = size.width * 0.75f,
                                )
                            },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bglogo),
                            contentDescription = "NUViA Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(104.dp)
                                .testTag("nuvia_welcome_logo"),
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                // Layer 2: Welcome Headline
                AnimatedVisibility(
                    visible = titleVisible,
                    enter = fadeIn(tween(650, easing = smoothEasing)) +
                        slideInVertically(tween(650, easing = smoothEasing)) { it / 3 },
                ) {
                    Text(
                        text = "Welcome to NUViA",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("nuvia_welcome_title"),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Layer 3: Tagline & Purpose Description
                AnimatedVisibility(
                    visible = subtitleVisible,
                    enter = fadeIn(tween(650, easing = smoothEasing)) +
                        slideInVertically(tween(650, easing = smoothEasing)) { it / 3 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your music. Your atmosphere.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp,
                            ),
                            color = NUViABrand.Primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nuvia_welcome_tagline"),
                        )

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "A modern music player designed around pure sound and dynamic album artwork.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag("nuvia_welcome_description"),
                        )
                    }
                }
            }

            // Layer 4: Liquid Glass "Get Started" CTA Button
            AnimatedVisibility(
                visible = ctaVisible,
                enter = fadeIn(tween(700, easing = smoothEasing)) +
                    slideInVertically(tween(700, easing = smoothEasing)) { it / 2 },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.14f),
                                    Color(0xFFFFFFFF).copy(alpha = 0.05f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        )
                        .clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
                            onClick = onGetStarted,
                        )
                        .testTag("get_started_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                            ),
                            color = Color(0xFFFFFFFF),
                        )
                    }
                }
            }
        }
    }
}
