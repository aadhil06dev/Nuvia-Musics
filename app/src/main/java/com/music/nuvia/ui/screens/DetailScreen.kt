package com.music.nuvia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Verified
import com.music.nuvia.data.artist.ArtistFacts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.nuvia.data.canvas.CanvasArtwork
import com.music.nuvia.data.canvas.CanvasRepository
import com.music.nuvia.data.model.BrowseType
import com.music.nuvia.data.model.DetailPage
import com.music.nuvia.data.model.CARD_ART_PX
import com.music.nuvia.data.model.HEADER_ART_PX
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.UiState
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.BrowseTarget
import com.music.nuvia.ui.components.MessageState
import com.music.nuvia.ui.components.PAGE_GUTTER
import com.music.nuvia.ui.components.ROW_DIVIDER_INSET
import com.music.nuvia.ui.components.SHELF_CARD_WIDTH
import com.music.nuvia.ui.components.detailSkeleton
import com.music.nuvia.ui.icons.NUViAIcons
import com.music.nuvia.ui.player.CanvasArtworkPlayer
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import com.music.nuvia.ui.theme.rememberArtworkPalette
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_ARTIST_SONGS = 20
private const val SONGS_PER_COLUMN = 4
private const val ARTIST_PHOTO_RATIO = 0.92f
private val HEADER_GUTTER = 20.dp

/**
 * Redesigned NUViA Playlist / Album / Artist Detail Experience.
 *
 * Embodying the AMOLED Black + Liquid Glass + Cinematic Dynamic Atmosphere aesthetic:
 * - Ultra deep true OLED canvas (#000000)
 * - Atmospheric dynamic color glow responding to current/playlist artwork
 * - Floating hero cover with multi-layered specular borders and ambient luminescence
 * - Elevated Sora & Manrope typography hierarchy
 * - Floating Liquid-Glass action pill and circular tactile control buttons
 * - Polished track listing with active playing track equalizer state
 */
@Composable
fun DetailScreen(
    page: DetailPage,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onSectionItemClick: (ShelfItem) -> Unit,
    onDownloadAll: (List<Song>) -> Unit,
    onArtistClick: (String, String) -> Unit,
    onAddSuggested: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onToggleLibrary: (() -> Unit)? = null,
    currentPlayingSongId: String? = null,
    isPlaying: Boolean = false,
    onOpenBrowseActions: ((BrowseTarget) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val songs = (page.songs as? UiState.Success)?.data.orEmpty()
    val isArtist = page.type == BrowseType.ARTIST
    val topSongs = remember(page.songs) {
        (page.songs as? UiState.Success)?.data?.take(MAX_ARTIST_SONGS).orEmpty()
    }
    val topSongColumns = remember(topSongs) {
        topSongs.chunked(SONGS_PER_COLUMN)
    }
    val topSongIndexMap = remember(topSongs) {
        topSongs.mapIndexed { idx, song -> song.videoId to idx }.toMap()
    }
    val trackKeys = remember(songs) {
        val counts = mutableMapOf<String, Int>()
        songs.map { song ->
            val count = counts.getOrDefault(song.videoId, 0)
            counts[song.videoId] = count + 1
            if (count == 0) "track_${song.videoId}" else "track_${song.videoId}_$count"
        }
    }
    val pagePalette = rememberArtworkPalette(page.thumbnailUrl)

    val canvasEnabled by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val playNext by AppSettings.swipeToPlayNext.collectAsStateWithLifecycle()
    val credit = remember(page.subtitle, songs) {
        page.headerLines(songs.size).first.ifBlank { songs.firstOrNull()?.artist.orEmpty() }
    }
    var canvas by remember(page.browseId) { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(page.browseId, page.title, credit, canvasEnabled) {
        if (!canvasEnabled || page.type != BrowseType.ALBUM) {
            canvas = null
            return@LaunchedEffect
        }
        canvas = CanvasRepository.canvasForAlbum(page.title, credit) ?: canvas
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val heroArtHeight = if (isArtist) screenWidth / ARTIST_PHOTO_RATIO else 0.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled),
    ) {
        // Atmospheric Dynamic Artwork Glow
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Dynamic top atmospheric wash
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                nuviaColors.primary.copy(alpha = 0.22f),
                                nuviaColors.ambientGlow.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // If Artist, render the edge-to-edge artist hero backdrop with smooth scrim
            if (isArtist) {
                val context = LocalContext.current
                val heroArtRequest = remember(page.thumbnailUrl) {
                    ImageRequest.Builder(context)
                        .data(page.thumbnailUrl.artworkAt(HEADER_ART_PX))
                        .crossfade(false)
                        .build()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroArtHeight)
                        .offset { IntOffset(0, listState.headerTop(heroArtHeight.toPx()).roundToInt()) },
                ) {
                    AsyncImage(
                        model = heroArtRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    // Multi-stop cinematic gradient to deep black
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.40f),
                                    0.45f to Color.Transparent,
                                    0.80f to nuviaColors.surfaceOled.copy(alpha = 0.75f),
                                    1.0f to nuviaColors.surfaceOled,
                                ),
                            ),
                    )
                }
            }
        }

        // Main Scrollable Content
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (isArtist) 0.dp else 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            // Header
            item(key = "header") {
                if (isArtist) {
                    ArtistHeroHeader(
                        page = page,
                        heroHeight = heroArtHeight,
                        songs = songs,
                        onPlay = { onSongClick(songs, 0) },
                        onShuffle = { onShuffle(songs) },
                        onOpenBrowseActions = onOpenBrowseActions,
                    )
                } else {
                    PlaylistHeroHeader(
                        page = page,
                        songs = songs,
                        canvas = canvas,
                        onPlay = { onSongClick(songs, 0) },
                        onShuffle = { onShuffle(songs) },
                        onDownload = onDownloadAll.takeUnless { page.browseId.startsWith("local:") },
                        onArtistClick = onArtistClick,
                        onToggleLibrary = onToggleLibrary,
                        onOpenBrowseActions = onOpenBrowseActions,
                    )
                }
            }

            // Songs List State
            when (val state = page.songs) {
                is UiState.Loading -> detailSkeleton(isArtist)
                is UiState.Error -> item {
                    MessageState(
                        message = state.message,
                        actionLabel = if (onRetry != null) "Retry" else null,
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> if (isArtist) {
                    // Artist top songs
                    item(key = "artist_popular_tracks", contentType = "artist_popular_tracks") {
                        DetailSectionHeading("Popular Tracks")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                items = topSongColumns,
                                key = { colIndex, colSongs ->
                                    colSongs.firstOrNull()?.videoId ?: "col_$colIndex"
                                },
                                contentType = { _, _ -> "popular_track_column" },
                            ) { _, column ->
                                Column(Modifier.fillParentMaxWidth(0.88f)) {
                                    column.forEach { song ->
                                        val isCurrent = song.videoId == currentPlayingSongId
                                        NuviaCompactTrackRow(
                                            song = song,
                                            isPlaying = isCurrent && isPlaying,
                                            isCurrent = isCurrent,
                                            onClick = {
                                                val trackIdx = topSongIndexMap[song.videoId] ?: 0
                                                onSongClick(topSongs, trackIdx)
                                            },
                                            onLongPress = { onSongLongPress(song) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Album or Playlist track listing
                    val numbered = page.type == BrowseType.ALBUM
                    itemsIndexed(
                        items = state.data,
                        key = { index, _ -> trackKeys.getOrElse(index) { "track_$index" } },
                        contentType = { _, _ -> "track_row" },
                    ) { index, song ->
                        val songItem = song
                        val songIdx = index
                        val isCurrent = songItem.videoId == currentPlayingSongId
                        NuviaTrackRow(
                            song = songItem,
                            fallbackThumbnailUrl = if (numbered) null else page.thumbnailUrl,
                            trackNumber = if (numbered) songIdx + 1 else null,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            playNext = playNext,
                            onClick = remember(songItem.videoId, songIdx) { { onSongClick(state.data, songIdx) } },
                            onLongPress = remember(songItem) { { onSongLongPress(songItem) } },
                            onSwipeToQueue = remember(songItem) { { onSongSwipe(songItem) } },
                        )
                        if (index < state.data.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = if (numbered) 64.dp else 82.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = nuviaColors.divider.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }

            // Suggested Tracks for Playlists
            if (page.suggestedSongs.isNotEmpty()) {
                item(key = "suggested_heading", contentType = "suggested_heading") {
                    Spacer(Modifier.height(24.dp))
                    DetailSectionHeading("Recommended Additions")
                }
                itemsIndexed(
                    items = page.suggestedSongs,
                    key = { _, song -> "suggested_${song.videoId}" },
                    contentType = { _, _ -> "suggested_track" },
                ) { index, song ->
                    val songItem = song
                    val songIdx = index
                    NuviaSuggestedTrackRow(
                        song = songItem,
                        onClick = remember(songItem.videoId, songIdx) { { onSongClick(page.suggestedSongs, songIdx) } },
                        onLongPress = remember(songItem) { { onSongLongPress(songItem) } },
                        onAdd = remember(songItem) { { onAddSuggested(songItem) } },
                    )
                    if (index < page.suggestedSongs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = nuviaColors.divider.copy(alpha = 0.35f),
                        )
                    }
                }
            }

            // Artist Album / Single shelves
            items(
                items = page.sections,
                key = { it.title },
                contentType = { "artist_section" },
            ) { shelf ->
                Column(Modifier.padding(top = 28.dp)) {
                    DetailSectionHeading(shelf.title)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = shelf.items,
                            key = { it.browseId ?: it.title },
                            contentType = { "shelf_card" },
                        ) { item ->
                            val currentItem = item
                            NuviaShelfCard(
                                item = currentItem,
                                onClick = remember(currentItem) { { onSectionItemClick(currentItem) } },
                                onLongPress = remember(currentItem, onOpenBrowseActions) {
                                    onOpenBrowseActions?.let { openActions ->
                                        {
                                            openActions(
                                                BrowseTarget(
                                                    browseId = currentItem.browseId,
                                                    title = currentItem.title,
                                                    subtitle = currentItem.subtitle,
                                                    thumbnailUrl = currentItem.thumbnailUrl,
                                                    type = BrowseType.ALBUM,
                                                    fromCard = true,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Artist Facts / About Section
            if (isArtist && page.artistFacts != null) {
                item(key = "artist_about_section", contentType = "artist_about_section") {
                    ArtistAboutSection(
                        artistName = page.title,
                        factsState = page.artistFacts,
                        onArtistClick = onArtistClick,
                    )
                }
            }
        }
    }
}

/**
 * Liquid Glass Playlist / Album Hero Header.
 * Features a floating artwork card with specular border and ambient backlight glow,
 * Sora bold title, metadata pill, and floating action controls.
 */
@Composable
private fun PlaylistHeroHeader(
    page: DetailPage,
    songs: List<Song>,
    canvas: CanvasArtwork?,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: ((List<Song>) -> Unit)?,
    onArtistClick: (String, String) -> Unit,
    onToggleLibrary: (() -> Unit)?,
    onOpenBrowseActions: ((BrowseTarget) -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val (credit, meta) = page.headerLines(songs.size)
    val artist = songs.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HEADER_GUTTER, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Floating Cover Art Card
        Box(
            modifier = Modifier
                .size(220.dp)
                .nuviaAmbientBacklight(
                    color = nuviaColors.ambientGlow,
                    radiusDp = 28.dp,
                    alpha = 0.55f,
                )
                .clip(RoundedCornerShape(20.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(20.dp),
                    width = 0.8.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val context = LocalContext.current
            val heroArtRequest = remember(page.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(page.thumbnailUrl.artworkAt(CARD_ART_PX))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = heroArtRequest,
                contentDescription = page.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Animated canvas if available
            canvas?.let { clip ->
                CanvasArtworkPlayer(
                    canvas = clip,
                    isPlaying = true,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Badge line (e.g. "PLAYLIST" / "ALBUM")
        val badgeText = page.type.label?.uppercase() ?: "PLAYLIST"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(nuviaColors.primary.copy(alpha = 0.14f))
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(6.dp),
                    width = 0.5.dp,
                    topHighlight = nuviaColors.primary.copy(alpha = 0.3f),
                    bottomBorder = nuviaColors.primary.copy(alpha = 0.1f),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                ),
                color = nuviaColors.primary,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Headline Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
            ),
            color = nuviaColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // Subtitle / Artist
        if (credit.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            val artistId = artist?.artistId
            Text(
                text = credit,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .then(
                        if (artistId != null) {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onArtistClick(artistId, artist.artist) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        } else Modifier,
                    ),
            )
        }

        // Meta info (Tracks • Duration • Year)
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = nuviaColors.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(18.dp))

        // Action Controls Row
        if (songs.isNotEmpty()) {
            val library = page.library?.takeIf { onToggleLibrary != null }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle Button
                NuviaGlassIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    onClick = onShuffle,
                )

                // Primary Play Pill
                NuviaHeroPlayPill(
                    onClick = onPlay,
                    label = "Play All",
                )

                // Download Button
                onDownload?.let { download ->
                    NuviaGlassIconButton(
                        icon = Icons.Rounded.FileDownload,
                        contentDescription = "Download All",
                        onClick = { download(songs) },
                    )
                }

                // Library / Favorite Toggle
                if (library != null) {
                    NuviaGlassIconButton(
                        icon = if (library.saved) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = if (library.saved) "Saved to Library" else "Save to Library",
                        tint = if (library.saved) nuviaColors.primary else nuviaColors.textPrimary,
                        onClick = { onToggleLibrary?.invoke() },
                    )
                }

                // Contextual Browse Actions (Pin, Play Next, Queue, Download, Rename, Delete, etc.)
                if (onOpenBrowseActions != null) {
                    NuviaGlassIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = "More Actions",
                        onClick = {
                            onOpenBrowseActions(
                                BrowseTarget(
                                    browseId = page.browseId,
                                    title = page.title,
                                    subtitle = credit.ifBlank { page.subtitle },
                                    thumbnailUrl = page.thumbnailUrl,
                                    type = page.type,
                                    songs = songs,
                                    fromCard = false,
                                )
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Artist Hero Header: display title over photo backdrop with action controls.
 */
@Composable
private fun ArtistHeroHeader(
    page: DetailPage,
    heroHeight: Dp,
    songs: List<Song>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onOpenBrowseActions: ((BrowseTarget) -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height((heroHeight - 90.dp).coerceAtLeast(140.dp)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HEADER_GUTTER),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                ),
                color = nuviaColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val facts = (page.artistFacts as? UiState.Success)?.data
            val factsSubtitle = facts?.formattedSubtitle
            if (!factsSubtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = factsSubtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = nuviaColors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val topTags = remember(facts) { facts?.allTags?.take(3).orEmpty() }
            if (topTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    topTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(nuviaColors.glassSurface.copy(alpha = 0.5f))
                                .nuviaSpecularBorder(
                                    shape = RoundedCornerShape(100.dp),
                                    width = 0.6.dp,
                                    topHighlight = nuviaColors.glassHighlight.copy(alpha = 0.2f),
                                    bottomBorder = nuviaColors.glassBorder.copy(alpha = 0.1f),
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.3.sp,
                                ),
                                color = nuviaColors.primary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (songs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NuviaGlassIconButton(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle Artist",
                        onClick = onShuffle,
                    )
                    NuviaHeroPlayPill(
                        onClick = onPlay,
                        label = "Play",
                    )
                    if (onOpenBrowseActions != null) {
                        NuviaGlassIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "More Actions",
                            onClick = {
                                onOpenBrowseActions(
                                    BrowseTarget(
                                        browseId = page.browseId,
                                        title = page.title,
                                        subtitle = "Artist",
                                        thumbnailUrl = page.thumbnailUrl,
                                        type = page.type,
                                        songs = songs,
                                        fromCard = false,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Liquid-Glass Hero Play Pill with vibrant dynamic gradient & tactile press.
 */
@Composable
private fun NuviaHeroPlayPill(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val gradientBrush = remember(nuviaColors.secondary, nuviaColors.primary) {
        Brush.horizontalGradient(listOf(nuviaColors.secondary, nuviaColors.primary))
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .nuviaAmbientBacklight(
                color = nuviaColors.ambientGlow,
                radiusDp = 12.dp,
                alpha = 0.5f,
            )
            .clip(CircleShape)
            .background(gradientBrush)
            .nuviaSpecularBorder(
                shape = CircleShape,
                width = 0.8.dp,
                topHighlight = nuviaColors.glassHighlight,
                bottomBorder = nuviaColors.glassBorder,
            )
            .nuviaTactilePress(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = nuviaColors.onPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = nuviaColors.onPrimary,
        )
    }
}

/**
 * Frosted Liquid-Glass Circular Action Button.
 */
@Composable
private fun NuviaGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color? = null,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val iconTint = tint ?: nuviaColors.textPrimary

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(
                shape = CircleShape,
                width = 0.75.dp,
                topHighlight = nuviaColors.glassHighlight,
                bottomBorder = nuviaColors.glassBorder,
            )
            .nuviaTactilePress(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val RowThumbnailShape = RoundedCornerShape(10.dp)
private val ActiveRowShape = RoundedCornerShape(12.dp)

private val TrackNumberTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
)
private val TrackTitleNormalTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
)
private val TrackTitleActiveTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
)
private val TrackArtistTextStyle = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
)
private val TrackDurationTextStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 11.sp,
)

private val SuggestedThumbnailShape = RoundedCornerShape(8.dp)
private val SuggestedTrackTitleTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
)
private val SuggestedTrackArtistTextStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 12.sp,
)

/**
 * Redesigned Liquid Glass Track Row with active playing indicator,
 * crisp typography (Sora / Manrope), duration pill, and swipe-to-queue.
 */
private val SWIPE_POSITIONAL_THRESHOLD: (Float) -> Float = { distance -> distance * 0.45f }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NuviaTrackRow(
    song: Song,
    fallbackThumbnailUrl: String? = null,
    trackNumber: Int?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    playNext: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeToQueue: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current

    val currentOnSwipeToQueue by rememberUpdatedState(onSwipeToQueue)
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = remember {
            { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    currentOnSwipeToQueue()
                }
                false
            }
        },
        positionalThreshold = SWIPE_POSITIONAL_THRESHOLD,
    )

    val isSwiping = swipeState.dismissDirection != SwipeToDismissBoxValue.Settled ||
        swipeState.targetValue != SwipeToDismissBoxValue.Settled

    SwipeToDismissBox(
        state = swipeState,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            if (isSwiping) {
                NuviaSwipeBackground(swipeState, playNext)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCurrent) {
                        Modifier
                            .background(nuviaColors.primary.copy(alpha = 0.10f))
                            .nuviaSpecularBorder(
                                shape = ActiveRowShape,
                                width = 0.6.dp,
                                topHighlight = nuviaColors.primary.copy(alpha = 0.25f),
                                bottomBorder = Color.Transparent,
                            )
                    } else {
                        Modifier.background(Color.Transparent)
                    },
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Track number or Artwork thumbnail
            if (trackNumber != null) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCurrent) {
                        EqualizerVisualizerIcon(isPlaying = isPlaying)
                    } else {
                        Text(
                            text = "$trackNumber",
                            style = TrackNumberTextStyle,
                            color = nuviaColors.textSecondary,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RowThumbnailShape)
                        .background(nuviaColors.glassSurface)
                        .nuviaSpecularBorder(
                            shape = RowThumbnailShape,
                            width = 0.5.dp,
                            topHighlight = nuviaColors.glassHighlight,
                            bottomBorder = nuviaColors.glassBorder,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val context = LocalContext.current
                    val artworkUrl = remember(song.thumbnailUrl, fallbackThumbnailUrl) {
                        (song.thumbnailUrl ?: fallbackThumbnailUrl).artworkAt(ROW_ART_PX)
                    }
                    val imageRequest = remember(artworkUrl) {
                        ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            EqualizerVisualizerIcon(isPlaying = isPlaying)
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Title & Artist
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = if (isCurrent) TrackTitleActiveTextStyle else TrackTitleNormalTextStyle,
                    color = if (isCurrent) nuviaColors.primary else nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = TrackArtistTextStyle,
                    color = nuviaColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Duration text
            song.durationText?.let { duration ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = duration,
                    style = TrackDurationTextStyle,
                    color = nuviaColors.textMuted,
                )
            }

            // More Options Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLongPress),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More Options",
                    tint = nuviaColors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Animated Equalizer Bars for currently active track.
 */
@Composable
private fun EqualizerVisualizerIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "eqAnim")
        val h1 by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
            label = "h1",
        )
        val h2 by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
            label = "h2",
        )
        val h3 by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
            label = "h3",
        )

        Row(
            modifier = modifier.size(18.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h1)
                    .clip(RoundedCornerShape(1.dp))
                    .background(nuviaColors.primary),
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h2)
                    .clip(RoundedCornerShape(1.dp))
                    .background(nuviaColors.primary),
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h3)
                    .clip(RoundedCornerShape(1.dp))
                    .background(nuviaColors.primary),
            )
        }
    } else {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = "Active Track",
            tint = nuviaColors.primary,
            modifier = modifier.size(18.dp),
        )
    }
}

/**
 * Swipe reveal background for queueing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuviaSwipeBackground(swipeState: SwipeToDismissBoxState, playNext: Boolean) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                val offset = try { swipeState.requireOffset() } catch (e: Exception) { 0f }
                if (offset > 0f) {
                    clipRect(left = 0f, top = 0f, right = offset, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                } else if (offset < 0f) {
                    clipRect(left = size.width + offset, top = 0f, right = size.width, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
            .background(nuviaColors.primary.copy(alpha = 0.16f))
            .padding(horizontal = PAGE_GUTTER + 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NuviaSwipeLabel(playNext)
        NuviaSwipeLabel(playNext)
    }
}

@Composable
private fun NuviaSwipeLabel(playNext: Boolean) {
    val nuviaColors = LocalNUViAColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (playNext) Icons.Rounded.PlayArrow else Icons.Rounded.Add,
            contentDescription = null,
            tint = nuviaColors.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (playNext) "Play Next" else "Queue Track",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
            ),
            color = nuviaColors.primary,
        )
    }
}

/**
 * Compact track row for horizontal artist carousels.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NuviaCompactTrackRow(
    song: Song,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(RoundedCornerShape(8.dp), 0.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            val context = LocalContext.current
            val artRequest = remember(song.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(song.artworkAt(ROW_ART_PX))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = artRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    EqualizerVisualizerIcon(isPlaying = isPlaying)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
                color = if (isCurrent) nuviaColors.primary else nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 12.sp,
                ),
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onLongPress),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More Options",
                tint = nuviaColors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Suggested Track Row with dedicated "Add" button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NuviaSuggestedTrackRow(
    song: Song,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAdd: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        val artRequest = remember(song.thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(song.artworkAt(ROW_ART_PX))
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = artRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(SuggestedThumbnailShape)
                .nuviaSpecularBorder(SuggestedThumbnailShape, 0.5.dp)
                .background(nuviaColors.glassSurface),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = SuggestedTrackTitleTextStyle,
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = SuggestedTrackArtistTextStyle,
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(nuviaColors.primary.copy(alpha = 0.16f))
                .nuviaSpecularBorder(CircleShape, 0.5.dp)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add to playlist",
                tint = nuviaColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Liquid Glass Section Card for artist albums & singles.
 */
@Composable
private fun NuviaShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Column(
        modifier = Modifier
            .width(SHELF_CARD_WIDTH)
            .nuviaTactilePress(pressedScale = 0.96f, onClick = onClick),
    ) {
        val context = LocalContext.current
        val artRequest = remember(item.thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(item.thumbnailUrl.artworkAt(CARD_ART_PX))
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = artRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(SHELF_CARD_WIDTH)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(12.dp),
                    width = 0.6.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .background(nuviaColors.glassSurface),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 11.sp,
            ),
            color = nuviaColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailSectionHeading(title: String) {
    val nuviaColors = LocalNUViAColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = Sora,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        ),
        color = nuviaColors.textPrimary,
        modifier = Modifier.padding(
            start = PAGE_GUTTER,
            end = PAGE_GUTTER,
            top = 12.dp,
            bottom = 10.dp,
        ),
    )
}

/** Header lines generator (credit, metadata) */
private fun DetailPage.headerLines(trackCount: Int): Pair<String, String> {
    val parts = subtitle.split("•", "·").map { it.trim() }.filter { it.isNotEmpty() }
    val year = parts.lastOrNull { it.length == 4 && it.all(Char::isDigit) }
    val kind = parts.firstOrNull { it.lowercase() in KIND_WORDS }
    val credit = parts.filter { it != year && it != kind }.joinToString(", ")
    val meta = listOfNotNull(
        kind ?: type.label,
        year,
        trackCount.takeIf { it > 0 }?.let { "$it ${if (it == 1) "song" else "songs"}" },
    ).joinToString(" • ").uppercase()
    return credit to meta
}

private val KIND_WORDS = setOf(
    "album", "single", "ep", "playlist", "artist", "podcast", "episode", "song", "video",
)

private val BrowseType.label: String?
    get() = when (this) {
        BrowseType.ALBUM -> "Album"
        BrowseType.PLAYLIST -> "Playlist"
        BrowseType.ARTIST -> "Artist"
        BrowseType.OTHER -> null
    }

private fun LazyListState.headerTop(artHeightPx: Float): Float =
    if (firstVisibleItemIndex == 0) -firstVisibleItemScrollOffset.toFloat() else -artHeightPx * 2f

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ArtistAboutSection(
    artistName: String,
    factsState: UiState<ArtistFacts>?,
    onArtistClick: (String, String) -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    when (factsState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
            ) {
                DetailSectionHeading("About $artistName")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER)
                        .clip(RoundedCornerShape(22.dp))
                        .background(nuviaColors.surfaceCard.copy(alpha = 0.65f))
                        .nuviaSpecularBorder(
                            shape = RoundedCornerShape(22.dp),
                            width = 0.8.dp,
                            topHighlight = nuviaColors.glassHighlight.copy(alpha = 0.25f),
                            bottomBorder = nuviaColors.glassBorder.copy(alpha = 0.15f),
                        )
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(width = 80.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(nuviaColors.glassSurfaceElevated.copy(alpha = 0.4f)),
                            )
                            Box(
                                Modifier
                                    .size(width = 110.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(nuviaColors.glassSurfaceElevated.copy(alpha = 0.4f)),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(nuviaColors.glassSurfaceElevated.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
        is UiState.Success -> {
            val facts = factsState.data
            if (facts.isEmpty) return

            var expanded by remember { mutableStateOf(false) }
            val bio = facts.biography ?: facts.biographySummary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
            ) {
                DetailSectionHeading("About $artistName")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER)
                        .clip(RoundedCornerShape(22.dp))
                        .background(nuviaColors.surfaceCard.copy(alpha = 0.82f))
                        .nuviaSpecularBorder(
                            shape = RoundedCornerShape(22.dp),
                            width = 0.8.dp,
                            topHighlight = nuviaColors.glassHighlight.copy(alpha = 0.35f),
                            bottomBorder = nuviaColors.glassBorder.copy(alpha = 0.18f),
                        )
                        .padding(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                    ) {
                        // Metadata badges row
                        val badges = buildList {
                            facts.origin?.takeIf { it.isNotBlank() }?.let {
                                add(Triple(Icons.Rounded.LocationOn, "Origin", it))
                            }
                            facts.formedYear?.takeIf { it.isNotBlank() }?.let {
                                add(Triple(Icons.Rounded.CalendarToday, "Formed", it))
                            }
                            facts.listeners?.takeIf { it > 0 }?.let {
                                add(Triple(Icons.Rounded.Headphones, "Listeners", ArtistFacts.formatCount(it)))
                            }
                            facts.playCount?.takeIf { it > 0 }?.let {
                                add(Triple(Icons.Rounded.GraphicEq, "Plays", ArtistFacts.formatCount(it)))
                            }
                            if (facts.onTour) {
                                add(Triple(Icons.Rounded.Sensors, "Tour", "On Tour"))
                            }
                        }

                        if (badges.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                badges.forEach { (icon, label, value) ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(nuviaColors.glassSurface.copy(alpha = 0.6f))
                                            .nuviaSpecularBorder(
                                                shape = RoundedCornerShape(12.dp),
                                                width = 0.6.dp,
                                                topHighlight = nuviaColors.glassHighlight.copy(alpha = 0.15f),
                                                bottomBorder = nuviaColors.glassBorder.copy(alpha = 0.08f),
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = nuviaColors.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Column {
                                            Text(
                                                text = label.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = Manrope,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.6.sp,
                                                ),
                                                color = nuviaColors.textMuted,
                                            )
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = Manrope,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = nuviaColors.textPrimary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }

                        // Biography
                        if (!bio.isNullOrBlank()) {
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Manrope,
                                    fontSize = 13.5.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = nuviaColors.textSecondary,
                                maxLines = if (expanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                            )

                            if (bio.length > 200) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (expanded) "Read less" else "Read more",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = Sora,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = nuviaColors.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { expanded = !expanded }
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                )
                            }
                        }

                        // Tags / Genres
                        val allTags = facts.allTags
                        if (allTags.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                allTags.take(8).forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(nuviaColors.glassSurfaceElevated.copy(alpha = 0.45f))
                                            .padding(horizontal = 11.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = Manrope,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                            color = nuviaColors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }

                        // Similar Artists
                        if (facts.similarArtists.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "Similar Artists",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = Sora,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = nuviaColors.textPrimary,
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                facts.similarArtists.take(6).forEach { similarName ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(nuviaColors.glassSurface.copy(alpha = 0.7f))
                                            .nuviaSpecularBorder(
                                                shape = RoundedCornerShape(14.dp),
                                                width = 0.6.dp,
                                                topHighlight = nuviaColors.glassHighlight.copy(alpha = 0.15f),
                                                bottomBorder = nuviaColors.glassBorder.copy(alpha = 0.08f),
                                            )
                                            .clickable {
                                                onArtistClick(similarName, similarName)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = similarName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = Manrope,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = nuviaColors.textPrimary,
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = nuviaColors.textMuted,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Verified source attribution
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = nuviaColors.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Verified artist facts • ${facts.source ?: "MusicBrainz & Last.fm"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Manrope,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = nuviaColors.textMuted,
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // Null or Error: gracefully hide without breaking discography
        }
    }
}
