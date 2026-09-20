package com.music.nuvia.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.music.nuvia.data.model.Account
import com.music.nuvia.ui.components.TopBarAccountButton
import com.music.nuvia.ui.components.TopBarDownloadButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.nuvia.R
import com.music.nuvia.data.model.CARD_ART_PX
import com.music.nuvia.data.model.HEADER_ART_PX
import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.data.model.UiState
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.ui.components.NUViABadge
import com.music.nuvia.ui.components.NUViAGlassButton
import com.music.nuvia.ui.components.NUViAPillTag
import com.music.nuvia.ui.components.PAGE_GUTTER
import com.music.nuvia.ui.components.PullToRefresh
import com.music.nuvia.ui.components.SHELF_CARD_WIDTH
import com.music.nuvia.ui.components.SignInBanner
import com.music.nuvia.ui.components.feedMoreSkeleton
import com.music.nuvia.ui.components.feedSkeleton
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.icons.NUViAIcons
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import java.util.Calendar

/**
 * NUViA Liquid-Glass Cinematic HomeScreen.
 *
 * Visual Architecture:
 * 1. AMOLED-first foundation (#000000) with atmospheric illumination
 * 2. Time-aware atmospheric greeting with NUViA emblem watermark (`bglogo.png`)
 * 3. Interactive mood / vibe filter chips
 * 4. Widescreen Hero Spotlight Showcase for lead featured mixes
 * 5. 2-column Quick Access shortcuts grid for immediate 1-tap playback
 * 6. Diverse, archetype-driven music shelves:
 *    - Multi-row compact track carousels for songs
 *    - Circular halo cards for artists
 *    - Vinyl/glass sleeve cards for albums
 *    - Liquid glass curated mix cards for playlists
 * 7. Specular edge highlighting and tactile haptic response
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    title: String = "Tonight's mix",
    account: Account? = null,
    onAccountClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    var selectedVibe by rememberSaveable { mutableStateOf("All") }

    val (quickItems, contentShelves, leadShelf) = remember(state, selectedVibe) {
        if (state is UiState.Success) {
            val curated = curateHomeFeed(state.data, selectedVibe)
            Triple(curated.quickItems, curated.contentShelves, curated.leadShelf)
        } else {
            Triple(emptyList<ShelfItem>(), emptyList<HomeShelf>(), null)
        }
    }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "home_header") {
                NuviaHomeHeader(
                    title = title,
                    account = account,
                    onAccountClick = onAccountClick,
                    onDownloadClick = onDownloadClick,
                    selectedVibe = selectedVibe,
                    onVibeSelected = { selectedVibe = it },
                )
            }
                if (!signedIn && onSignIn != null) {
                    item(key = "sign_in_banner") {
                        SignInBanner(
                            onSignIn = onSignIn,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }

            when (state) {
                is UiState.Loading -> feedSkeleton()
                is UiState.Error -> item(key = "error_state") {
                    HomeErrorState(
                        message = state.message,
                        onRetry = onRetry,
                    )
                }
                is UiState.Success -> {
                    val shelves = state.data
                    if (shelves.isEmpty()) {
                        item(key = "empty_state") {
                            HomeEmptyState(onRefresh = onRefresh)
                        }
                    } else {
                        // 1. Featured Spotlight Area (First shelf lead items)
                        if (leadShelf != null && leadShelf.items.isNotEmpty()) {
                            item(key = "hero_spotlight", contentType = "hero_spotlight") {
                                NuviaHeroSpotlight(
                                    shelf = leadShelf,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                )
                            }
                        }

                        // 2. Quick Access / Shortcuts Grid (First 6 playable/mix items for 1-tap playback)
                        if (quickItems.size >= 2) {
                            item(key = "quick_access_grid", contentType = "quick_access") {
                                NuviaQuickAccessGrid(
                                    items = quickItems,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                )
                            }
                        }

                        // 3. Diverse Archetype Shelves (Remaining shelves with tailored visual presentations)
                        itemsIndexed(
                            items = contentShelves,
                            key = { index, shelf -> "shelf_${shelf.title}_${index + 1}" },
                            contentType = { _, shelf -> detectShelfArchetype(shelf).name },
                        ) { _, shelf ->
                            NuviaDiverseShelf(
                                shelf = shelf,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                                isItemPinned = isItemPinned,
                            )
                        }

                        if (loadingMore) {
                            feedMoreSkeleton()
                        }
                    }
                }
        }
    }
}

    if (onLoadMore != null && state is UiState.Success) {
        val nearEnd by remember {
            derivedStateOf {
                val layout = listState.layoutInfo
                val last = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                layout.totalItemsCount > 0 && last >= layout.totalItemsCount - 3
            }
        }
        LaunchedEffect(nearEnd) {
            if (nearEnd) onLoadMore()
        }
    }
}



@Composable
private fun rememberRealtimeGreeting(title: String): String {
    var greeting by remember(title) { mutableStateOf(getGreetingForCurrentTime(title)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(title) {
        greeting = getGreetingForCurrentTime(title)
        onPauseOrDispose { }
    }
    return greeting
}

internal fun getGreetingForCurrentTime(title: String, calendar: Calendar = Calendar.getInstance()): String {
    if (title != "Tonight's mix" && title != "Explore" && title.isNotBlank()) {
        return title
    }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

/**
 * Top contextual greeting and atmospheric brand header.
 * Displays dynamic greeting based on real-time local daypart.
 */
@Composable
private fun NuviaHomeHeader(
    title: String,
    account: Account?,
    onAccountClick: () -> Unit,
    onDownloadClick: () -> Unit,
    selectedVibe: String,
    onVibeSelected: (String) -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val greetingText = rememberRealtimeGreeting(title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        // Top Action Row: NUViA Logo on Left, Download & Account buttons on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                colorFilter = ColorFilter.tint(nuviaColors.primary),
                modifier = Modifier.size(28.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TopBarDownloadButton(onClick = onDownloadClick)
                TopBarAccountButton(
                    account = account,
                    onClick = onAccountClick,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(nuviaColors.primary)
                            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 8.dp, alpha = 0.60f)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "NUViA AUDIO ENGINE • LOSSLESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.sp,
                        ),
                        color = nuviaColors.primary,
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Personalized atmospheric flow",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Mood / Vibe Filter Pills
        val vibes = remember { listOf("All", "Atmosphere", "Chill", "Energy", "Focus", "Night") }
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vibes, key = { it }) { vibe ->
                NUViAPillTag(
                    label = vibe,
                    selected = vibe == selectedVibe,
                    onClick = { onVibeSelected(vibe) },
                )
            }
        }
    }
}

/**
 * 2-Column Quick Access Shortcuts Grid.
 * Presents 4-6 compact glass tiles for instantaneous 1-tap playback.
 */
@Composable
private fun NuviaQuickAccessGrid(
    items: List<ShelfItem>,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    val rows = remember(items) { items.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    val current = item
                    QuickAccessTile(
                        item = current,
                        onClick = remember(current) { { onItemClick(current) } },
                        onLongPress = remember(current, onItemLongPress) {
                            onItemLongPress?.let { cb -> { cb(current) } }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private val QuickTileShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAccessTile(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(54.dp)
            .clip(QuickTileShape)
            .background(nuviaColors.glassSurfaceElevated)
            .nuviaSpecularBorder(
                shape = QuickTileShape,
                width = 0.75.dp,
                topHighlight = Color.White.copy(alpha = 0.20f),
                bottomBorder = Color.Black.copy(alpha = 0.50f),
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        val artRequest = remember(item.thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(item.thumbnailUrl.artworkAt(CARD_ART_PX))
                .crossfade(false)
                .build()
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .background(nuviaColors.surfaceCard),
        ) {
            AsyncImage(
                model = artRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )

        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(nuviaColors.primary.copy(alpha = 0.16f))
                .border(0.5.dp, nuviaColors.primary.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = nuviaColors.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Widescreen Cinematic Hero Spotlight Showcase.
 * Replaces the generic card carousel with an Apple-Music-quality spotlight magazine hero.
 */
@Composable
private fun NuviaHeroSpotlight(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Curated spotlight mix" },
            badge = "FEATURED",
        )

        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "hero_spotlight_card" },
            ) { item ->
                val currentItem = item
                CinematicSpotlightCard(
                    item = currentItem,
                    onClick = remember(currentItem) { { onItemClick(currentItem) } },
                    onLongPress = remember(currentItem, onItemLongPress) {
                        onItemLongPress?.let { cb -> { cb(currentItem) } }
                    },
                    modifier = Modifier.fillParentMaxWidth(0.88f),
                )
            }
        }
    }
}

private val SpotlightCardShape = RoundedCornerShape(22.dp)
private val SpotlightGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.35f),
        Color.Black.copy(alpha = 0.90f),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CinematicSpotlightCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .nuviaAmbientBacklight(nuviaColors.ambientGlow, radiusDp = 28.dp, alpha = 0.28f)
            .clip(SpotlightCardShape)
            .background(nuviaColors.surfaceCard)
            .nuviaSpecularBorder(
                shape = SpotlightCardShape,
                width = 1.dp,
                topHighlight = Color.White.copy(alpha = 0.35f),
                bottomBorder = Color.Black.copy(alpha = 0.60f),
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
    ) {
        val context = LocalContext.current
        val artRequest = remember(item.thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(item.thumbnailUrl.artworkAt(HEADER_ART_PX))
                .crossfade(false)
                .build()
        }

        AsyncImage(
            model = artRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Top Badges Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(nuviaColors.primary),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "SPOTLIGHT",
                    style = TextStyle(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = nuviaColors.primary,
                )
            }

            NUViABadge(text = "HI-RES LOSSLESS")
        }

        // Bottom Frosted Glass Action Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(SpotlightGradient)
                .padding(start = 18.dp, end = 18.dp, top = 36.dp, bottom = 16.dp),
        ) {
            Text(
                text = item.title,
                style = TextStyle(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.4).sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    style = TextStyle(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Interactive Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Glow Play Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(nuviaColors.secondary, nuviaColors.primary),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 14.dp, alpha = 0.40f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = nuviaColors.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        style = TextStyle(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = nuviaColors.onPrimary,
                    )
                }

                if (onLongPress != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Categorization engine to give each shelf type a distinct, tailored visual layout.
 */
private enum class ShelfArchetype {
    TRACKS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS,
    COMMUNITY_PLAYLISTS,
    STANDARD,
}

private fun detectShelfArchetype(shelf: HomeShelf): ShelfArchetype {
    val titleLower = shelf.title.lowercase()
    return when {
        titleLower.contains("artist") || titleLower.contains("similar to") ||
            shelf.items.any { it.browseId?.startsWith("UC") == true } -> ShelfArchetype.ARTISTS

        titleLower.contains("album") || titleLower.contains("new release") || titleLower.contains("records") -> ShelfArchetype.ALBUMS

        titleLower.contains("quick pick") || titleLower.contains("song") || titleLower.contains("track") ||
            titleLower.contains("top hit") || titleLower.contains("trending song") ||
            (shelf.items.count { it.videoId != null } >= shelf.items.size * 0.7 && shelf.items.isNotEmpty()) -> ShelfArchetype.TRACKS

        titleLower.contains("community") -> ShelfArchetype.COMMUNITY_PLAYLISTS

        titleLower.contains("playlist") || titleLower.contains("mix") || titleLower.contains("radio") ||
            titleLower.contains("atmosphere") || titleLower.contains("vibe") ||
            titleLower.contains("trending") -> ShelfArchetype.PLAYLISTS

        else -> ShelfArchetype.STANDARD
    }
}

/**
 * Dispatches the shelf to the proper visual archetype.
 */
@Composable
private fun NuviaDiverseShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
) {
    when (detectShelfArchetype(shelf)) {
        ShelfArchetype.TRACKS -> {
            NuviaTrackShelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
            )
        }
        ShelfArchetype.ARTISTS -> {
            NuviaArtistShelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
            )
        }
        ShelfArchetype.ALBUMS -> {
            NuviaAlbumShelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                isItemPinned = isItemPinned,
            )
        }
        ShelfArchetype.COMMUNITY_PLAYLISTS -> {
            NuviaCommunityPlaylistShelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                isItemPinned = isItemPinned,
            )
        }
        ShelfArchetype.PLAYLISTS -> {
            NuviaPlaylistShelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                isItemPinned = isItemPinned,
            )
        }
        ShelfArchetype.STANDARD -> {
            Shelf(
                shelf = shelf,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                isItemPinned = isItemPinned,
            )
        }
    }
}

/**
 * Multi-Row Compact Track Carousel for song-centric shelves.
 * Organizes tracks into 2 horizontal rows with track rows rather than tall square posters.
 */
@Composable
private fun NuviaTrackShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    val columns = remember(shelf.items) { shelf.items.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Tap to play instantly" },
            badge = "TRACKS",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = columns,
                key = { colIdx, col -> col.firstOrNull()?.let { "${it.videoId ?: it.browseId ?: it.title}_$colIdx" } ?: "col_$colIdx" },
                contentType = { _, _ -> "track_column" },
            ) { _, columnItems ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    columnItems.forEach { item ->
                        val current = item
                        CompactTrackTile(
                            item = current,
                            onClick = remember(current) { { onItemClick(current) } },
                            onLongPress = remember(current, onItemLongPress) {
                                onItemLongPress?.let { cb -> { cb(current) } }
                            },
                        )
                    }
                }
            }
        }
    }
}

private val TrackTileShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTrackTile(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .width(260.dp)
            .height(56.dp)
            .clip(TrackTileShape)
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(
                shape = TrackTileShape,
                width = 0.75.dp,
                topHighlight = Color.White.copy(alpha = 0.18f),
                bottomBorder = Color.Black.copy(alpha = 0.40f),
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        val artRequest = remember(item.thumbnailUrl) {
            ImageRequest.Builder(context)
                .data(item.thumbnailUrl.artworkAt(CARD_ART_PX))
                .crossfade(false)
                .build()
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(nuviaColors.surfaceCard),
        ) {
            AsyncImage(
                model = artRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = TextStyle(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = TextStyle(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                ),
                color = nuviaColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play",
            tint = nuviaColors.primary,
            modifier = Modifier
                .size(24.dp)
                .padding(start = 4.dp),
        )
    }
}

/**
 * Circular Artist Showcase Shelf.
 * Features circular avatars with ambient halos to break rectangular card repetition.
 */
@Composable
private fun NuviaArtistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Favorite & similar artists" },
            badge = "ARTISTS",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "artist_card" },
            ) { item ->
                val current = item
                ArtistCircleCard(
                    item = current,
                    onClick = remember(current) { { onItemClick(current) } },
                    onLongPress = remember(current, onItemLongPress) {
                        onItemLongPress?.let { cb -> { cb(current) } }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistCircleCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(112.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 18.dp, alpha = 0.22f)
                .clip(CircleShape)
                .nuviaSpecularBorder(CircleShape, 1.dp)
                .background(nuviaColors.surfaceCard),
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
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "Artist",
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            ),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Vinyl & Glass Sleeve Album Shelf.
 * Features 1:1 square sleeve cards with subtle right-edge vinyl sleeve groove accent.
 */
@Composable
private fun NuviaAlbumShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Full length records & releases" },
            badge = "ALBUMS",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "album_sleeve_card" },
            ) { item ->
                val current = item
                AlbumSleeveCard(
                    item = current,
                    onClick = remember(current) { { onItemClick(current) } },
                    onLongPress = remember(current, onItemLongPress) {
                        onItemLongPress?.let { cb -> { cb(current) } }
                    },
                    isPinned = isItemPinned?.invoke(current) == true,
                )
            }
        }
    }
}

private val AlbumSleeveShape = RoundedCornerShape(14.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumSleeveCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    isPinned: Boolean = false,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(160.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(1f)
                .nuviaAmbientBacklight(nuviaColors.ambientGlow, radiusDp = 18.dp, alpha = 0.20f)
                .clip(AlbumSleeveShape)
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(
                    shape = AlbumSleeveShape,
                    width = 0.75.dp,
                    topHighlight = Color.White.copy(alpha = 0.25f),
                    bottomBorder = Color.Black.copy(alpha = 0.50f),
                ),
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
                modifier = Modifier.fillMaxSize(),
            )

            // Micro album badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "ALBUM",
                    style = TextStyle(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = Color.White.copy(alpha = 0.90f),
                )
            }

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = NUViAIcons.Pin,
                        contentDescription = "Pinned",
                        tint = nuviaColors.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.subtitle,
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
            ),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Dedicated Trending Community Playlists Shelf.
 * Strictly enforces 1:1 true square artwork (width == height, aspectRatio(1f)).
 * No wide or cinematic framing.
 */
@Composable
private fun NuviaCommunityPlaylistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Curated by listeners & communities" },
            badge = "COMMUNITY",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "community_playlist_card" },
            ) { item ->
                val current = item
                CommunityPlaylistSquareCard(
                    item = current,
                    onClick = remember(current) { { onItemClick(current) } },
                    onLongPress = remember(current, onItemLongPress) {
                        onItemLongPress?.let { cb -> { cb(current) } }
                    },
                    isPinned = isItemPinned?.invoke(current) == true,
                )
            }
        }
    }
}

private val CommunityCardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommunityPlaylistSquareCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    isPinned: Boolean = false,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(156.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
    ) {
        // Guaranteed 1:1 Square Artwork Container (width == height)
        Box(
            modifier = Modifier
                .width(156.dp)
                .aspectRatio(1f)
                .clip(CommunityCardShape)
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(
                    shape = CommunityCardShape,
                    width = 0.75.dp,
                    topHighlight = Color.White.copy(alpha = 0.25f),
                    bottomBorder = Color.Black.copy(alpha = 0.50f),
                ),
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
                    .fillMaxSize()
                    .aspectRatio(1f),
            )

            // Top Community Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "COMMUNITY",
                    style = TextStyle(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = nuviaColors.primary,
                )
            }

            // Floating Play Action over square artwork
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(nuviaColors.secondary, nuviaColors.primary),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = nuviaColors.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = NUViAIcons.Pin,
                        contentDescription = "Pinned",
                        tint = nuviaColors.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Playlist Title
        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        // Creator / Metadata
        Text(
            text = item.subtitle.ifBlank { "Community Playlist" },
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
            ),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Curated Liquid Glass Playlist & Mix Shelf.
 */
@Composable
private fun NuviaPlaylistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Personalized blends & mixes" },
            badge = "MIXES",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "playlist_card" },
            ) { item ->
                val current = item
                CuratedPlaylistCard(
                    item = current,
                    onClick = remember(current) { { onItemClick(current) } },
                    onLongPress = remember(current, onItemLongPress) {
                        onItemLongPress?.let { cb -> { cb(current) } }
                    },
                    isPinned = isItemPinned?.invoke(current) == true,
                )
            }
        }
    }
}

private val PlaylistCardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CuratedPlaylistCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    isPinned: Boolean = false,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(156.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .width(156.dp)
                .aspectRatio(1f)
                .nuviaAmbientBacklight(nuviaColors.secondary, radiusDp = 18.dp, alpha = 0.22f)
                .clip(PlaylistCardShape)
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(
                    shape = PlaylistCardShape,
                    width = 0.75.dp,
                    topHighlight = Color.White.copy(alpha = 0.25f),
                    bottomBorder = Color.Black.copy(alpha = 0.50f),
                ),
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
                    .fillMaxSize()
                    .aspectRatio(1f),
            )

            // Top Mix/Playlist Badge
            val badgeLabel = if (item.subtitle.contains("playlist", ignoreCase = true)) "PLAYLIST" else "NUViA MIX"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badgeLabel,
                    style = TextStyle(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = nuviaColors.primary,
                )
            }

            // Bottom Right Floating Play Icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(nuviaColors.secondary, nuviaColors.primary),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 10.dp, alpha = 0.40f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = nuviaColors.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = NUViAIcons.Pin,
                        contentDescription = "Pinned",
                        tint = nuviaColors.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.subtitle,
            style = TextStyle(
                fontFamily = Manrope,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
            ),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Standard NUViA Section Header with typography hierarchy and glowing accent indicator.
 * Maintained with full backwards compatibility for ExploreScreen and LibraryScreen.
 */
@Composable
internal fun SectionHeader(
    title: String,
    subtitle: String = "",
    badge: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glowing orange vertical brand indicator
                Box(
                    modifier = Modifier
                        .size(3.5.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(nuviaColors.primary)
                        .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 6.dp, alpha = 0.60f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                    ),
                    color = nuviaColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        if (actionText != null && onAction != null) {
            Box(
                modifier = Modifier
                    .nuviaTactilePress(onClick = onAction)
                    .clip(CircleShape)
                    .background(nuviaColors.glassSurface)
                    .border(0.75.dp, nuviaColors.glassBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = actionText,
                    style = TextStyle(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                    ),
                    color = nuviaColors.primary,
                )
            }
        } else if (badge != null) {
            NUViABadge(text = badge, isHighlight = true)
        }
    }
}

/**
 * Music shelf carousel used across HomeScreen, Explore, and Library.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    isItemPinned: ((ShelfItem) -> Boolean)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading", contentType = "leading_card") { card() } }
            items(
                items = shelf.items,
                key = { it.videoId ?: it.browseId ?: "${shelf.title}_${it.title}" },
                contentType = { "shelf_card" },
            ) { item ->
                val currentItem = item
                NuviaShelfCard(
                    item = currentItem,
                    onClick = remember(currentItem) { { onItemClick(currentItem) } },
                    onLongPress = remember(currentItem, onItemLongPress) {
                        onItemLongPress?.let { callback -> { callback(currentItem) } }
                    },
                    isPinned = isItemPinned?.invoke(currentItem) == true,
                )
            }
        }
    }
}

private val ShelfCardShape = RoundedCornerShape(16.dp)
private val ShelfCardTitleTextStyle = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    letterSpacing = (-0.1).sp,
)
private val ShelfCardSubtitleTextStyle = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
)

/**
 * Refined NUViA Shelf Card with tactile animation, specular highlight, and high-contrast typography.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NuviaShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    isPinned: Boolean = false,
) {
    val nuviaColors = LocalNUViAColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(SHELF_CARD_WIDTH)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .nuviaTactilePress(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .width(SHELF_CARD_WIDTH)
                .aspectRatio(1f),
        ) {
            when {
                item.browseId == "local:downloads" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .background(nuviaColors.glassSurfaceElevated)
                            .nuviaSpecularBorder(ShelfCardShape, 0.75.dp)
                            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 16.dp, alpha = 0.20f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Downloads",
                            tint = nuviaColors.primary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                item.browseId == "local:all" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .background(nuviaColors.glassSurfaceElevated)
                            .nuviaSpecularBorder(ShelfCardShape, 0.75.dp)
                            .nuviaAmbientBacklight(nuviaColors.secondary, radiusDp = 16.dp, alpha = 0.18f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = "Local Music",
                            tint = nuviaColors.secondary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                item.browseId == "nuvia:history" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .background(nuviaColors.glassSurfaceElevated)
                            .nuviaSpecularBorder(ShelfCardShape, 0.75.dp)
                            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 16.dp, alpha = 0.20f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "History",
                            tint = nuviaColors.primary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                item.browseId == "nuvia:replay" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .background(nuviaColors.glassSurfaceElevated)
                            .nuviaSpecularBorder(ShelfCardShape, 0.75.dp)
                            .nuviaAmbientBacklight(nuviaColors.secondary, radiusDp = 16.dp, alpha = 0.20f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Replay",
                            tint = nuviaColors.secondary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                item.thumbnailUrl == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        nuviaColors.surfaceCard,
                                        nuviaColors.primary.copy(alpha = 0.25f),
                                    )
                                )
                            )
                            .nuviaSpecularBorder(ShelfCardShape, 0.75.dp)
                            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 14.dp, alpha = 0.18f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = item.title,
                            tint = nuviaColors.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShelfCardShape)
                            .nuviaSpecularBorder(
                                shape = ShelfCardShape,
                                width = 0.75.dp,
                                topHighlight = Color.White.copy(alpha = 0.25f),
                                bottomBorder = Color.Black.copy(alpha = 0.40f),
                            )
                            .background(nuviaColors.surfaceCard),
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
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = NUViAIcons.Pin,
                        contentDescription = "Pinned",
                        tint = nuviaColors.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = ShelfCardTitleTextStyle,
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.subtitle,
            style = ShelfCardSubtitleTextStyle,
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Action card for creating a playlist or adding media (used in LibraryScreen).
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val cardCorner = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .width(SHELF_CARD_WIDTH)
            .nuviaTactilePress(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(SHELF_CARD_WIDTH)
                .aspectRatio(1f)
                .clip(cardCorner)
                .background(nuviaColors.glassSurfaceElevated)
                .border(
                    width = 1.dp,
                    color = nuviaColors.primary.copy(alpha = 0.40f),
                    shape = cardCorner,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = nuviaColors.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 12.sp,
            ),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sleek OLED-black empty state with transparent NUViA branding.
 */
@Composable
private fun HomeEmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 32.dp, alpha = 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                colorFilter = ColorFilter.tint(nuviaColors.primary),
                modifier = Modifier.size(54.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Your Universe Awaits",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            color = nuviaColors.textPrimary,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Discover recommended tracks, personalized mixes, and trending albums.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 14.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(24.dp))

        NUViAGlassButton(
            text = "Refresh Feed",
            icon = Icons.Rounded.Refresh,
            onClick = onRefresh,
            isPrimary = true,
        )
    }
}

/**
 * Error state with retry action.
 */
@Composable
private fun HomeErrorState(
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
                fontSize = 15.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        NUViAGlassButton(
            text = "Retry",
            onClick = onRetry,
            isPrimary = true,
        )
    }
}
