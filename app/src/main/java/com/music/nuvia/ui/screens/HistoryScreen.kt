package com.music.nuvia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.UiState
import com.music.nuvia.ui.components.MessageState
import com.music.nuvia.ui.components.NUViAGlassButton
import com.music.nuvia.ui.components.ROW_DIVIDER_INSET
import com.music.nuvia.ui.components.SongRow
import com.music.nuvia.ui.components.songListSkeleton
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * NUViA Listening History.
 * Chronological timeline of played tracks, supporting online and offline listening.
 */
@Composable
fun HistoryScreen(
    state: UiState<List<Song>>,
    listState: LazyListState,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClearHistory: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled),
        contentPadding = contentPadding,
    ) {
        when (state) {
            is UiState.Loading -> songListSkeleton(count = 12, keyPrefix = "skeleton:history")

            is UiState.Error -> item(key = "history:error") {
                MessageState(
                    message = state.message,
                    actionLabel = "Try again",
                    onAction = onRetry,
                )
            }

            is UiState.Success -> {
                val songs = state.data
                if (songs.isEmpty()) {
                    item(key = "history:empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No listening history yet.\nPlay some music to see it here.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Manrope,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                ),
                                color = nuviaColors.textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else {
                    if (onClearHistory != null) {
                        item(key = "history:header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                NUViAGlassButton(
                                    text = "Clear History",
                                    onClick = onClearHistory,
                                    isPrimary = false,
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = songs,
                        key = { index, song -> "${song.videoId}_$index" },
                    ) { index, song ->
                        SongRow(
                            song = song,
                            onClick = { onSongClick(songs, index) },
                            onLongPress = { onSongLongPress(song) },
                            onSwipeToQueue = { onSongSwipe(song) },
                        )
                        if (index < songs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                thickness = 0.5.dp,
                                color = nuviaColors.divider,
                            )
                        }
                    }
                }
            }
        }
    }
}

