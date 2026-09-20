package com.music.nuvia.data.backup.model

import kotlinx.serialization.Serializable

/**
 * Root data structure for NUViA versioned backup files.
 *
 * Designed with strict privacy and security rules:
 * - NEVER contains binary audio or media files
 * - NEVER contains authentication tokens, cookies, or session keys
 * - NEVER contains API keys, passwords, or secrets
 * - Fully versioned with schema validation and unknown-field resilience
 */
@Serializable
data class NuviaBackup(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val app: String = APP_NAME,
    val appVersion: String,
    val exportedAt: Long,
    val settings: SettingsBackup? = null,
    val playlists: List<PlaylistBackup> = emptyList(),
    val pinnedPlaylists: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val queueSnapshot: QueueBackup? = null,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val APP_NAME = "NUViA"
        val SUPPORTED_APPS = setOf("NUViA", "BitChord")
    }
}

@Serializable
data class PlaylistBackup(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val tracks: List<TrackMetadataBackup> = emptyList(),
)

@Serializable
data class TrackMetadataBackup(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val albumName: String? = null,
)

@Serializable
data class QueueBackup(
    val tracks: List<TrackMetadataBackup> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
)

@Serializable
data class SettingsBackup(
    // Audio quality & format
    val audioQualityWifi: String? = null,
    val audioQualityCellular: String? = null,
    val downloadQuality: String? = null,
    val wifiOnlyDownloads: Boolean? = null,
    val losslessAudio: Boolean? = null,

    // Playback transitions & behavior
    val crossfadeSeconds: Int? = null,
    val smartFadeEnabled: Boolean? = null,
    val skipSilence: Boolean? = null,
    val spatialAudio: Boolean? = null,
    val playbackSpeed: Float? = null,
    val autoplay: Boolean? = null,
    val showNerdStats: Boolean? = null,
    val stopOnTaskRemoved: Boolean? = null,
    val hideVolumeBar: Boolean? = null,
    val swipeToPlayNext: Boolean? = null,
    val audioCacheLimitBytes: Long? = null,

    // Visuals & Liquid-Glass Theme
    val themeMode: String? = null,
    val reduceAnimation: Boolean? = null,
    val reduceDynamicBlur: Boolean? = null,
    val dynamicLighting: Boolean? = null,
    val minimalUi: Boolean? = null,
    val dynamicArtworkColors: Boolean? = null,
    val ambientLighting: Boolean? = null,
    val uiGlow: Boolean? = null,
    val glassEffects: Boolean? = null,
    val animatedCanvas: Boolean? = null,
    val fullBleedArtwork: Boolean? = null,

    // Lyrics configuration
    val syncedLyrics: Boolean? = null,
    val lyricsSources: List<String>? = null,
    val lyricsSourceOrder: List<String>? = null,
    val prioritizeSyllableSync: Boolean? = null,

    // Non-sensitive playback scrobble timing preferences
    val scrobbleMinDuration: Int? = null,
    val scrobbleDelayPercent: Float? = null,
    val scrobbleDelaySeconds: Int? = null,
)
