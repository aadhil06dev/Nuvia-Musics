package com.music.nuvia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.data.jiosaavn.JioSaavnService
import com.music.nuvia.data.jiosaavn.JioSaavnTestResult
import com.music.nuvia.data.sources.SourceKind
import com.music.nuvia.data.sources.SourceRegistry
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated source configuration screen for JioSaavn, the primary music provider.
 * Follows NUViA's dark/red liquid glass visual language, real capabilities display,
 * and live reachability connection test.
 */
@Composable
fun JioSaavnSourceScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val scope = rememberCoroutineScope()

    val configs by SourceRegistry.configs.collectAsStateWithLifecycle()
    val jioSaavnConfig = configs.firstOrNull { it.kind == SourceKind.JIOSAAVN }
    val youtubeConfig = configs.firstOrNull { it.kind == SourceKind.YOUTUBE }

    val isEnabled = jioSaavnConfig?.enabled ?: true

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<JioSaavnTestResult?>(null) }

    // Initial passive probe
    LaunchedEffect(Unit) {
        if (testResult == null) {
            withContext(Dispatchers.IO) {
                testResult = JioSaavnService.testConnection()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Spacer(Modifier.height(12.dp))

        // --- 2. HERO STATUS CARD ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            nuviaColors.surfaceCard.copy(alpha = 0.85f),
                            nuviaColors.surfaceOled.copy(alpha = 0.95f),
                        ),
                    ),
                )
                .nuviaSpecularBorder(RoundedCornerShape(22.dp))
                .nuviaAmbientBacklight(
                    color = if (isEnabled && (testResult?.success != false)) nuviaColors.primary else Color.DarkGray,
                    radiusDp = 32.dp,
                    alpha = if (isEnabled) 0.12f else 0.04f,
                )
                .padding(20.dp),
        ) {
            // Subtle watermark
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.BottomEnd)
                    .alpha(0.04f),
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(nuviaColors.primary.copy(alpha = 0.14f))
                                .nuviaSpecularBorder(RoundedCornerShape(12.dp), width = 0.5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = nuviaColors.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Primary Music Provider",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = nuviaColors.primary,
                            )
                            Text(
                                text = "JioSaavn Official",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                ),
                                color = nuviaColors.textPrimary,
                            )
                        }
                    }

                    // Active badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEnabled) nuviaColors.primary.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.08f),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEnabled && testResult?.success == true) Color(0xFF4CAF50)
                                        else if (isEnabled) Color(0xFFFF9800)
                                        else Color.Gray,
                                    )
                                    .alpha(if (isEnabled && testResult?.success == true) pulseAlpha else 1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isEnabled) "Active" else "Disabled",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = if (isEnabled) nuviaColors.textPrimary else nuviaColors.textMuted,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "High-fidelity music search, rich Indian & Regional metadata, and streaming fallback integrated into NUViA.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- 3. CONNECTION TEST SECTION ---
        SettingsGroup(
            header = "Connection test",
            footer = "Performs a live API query, measures response time, and verifies search response status.",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Test button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(nuviaColors.primary.copy(alpha = 0.12f))
                        .nuviaSpecularBorder(RoundedCornerShape(14.dp), width = 0.5.dp)
                        .nuviaTactilePress()
                        .clickable(enabled = !isTesting) {
                            isTesting = true
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    JioSaavnService.testConnection()
                                }
                                testResult = res
                                isTesting = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = nuviaColors.primary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = nuviaColors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isTesting) "Testing connection…" else "Test connection",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                ),
                                color = nuviaColors.textPrimary,
                            )
                            Text(
                                text = if (isTesting) "Sending probe request to JioSaavn API…" else "Tap to check server reachability & latency",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Manrope,
                                    fontSize = 12.sp,
                                ),
                                color = nuviaColors.textSecondary,
                            )
                        }
                    }

                    if (!isTesting) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Test",
                            tint = nuviaColors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Test result details
                AnimatedVisibility(
                    visible = testResult != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    testResult?.let { res ->
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (res.success) Color(0xFF1B2E1D).copy(alpha = 0.7f)
                                    else Color(0xFF331414).copy(alpha = 0.7f),
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (res.success) Color(0xFF4CAF50).copy(alpha = 0.5f)
                                    else Color(0xFFE53935).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (res.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (res.success) Color(0xFF4CAF50) else Color(0xFFE53935),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (res.success) "Connected (HTTP ${res.httpStatus} OK)" else "Connection Failed",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = Sora,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        ),
                                        color = if (res.success) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (res.latencyMs > 0) {
                                        Text(
                                            text = "${res.latencyMs} ms",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = Manrope,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                            ),
                                            color = if (res.success) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                        )
                                    }
                                }

                                if (res.success) {
                                    Spacer(Modifier.height(6.dp))
                                    res.sampleTrack?.let { track ->
                                        Text(
                                            text = "Sample Query: $track",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = Manrope,
                                                fontSize = 12.sp,
                                            ),
                                            color = Color.White.copy(alpha = 0.85f),
                                        )
                                    }
                                    Text(
                                        text = "Stream Engine: Ready",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = Manrope,
                                            fontSize = 11.sp,
                                        ),
                                        color = Color.White.copy(alpha = 0.65f),
                                    )
                                } else {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = res.errorMessage ?: "Unknown error connecting to API endpoint",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = Manrope,
                                            fontSize = 12.sp,
                                        ),
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. REAL CAPABILITIES BREAKDOWN ---
        SettingsGroup(
            header = "Capabilities & features",
            footer = "These reflect the real capabilities provided by the integrated JioSaavn service in NUViA.",
        ) {
            CapabilityRow(
                icon = Icons.Rounded.Search,
                title = "Search Engine",
                subtitle = "Fast catalogue search & autocompletion",
                badge = "Active",
            )
            RowDivider()
            CapabilityRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Audio Streaming",
                subtitle = "High quality AAC / MP4 streams",
                badge = "High Quality",
            )
            RowDivider()
            CapabilityRow(
                icon = Icons.Rounded.Info,
                title = "Rich Metadata",
                subtitle = "Track titles, artist mappings & duration",
                badge = "Full Tagging",
            )
            RowDivider()
            CapabilityRow(
                icon = Icons.Rounded.Image,
                title = "Cover Artwork",
                subtitle = "High-resolution 500x500 album art",
                badge = "500x500 HD",
            )
        }

        // --- 5. SOURCE CONFIGURATION & TOGGLE ---
        SettingsGroup(
            header = "Source controls",
            footer = "When enabled, JioSaavn is queried during music search and track playback resolution.",
        ) {
            SettingsRow(
                icon = Icons.Rounded.CloudDone,
                title = "Enable JioSaavn source",
                subtitle = "Use JioSaavn for music search and playback",
                trailing = {
                    NUViASwitch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            jioSaavnConfig?.let {
                                SourceRegistry.setEnabled(it.id, checked)
                            }
                        },
                    )
                },
                onClick = {
                    jioSaavnConfig?.let {
                        SourceRegistry.setEnabled(it.id, !isEnabled)
                    }
                },
            )
        }

        // --- 6. PROVIDER HIERARCHY ---
        SettingsGroup(
            header = "Provider hierarchy",
            footer = "JioSaavn operates as your primary music source. If a track is unavailable on JioSaavn, YouTube Music acts as the secondary catalogue fallback.",
        ) {
            ProviderHierarchyRow(
                position = 1,
                name = "JioSaavn",
                role = "Primary Music Source",
                quality = "Up to 320kbps",
                icon = Icons.Rounded.GraphicEq,
                active = isEnabled,
            )
            RowDivider()
            ProviderHierarchyRow(
                position = 2,
                name = "YouTube Music",
                role = "Catalogue Fallback",
                quality = "Up to 171kbps Opus",
                icon = Icons.Rounded.PlayCircle,
                active = true,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CapabilityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(RoundedCornerShape(10.dp), width = 0.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = nuviaColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    fontSize = 12.sp,
                ),
                color = nuviaColors.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(nuviaColors.primary.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.primary,
            )
        }
    }
}

@Composable
private fun ProviderHierarchyRow(
    position: Int,
    name: String,
    role: String,
    quality: String,
    icon: ImageVector,
    active: Boolean,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (active) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (position == 1) nuviaColors.primary else nuviaColors.surfaceCard),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$position",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = if (position == 1) Color.White else nuviaColors.textSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(RoundedCornerShape(9.dp), width = 0.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (position == 1) nuviaColors.primary else nuviaColors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Text(
                text = "$role · $quality",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    fontSize = 12.sp,
                ),
                color = nuviaColors.textSecondary,
            )
        }
    }
}
