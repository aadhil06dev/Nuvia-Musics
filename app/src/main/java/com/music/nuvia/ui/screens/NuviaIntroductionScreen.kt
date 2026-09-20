package com.music.nuvia.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * NUViA Introduction Screen.
 *
 * Welcomes the user with a focused overview of NUViA's design philosophy:
 * an immersive music player centered around dynamic album atmospheres,
 * pristine acoustic clarity, seamless gestures, and curated soundscapes.
 *
 * Includes the official developer attribution: "Developer: Adhil CLT".
 */
@Composable
fun NuviaIntroductionScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }

    var contentVisible by remember { mutableStateOf(false) }
    val glowAnim = remember { Animatable(0f) }
    val smoothEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f) }

    LaunchedEffect(Unit) {
        delay(60)
        contentVisible = true
        glowAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = smoothEasing),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width / 2f, size.height * 0.28f)
                val glowAlpha = 0.18f * glowAnim.value
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NUViABrand.DeepOrange.copy(alpha = glowAlpha),
                            NUViABrand.PrimaryOrange.copy(alpha = glowAlpha * 0.35f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.85f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.85f,
                )
            }
            .testTag("nuvia_introduction_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))

                // Official Transparent NUViA Logo
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(600, easing = smoothEasing)) +
                        slideInVertically(tween(600, easing = smoothEasing)) { it / 3 },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
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
                            contentDescription = "NUViA",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(80.dp)
                                .testTag("nuvia_intro_logo"),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Title & Subtitle
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(650, delayMillis = 100, easing = smoothEasing)) +
                        slideInVertically(tween(650, delayMillis = 100, easing = smoothEasing)) { it / 3 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Meet NUViA",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = Color(0xFFFFFFFF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nuvia_intro_title"),
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Your music. Your atmosphere.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                            ),
                            color = NUViABrand.Primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nuvia_intro_subtitle"),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Description Card
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(650, delayMillis = 200, easing = smoothEasing)) +
                        slideInVertically(tween(650, delayMillis = 200, easing = smoothEasing)) { it / 3 },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.07f),
                                        Color(0xFFFFFFFF).copy(alpha = 0.02f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.04f),
                                    ),
                                ),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "NUViA is a modern music player crafted around an immersive listening experience. Every track breathes with dynamic album artwork, adaptive visual atmosphere, fluid gestures, and deeply personalized music discovery.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nuvia_intro_description"),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            // Bottom Section: Developer Credit & Continue Button
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(700, delayMillis = 300, easing = smoothEasing)) +
                    slideInVertically(tween(700, delayMillis = 300, easing = smoothEasing)) { it / 2 },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Developer Attribution
                    Text(
                        text = "Developer: Adhil CLT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 18.dp)
                            .testTag("nuvia_developer_credit"),
                    )

                    // Continue Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.16f),
                                        Color(0xFFFFFFFF).copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.36f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                shape = RoundedCornerShape(28.dp),
                            )
                            .clickable(
                                role = Role.Button,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White),
                                onClick = onContinue,
                            )
                            .testTag("intro_continue_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Continue",
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
