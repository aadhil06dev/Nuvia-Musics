package com.music.nuvia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Sora
import com.music.nuvia.ui.components.nuviaSpecularBorder

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val HIGHLIGHTS = listOf(
    FeatureHighlight(
        icon = Icons.Rounded.Layers,
        title = "Fluid Navigation & Scroll Shell",
        description = "Hardware-accelerated GPU translation keeps navigation smooth and fixed when idle, auto-hiding symmetrically during content exploration.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.ViewAgenda,
        title = "Pinned Cinematic Headers",
        description = "Atmospheric greeting, brand identity, and quick vibe selectors stay pinned above scrollable feeds on Home and Settings.",
        title = "Natural Fluid Headers",
        description = "Atmospheric greeting, brand identity, and vibe selector chips integrate seamlessly into your music feed, scrolling naturally with content.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.Search,
        title = "Instant Settings Search",
        description = "Real-time instant filtering across all 14 acoustic and audio categories with direct navigation to controls.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.ColorLens,
        title = "True White & Silver Colors",
        description = "Pure 0% saturation neutral color support for pristine White, Soft White, and Silver accents without color shifts.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.ToggleOn,
        title = "Subpixel Liquid Toggles",
        description = "Elastic spring-physics switches with zero layout remeasurement and seamless rendering.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.Lyrics,
        title = "Compact Lyrics Sources",
        description = "Streamlined bottom sheet for prioritizing synced lyric databases and syllable-level tracking.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.AutoAwesome,
        title = "Spotify Canvas Bottom Sheet",
        description = "Compact in-app setup sheet to stream dynamic, looping video backdrops directly behind album art.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.GraphicEq,
        title = "Spatial Audio & 10-Band EQ",
        description = "Stereo cross-feed widening effect paired with a 10-band studio parametric equalizer and presets.",
    ),
    FeatureHighlight(
        icon = Icons.Rounded.Download,
        title = "Offline Downloads & Local Library",
        description = "Full offline music playback with lossless downloads and automatic local device file indexing.",
    ),
)

@Composable
fun WhatsNewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val nuviaColors = LocalNUViAColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        // Back Button & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = nuviaColors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "What's New in NUViA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Text(
                    text = "Key Features & Capabilities",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Sora,
                        fontSize = 11.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            HIGHLIGHTS.forEach { feature ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(nuviaColors.glassSurface)
                        .nuviaSpecularBorder(
                            shape = RoundedCornerShape(20.dp),
                            width = 0.75.dp,
                            topHighlight = nuviaColors.glassHighlight,
                            bottomBorder = nuviaColors.glassBorder,
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(nuviaColors.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = nuviaColors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = nuviaColors.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = feature.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Sora,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                ),
                                color = nuviaColors.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 32.dp))
    }
}

