package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

data class SelectionOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val badge: String? = null,
)

/**
 * Canonical NUViA Liquid Glass Selection Bottom Sheet.
 *
 * Provides a unified design language for modal selectors across NUViA:
 * - Dark translucent Liquid Glass container with real specular edge
 * - Pill drag handle
 * - Sora typography header with optional subtitle
 * - Clean option rows with subtle dividers and primary checkmarks
 * - Content-driven height and safe-area inset handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> NUViASelectionSheet(
    title: String,
    options: List<SelectionOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val nuviaColors = LocalNUViAColors.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF4101115),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
            }

            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Manrope,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        color = nuviaColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Options List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(nuviaColors.glassSurface)
                    .nuviaSpecularBorder(
                        shape = RoundedCornerShape(20.dp),
                        width = 0.75.dp,
                        topHighlight = nuviaColors.glassHighlight,
                        bottomBorder = nuviaColors.glassBorder,
                    ),
            ) {
                options.forEachIndexed { index, option ->
                    val isChosen = option.value == selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    onSelect(option.value)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (option.icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChosen) nuviaColors.primary.copy(alpha = 0.18f)
                                            else Color(0x14FFFFFF)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = if (isChosen) nuviaColors.primary else nuviaColors.textSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = Sora,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp,
                                        ),
                                        color = if (isChosen) nuviaColors.textPrimary else nuviaColors.textPrimary.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (option.badge != null) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(nuviaColors.primary.copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = option.badge,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = Sora,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                ),
                                                color = nuviaColors.primary,
                                            )
                                        }
                                    }
                                }
                                if (option.subtitle != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = Manrope,
                                            fontSize = 12.sp,
                                        ),
                                        color = nuviaColors.textMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        if (isChosen) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(nuviaColors.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = nuviaColors.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            color = Color(0x12FFFFFF),
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

