package com.music.nuvia.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.settings.AppSettings
import kotlin.math.abs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.artworkAt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

private val DarkThumbnailBorderColor = Color.White.copy(alpha = 0.15f)
private val LightThumbnailBorderColor = Color.Black.copy(alpha = 0.15f)

@Composable
fun Modifier.thumbnailBorder(shape: Shape): Modifier {
    val borderColor = if (isSystemInDarkTheme()) DarkThumbnailBorderColor else LightThumbnailBorderColor
    return this.border(
        width = 1.dp,
        color = borderColor,
        shape = shape,
    )
}

/**
 * The left and right inset every page's content sits at.
 *
 * It is the same inset the mini player and the tab bar float at, so the edge of
 * a track row, a card or a heading lines up with the edge of the bars stacked
 * below them rather than stepping in from them. One constant, shared by the
 * bars and the pages, is what keeps that true.
 */
val PAGE_GUTTER = 20.dp

/** Where a divider under a track row starts: clear of the 52dp of artwork. */
val ROW_DIVIDER_INSET = PAGE_GUTTER + 68.dp

/**
 * Width of a card in the compact carousels — home shelves, library shelves and
 * the artist page's releases alike.
 *
 * Sized so a phone-width row shows two cards whole with the edge of a third
 * showing: enough to say the row scrolls without a card being half a card.
 */
val SHELF_CARD_WIDTH = 132.dp

/**
 * One track row, used by search, library and detail pages.
 *
 * Swiping it either way queues the track or plays it next, per
 * [AppSettings.swipeToPlayNext] — the row springs back rather than
 * dismissing, since nothing is being removed. Long-press opens the actions
 * menu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onSwipeToQueue: (() -> Unit)? = null,
    /**
     * What the row paints over the swipe reveal as it slides back.
     *
     * It has to be the colour of the page the row is *on*, not the theme's
     * background — an album page tinted from its sleeve would otherwise drag a
     * black band across itself on every swipe.
     */
    rowBackground: Color = MaterialTheme.colorScheme.background,
    /**
     * Drawn in place of the artwork, for lists where every row would otherwise
     * repeat the same cover — an album's own track listing.
     */
    trackNumber: Int? = null,
    /**
     * The artist line, and the track number when there is one.
     *
     * A page tinted from its artwork wants this brighter than the flat feeds
     * do: the usual dim grey is pitched against black, and against a mid-toned
     * wash it stops being legible as a second line and starts disappearing.
     */
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (onSwipeToQueue == null) {
        SongRowContent(song, onClick, onLongPress, modifier, trackNumber, subtitleColor)
    } else {
        SwipeableSongRow(
            song = song,
            onClick = onClick,
            onLongPress = onLongPress,
            onSwipeToQueue = onSwipeToQueue,
            modifier = modifier,
            rowBackground = rowBackground,
            trackNumber = trackNumber,
            subtitleColor = subtitleColor,
        )
    }
}

private val SWIPE_POSITIONAL_THRESHOLD: (Float) -> Float = { distance -> distance * 0.45f }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSongRow(
    song: Song,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    onSwipeToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    rowBackground: Color = MaterialTheme.colorScheme.background,
    trackNumber: Int? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val currentOnSwipeToQueue by rememberUpdatedState(onSwipeToQueue)
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = remember {
            { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    currentOnSwipeToQueue()
                }
                false // never actually dismiss; snap back
            }
        },
        positionalThreshold = SWIPE_POSITIONAL_THRESHOLD,
    )

    val isSwiping = swipeState.dismissDirection != SwipeToDismissBoxValue.Settled ||
        swipeState.targetValue != SwipeToDismissBoxValue.Settled

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        backgroundContent = {
            if (isSwiping) {
                QueueSwipeBackground(swipeState)
            }
        },
    ) {
        SongRowContent(
            song = song,
            onClick = onClick,
            onLongPress = onLongPress,
            modifier = Modifier.background(rowBackground),
            trackNumber = trackNumber,
            subtitleColor = subtitleColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSwipeBackground(swipeState: SwipeToDismissBoxState) {
    val playNext = AppSettings.swipeToPlayNext.value
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .padding(horizontal = PAGE_GUTTER + 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QueueSwipeLabel(playNext)
        QueueSwipeLabel(playNext)
    }
}

@Composable
private fun QueueSwipeLabel(playNext: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (playNext) Icons.Rounded.PlaylistPlay else Icons.Rounded.PlaylistAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (playNext) "Play next" else "Queue",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRowContent(
    song: Song,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trackNumber: Int? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (trackNumber != null) {
            // Same 52dp the artwork would take, so a numbered list and an
            // illustrated one share a left edge and a divider inset.
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "$trackNumber",
                    style = MaterialTheme.typography.bodyLarge,
                    color = subtitleColor,
                )
            }
        } else {
            val context = LocalContext.current
            val artworkUrl = remember(song.thumbnailUrl) {
                song.artworkAt(ROW_ART_PX)
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
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .thumbnailBorder(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        song.durationText?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = subtitleColor,
            )
        }
        // Same sheet the long-press opens, for anyone who doesn't think to hold.
        if (onLongPress != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLongPress),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Pull-to-refresh for the tab feeds, with the usual circular puck suppressed.
 *
 * The feeds sit under a frosted bar that already occupies the top 96dp, so a
 * puck dropping into that space would be blurred out by the glass it lands
 * behind. The drag feedback is the loader line along the bottom edge of the
 * bar instead — which is why [state] is hoisted: the bar lives beside this
 * content, not inside it, and has to follow the same drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier.fillMaxSize(),
        indicator = {},
    ) {
        content()
    }
}

/** Slim dismissible-looking prompt shown atop Home while signed out. */
@Composable
fun SignInBanner(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val nuviaColors = LocalNUViAColors.current
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .clip(shape)
            .background(nuviaColors.glassSurfaceElevated)
            .nuviaSpecularBorder(shape, 0.75.dp)
            .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 18.dp, alpha = 0.16f)
            .clickable(onClick = onSignIn)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Sign in to YouTube Music",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Unlock personalized recommendations and mixes",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 12.sp,
                ),
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        NUViAGlassButton(
            text = "Sign in",
            onClick = onSignIn,
            isPrimary = true,
        )
    }
}

@Composable
fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER + 12.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Manrope,
                fontSize = 14.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            NUViAGlassButton(
                text = actionLabel,
                onClick = onAction,
                isPrimary = true,
            )
        }
    }
}
