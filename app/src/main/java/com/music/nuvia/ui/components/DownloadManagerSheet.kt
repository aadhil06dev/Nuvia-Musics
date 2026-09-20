package com.music.nuvia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.download.DownloadProgress
import com.music.nuvia.download.DownloadSession
import com.music.nuvia.download.Downloads
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * NUViA Liquid-Glass Top Bar Download Button.
 *
 * Appears dynamically when active downloads or unviewed settled batches are present.
 * Shows an animated circular progress indicator tracking the aggregate session fraction.
 */
@Composable
fun TopBarDownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session by DownloadSession.state.collectAsStateWithLifecycle()
    if (!session.visible) return

    val nuviaColors = LocalNUViAColors.current
    val progress by animateFloatAsState(
        targetValue = session.fraction,
        animationSpec = tween(300),
        label = "downloadRingProgress",
    )
    val failed = session.failed > 0
    val tint by animateColorAsState(
        targetValue = when {
            failed -> MaterialTheme.colorScheme.error
            else -> nuviaColors.primary
        },
        animationSpec = tween(220),
        label = "downloadRingTint",
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.nuviaTactilePress(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (session.busy) {
                CircularProgressIndicator(
                    progress = { progress.coerceAtLeast(0.02f) },
                    modifier = Modifier.size(RING_SIZE),
                    color = tint,
                    trackColor = tint.copy(alpha = 0.22f),
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
            }
            Icon(
                imageVector = when {
                    failed -> Icons.Rounded.ErrorOutline
                    session.busy -> Icons.Rounded.Downloading
                    else -> Icons.Rounded.DownloadDone
                },
                contentDescription = when {
                    session.busy -> "Downloads · ${(session.fraction * 100).toInt()}%"
                    failed -> "Downloads · ${session.failed} failed"
                    else -> "Downloads · finished"
                },
                tint = tint,
                modifier = Modifier.size(if (session.busy) GLYPH_IN_RING else GLYPH_SIZE),
            )
        }
    }
}

/**
 * NUViA Liquid-Glass Download Manager Sheet.
 *
 * Displays active and recent session downloads with individual progress, retry, cancel,
 * and aggregate session controls.
 */
@Composable
fun DownloadManagerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nuviaColors = LocalNUViAColors.current
    val session by DownloadSession.state.collectAsStateWithLifecycle()
    val items = remember(session.items) { session.items.sortedBy { it.sequence } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = session.summary(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                    ),
                    color = if (session.failed > 0 && !session.busy) {
                        MaterialTheme.colorScheme.error
                    } else {
                        nuviaColors.textSecondary
                    },
                )
            }
            if (session.busy) {
                TextButton(
                    onClick = {
                        items.filterNot { it.progress.settled }
                            .forEach { Downloads.cancel(it.videoId) }
                    },
                ) {
                    Text(
                        text = "Cancel all",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = nuviaColors.primary,
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        DownloadSession.clear()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = nuviaColors.primary,
                    )
                }
            }
        }

        if (session.busy) {
            LinearProgressIndicator(
                progress = { session.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .height(3.dp),
                color = nuviaColors.primary,
                trackColor = nuviaColors.primary.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
        }

        HorizontalDivider(thickness = 0.5.dp, color = nuviaColors.divider)

        LazyColumn(Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
            items(
                items = items,
                key = { it.videoId },
                contentType = { "download_row" },
            ) { item ->
                DownloadManagerRow(
                    item = item,
                    onCancel = { Downloads.cancel(item.videoId) },
                    onRetry = { Downloads.enqueue(context, item.song, item.from) },
                )
            }
        }
    }
}

@Composable
private fun DownloadManagerRow(
    item: DownloadSession.Item,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val progress = item.progress
    val failed = progress as? DownloadProgress.Failed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(ART_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(ART_SIZE)
                    .clip(RoundedCornerShape(8.dp))
                    .thumbnailBorder(RoundedCornerShape(8.dp))
                    .background(nuviaColors.glassSurfaceElevated),
            )
            if (progress.settled) {
                Box(
                    modifier = Modifier
                        .size(ART_SIZE)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.50f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (failed != null) {
                            Icons.Rounded.ErrorOutline
                        } else {
                            Icons.Rounded.DownloadDone
                        },
                        contentDescription = null,
                        tint = if (failed != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = listOfNotNull(
                    item.song.artist.takeIf { it.isNotBlank() },
                    item.from?.takeIf { it.isNotBlank() && it != item.song.artist },
                ).joinToString(" · ").ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 12.sp,
                ),
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            when (progress) {
                is DownloadProgress.Queued -> RowStatus("Queued")
                is DownloadProgress.Running -> {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = nuviaColors.primary,
                        trackColor = nuviaColors.primary.copy(alpha = 0.18f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(3.dp))
                    RowStatus(
                        if (progress.fraction > 0f) {
                            "Downloading · ${(progress.fraction * 100).toInt()}%"
                        } else {
                            "Starting"
                        },
                    )
                }
                is DownloadProgress.Done -> RowStatus("Saved to offline storage")
                is DownloadProgress.Failed ->
                    RowStatus(progress.reason, MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            failed != null -> RowAction(Icons.Rounded.Refresh, "Retry", onRetry)
            !progress.settled -> RowAction(Icons.Rounded.Close, "Cancel", onCancel)
            else -> Spacer(Modifier.width(36.dp))
        }
    }
}

@Composable
private fun RowStatus(
    text: String,
    color: Color = LocalNUViAColors.current.textSecondary,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = Manrope,
            fontSize = 11.sp,
        ),
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(CircleShape, 0.75.dp)
            .nuviaTactilePress(onClick = onClick)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = nuviaColors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun DownloadSession.State.summary(): String {
    val parts = buildList {
        if (waiting > 0) add("$waiting waiting")
        if (finished > 0) add("$finished done")
        if (failed > 0) add("$failed failed")
    }
    return when {
        parts.isEmpty() -> "Nothing downloading"
        busy -> parts.joinToString(" · ")
        failed > 0 -> parts.joinToString(" · ")
        else -> "All $finished ${if (finished == 1) "song" else "songs"} downloaded"
    }
}

private val RING_SIZE = 26.dp
private val GLYPH_IN_RING = 15.dp
private val GLYPH_SIZE = 22.dp
private val ART_SIZE = 44.dp
private val LIST_MAX_HEIGHT = 380.dp
