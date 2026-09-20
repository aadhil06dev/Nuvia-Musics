package com.music.nuvia.ui.screens

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.playback.AudioCache
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Sora
import com.music.nuvia.ui.components.nuviaSpecularBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeveloperDiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val nuviaColors = LocalNUViAColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var cacheBytes by remember { mutableLongStateOf(AudioCache.currentSizeBytes()) }
    var clearedMessage by remember { mutableStateOf<String?>(null) }

    val audioSink = remember { detectAudioSink(context) }
    val pcmMode by AppSettings.outputPcmMode.collectAsStateWithLifecycle()
    val cacheLimitMb by AppSettings.audioCacheLimitMb.collectAsStateWithLifecycle()
    val glassEffects by AppSettings.glassEffects.collectAsStateWithLifecycle()
    val liquidGlassBlur by AppSettings.liquidGlassBlur.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()
    val eqEnabled by AppSettings.equalizerEnabled.collectAsStateWithLifecycle()
    val eqMode by AppSettings.equalizerMode.collectAsStateWithLifecycle()
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val dynamicColors by AppSettings.dynamicArtworkColors.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
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
                    text = "Developer & Diagnostics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Text(
                    text = "Hardware & Runtime Telemetry",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Sora,
                        fontSize = 11.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Device & Platform Section
        DiagnosticsSection(title = "DEVICE & PLATFORM") {
            DiagnosticsRow(label = "Device", value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
            DiagnosticsRow(label = "Android Version", value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            DiagnosticsRow(label = "Hardware / Board", value = "${Build.HARDWARE} (${Build.BOARD})")
            DiagnosticsRow(label = "Supported ABIs", value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
        }

        Spacer(Modifier.height(18.dp))

        // Graphics & Shaders Section
        DiagnosticsSection(title = "GRAPHICS & GLASS PIPELINE") {
            val hasHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            DiagnosticsRow(
                label = "RenderEffect Blur (API 31+)",
                value = if (hasHardwareBlur) "Supported (Hardware)" else "Unsupported (Fallback to Dim Surface)",
                valueColor = if (hasHardwareBlur) Color(0xFF10B981) else Color(0xFFF59E0B),
            )
            DiagnosticsRow(label = "Liquid Glass Blur", value = if (liquidGlassBlur) "Active" else "Disabled")
            DiagnosticsRow(label = "Glass Refraction Surfaces", value = if (glassEffects) "Enabled" else "Disabled")
            DiagnosticsRow(label = "Reduce Dynamic Blur", value = if (reduceDynamicBlur) "On (Solid Fills)" else "Off (Glass Transparency)")
            DiagnosticsRow(label = "Minimal UI Mode", value = if (minimalUi) "Active (Monochrome)" else "Inactive")
            DiagnosticsRow(label = "Dynamic Artwork Palette", value = if (dynamicColors) "Dynamic" else "Custom Palette")
        }

        Spacer(Modifier.height(18.dp))

        // Audio Routing & Engine Section
        DiagnosticsSection(title = "AUDIO ENGINE & ROUTING") {
            DiagnosticsRow(label = "Active Audio Sink", value = audioSink)
            DiagnosticsRow(label = "Output PCM Mode", value = pcmMode.name)
            DiagnosticsRow(label = "Spatial Audio Processor", value = if (spatialAudio) "Active (Widening)" else "Stereo Passthrough")
            DiagnosticsRow(label = "Hardware Equalizer", value = if (eqEnabled) "Enabled (${eqMode.name})" else "Bypassed")
        }

        Spacer(Modifier.height(18.dp))

        // Cache & Storage Section
        DiagnosticsSection(title = "STORAGE & AUDIO CACHE") {
            val cacheMb = cacheBytes / (1024 * 1024)
            DiagnosticsRow(label = "Current Disk Cache", value = "$cacheMb MB")
            DiagnosticsRow(label = "Allocated Cache Budget", value = "$cacheLimitMb MB")

            Spacer(Modifier.height(12.dp))

            // Clear Cache Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFF4D4D))
                    .clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                AudioCache.clear()
                            }
                            cacheBytes = AudioCache.currentSizeBytes()
                            clearedMessage = "Cache cleared"
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = clearedMessage ?: "Clear Audio Cache",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFFFF6B6B),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 32.dp))
    }
}

@Composable
private fun DiagnosticsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp,
            ),
            color = nuviaColors.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(18.dp),
                    width = 0.75.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Sora,
                fontSize = 13.sp,
            ),
            color = nuviaColors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = valueColor ?: nuviaColors.textPrimary,
        )
    }
}

private fun detectAudioSink(context: Context): String {
    val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Default Speaker"
    val outputs = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    for (dev in outputs) {
        when (dev.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> return "Bluetooth (${dev.productName})"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return "Wired Headphones (3.5mm)"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> return "USB-C Audio (${dev.productName})"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return "Built-in Speaker"
        }
    }
    return "Built-in Speaker"
}

