package com.music.nuvia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.R
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.NUViABrand
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassChip
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.NUViAGlassTier

// Curated music preference categories
private val LANGUAGES = listOf(
    "Malayalam",
    "English",
    "Hindi",
    "Tamil",
    "Telugu",
    "Kannada",
    "Bengali",
    "Punjabi",
    "Other",
)

private val VIBES = listOf(
    "Chill",
    "Energy",
    "Focus",
    "Atmosphere",
    "Night",
)

private val LISTENING_MOMENTS = listOf(
    "Morning",
    "Commute",
    "Study",
    "Workout",
    "Evening",
    "Late Night",
)

private val DISCOVERY_STYLES = listOf(
    "Familiar favorites",
    "Discover new music",
    "A mix of both",
)

private val CONTENT_PREFERENCES = listOf(
    "Songs",
    "Albums",
    "Playlists",
    "Artists",
)

private val GENRES = listOf(
    "Pop",
    "Hip-Hop",
    "Rock",
    "Electronic",
    "R&B",
    "Indie",
    "Lo-fi",
    "Acoustic",
)

/**
 * NUViA Music Preferences Personalization Screen.
 *
 * Compact, premium Liquid Glass experience:
 * - Allows users who skipped account login to tune their music preferences
 * - Categories: Languages, Vibes, Listening Moments, Discovery Style, Content, Genres
 * - Glass selection chips with animated states and tactile haptic feedback
 * - Clear "Finish & Enter NUViA" and "Skip for now" navigation actions
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSurveyScreen(
    onFinish: (
        selectedGenres: Set<String>,
        selectedMoods: Set<String>,
        selectedLanguages: Set<String>,
        selectedMoments: Set<String>,
        selectedDiscovery: Set<String>,
        selectedContent: Set<String>,
    ) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }

    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
    var selectedMoods by remember { mutableStateOf(setOf<String>()) }
    var selectedMoments by remember { mutableStateOf(setOf<String>()) }
    var selectedDiscovery by remember { mutableStateOf(setOf<String>()) }
    var selectedContent by remember { mutableStateOf(setOf<String>()) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }

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
                            NUViABrand.DeepOrange.copy(alpha = 0.18f),
                            NUViABrand.PrimaryOrange.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = size.width * 0.85f,
                    ),
                    center = centerOffset,
                    radius = size.width * 0.85f,
                )
            }
            .testTag("nuvia_music_preferences_screen"),
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
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onBack()
                    },
                    contentDescription = "Back",
                    size = 38.dp,
                    iconSize = 18.dp,
                    tier = NUViAGlassTier.Subtle,
                    modifier = Modifier.testTag("preferences_back_button"),
                )

                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
                            onClick = {
                                haptics.play(Haptic.Tap)
                                onSkip()
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("preferences_top_skip_button"),
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = contentVisible,
                    enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(500, easing = smoothEasing)) +
                        slideInVertically(tween(500, easing = smoothEasing)) { it / 5 },
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.bglogo),
                                contentDescription = "NUViA",
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("nuvia_preferences_logo"),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "NUViA Atmosphere",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = NUViABrand.HighlightOrange,
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Music Preferences",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("preferences_title"),
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Personalize your experience. Select the languages, vibes, and styles that match how you listen.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color(0xFFCBD5E1),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Section 1: Languages
                PreferenceSection(
                    title = "LANGUAGES",
                    subtitle = "Languages you listen to",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 40,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LANGUAGES.forEach { language ->
                            val isSelected = language in selectedLanguages
                            PreferenceChip(
                                label = language,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.ToggleOn)
                                    selectedLanguages = if (isSelected) {
                                        selectedLanguages - language
                                    } else {
                                        selectedLanguages + language
                                    }
                                },
                                testTag = "language_chip_$language",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Section 2: Vibes
                PreferenceSection(
                    title = "VIBES",
                    subtitle = "Atmospheric feeling",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 80,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        VIBES.forEach { vibe ->
                            val isSelected = vibe in selectedMoods
                            PreferenceChip(
                                label = vibe,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.ToggleOn)
                                    selectedMoods = if (isSelected) {
                                        selectedMoods - vibe
                                    } else {
                                        selectedMoods + vibe
                                    }
                                },
                                testTag = "vibe_chip_$vibe",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Section 3: Listening Moments
                PreferenceSection(
                    title = "LISTENING MOMENTS",
                    subtitle = "When you listen",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 120,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LISTENING_MOMENTS.forEach { moment ->
                            val isSelected = moment in selectedMoments
                            PreferenceChip(
                                label = moment,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.ToggleOn)
                                    selectedMoments = if (isSelected) {
                                        selectedMoments - moment
                                    } else {
                                        selectedMoments + moment
                                    }
                                },
                                testTag = "moment_chip_$moment",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Section 4: Discovery Style
                PreferenceSection(
                    title = "DISCOVERY STYLE",
                    subtitle = "How you explore",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 160,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DISCOVERY_STYLES.forEach { style ->
                            val isSelected = style in selectedDiscovery
                            PreferenceChip(
                                label = style,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.Select)
                                    selectedDiscovery = if (isSelected) {
                                        emptySet()
                                    } else {
                                        setOf(style)
                                    }
                                },
                                testTag = "discovery_chip_$style",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Section 5: Content
                PreferenceSection(
                    title = "CONTENT",
                    subtitle = "What you listen to most",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 200,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CONTENT_PREFERENCES.forEach { content ->
                            val isSelected = content in selectedContent
                            PreferenceChip(
                                label = content,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.ToggleOn)
                                    selectedContent = if (isSelected) {
                                        selectedContent - content
                                    } else {
                                        selectedContent + content
                                    }
                                },
                                testTag = "content_chip_$content",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Section 6: Musical Styles & Genres
                PreferenceSection(
                    title = "GENRES & SOUNDS",
                    subtitle = "Favorite musical genres",
                    visible = contentVisible,
                    reduceMotion = reduceMotion,
                    delayMillis = 240,
                    smoothEasing = smoothEasing,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        GENRES.forEach { genre ->
                            val isSelected = genre in selectedGenres
                            PreferenceChip(
                                label = genre,
                                selected = isSelected,
                                onClick = {
                                    haptics.play(if (isSelected) Haptic.ToggleOff else Haptic.ToggleOn)
                                    selectedGenres = if (isSelected) {
                                        selectedGenres - genre
                                    } else {
                                        selectedGenres + genre
                                    }
                                },
                                testTag = "genre_chip_$genre",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            // Bottom Actions: Finish & Enter NUViA or Skip for now
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary Action: Finish & Enter NUViA
                LiquidGlassButton(
                    text = "Finish & Enter NUViA",
                    onClick = {
                        haptics.play(Haptic.Select)
                        onFinish(
                            selectedGenres,
                            selectedMoods,
                            selectedLanguages,
                            selectedMoments,
                            selectedDiscovery,
                            selectedContent,
                        )
                    },
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finish_preferences_button"),
                )

                // Secondary Action: Skip for now
                LiquidGlassButton(
                    text = "Skip for now",
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onSkip()
                    },
                    isPrimary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("skip_preferences_button"),
                )
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    subtitle: String,
    visible: Boolean,
    reduceMotion: Boolean,
    delayMillis: Int,
    smoothEasing: CubicBezierEasing,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) fadeIn(tween(150)) else fadeIn(tween(450, delayMillis = delayMillis, easing = smoothEasing)),
        modifier = modifier,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    ),
                    color = Color(0xFF94A3B8),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                    ),
                    color = Color(0xFF64748B),
                )
            }

            Spacer(Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    LiquidGlassChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        icon = if (selected) Icons.Rounded.Check else null,
    )
}

