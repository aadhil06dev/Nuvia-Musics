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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.LibraryPage
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.data.model.UiState
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.MessageState
import com.music.nuvia.ui.components.PAGE_GUTTER
import com.music.nuvia.ui.components.PullToRefresh
import com.music.nuvia.ui.components.librarySkeleton
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.icons.NUViAIcons
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * The signed-in library: the saved collections, as shelves of cards.
 *
 * Deliberately only the collections. This page used to end with two runs of
 * track rows — "Liked Music" and "Songs" — which are two overlapping answers
 * to the same question and read as one list that couldn't make up its mind: a
 * track that stopped being liked didn't leave the page, it moved down it, into
 * a section most people had taken for more of the same. Liked Music is a
 * playlist, and it is reached the way every other playlist here is, by opening
 * its card.
 *
 * The liked list is still fetched — it is what the rest of the app reads a
 * track's rating off (see MainViewModel's `likeStatuses`); it just isn't a
 * second place to browse it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    listState: LazyListState,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val nuviaColors = LocalNUViAColors.current
    val pinnedList by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()

    val pinnedSet = remember(pinnedList) {
        val set = HashSet<String>(pinnedList.size * 3)
        for (id in pinnedList) {
            set.add(id)
            set.add(id.removePrefix("VL"))
            set.add("VL$id")
        }
        set
    }
    val isPinned: (ShelfItem) -> Boolean = remember(pinnedSet) {
        { item -> item.browseId != null && item.browseId in pinnedSet }
    }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.background(nuviaColors.surfaceOled),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "library_header", contentType = "library_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 18.dp)
                        .padding(horizontal = PAGE_GUTTER),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    letterSpacing = (-0.6).sp,
                                ),
                                color = nuviaColors.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Your personal collections, playlists & downloads.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                ),
                                color = nuviaColors.textSecondary,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(nuviaColors.primary.copy(alpha = 0.14f))
                                .nuviaSpecularBorder(RoundedCornerShape(12.dp), 0.75.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = nuviaColors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            item(key = "shelf:$ON_DEVICE", contentType = "on_device_shelf") {
                Shelf(
                    shelf = HomeShelf(
                        title = ON_DEVICE,
                        items = listOf(
                            ShelfItem(
                                title = "Downloads",
                                subtitle = "Downloaded songs",
                                thumbnailUrl = null,
                                videoId = null,
                                browseId = "local:downloads",
                            ),
                            ShelfItem(
                                title = "Local Music",
                                subtitle = "Audio files on device",
                                thumbnailUrl = null,
                                videoId = null,
                                browseId = "local:all",
                            ),
                            ShelfItem(
                                title = "History",
                                subtitle = "Recently played",
                                thumbnailUrl = null,
                                videoId = null,
                                browseId = "nuvia:history",
                            ),
                            ShelfItem(
                                title = "Replay",
                                subtitle = "Your listening recap",
                                thumbnailUrl = null,
                                videoId = null,
                                browseId = "nuvia:replay",
                            ),
                        ),
                    ),
                    onItemClick = onShelfItemClick,
                )
            }
            if (!signedIn) {
                item(key = "library_signin", contentType = "message_state") {
                    MessageState(
                        message = "Sign in to your Google account to see your YouTube Music " +
                            "liked songs, playlists and history.",
                        actionLabel = "Sign in",
                        onAction = onSignIn,
                    )
                }
                return@LazyColumn
            }
            when (state) {
                is UiState.Loading -> librarySkeleton()
                is UiState.Error -> item(key = "library_error", contentType = "message_state") {
                    MessageState(state.message, actionLabel = "Retry", onAction = onRetry)
                }
                is UiState.Success -> {
                    // A fresh account has no Playlists shelf at all, and that
                    // is exactly the account most in need of the button that
                    // makes one — so the row is drawn either way, empty but
                    // for the tile that creates the first playlist.
                    val shelves = state.data.shelves
                    if (shelves.none { it.title == PLAYLISTS }) {
                        item(key = "shelf:$PLAYLISTS", contentType = "playlist_shelf") {
                            PlaylistShelf(
                                shelf = HomeShelf(PLAYLISTS, emptyList()),
                                onItemClick = onShelfItemClick,
                                onItemLongPress = onShelfItemLongPress,
                                onNewPlaylist = onNewPlaylist,
                                isItemPinned = isPinned,
                                pinnedList = pinnedList,
                            )
                        }
                    }
                    shelves.forEach { shelf ->
                        item(
                            key = "shelf:${shelf.title}",
                            contentType = if (shelf.title == PLAYLISTS) "playlist_shelf" else "regular_shelf",
                        ) {
                            if (shelf.title == PLAYLISTS) {
                                PlaylistShelf(
                                    shelf = shelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onNewPlaylist = onNewPlaylist,
                                    isItemPinned = isPinned,
                                    pinnedList = pinnedList,
                                )
                            } else {
                                Shelf(
                                    shelf = shelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    isItemPinned = isPinned,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The one shelf on this page that can be written to: it leads with the tile
 * that creates a playlist, and holding a card opens the contextual actions menu.
 * Pinned playlists are reordered to the front.
 */
@Composable
private fun PlaylistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    isItemPinned: (ShelfItem) -> Boolean,
    pinnedList: List<String>,
) {
    val reorderedShelf = remember(shelf, pinnedList) {
        val (pinned, unpinned) = shelf.items.partition { isItemPinned(it) }
        val sortedPinned = pinned.sortedBy { item ->
            val id = item.browseId.orEmpty()
            val index1 = pinnedList.indexOf(id)
            val index2 = pinnedList.indexOf(id.removePrefix("VL"))
            val index3 = pinnedList.indexOf("VL$id")
            listOf(index1, index2, index3).filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        }
        shelf.copy(items = sortedPinned + unpinned)
    }

    Shelf(
        shelf = reorderedShelf,
        onItemClick = onItemClick,
        onItemLongPress = onItemLongPress,
        isItemPinned = isItemPinned,
        leadingCard = {
            NewShelfCard(
                icon = NUViAIcons.Plus,
                label = "New playlist",
                subtitle = "Saved to YouTube Music",
                onClick = onNewPlaylist,
            )
        },
    )
}

/** The library feed whose cards are the account's own — see [PlaylistShelf]. */
private const val PLAYLISTS = "Playlists"
private const val ON_DEVICE = "On Device"
