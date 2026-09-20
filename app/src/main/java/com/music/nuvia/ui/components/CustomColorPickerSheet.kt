package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Sora
import com.music.nuvia.ui.theme.Manrope

private data class ColorPreset(val name: String, val color: Int)

private val PRESET_SWATCHES = listOf(
    ColorPreset("NUViA Orange", 0xFFE85D04.toInt()),
    ColorPreset("Red", 0xFFE63946.toInt()),
    ColorPreset("Pink", 0xFFF72585.toInt()),
    ColorPreset("Purple", 0xFF9D4EDD.toInt()),
    ColorPreset("Blue", 0xFF3B82F6.toInt()),
    ColorPreset("Cyan", 0xFF00B4D8.toInt()),
    ColorPreset("Green", 0xFF10B981.toInt()),
    ColorPreset("Amber", 0xFFFFB703.toInt()),
    ColorPreset("White", 0xFFFFFFFF.toInt()),
    ColorPreset("Soft White", 0xFFF8FAFC.toInt()),
    ColorPreset("Silver", 0xFFE2E8F0.toInt()),
    ColorPreset("Soft Gray", 0xFF94A3B8.toInt()),
    ColorPreset("Black", 0xFF121318.toInt()),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerSheet(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialHsl = remember(initialColor) {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(initialColor, out)
        out
    }

    var hue by remember { mutableFloatStateOf(initialHsl[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsl[1]) }
    var lightness by remember { mutableFloatStateOf(initialHsl[2]) }

    val currentColor = remember(hue, saturation, lightness) {
        val hsl = floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
        Color(ColorUtils.HSLToColor(hsl))
    }

    val hexString = remember(currentColor) {
        String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
    }

    val contrastWithBlack = remember(currentColor) {
        ColorUtils.calculateContrast(currentColor.toArgb(), 0xFF000000.toInt())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF4101115),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            // Drag pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(16.dp))

            // Header
            Text(
                text = "Custom Accent Color",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Text(
                text = "Applied across NUViA when Dynamic Artwork Colors is turned off",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    fontSize = 13.sp,
                ),
                color = nuviaColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            // Color Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(currentColor)
                    .nuviaSpecularBorder(
                        shape = RoundedCornerShape(16.dp),
                        width = 1.dp,
                        topHighlight = Color(0x60FFFFFF),
                        bottomBorder = Color(0x30000000),
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val onPreviewTextColor = if (contrastWithBlack > 4.5) Color.Black else Color.White
                    Column {
                        Text(
                            text = hexString,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Sora,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = onPreviewTextColor,
                        )
                        Text(
                            text = if (contrastWithBlack >= 4.5) "WCAG AA Contrast: ${String.format("%.1f", contrastWithBlack)}:1" else "Low contrast against black",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Manrope,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = onPreviewTextColor.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Preset swatches
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PRESET_SWATCHES.forEach { preset ->
                    val isSelected = (currentColor.toArgb() and 0xFFFFFF) == (preset.color and 0xFFFFFF)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(preset.color))
                            .clickable {
                                val out = FloatArray(3)
                                ColorUtils.colorToHSL(preset.color, out)
                                hue = out[0]
                                saturation = out[1]
                                lightness = out[2]
                            }
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x30FFFFFF),
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hue Slider
            Text(
                text = "HUE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.textMuted,
            )
            Slider(
                value = hue,
                onValueChange = { hue = it },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = currentColor,
                    inactiveTrackColor = Color(0x33FFFFFF),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Saturation Slider
            Text(
                text = "SATURATION (${(saturation * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.textMuted,
            )
            Slider(
                value = saturation,
                onValueChange = { saturation = it },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = currentColor,
                    inactiveTrackColor = Color(0x33FFFFFF),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Lightness Slider
            Text(
                text = "LIGHTNESS (${(lightness * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.textMuted,
            )
            Slider(
                value = lightness,
                onValueChange = { lightness = it },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = currentColor,
                    inactiveTrackColor = Color(0x33FFFFFF),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0x1AFFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = nuviaColors.textPrimary,
                    )
                }

                // Apply
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(currentColor)
                        .clickable {
                            onColorSelected(currentColor.toArgb())
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val onButtonTextColor = if (contrastWithBlack > 4.5) Color.Black else Color.White
                    Text(
                        text = "Apply Color",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = onButtonTextColor,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
