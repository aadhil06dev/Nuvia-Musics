package com.music.nuvia.data.backup

import android.content.Context
import android.net.Uri
import com.music.nuvia.data.backup.model.NuviaBackup
import com.music.nuvia.data.backup.model.PlaylistBackup
import com.music.nuvia.data.backup.model.QueueBackup
import com.music.nuvia.data.backup.model.SettingsBackup
import com.music.nuvia.data.backup.model.TrackMetadataBackup
import com.music.nuvia.data.lyrics.LyricsSource
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.playlist.LocalPlaylist
import com.music.nuvia.data.playlist.LocalPlaylistStore
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.AudioQuality
import com.music.nuvia.data.settings.DownloadQuality
import com.music.nuvia.data.settings.SearchHistory
import com.music.nuvia.data.settings.ThemeMode
import com.music.nuvia.download.Downloads
import com.music.nuvia.download.SavedCollection
import com.music.nuvia.download.SavedSongMetadata
import com.music.nuvia.playback.LastPlayed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RestoreStrategy {
    MERGE,
    OVERWRITE,
}

data class RestoreOptions(
    val strategy: RestoreStrategy = RestoreStrategy.MERGE,
    val restoreSettings: Boolean = true,
    val restorePlaylists: Boolean = true,
    val restorePinnedPlaylists: Boolean = true,
    val restoreSearchHistory: Boolean = true,
    val restoreQueue: Boolean = false,
)

data class RestoreSummary(
    val restoredPlaylists: Int,
    val restoredTracks: Int,
    val restoredPinnedCount: Int,
    val restoredSearchHistoryCount: Int,
    val settingsRestored: Boolean,
)

object BackupRepository {

    /**
     * Builds a comprehensive, versioned, sensitive-data-free NuviaBackup object
     * from current device state.
     */
    suspend fun createBackup(context: Context): NuviaBackup = withContext(Dispatchers.IO) {
        // 1. Safe Settings (Strictly no auth, tokens, secrets or keys!)
        val settingsBackup = SettingsBackup(
            audioQualityWifi = AppSettings.audioQualityWifi.value.name,
            audioQualityCellular = AppSettings.audioQualityCellular.value.name,
            downloadQuality = AppSettings.downloadQuality.value.name,
            wifiOnlyDownloads = AppSettings.wifiOnlyDownloads.value,
            losslessAudio = AppSettings.losslessAudio.value,
            crossfadeSeconds = AppSettings.crossfadeSeconds.value,
            smartFadeEnabled = AppSettings.smartFadeEnabled.value,
            skipSilence = AppSettings.skipSilence.value,
            spatialAudio = AppSettings.spatialAudio.value,
            playbackSpeed = AppSettings.playbackSpeed.value,
            autoplay = AppSettings.autoplay.value,
            showNerdStats = AppSettings.showNerdStats.value,
            stopOnTaskRemoved = AppSettings.stopOnTaskRemoved.value,
            hideVolumeBar = AppSettings.hideVolumeBar.value,
            swipeToPlayNext = AppSettings.swipeToPlayNext.value,
            audioCacheLimitBytes = AppSettings.audioCacheLimitBytes.value,
            themeMode = AppSettings.themeMode.value.name,
            reduceAnimation = AppSettings.reduceAnimation.value,
            reduceDynamicBlur = AppSettings.reduceDynamicBlur.value,
            dynamicLighting = AppSettings.dynamicLighting.value,
            minimalUi = AppSettings.minimalUi.value,
            dynamicArtworkColors = AppSettings.dynamicArtworkColors.value,
            ambientLighting = AppSettings.ambientLighting.value,
            uiGlow = AppSettings.uiGlow.value,
            glassEffects = AppSettings.glassEffects.value,
            animatedCanvas = AppSettings.animatedCanvas.value,
            fullBleedArtwork = AppSettings.fullBleedArtwork.value,
            syncedLyrics = AppSettings.syncedLyrics.value,
            lyricsSources = AppSettings.lyricsSources.value.map { it.name },
            lyricsSourceOrder = AppSettings.lyricsSourceOrder.value.map { it.name },
            prioritizeSyllableSync = AppSettings.prioritizeSyllableSync.value,
            scrobbleMinDuration = AppSettings.scrobbleMinDuration.value,
            scrobbleDelayPercent = AppSettings.scrobbleDelayPercent.value,
            scrobbleDelaySeconds = AppSettings.scrobbleDelaySeconds.value,
        )

        // 2. Playlists & Tracks
        val pinned = AppSettings.pinnedPlaylists.value
        val localPlaylists = LocalPlaylistStore.getAll()
        val collections = Downloads.collections.value.values.filter { it.playlist }
        val savedMeta = Downloads.savedMetadata.value

        val playlistBackups = mutableListOf<PlaylistBackup>()
        val seenIds = mutableSetOf<String>()

        // From LocalPlaylistStore
        for (pl in localPlaylists) {
            seenIds.add(pl.id)
            seenIds.add(pl.id.removePrefix("VL"))
            playlistBackups.add(
                PlaylistBackup(
                    id = pl.id,
                    title = pl.title,
                    subtitle = pl.subtitle,
                    thumbnailUrl = pl.thumbnailUrl,
                    isPinned = pl.isPinned || pl.id in pinned || "VL${pl.id}" in pinned,
                    createdAt = pl.createdAt,
                    updatedAt = pl.updatedAt,
                    tracks = pl.tracks.map { song ->
                        TrackMetadataBackup(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            durationText = song.durationText,
                            albumName = song.albumName,
                        )
                    },
                )
            )
        }

        // From Downloads collections
        for (col in collections) {
            val rawId = col.id.removePrefix("VL")
            if (rawId in seenIds || col.id in seenIds) continue
            seenIds.add(rawId)
            seenIds.add(col.id)

            val tracks = col.videoIds.mapNotNull { videoId ->
                val meta = savedMeta[videoId]
                if (meta != null) {
                    TrackMetadataBackup(
                        videoId = meta.videoId,
                        title = meta.title,
                        artist = meta.artist,
                        thumbnailUrl = meta.thumbnailUrl,
                        durationText = meta.durationText,
                        albumName = meta.albumName,
                    )
                } else null
            }

            playlistBackups.add(
                PlaylistBackup(
                    id = col.id,
                    title = col.title,
                    subtitle = col.subtitle,
                    thumbnailUrl = col.thumbnailUrl,
                    isPinned = col.id in pinned || rawId in pinned,
                    tracks = tracks,
                )
            )
        }

        // 3. Search History
        val history = SearchHistory.recent.value

        // 4. Queue Snapshot (Track metadata only, never audio files)
        val snapshot = LastPlayed.load()
        val queueBackup = snapshot?.let { snap ->
            QueueBackup(
                tracks = snap.songs.map { song ->
                    TrackMetadataBackup(
                        videoId = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        durationText = song.durationText,
                        albumName = song.albumName,
                    )
                },
                currentIndex = snap.index,
                positionMs = snap.positionMs,
            )
        }

        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"

        NuviaBackup(
            formatVersion = NuviaBackup.CURRENT_FORMAT_VERSION,
            app = NuviaBackup.APP_NAME,
            appVersion = appVersion,
            exportedAt = System.currentTimeMillis(),
            settings = settingsBackup,
            playlists = playlistBackups,
            pinnedPlaylists = pinned,
            searchHistory = history,
            queueSnapshot = queueBackup,
        )
    }

    /**
     * Serializes backup and writes to output stream (SAF Uri).
     */
    suspend fun exportToFile(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = createBackup(context)
            val jsonString = BackupValidator.json.encodeToString(NuviaBackup.serializer(), backup)
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                outStream.bufferedWriter().use { it.write(jsonString) }
            } ?: throw IllegalStateException("Could not open destination file for writing")
        }
    }

    /**
     * Reads from input stream (SAF Uri) and validates the backup.
     */
    suspend fun readAndValidate(context: Context, uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inStream ->
                inStream.bufferedReader().use { it.readText() }
            } ?: return@withContext ValidationResult.Error("Could not open source file for reading")

            val existingPlaylists = LocalPlaylistStore.getAll()
            val existingIds = existingPlaylists.map { it.id }.toSet() +
                Downloads.collections.value.keys
            val existingTitles = existingPlaylists.map { it.title }.toSet() +
                Downloads.collections.value.values.map { it.title }

            BackupValidator.validate(jsonString, existingIds, existingTitles)
        } catch (e: Exception) {
            ValidationResult.Error("Failed to read backup file: ${e.message}", e)
        }
    }

    /**
     * Restores backup according to options and strategy.
     */
    suspend fun restore(
        context: Context,
        backup: NuviaBackup,
        options: RestoreOptions,
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            var restoredPlaylistsCount = 0
            var restoredTracksCount = 0
            var restoredPinnedCount = 0
            var restoredSearchCount = 0
            var settingsRestored = false

            // 1. Settings restoration
            if (options.restoreSettings && backup.settings != null) {
                val s = backup.settings
                s.audioQualityWifi?.let { name ->
                    AudioQuality.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.let { AppSettings.setAudioQualityWifi(it) }
                }
                s.audioQualityCellular?.let { name ->
                    AudioQuality.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.let { AppSettings.setAudioQualityCellular(it) }
                }
                s.downloadQuality?.let { name ->
                    DownloadQuality.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.let { AppSettings.setDownloadQuality(it) }
                }
                s.wifiOnlyDownloads?.let { AppSettings.setWifiOnlyDownloads(it) }
                s.losslessAudio?.let { AppSettings.setLosslessAudio(it) }
                s.crossfadeSeconds?.let { AppSettings.setCrossfadeSeconds(it) }
                s.smartFadeEnabled?.let { AppSettings.setSmartFadeEnabled(it) }
                s.skipSilence?.let { AppSettings.setSkipSilence(it) }
                s.spatialAudio?.let { AppSettings.setSpatialAudio(it) }
                s.playbackSpeed?.let { AppSettings.setPlaybackSpeed(it) }
                s.autoplay?.let { AppSettings.setAutoplay(it) }
                s.showNerdStats?.let { AppSettings.setShowNerdStats(it) }
                s.stopOnTaskRemoved?.let { AppSettings.setStopOnTaskRemoved(it) }
                s.hideVolumeBar?.let { AppSettings.setHideVolumeBar(it) }
                s.swipeToPlayNext?.let { AppSettings.setSwipeToPlayNext(it) }
                s.audioCacheLimitBytes?.let { AppSettings.setAudioCacheLimitBytes(it) }

                s.themeMode?.let { name ->
                    ThemeMode.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.let { AppSettings.setThemeMode(it) }
                }
                s.reduceAnimation?.let { AppSettings.setReduceAnimation(it) }
                s.reduceDynamicBlur?.let { AppSettings.setReduceDynamicBlur(it) }
                s.dynamicLighting?.let { AppSettings.setDynamicLighting(it) }
                s.minimalUi?.let { AppSettings.setMinimalUi(it) }
                s.dynamicArtworkColors?.let { AppSettings.setDynamicArtworkColors(it) }
                s.ambientLighting?.let { AppSettings.setAmbientLighting(it) }
                s.uiGlow?.let { AppSettings.setUiGlow(it) }
                s.glassEffects?.let { AppSettings.setGlassEffects(it) }
                s.animatedCanvas?.let { AppSettings.setAnimatedCanvas(it) }
                s.fullBleedArtwork?.let { AppSettings.setFullBleedArtwork(it) }

                s.syncedLyrics?.let { AppSettings.setSyncedLyrics(it) }
                s.lyricsSources?.let { list ->
                    val sources = list.mapNotNull { name ->
                        LyricsSource.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    }.toSet()
                    if (sources.isNotEmpty()) AppSettings.setLyricsSources(sources)
                }
                s.lyricsSourceOrder?.let { list ->
                    val sources = list.mapNotNull { name ->
                        LyricsSource.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    }
                    if (sources.isNotEmpty()) AppSettings.setLyricsSourceOrder(sources)
                }
                s.prioritizeSyllableSync?.let { AppSettings.setPrioritizeSyllableSync(it) }

                s.scrobbleMinDuration?.let { AppSettings.setScrobbleMinDuration(it) }
                s.scrobbleDelayPercent?.let { AppSettings.setScrobbleDelayPercent(it) }
                s.scrobbleDelaySeconds?.let { AppSettings.setScrobbleDelaySeconds(it) }

                settingsRestored = true
            }

            // 2. Playlists & Tracks restoration
            if (options.restorePlaylists && backup.playlists.isNotEmpty()) {
                val overwrite = options.strategy == RestoreStrategy.OVERWRITE
                val localPlaylistsToRestore = backup.playlists.map { pl ->
                    val songs = pl.tracks.map { track ->
                        Song(
                            videoId = track.videoId,
                            title = track.title,
                            artist = track.artist,
                            thumbnailUrl = track.thumbnailUrl,
                            durationText = track.durationText,
                            albumName = track.albumName,
                        )
                    }
                    LocalPlaylist(
                        id = pl.id,
                        title = pl.title,
                        subtitle = pl.subtitle ?: "${songs.size} tracks",
                        thumbnailUrl = pl.thumbnailUrl ?: songs.firstOrNull()?.thumbnailUrl,
                        isPinned = pl.isPinned,
                        createdAt = if (pl.createdAt > 0L) pl.createdAt else System.currentTimeMillis(),
                        updatedAt = if (pl.updatedAt > 0L) pl.updatedAt else System.currentTimeMillis(),
                        tracks = songs,
                    )
                }

                LocalPlaylistStore.restorePlaylists(localPlaylistsToRestore, overwrite)

                // Also register in Downloads collections & metadata so they appear in library collections
                val collectionsToSave = mutableMapOf<String, SavedCollection>()
                val metaToSave = mutableMapOf<String, SavedSongMetadata>()

                for (pl in backup.playlists) {
                    collectionsToSave[pl.id] = SavedCollection(
                        id = pl.id,
                        title = pl.title,
                        subtitle = pl.subtitle ?: "${pl.tracks.size} tracks",
                        thumbnailUrl = pl.thumbnailUrl ?: pl.tracks.firstOrNull()?.thumbnailUrl,
                        playlist = true,
                        videoIds = pl.tracks.map { it.videoId },
                    )
                    for (track in pl.tracks) {
                        if (track.videoId !in metaToSave) {
                            metaToSave[track.videoId] = SavedSongMetadata(
                                videoId = track.videoId,
                                title = track.title,
                                artist = track.artist,
                                thumbnailUrl = track.thumbnailUrl,
                                durationText = track.durationText,
                                albumName = track.albumName,
                                uri = "", // Never fake binary files
                            )
                        }
                    }
                }

                Downloads.restoreCollectionsAndMetadata(collectionsToSave, metaToSave, overwrite)

                restoredPlaylistsCount = localPlaylistsToRestore.size
                restoredTracksCount = localPlaylistsToRestore.sumOf { it.tracks.size }
            }

            // 3. Pinned Playlists restoration
            if (options.restorePinnedPlaylists && backup.pinnedPlaylists.isNotEmpty()) {
                val cleanPins = backup.pinnedPlaylists.filter { it.isNotBlank() }.distinct()
                val finalPins = if (options.strategy == RestoreStrategy.OVERWRITE) {
                    cleanPins.take(AppSettings.MAX_PINNED_PLAYLISTS)
                } else {
                    val current = AppSettings.pinnedPlaylists.value
                    (current + cleanPins).distinct().take(AppSettings.MAX_PINNED_PLAYLISTS)
                }
                AppSettings.setPinnedPlaylists(finalPins)
                restoredPinnedCount = finalPins.size
            }

            // 4. Search History restoration
            if (options.restoreSearchHistory && backup.searchHistory.isNotEmpty()) {
                SearchHistory.restore(
                    queries = backup.searchHistory,
                    overwrite = options.strategy == RestoreStrategy.OVERWRITE,
                )
                restoredSearchCount = backup.searchHistory.size
            }

            // 5. Queue Snapshot restoration
            if (options.restoreQueue && backup.queueSnapshot != null) {
                val q = backup.queueSnapshot
                val songs = q.tracks.map {
                    Song(
                        videoId = it.videoId,
                        title = it.title,
                        artist = it.artist,
                        thumbnailUrl = it.thumbnailUrl,
                        durationText = it.durationText,
                        albumName = it.albumName,
                    )
                }
                if (songs.isNotEmpty()) {
                    LastPlayed.save(songs, q.currentIndex, q.positionMs)
                }
            }

            RestoreSummary(
                restoredPlaylists = restoredPlaylistsCount,
                restoredTracks = restoredTracksCount,
                restoredPinnedCount = restoredPinnedCount,
                restoredSearchHistoryCount = restoredSearchCount,
                settingsRestored = settingsRestored,
            )
        }
    }
}
