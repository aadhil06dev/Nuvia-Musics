package com.music.nuvia.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.nuvia.R
import com.music.nuvia.data.model.BrowseItem
import com.music.nuvia.data.model.BrowseType
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.SearchFilter
import com.music.nuvia.data.model.SearchResult
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.UiState
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.ui.components.NUViAAmbientBackground
import com.music.nuvia.ui.components.NUViABadge
import com.music.nuvia.ui.components.NUViAGlassButton
import com.music.nuvia.ui.components.NUViAPillTag
import com.music.nuvia.ui.components.PAGE_GUTTER
import com.music.nuvia.ui.components.SongRow
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaLiquidGlass
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.components.songListSkeleton
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.NUViABrand
import com.music.nuvia.ui.theme.Sora

private val QuickSearchSuggestions = listOf(
    "Trending",
    "Synthwave",
    "Lo-Fi Beats",
    "Acoustic",
    "Electronic",
    "Hip Hop",
    "Rock Classics",
    "Chillout",
)

/**
 * Phase 3 — Complete NUViA Search Redesign.
 *
 * Implements an Apple-quality liquid glass search experience on an AMOLED-first canvas:
 * - Liquid glass search capsule with subtle specular sheen and glowing focus halo
 * - Category filter pills with tactile micro-interactions
 * - Polished suggestion rows with typeahead fill actions
 * - Sleek recent searches with clear history control and individual deletion
 * - Cinematic rich search results for Songs, Artists, Albums, and Playlists
 * - Ambient atmospheric backdrop and transparent NUViA in-app branding
 * - Fully preserves all audio playback callbacks, radio creation, and navigation logic
 */
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    results: UiState<List<SearchResult>>?,
    listState: LazyListState,
    focusTrigger: Int = 0,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onBrowseClick: (BrowseItem) -> Unit,
    history: List<String>,
    suggestions: List<String>,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val nuviaColors = LocalNUViAColors.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) focusRequester.requestFocus()
    }

    val suggesting = suggestions.isNotEmpty()
    val searchTracks = remember(results) {
        val success = results as? UiState.Success ?: return@remember emptyList()
        success.data.filterIsInstance<SearchResult.Track>().map { it.song }
    }
    val searchTrackIndexMap = remember(searchTracks) {
        searchTracks.mapIndexed { idx, song -> song.videoId to idx }.toMap()
    }

    NUViAAmbientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Top Header: Liquid Glass Search Capsule + Dynamic Category Filter Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                NuviaSearchHeader(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSubmit = onSubmit,
                    focusRequester = focusRequester,
                    modifier = Modifier.padding(
                        start = PAGE_GUTTER,
                        end = PAGE_GUTTER,
                        top = 6.dp,
                        bottom = 10.dp,
                    ),
                )

                // Render filter tags when there is an active result query and not currently suggesting
                if (results != null && !suggesting) {
                    NuviaSearchFilterTabs(
                        filter = filter,
                        onFilterChange = onFilterChange,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            // Scrollable Body: Suggestions, Recents, Loading Skeletons, Errors, or Rich Result Rows
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                when {
                    // Mid-typing autocomplete suggestion strip
                    suggesting -> {
                        searchSuggestionsSection(
                            suggestions = suggestions,
                            onClick = { term ->
                                onSuggestionClick(term)
                                focusManager.clearFocus()
                            },
                            onFill = onQueryChange,
                        )
                    }

                    // Initial empty state or recent searches
                    results == null -> {
                        if (history.isEmpty()) {
                            item(key = "empty_discovery") {
                                SearchDiscoveryEmptyState(
                                    onSuggestionSelected = { selectedTerm ->
                                        onQueryChange(selectedTerm)
                                        onSubmit()
                                        focusManager.clearFocus()
                                    },
                                )
                            }
                        } else {
                            recentSearchesSection(
                                history = history,
                                onClick = { term ->
                                    onHistoryClick(term)
                                    focusManager.clearFocus()
                                },
                                onRemove = onHistoryRemove,
                                onClear = onHistoryClear,
                            )
                        }
                    }

                    // Loading Shimmer Skeletons
                    results is UiState.Loading -> {
                        songListSkeleton(circular = filter == SearchFilter.ARTISTS)
                    }

                    // Error Message with retry
                    results is UiState.Error -> {
                        item(key = "search_error") {
                            SearchErrorState(
                                message = results.message,
                                onRetry = onSubmit,
                            )
                        }
                    }

                    // Successful search result list
                    results is UiState.Success -> {
                        val itemsList = results.data
                        if (itemsList.isEmpty()) {
                            item(key = "no_results") {
                                SearchNoResultsState(
                                    query = query,
                                    onClear = { onQueryChange("") },
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = itemsList,
                                key = { index, row ->
                                    when (row) {
                                        is SearchResult.Track -> "track_${row.song.videoId}"
                                        is SearchResult.Browse -> "browse_${row.item.browseId ?: index}"
                                    }
                                },
                                contentType = { _, row ->
                                    when (row) {
                                        is SearchResult.Track -> "track"
                                        is SearchResult.Browse -> "browse"
                                    }
                                },
                            ) { index, row ->
                                when (row) {
                                    is SearchResult.Track -> {
                                        val song = row.song
                                        val videoId = song.videoId
                                        SongRow(
                                            song = song,
                                            onClick = remember(videoId, searchTracks) {
                                                {
                                                    val trackIdx = searchTrackIndexMap[videoId] ?: 0
                                                    onSongClick(searchTracks, trackIdx)
                                                }
                                            },
                                            onLongPress = remember(song) { { onSongLongPress(song) } },
                                            onSwipeToQueue = remember(song) { { onSongSwipe(song) } },
                                        )
                                    }
                                    is SearchResult.Browse -> {
                                        val browseItem = row.item
                                        NuviaBrowseRow(
                                            item = browseItem,
                                            onClick = remember(browseItem) { { onBrowseClick(browseItem) } },
                                        )
                                    }
                                }

                                if (index < itemsList.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 78.dp, end = PAGE_GUTTER),
                                        thickness = 0.5.dp,
                                        color = nuviaColors.glassBorder.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium NUViA Liquid Glass Search Header.
 * Features an Apple-quality pill capsule geometry, specular top highlight,
 * dynamic focus illumination, clear action, and custom typography.
 */
@Composable
private fun NuviaSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val capsuleShape = RoundedCornerShape(24.dp)

    val haloGlowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.38f else 0.12f,
        animationSpec = spring(stiffness = 300f),
        label = "search_halo",
    )

    val borderHighlightColor by animateColorAsState(
        targetValue = if (isFocused) nuviaColors.primary.copy(alpha = 0.85f) else nuviaColors.glassBorder,
        animationSpec = tween(220),
        label = "search_border",
    )

    val submit = {
        if (query.isNotBlank()) {
            onSubmit()
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 22.dp, alpha = haloGlowAlpha)
            .clip(capsuleShape)
            .background(nuviaColors.glassSurfaceElevated)
            .border(0.85.dp, borderHighlightColor, capsuleShape)
            .nuviaSpecularBorder(
                shape = capsuleShape,
                width = 0.75.dp,
                topHighlight = Color.White.copy(alpha = if (isFocused) 0.45f else 0.22f),
                bottomBorder = Color.Black.copy(alpha = 0.65f),
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search Icon / Submit Trigger
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(enabled = query.isNotBlank(), onClick = submit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = if (isFocused || query.isNotBlank()) nuviaColors.primary else nuviaColors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(10.dp))

            // Main Text Input Field
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Artists, songs, albums, playlists...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.5.sp,
                        ),
                        color = nuviaColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = nuviaColors.textPrimary,
                    ),
                    cursorBrush = SolidColor(nuviaColors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            // Quick Clear Button
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(nuviaColors.glassSurface)
                        .border(0.5.dp, nuviaColors.glassBorder, CircleShape)
                        .clickable {
                            onQueryChange("")
                            focusManager.clearFocus()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = nuviaColors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Filter Tabs using tactile NUViAPillTag components.
 */
@Composable
private fun NuviaSearchFilterTabs(
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchFilter.entries.forEach { entry ->
            val selected = entry == filter
            NUViAPillTag(
                label = entry.label,
                selected = selected,
                onClick = { onFilterChange(entry) },
            )
        }
    }
}

/**
 * Search autocomplete typeahead suggestions.
 */
private fun LazyListScope.searchSuggestionsSection(
    suggestions: List<String>,
    onClick: (String) -> Unit,
    onFill: (String) -> Unit,
) {
    itemsIndexed(
        items = suggestions,
        key = { _, term -> "suggest:$term" },
        contentType = { _, _ -> "suggestion_row" },
    ) { index, term ->
        NuviaSuggestionRow(
            term = term,
            isLead = index == 0,
            onFill = if (index == 0) null else ({ onFill(term) }),
            onClick = { onClick(term) },
        )
    }
}

/**
 * Redesigned autocomplete suggestion row with glass touch highlight.
 */
@Composable
private fun NuviaSuggestionRow(
    term: String,
    isLead: Boolean,
    onFill: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isLead) nuviaColors.primary.copy(alpha = 0.15f) else nuviaColors.glassSurface)
                .border(0.5.dp, if (isLead) nuviaColors.primary.copy(alpha = 0.40f) else nuviaColors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isLead) Icons.Rounded.Search else Icons.Rounded.TrendingUp,
                contentDescription = null,
                tint = if (isLead) nuviaColors.primary else nuviaColors.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = term,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Manrope,
                fontWeight = if (isLead) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 15.sp,
            ),
            color = if (isLead) nuviaColors.textPrimary else nuviaColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (onFill != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.NorthWest,
                    contentDescription = "Edit \"$term\"",
                    tint = nuviaColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Spacer(Modifier.width(36.dp))
        }
    }
}

/**
 * Recent searches section with clear button and item removal.
 */
private fun LazyListScope.recentSearchesSection(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "recent:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LocalNUViAColors.current.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = LocalNUViAColors.current.textPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(LocalNUViAColors.current.glassSurface)
                    .border(0.5.dp, LocalNUViAColors.current.glassBorder, CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    ),
                    color = LocalNUViAColors.current.primary,
                )
            }
        }
    }

    items(
        items = history,
        key = { "recent:$it" },
        contentType = { "recent_search_row" },
    ) { term ->
        NuviaRecentSearchRow(
            term = term,
            onClick = { onClick(term) },
            onRemove = { onRemove(term) },
        )
    }
}

/**
 * Redesigned recent search row.
 */
@Composable
private fun NuviaRecentSearchRow(
    term: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(nuviaColors.glassSurface)
                .border(0.5.dp, nuviaColors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = nuviaColors.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = term,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Manrope,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove \"$term\"",
                tint = nuviaColors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Rich Browse Row for Artists, Albums, and Playlists.
 */
@Composable
private fun NuviaBrowseRow(
    item: BrowseItem,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val isArtist = item.type == BrowseType.ARTIST
    val shape = if (isArtist) CircleShape else RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .nuviaTactilePress(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // High quality artwork
        Box(
            modifier = Modifier
                .size(52.dp)
                .then(
                    if (isArtist) {
                        Modifier.nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 14.dp, alpha = 0.22f)
                    } else Modifier
                )
                .clip(shape)
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(shape, 0.75.dp),
        ) {
            val context = LocalContext.current
            val artRequest = remember(item.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(item.thumbnailUrl.artworkAt(ROW_ART_PX))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = artRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.width(14.dp))

        // Title and meta
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp,
                ),
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeText = when (item.type) {
                    BrowseType.ARTIST -> "ARTIST"
                    BrowseType.ALBUM -> "ALBUM"
                    BrowseType.PLAYLIST -> "PLAYLIST"
                    else -> "MEDIA"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(nuviaColors.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = nuviaColors.primary,
                    )
                }

                val subtitleText = item.subtitle.ifBlank { "" }
                if (subtitleText.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Manrope,
                            fontSize = 12.5.sp,
                        ),
                        color = nuviaColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Atmospheric empty discovery state when no search query has been entered yet.
 */
@Composable
private fun SearchDiscoveryEmptyState(
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Transparent NUViA in-app logo
        Box(
            modifier = Modifier
                .size(68.dp)
                .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 30.dp, alpha = 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                colorFilter = ColorFilter.tint(nuviaColors.primary),
                modifier = Modifier.size(52.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Explore the Soundscape",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            color = nuviaColors.textPrimary,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Search millions of tracks, albums, artists, and personalized mixes.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 13.5.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(28.dp))

        // Quick Pick Cloud / Chips
        Text(
            text = "TRENDING TOPICS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
            color = nuviaColors.primary,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickSearchSuggestions.forEach { suggestion ->
                NUViAPillTag(
                    label = suggestion,
                    selected = false,
                    onClick = { onSuggestionSelected(suggestion) },
                )
            }
        }
    }
}

/**
 * Polished state when query returns no results.
 */
@Composable
private fun SearchNoResultsState(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(nuviaColors.glassSurface)
                .border(0.75.dp, nuviaColors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = nuviaColors.textMuted,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = nuviaColors.textPrimary,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "We couldn't find anything matching \"$query\". Check your spelling or try broader terms.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 13.5.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(22.dp))

        NUViAGlassButton(
            text = "Clear Search",
            onClick = onClear,
            isPrimary = false,
        )
    }
}

/**
 * Error state for SearchScreen.
 */
@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Manrope,
                fontSize = 14.5.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        NUViAGlassButton(
            text = "Retry",
            icon = Icons.Rounded.Refresh,
            onClick = onRetry,
            isPrimary = true,
        )
    }
}
