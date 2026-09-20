package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.nuvia.data.model.BrowseType
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.UserPlaylist
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.download.DownloadState
import com.music.nuvia.download.Downloads
import com.music.nuvia.ui.icons.NUViAIcons
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import java.util.Locale

/**
 * The album or playlist a long-press is acting on.
 */
data class BrowseTarget(
    /**
     * What to fetch the track list with, or null when there is nothing to fetch.
     */
    val browseId: String?,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val type: BrowseType = BrowseType.OTHER,
    /**
     * The tracks already in hand.
     */
    val songs: List<Song> = emptyList(),
    /**
     * Set when this is one of the account's own playlists.
     */
    val playlist: UserPlaylist? = null,
    /**
     * False when the menu was opened from the page it would otherwise navigate to.
     */
    val fromCard: Boolean = true,
    /**
     * The id this release is recorded under in Downloads.collections when downloaded.
     */
    val downloadId: String? = null,
)

/**
 * NUViA Liquid-Glass Contextual long-press menu for an album or playlist.
 */
@Composable
fun BrowseActionsSheet(
    target: BrowseTarget,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onDownloadAll: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var confirmingDeleteDownload by remember { mutableStateOf(false) }

    val playlist = target.playlist
    if (renaming && playlist != null && onRename != null) {
        RenamePlaylistForm(
            playlist = playlist,
            onBack = { renaming = false },
            onRename = onRename,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BrowseSheetHeader(target)
        HorizontalDivider(thickness = 0.5.dp, color = nuviaColors.glassBorder.copy(alpha = 0.4f))

        onPlay?.let { ActionRow(Icons.Rounded.PlayArrow, "Play", onClick = it) }
        onShuffle?.let { ActionRow(NUViAIcons.Shuffle, "Shuffle", onClick = it) }
        ActionRow(Icons.AutoMirrored.Rounded.PlaylistPlay, "Play next", onClick = onPlayNext)
        ActionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue", onClick = onAddToQueue)

        onDownloadAll?.let { download ->
            val active by Downloads.active.collectAsStateWithLifecycle()
            val requested by Downloads.requested.collectAsStateWithLifecycle()
            val saved by Downloads.saved.collectAsStateWithLifecycle()

            val waiting = target.browseId?.let { requested[it] }.orEmpty().any { id ->
                when (active[id]) {
                    is DownloadState.Queued, is DownloadState.Running -> true
                    else -> false
                }
            }

            val ids = remember(target.songs) { target.songs.mapTo(HashSet()) { it.videoId } }
            val downloaded = !waiting && ids.isNotEmpty() && ids.all { it in saved }

            ActionRow(
                icon = when {
                    waiting -> NUViAIcons.Clock
                    downloaded -> NUViAIcons.Check
                    else -> NUViAIcons.Download
                },
                label = "Download all",
                value = when {
                    waiting -> "Downloading"
                    downloaded -> "Downloaded"
                    else -> null
                },
                accent = nuviaColors.primary,
                onClick = download,
            )
        }

        onOpen?.let {
            ActionRow(NUViAIcons.ChevronRight, "Open ${target.type.noun}".trim(), onClick = it)
        }

        onTogglePin?.let {
            ActionRow(NUViAIcons.Pin, if (isPinned) "Unpin from Library" else "Pin to Library", onClick = it)
        }

        if (onRename != null) {
            ActionRow(Icons.Rounded.Edit, "Rename") { renaming = true }
        }

        if (onDelete != null) {
            if (confirmingDelete) {
                ActionRow(
                    icon = Icons.Rounded.DeleteForever,
                    label = "Delete \"${target.title}\" — tap to confirm",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            } else {
                ActionRow(Icons.Rounded.Delete, "Delete playlist") { confirmingDelete = true }
            }
        }

        if (onDeleteDownload != null) {
            if (confirmingDeleteDownload) {
                ActionRow(
                    icon = Icons.Rounded.DeleteForever,
                    label = "Remove \"${target.title}\" from device — tap to confirm",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteDownload,
                )
            } else {
                ActionRow(Icons.Rounded.Delete, "Delete download") { confirmingDeleteDownload = true }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Which release the sheet is about: header representation.
 */
@Composable
private fun BrowseSheetHeader(target: BrowseTarget) {
    val nuviaColors = LocalNUViAColors.current
    val shape = if (target.type == BrowseType.ARTIST) CircleShape else RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = target.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = target.title,
            modifier = Modifier
                .size(54.dp)
                .clip(shape)
                .thumbnailBorder(shape)
                .background(nuviaColors.surfaceCard),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = target.subtitle.ifBlank {
                    target.type.noun.replaceFirstChar { it.uppercase(Locale.ROOT) }.ifBlank {
                        target.songs.size.takeIf { it > 0 }
                            ?.let { "$it ${if (it == 1) "song" else "songs"}" }
                            .orEmpty()
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 13.sp,
                ),
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Formats the noun representation for a [BrowseType].
 */
private val BrowseType.noun: String
    get() = when (this) {
        BrowseType.ALBUM -> "album"
        BrowseType.PLAYLIST -> "playlist"
        BrowseType.ARTIST -> "artist"
        BrowseType.OTHER -> ""
    }
