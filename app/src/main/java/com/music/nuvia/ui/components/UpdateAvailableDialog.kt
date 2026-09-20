package com.music.nuvia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.data.AppUpdateChecker
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import dev.chrisbanes.haze.HazeState

private val UPDATE_ALERT_CORNER = 24.dp
private val UPDATE_SCRIM_COLOR = Color.Black.copy(alpha = 0.5f)

/**
 * NUViA Liquid Glass Update Available Dialog.
 */
@Composable
fun UpdateAvailableDialog(
    version: String,
    notes: String?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()
    val state by AppUpdateChecker.download.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(UPDATE_ALERT_CORNER)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(UPDATE_SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(shape)
                .nuviaGlass(
                    tier = NUViAGlassTier.Elevated,
                    shape = shape,
                    hazeState = hazeState,
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(nuviaColors.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = nuviaColors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.software_update),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = nuviaColors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            val bodyText = when (state) {
                is AppUpdateChecker.DownloadState.Downloading ->
                    stringResource(R.string.update_downloading_body, version)
                is AppUpdateChecker.DownloadState.Ready ->
                    stringResource(R.string.update_ready_body, version)
                is AppUpdateChecker.DownloadState.Failed ->
                    stringResource(R.string.update_failed_body, version)
                else ->
                    stringResource(R.string.update_available_body, version)
            }

            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    lineHeight = 20.sp,
                ),
                color = nuviaColors.textSecondary,
                textAlign = TextAlign.Center,
            )

            // Progress bar during download
            if (state is AppUpdateChecker.DownloadState.Downloading) {
                val fraction = (state as AppUpdateChecker.DownloadState.Downloading).fraction
                val animatedFraction by animateFloatAsState(targetValue = fraction, label = "update_progress")
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = nuviaColors.primary,
                    trackColor = nuviaColors.surfaceCard,
                )
            }

            // Release Notes (if any)
            if (!notes.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(nuviaColors.surfaceOled.copy(alpha = 0.5f))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(R.string.whats_new),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = nuviaColors.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notes.trim(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Manrope,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                        color = nuviaColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Buttons
            when (state) {
                is AppUpdateChecker.DownloadState.Ready -> {
                    LiquidGlassButton(
                        text = stringResource(R.string.install_now),
                        onClick = {
                            haptics.play(Haptic.Select)
                            onInstall()
                        },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    )
                }
                is AppUpdateChecker.DownloadState.Downloading -> {
                    LiquidGlassButton(
                        text = stringResource(R.string.cancel),
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onCancelDownload()
                        },
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    )
                }
                is AppUpdateChecker.DownloadState.Failed -> {
                    LiquidGlassButton(
                        text = stringResource(R.string.try_again),
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onDownload()
                        },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    )
                }
                else -> {
                    LiquidGlassButton(
                        text = stringResource(R.string.download_now),
                        onClick = {
                            haptics.play(Haptic.Select)
                            onDownload()
                        },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LiquidGlassButton(
                text = stringResource(R.string.remind_me_later),
                onClick = {
                    haptics.play(Haptic.Tap)
                    onDismiss()
                },
                isPrimary = false,
                modifier = Modifier.fillMaxWidth().height(42.dp),
            )
        }
    }
}

