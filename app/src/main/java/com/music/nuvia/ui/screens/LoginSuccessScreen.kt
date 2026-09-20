package com.music.nuvia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassCard
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.LiquidGlassSurface
import com.music.nuvia.ui.components.NUViAGlassShapes
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.NUViABrand

/**
 * NUViA Login Congratulations / Account Connected State.
 *
 * Cinematic, premium Liquid Glass moment following successful account login:
 * - Atmospheric black background with subtle amber/orange illumination
 * - Layered translucent glass presentation card with specular highlights
 * - Real authenticated username/account identity presentation
 * - Verified YouTube Music connection confirmation
 * - Tactile primary button that proceeds directly to Home
 */
@Composable
fun LoginSuccessScreen(
    accountName: String?,
    accountEmail: String? = null,
    accountThumbnailUrl: String? = null,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }

    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    var contentVisible by remember { mutableStateOf(false) }

    val smoothEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f) }

    // Subtle ambient breathing glow animation
    val ambientTransition = rememberInfiniteTransition(label = "ambientPulse")
    val pulseIntensity by if (reduceMotion) {
        remember { mutableStateOf(0.85f) }
    } else {
        ambientTransition.animateFloat(
            initialValue = 0.70f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseFloat",
        )
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.38f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NUViABrand.DeepOrange.copy(alpha = 0.22f * pulseIntensity),
                            NUViABrand.PrimaryOrange.copy(alpha = 0.08f * pulseIntensity),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.90f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.90f,
                )
            }
            .testTag("nuvia_login_success_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onBack()
                    },
                    contentDescription = "Back",
                    size = 38.dp,
                    iconSize = 18.dp,
                    tier = NUViAGlassTier.Subtle,
                    modifier = Modifier.testTag("login_success_back_button"),
                )
            }

            // Center Content: Liquid Glass Identity Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(500, easing = smoothEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(500, easing = smoothEasing)),
                ) {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        tier = NUViAGlassTier.Elevated,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                        ) {
                            // Avatar Presentation
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                if (!accountThumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = accountThumbnailUrl,
                                        contentDescription = accountName ?: "Account Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        NUViABrand.HighlightOrange.copy(alpha = 0.70f),
                                                        NUViABrand.PrimaryOrange.copy(alpha = 0.30f),
                                                    ),
                                                ),
                                                shape = CircleShape,
                                            )
                                            .testTag("login_success_avatar"),
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        NUViABrand.DeepOrange.copy(alpha = 0.35f),
                                                        Color(0xFF181A20),
                                                    ),
                                                ),
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        NUViABrand.HighlightOrange.copy(alpha = 0.60f),
                                                        NUViABrand.PrimaryOrange.copy(alpha = 0.20f),
                                                    ),
                                                ),
                                                shape = CircleShape,
                                            )
                                            .testTag("login_success_generic_avatar"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = NUViABrand.HighlightOrange,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    }
                                }

                                // Connected Micro-Badge
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 0.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F1115))
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF34D399).copy(alpha = 0.60f),
                                                    Color(0xFF059669).copy(alpha = 0.20f),
                                                ),
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34D399)),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(11.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Status Eyebrow
                            Text(
                                text = "You're logged in",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp,
                                ),
                                color = NUViABrand.HighlightOrange,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(8.dp))

                            // Username / Welcome Heading
                            Text(
                                text = if (!accountName.isNullOrBlank()) {
                                    "Welcome, $accountName"
                                } else {
                                    "Welcome to NUViA"
                                },
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("login_success_title"),
                            )

                            Spacer(Modifier.height(10.dp))

                            // Service Connection
                            Text(
                                text = "Your YouTube Music account is connected.",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("login_success_service"),
                            )

                            Spacer(Modifier.height(6.dp))

                            // Supported Data Reassurance
                            Text(
                                text = "Your library and supported account data are now available in NUViA.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                ),
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )

                            Spacer(Modifier.height(20.dp))

                            // Verified Capabilities Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(
                                        width = 0.75.dp,
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = NUViABrand.HighlightOrange,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Library  •  Playlists  •  Favorites",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = Color(0xFF94A3B8),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action: Primary Continue to NUViA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NUViABrand.PrimaryOrange,
                                    NUViABrand.DeepOrange,
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NUViABrand.HighlightOrange,
                                    NUViABrand.PrimaryOrange.copy(alpha = 0.4f),
                                ),
                            ),
                            shape = RoundedCornerShape(26.dp),
                        )
                        .clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
                            onClick = {
                                haptics.play(Haptic.Select)
                                onContinue()
                            },
                        )
                        .testTag("login_success_continue_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Continue to NUViA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                            ),
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
