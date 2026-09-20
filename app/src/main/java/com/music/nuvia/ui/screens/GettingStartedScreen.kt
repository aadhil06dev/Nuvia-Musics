package com.music.nuvia.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.R
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassCard
import com.music.nuvia.ui.components.LiquidGlassSurface
import com.music.nuvia.ui.components.NUViAGlassShapes
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.theme.NUViABrand

/**
 * NUViA Phase 3 Single-Page Getting Started Screen.
 *
 * A premium, single vertically-scrollable introduction establishing:
 * - Brand identity with official bglogo.png
 * - "Meet NUViA: Your music. Your atmosphere. Your way."
 * - Completely free, ad-free listening experience
 * - Clear "Continue →" and "Skip" actions that both proceed to Account Connection
 * - Optional scroll to explore verified features (Music sources, Atmosphere, Liquid Glass, Minimal UI, UI Glow, Connected Services)
 * - Compact developer & support section (Adhil CLT, https://buymeatea.online/adhil)
 */
@Composable
fun GettingStartedScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

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
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.18f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NUViABrand.DeepOrange.copy(alpha = 0.24f),
                            NUViABrand.PrimaryOrange.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.90f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.90f,
                )
            }
            .testTag("nuvia_getting_started_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Top App Bar with NUViA Title & Skip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.bglogo),
                        contentDescription = "NUViA Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "NUViA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = Color.White,
                    )
                }

                // Skip Button (proceeds to Account Connection)
                LiquidGlassSurface(
                    tier = NUViAGlassTier.Subtle,
                    shape = NUViAGlassShapes.Pill,
                    modifier = Modifier
                        .nuviaTactilePress(onClick = onSkip)
                        .testTag("getting_started_skip_button"),
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                // Hero Section
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(500, easing = smoothEasing)) +
                        slideInVertically(tween(500, easing = smoothEasing)) { it / 5 },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // NUViA Glowing Logo
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            NUViABrand.DeepOrange.copy(alpha = 0.35f),
                                            Color.Transparent,
                                        ),
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NUViABrand.HighlightOrange.copy(alpha = 0.6f),
                                            NUViABrand.PrimaryOrange.copy(alpha = 0.2f),
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.bglogo),
                                contentDescription = "NUViA",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("getting_started_logo"),
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Meet NUViA",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("getting_started_headline"),
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Your music. Your atmosphere. Your way.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp,
                            ),
                            color = NUViABrand.HighlightOrange,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("getting_started_tagline"),
                        )

                        Spacer(Modifier.height(14.dp))

                        // Concise Value Proposition
                        Text(
                            text = "A completely free, ad-free music player crafted for acoustic fidelity, dynamic artwork atmospheres, and distraction-free listening.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 22.sp,
                            ),
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Feature Exploration Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                    Text(
                        text = "FEATURES AT A GLANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Compact Feature Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 1. Music Sources
                    CompactFeatureCard(
                        icon = Icons.Rounded.TravelExplore,
                        title = "Music Sources",
                        description = "Explore streaming music and local audio files stored on your device.",
                    )

                    // 2. Your Atmosphere
                    CompactFeatureCard(
                        icon = Icons.Rounded.Palette,
                        title = "Your Atmosphere",
                        description = "Artwork-reactive colors adapt dynamically in real time to every track and album you play.",
                    )

                    // 3. Liquid Glass
                    CompactFeatureCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Liquid Glass Design",
                        description = "Custom translucent surfaces with delicate reflections and thin borders tuned for deep AMOLED blacks.",
                    )

                    // 4. Make It Yours (Minimal UI, UI Glow, Advanced audio, Crossfade, Autoplay)
                    MakeItYoursCard()

                    // 5. Connected Integrations
                    CompactFeatureCard(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Connected Integrations",
                        description = "YouTube Music account, Discord Rich Presence, and Last.fm scrobbling.",
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Bottom Continue Action (The ONLY Continue action on Getting Started)
                LiquidGlassButton(
                    text = "Continue",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = onContinue,
                    isPrimary = true,
                    tier = NUViAGlassTier.Prominent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("getting_started_continue_button"),
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CompactFeatureCard(
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
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NUViABrand.PrimaryOrange.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NUViABrand.HighlightOrange,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
            }
        }
    }
}

@Composable
private fun MakeItYoursCard(
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = NUViAGlassShapes.Card,
        tier = NUViAGlassTier.Elevated,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NUViABrand.PrimaryOrange.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = NUViABrand.HighlightOrange,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Make It Yours",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))

                // Minimal UI
                Text(
                    text = "• Minimal UI: Reduce extra interface elements for a cleaner listening experience.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xFFCBD5E1),
                )
                Spacer(Modifier.height(4.dp))

                // UI Glow
                Text(
                    text = "• UI Glow: Adds subtle ambient lighting effects. Off by default.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
                Spacer(Modifier.height(4.dp))

                // Advanced Audio & DSP
                Text(
                    text = "• Advanced Audio & DSP: Smooth crossfade and high-fidelity sound processing.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
                Spacer(Modifier.height(4.dp))

                // Autoplay
                Text(
                    text = "• Autoplay: Keep playing similar music once the queue runs out.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
            }
        }
    }
}

