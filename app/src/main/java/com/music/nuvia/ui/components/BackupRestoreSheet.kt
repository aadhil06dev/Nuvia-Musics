package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.data.backup.BackupSummary
import com.music.nuvia.data.backup.RestoreOptions
import com.music.nuvia.data.backup.RestoreStrategy
import com.music.nuvia.data.backup.ValidationResult
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * AMOLED + Liquid-Glass modal bottom sheet for validating, configuring,
 * and restoring NUViA backup files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(
    validationResult: ValidationResult,
    onDismiss: () -> Unit,
    onRestoreConfirmed: (RestoreOptions) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val nuviaColors = LocalNUViAColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = nuviaColors.glassSurface,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(nuviaColors.primary.copy(alpha = 0.15f))
                            .nuviaSpecularBorder(RoundedCornerShape(12.dp), 0.75.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (validationResult is ValidationResult.Success) {
                                Icons.Rounded.Restore
                            } else {
                                Icons.Rounded.ErrorOutline
                            },
                            contentDescription = null,
                            tint = if (validationResult is ValidationResult.Success) {
                                nuviaColors.primary
                            } else {
                                Color(0xFFFF5252)
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column {
                        Text(
                            text = if (validationResult is ValidationResult.Success) {
                                "Restore Backup"
                            } else {
                                "Invalid Backup File"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = Sora,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = nuviaColors.textPrimary,
                        )
                        Text(
                            text = if (validationResult is ValidationResult.Success) {
                                "Review and select items to restore"
                            } else {
                                "The file could not be parsed or validated"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Manrope,
                                color = nuviaColors.textSecondary,
                            ),
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(nuviaColors.glassSurface),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = nuviaColors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            when (validationResult) {
                is ValidationResult.Success -> {
                    SuccessContent(
                        result = validationResult,
                        onRestore = onRestoreConfirmed,
                        onCancel = onDismiss,
                    )
                }
                is ValidationResult.Error -> {
                    ErrorContent(
                        error = validationResult,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    result: ValidationResult.Success,
    onRestore: (RestoreOptions) -> Unit,
    onCancel: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val summary = result.summary

    var strategy by remember { mutableStateOf(RestoreStrategy.MERGE) }
    var restoreSettings by remember { mutableStateOf(summary.hasSettings) }
    var restorePlaylists by remember { mutableStateOf(summary.playlistCount > 0) }
    var restorePinned by remember { mutableStateOf(summary.pinnedPlaylistCount > 0) }
    var restoreHistory by remember { mutableStateOf(summary.searchHistoryCount > 0) }

    // Backup Origin & Date Badge
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${summary.appName} v${summary.appVersion}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    color = nuviaColors.primary,
                )
                Text(
                    text = "• Format v${summary.formatVersion}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        color = nuviaColors.textMuted,
                    ),
                )
            }
            Text(
                text = "Exported on ${summary.exportDateFormatted}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    color = nuviaColors.textSecondary,
                ),
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // Summary statistics grid
    Text(
        text = "BACKUP CONTENTS",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = Sora,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        ),
        color = nuviaColors.textMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp)
            .padding(12.dp),
    ) {
        StatRow(
            icon = Icons.Rounded.PlaylistPlay,
            label = "Playlists",
            value = "${summary.playlistCount} (${summary.totalTrackCount} songs)",
        )
        StatRow(
            icon = Icons.Rounded.Pin,
            label = "Pinned Playlists",
            value = "${summary.pinnedPlaylistCount}",
        )
        StatRow(
            icon = Icons.Rounded.History,
            label = "Search History",
            value = "${summary.searchHistoryCount} searches",
        )
        StatRow(
            icon = Icons.Rounded.Settings,
            label = "Preferences & Settings",
            value = if (summary.hasSettings) "Included" else "None",
        )
    }

    // Duplicate detection warning if any
    if (result.duplicatePlaylists.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1AFFB300))
                .border(0.75.dp, Color(0x4DFFB300), RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = "${result.duplicatePlaylists.size} Duplicate Playlist(s) Detected",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = Color(0xFFFFE082),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "The 'Merge' strategy is recommended to keep existing library playlists safe without overwriting.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Manrope,
                            fontSize = 12.sp,
                            color = Color(0xFFFFF8E1).copy(alpha = 0.8f),
                        ),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Restore Strategy Selection
    Text(
        text = "RESTORATION STRATEGY",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = Sora,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        ),
        color = nuviaColors.textMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp)
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { strategy = RestoreStrategy.MERGE }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            RadioButton(
                selected = strategy == RestoreStrategy.MERGE,
                onClick = { strategy = RestoreStrategy.MERGE },
                colors = RadioButtonDefaults.colors(
                    selectedColor = nuviaColors.primary,
                    unselectedColor = nuviaColors.textMuted,
                ),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Merge with existing data (Recommended)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Text(
                    text = "Preserves current playlists and adds new items from backup",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { strategy = RestoreStrategy.OVERWRITE }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            RadioButton(
                selected = strategy == RestoreStrategy.OVERWRITE,
                onClick = { strategy = RestoreStrategy.OVERWRITE },
                colors = RadioButtonDefaults.colors(
                    selectedColor = nuviaColors.primary,
                    unselectedColor = nuviaColors.textMuted,
                ),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Replace existing data",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Text(
                    text = "Replaces playlists and settings with the backup file",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Selective Restores Checkboxes
    Text(
        text = "ITEMS TO RESTORE",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = Sora,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        ),
        color = nuviaColors.textMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp)
            .padding(vertical = 4.dp),
    ) {
        RestoreCheckboxRow(
            title = "Settings & Preferences",
            subtitle = "Audio quality, theme, and playback configuration",
            checked = restoreSettings,
            enabled = summary.hasSettings,
            onCheckedChange = { restoreSettings = it },
        )
        RestoreCheckboxRow(
            title = "Playlists & Track Metadata",
            subtitle = "${summary.playlistCount} playlist(s) with track listings",
            checked = restorePlaylists,
            enabled = summary.playlistCount > 0,
            onCheckedChange = { restorePlaylists = it },
        )
        RestoreCheckboxRow(
            title = "Pinned Playlists",
            subtitle = "${summary.pinnedPlaylistCount} pinned library items",
            checked = restorePinned,
            enabled = summary.pinnedPlaylistCount > 0,
            onCheckedChange = { restorePinned = it },
        )
        RestoreCheckboxRow(
            title = "Search History",
            subtitle = "${summary.searchHistoryCount} recent search terms",
            checked = restoreHistory,
            enabled = summary.searchHistoryCount > 0,
            onCheckedChange = { restoreHistory = it },
        )
    }

    Spacer(Modifier.height(24.dp))

    // Action buttons
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = nuviaColors.glassSurface,
                contentColor = nuviaColors.textPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp),
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = Sora),
            )
        }

        val hasSelectedItems = restoreSettings || restorePlaylists || restorePinned || restoreHistory
        Button(
            onClick = {
                onRestore(
                    RestoreOptions(
                        strategy = strategy,
                        restoreSettings = restoreSettings,
                        restorePlaylists = restorePlaylists,
                        restorePinnedPlaylists = restorePinned,
                        restoreSearchHistory = restoreHistory,
                    )
                )
            },
            enabled = hasSelectedItems,
            colors = ButtonDefaults.buttonColors(
                containerColor = nuviaColors.primary,
                contentColor = nuviaColors.onPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp)
                .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Restore Now",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: ValidationResult.Error,
    onDismiss: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AFF5252))
            .border(0.75.dp, Color(0x4DFF5252), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Validation Failed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color(0xFFFF8A80),
            )
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                ),
                color = Color(0xFFFFCDD2),
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
            containerColor = nuviaColors.glassSurface,
            contentColor = nuviaColors.textPrimary,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .nuviaSpecularBorder(RoundedCornerShape(14.dp), 0.75.dp),
    ) {
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Sora),
        )
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = nuviaColors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Manrope),
                color = nuviaColors.textSecondary,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontWeight = FontWeight.SemiBold,
            ),
            color = nuviaColors.textPrimary,
        )
    }
}

@Composable
private fun RestoreCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Checkbox(
            checked = checked && enabled,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = nuviaColors.primary,
                uncheckedColor = nuviaColors.textMuted,
                checkmarkColor = nuviaColors.onPrimary,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (enabled) nuviaColors.textPrimary else nuviaColors.textMuted,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                ),
                color = if (enabled) nuviaColors.textSecondary else nuviaColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
