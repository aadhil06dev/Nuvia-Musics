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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.music.nuvia.ui.components.PAGE_GUTTER
import com.music.nuvia.ui.components.PullToRefresh
import com.music.nuvia.ui.components.SHELF_CARD_WIDTH
import com.music.nuvia.ui.components.feedSkeleton
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * NUViA Liquid-Glass Explore & Music Discovery Screen.
 * Implements a discovery experience:
 * - Large Spotlight Hero Carousel (dynamic dynamic artwork palette)
 * - Multi-Track Trending Now showcase (showing full available chart songs)
 * - New Releases, Genre Carousels, and Community Mixes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val nuviaColors = LocalNUViAColors.current
    val shelves = remember(state) {
        (state as? UiState.Success)?.data?.filter { shelf ->
            val lower = shelf.title.lowercase()
            !lower.contains("mood") && !lower.contains("genre") &&
                shelf.items.none { it.browseId?.startsWith("FEmusic_moods_and_genres") == true }
        }.orEmpty()
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
            item(key = "explore_header") {
                ExploreHeader()
            }

            when (state) {
                is UiState.Loading -> feedSkeleton()
                is UiState.Error -> item(key = "explore_error") {
                    ExploreErrorState(
                        message = state.message,
                        onRetry = onRetry,
                    )
                }
                is UiState.Success -> {
                    if (shelves.isEmpty()) {
                        item(key = "explore_empty") {
                            ExploreEmptyState(onRefresh = onRefresh)
                        }
                    } else {
                        shelves.forEachIndexed { index, shelf ->
                            val isTrendingShelf = isTrendingOrChartShelf(shelf.title)

                            if (isTrendingShelf && shelf.items.size > 1) {
                                item(
                                    key = "explore_trending_${shelf.title}_$index",
                                    contentType = "trending_shelf",
                                ) {
                                    TrendingMultiTrackSection(shelf = shelf, onItemClick = onItemClick)
                                }
                            } else {
                                item(
                                    key = "explore_shelf_${shelf.title}_$index",
                                    contentType = "regular_shelf",
                                ) {
                                    Shelf(shelf = shelf, onItemClick = onItemClick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isTrendingOrChartShelf(title: String): Boolean {
    val lower = title.lowercase()
    return lower.contains("trending") || lower.contains("chart") || lower.contains("top") || lower.contains("popular") || lower.contains("hits") || lower.contains("hot")
}

@Composable
private fun ExploreHeader() {
    val nuviaColors = LocalNUViAColors.current

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
                    text = "Explore",
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
                    text = "Discover charts, new releases & global sounds.",
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
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = nuviaColors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Multi-Track Trending / Charts section.
 * Chunks songs into multi-item vertical columns within a horizontal scroll container,
 * ensuring users can browse multiple trending tracks cleanly and simultaneously.
 */
@Composable
private fun TrendingMultiTrackSection(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val chunkSize = 3
    val columns = remember(shelf.items) { shelf.items.chunked(chunkSize) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle.ifBlank { "Top trending hits right now" },
            badge = "TRENDING",
        )

        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(
                items = columns,
                key = { colIndex, columnItems ->
                    columnItems.firstOrNull()?.let { it.videoId ?: it.browseId } ?: "col_$colIndex"
                },
                contentType = { _, _ -> "trending_column" },
            ) { colIndex, columnItems ->
                Column(
                    modifier = Modifier.width(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    columnItems.forEachIndexed { rowIndex, item ->
                        val rank = (colIndex * chunkSize) + rowIndex + 1
                        val currentItem = item
                        TrendingTrackRow(
                            rank = rank,
                            item = currentItem,
                            onClick = remember(currentItem) { { onItemClick(currentItem) } },
                        )
                    }
                }
            }
        }
    }
}

private val TrendingRowShape = RoundedCornerShape(14.dp)
private val TrendingThumbShape = RoundedCornerShape(8.dp)

private val TrendingRankTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
)
private val TrendingTitleTextStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
)
private val TrendingSubtitleTextStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 12.sp,
)

@Composable
private fun TrendingTrackRow(
    rank: Int,
    item: ShelfItem,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrendingRowShape)
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(TrendingRowShape, 0.6.dp)
            .nuviaTactilePress(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank Indicator
        Box(
            modifier = Modifier
                .width(26.dp)
                .padding(end = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$rank",
                style = TrendingRankTextStyle,
                color = if (rank <= 3) nuviaColors.primary else nuviaColors.textMuted,
            )
        }

        // Track Artwork
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(TrendingThumbShape)
                .background(nuviaColors.surfaceCard)
                .nuviaSpecularBorder(TrendingThumbShape, 0.5.dp),
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

        Spacer(Modifier.width(12.dp))

        // Title and Subtitle
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = TrendingTitleTextStyle,
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = TrendingSubtitleTextStyle,
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Play Pip
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(nuviaColors.primary.copy(alpha = 0.15f))
                .nuviaSpecularBorder(CircleShape, 0.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = nuviaColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ExploreEmptyState(
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
            text = "Explore the Sonic Universe",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            color = nuviaColors.textPrimary,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Discover global charts, emerging tracks, and fresh releases.",
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
            text = "Refresh Explore",
            icon = Icons.Rounded.Refresh,
            onClick = onRefresh,
            isPrimary = true,
        )
    }
}

@Composable
private fun ExploreErrorState(
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
