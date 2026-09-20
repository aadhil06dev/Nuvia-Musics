package com.music.nuvia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassCard
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.LiquidGlassSurface
import com.music.nuvia.ui.components.NUViAGlassShapes
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.theme.NUViABrand

/**
 * NUViA Phase 3 Redesigned Optional Account Connection Screen.
 *
 * Calm, transparent, and trustworthy:
 * - Emphasizes that account login is 100% OPTIONAL
 * - NUViA is completely usable offline and anonymously without an account
 * - Explains verified benefits (access library, sync liked tracks)
 * - "Connect account" launches the unified in-app Google login WebView
 * - "Skip for now" proceeds smoothly to the Vibe Survey
 * - Back button returns to Getting Started
 */
@Composable
fun AccountConnectScreen(
    onConnectAccount: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }

    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    var contentVisible by remember { mutableStateOf(false) }
    val smoothEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .drawBehind {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.22f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NUViABrand.DeepOrange.copy(alpha = 0.20f),
                            NUViABrand.PrimaryOrange.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.85f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.85f,
                )
            }
            .testTag("nuvia_account_connect_screen"),
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                    size = 38.dp,
                    iconSize = 18.dp,
                    tier = NUViAGlassTier.Subtle,
                    modifier = Modifier.testTag("account_connect_back_button"),
                )

                // Skip for now Action in top bar
                LiquidGlassSurface(
                    tier = NUViAGlassTier.Subtle,
                    shape = NUViAGlassShapes.Pill,
                    modifier = Modifier
                        .nuviaTactilePress(onClick = onSkip)
                        .testTag("account_connect_top_skip_button"),
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = contentVisible,
                    enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(500, easing = smoothEasing)) +
                        slideInVertically(tween(500, easing = smoothEasing)) { it / 5 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Music Service Icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            NUViABrand.DeepOrange.copy(alpha = 0.30f),
                                            Color.Transparent,
                                        ),
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NUViABrand.HighlightOrange.copy(alpha = 0.50f),
                                            NUViABrand.PrimaryOrange.copy(alpha = 0.15f),
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = NUViABrand.HighlightOrange,
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Connect YouTube Music",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("account_connect_title"),
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Connect your account to bring your YouTube Music library, playlists, favorites, and supported account data into NUViA.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 21.sp,
                            ),
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                // Verified Benefits List
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(500, delayMillis = 100, easing = smoothEasing)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BenefitRow(
                            icon = Icons.Rounded.LibraryMusic,
                            title = "Access your playlists & library",
                            description = "Your saved playlists and library items appear directly in NUViA.",
                        )

                        BenefitRow(
                            icon = Icons.Rounded.Favorite,
                            title = "Sync liked songs & favorites",
                            description = "Keep your liked tracks and ratings synchronized with your account.",
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Bottom Actions: Connect Account & Skip for Now
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary Action: Connect Account
                LiquidGlassButton(
                    text = "Connect account",
                    onClick = onConnectAccount,
                    isPrimary = true,
                    tier = NUViAGlassTier.Prominent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("connect_account_button"),
                )

                // Secondary Action: Skip for now
                LiquidGlassButton(
                    text = "Skip for now",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = onSkip,
                    isPrimary = false,
                    tier = NUViAGlassTier.Elevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("account_skip_for_now_button"),
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = NUViAGlassShapes.Card,
        tier = NUViAGlassTier.Elevated,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NUViABrand.PrimaryOrange.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NUViABrand.HighlightOrange,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
            }
        }
    }
}
