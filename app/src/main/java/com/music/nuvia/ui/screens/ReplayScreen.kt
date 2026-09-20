package com.music.nuvia.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.stats.ArtistFacts
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.stats.ListeningStats
import com.music.nuvia.data.stats.RankedEntry
import com.music.nuvia.data.stats.RankedSong
import com.music.nuvia.data.stats.ReplayPeriod
import com.music.nuvia.data.stats.ReplaySummary
import com.music.nuvia.ui.components.LiquidGlassCard
import com.music.nuvia.ui.components.LiquidGlassChip
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.NUViASectionHeader
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import kotlinx.coroutines.launch

@Composable
fun ReplayScreen(
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(ReplayPeriod.THIS_MONTH) }
    var summary by remember { mutableStateOf<ReplaySummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val factsRevision by ArtistFacts.revision.collectAsStateWithLifecycle()

    LaunchedEffect(selectedPeriod, factsRevision) {
        if (summary == null) isLoading = true
        summary = ListeningStats.summary(selectedPeriod)
        isLoading = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled),
        contentPadding = contentPadding,
    ) {
        // Top Back Header
        item(key = "replay_top_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                    size = 40.dp,
                    iconSize = 20.dp,
                    tier = NUViAGlassTier.Subtle,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Listening Replay",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
            }
        }

        // Period Selection Chips
        item(key = "replay_periods") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReplayPeriod.entries.forEach { period ->
                    LiquidGlassChip(
                        label = period.chip,
                        selected = period == selectedPeriod,
                        onClick = { selectedPeriod = period },
                    )
                }
            }
        }

        if (isLoading) {
            item(key = "replay_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = nuviaColors.primary)
                }
            }
        } else {
            val s = summary
            if (s == null || s.isEmpty) {
                item(key = "replay_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No listening recorded for ${selectedPeriod.chip.lowercase()}.\nPlay music in NUViA to generate your Replay.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = Manrope,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            ),
                            color = nuviaColors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                // Key Metrics Hero Card
                item(key = "replay_metrics_card") {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        tier = NUViAGlassTier.Elevated,
                        ambientGlow = true,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                text = s.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                ),
                                color = nuviaColors.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "${s.minutes}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = Sora,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp,
                                        ),
                                        color = nuviaColors.textPrimary,
                                    )
                                    Text(
                                        text = "Minutes listened",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Manrope),
                                        color = nuviaColors.textSecondary,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${s.totalPlays}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = Sora,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp,
                                        ),
                                        color = nuviaColors.textPrimary,
                                    )
                                    Text(
                                        text = "Tracks played",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Manrope),
                                        color = nuviaColors.textSecondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${s.distinctArtists} artists • ${s.distinctAlbums} albums",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Manrope),
                                    color = nuviaColors.textMuted,
                                )
                                s.peakHour?.let { peak ->
                                    Text(
                                        text = "Peak hour: %02d:00".format(peak),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Manrope),
                                        color = nuviaColors.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Songs Section
                if (s.songs.isNotEmpty()) {
                    item(key = "top_songs_header") {
                        NUViASectionHeader(title = "Top Songs")
                    }
                    itemsIndexed(
                        items = s.songs.take(10),
                        key = { index, rs -> "replay_song_${rs.song.videoId}_$index" },
                    ) { index, ranked ->
                        ReplaySongRow(
                            rank = index + 1,
                            song = ranked.song,
                            plays = ranked.plays,
                            minutes = ranked.ms / 60_000,
                            onClick = { onSongClick(ranked.song) },
                        )
                    }
                }

                // Top Artists Section
                if (s.artists.isNotEmpty()) {
                    item(key = "top_artists_header") {
                        NUViASectionHeader(title = "Top Artists")
                    }
                    item(key = "top_artists_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(s.artists.take(8)) { artist ->
                                ReplayArtistCard(artist = artist)
                            }
                        }
                    }
                }

                // Top Albums Section
                if (s.albums.isNotEmpty()) {
                    item(key = "top_albums_header") {
                        NUViASectionHeader(title = "Top Albums")
                    }
                    item(key = "top_albums_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(s.albums.take(8)) { album ->
                                ReplayAlbumCard(album = album)
                            }
                        }
                    }
                }

                // Top Genres Section
                if (s.genres.isNotEmpty()) {
                    item(key = "top_genres_header") {
                        NUViASectionHeader(title = "Top Genres")
                    }
                    item(key = "top_genres_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(s.genres.take(8)) { genre ->
                                ReplayGenreChip(genre = genre)
                            }
                        }
                    }
                }

                item(key = "bottom_space") {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ReplayGenreChip(genre: RankedEntry) {
    val nuviaColors = LocalNUViAColors.current
    LiquidGlassCard(
        modifier = Modifier.height(38.dp),
        tier = NUViAGlassTier.Subtle,
        shape = RoundedCornerShape(19.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = genre.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Text(
                text = "${genre.ms / 60_000}m",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Manrope,
                    fontSize = 11.sp,
                ),
                color = nuviaColors.textMuted,
            )
        }
    }
}

@Composable
private fun ReplaySongRow(
    rank: Int,
    song: Song,
    plays: Int,
    minutes: Long,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            color = if (rank <= 3) nuviaColors.primary else nuviaColors.textMuted,
            modifier = Modifier.width(28.dp),
        )
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textPrimary,
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
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$plays plays",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                ),
                color = nuviaColors.textPrimary,
            )
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Manrope),
                color = nuviaColors.textMuted,
            )
        }
    }
}

@Composable
private fun ReplayArtistCard(artist: RankedEntry) {
    val nuviaColors = LocalNUViAColors.current
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${artist.plays} plays",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Manrope),
            color = nuviaColors.textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReplayAlbumCard(album: RankedEntry) {
    val nuviaColors = LocalNUViAColors.current
    Column(
        modifier = Modifier.width(110.dp),
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
            color = nuviaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.subtitle ?: "${album.plays} plays",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Manrope),
            color = nuviaColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

