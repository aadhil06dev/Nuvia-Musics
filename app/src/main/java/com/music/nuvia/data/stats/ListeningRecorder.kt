package com.music.nuvia.data.stats

import com.music.nuvia.data.YtMusicRepository
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.durationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Turns the player's progress ticking into aggregate numbers kept by [ListeningStats].
 *
 * Samples wall-clock time between audio decoder ticks to accurately track real listening:
 * - Seeking cannot inflate minutes because position is never consulted.
 * - Pauses cannot inflate minutes because no tick happens.
 * - Caps interval step at [MAX_STEP_MS] to prevent background/resume jumps.
 * - Enriches songs missing album or artist IDs using [YtMusicRepository.trackLinks].
 * - Enforces minimum [PLAY_FLOOR_MS] (30 seconds) for a play count.
 */
object ListeningRecorder {

    private var currentId: String? = null
    private var lastSampleAt: Long = 0L
    private var playedThisTrack: Long = 0L
    private var playCounted = false
    private var samplesSinceFlush = 0

    private val extras = ConcurrentHashMap<String, Song>()
    private val asked = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val MAX_STEP_MS = 8_000L
    private const val PLAY_FLOOR_MS = 30_000L
    private const val PLAY_CEILING_MS = 4 * 60 * 1000L
    private const val FLUSH_EVERY = 6
    private const val YOUTUBE_ID_LENGTH = 11

    @Synchronized
    fun onSample(song: Song, durationMs: Long) {
        val now = System.currentTimeMillis()
        if (song.videoId != currentId) {
            currentId = song.videoId
            lastSampleAt = now
            playedThisTrack = 0L
            playCounted = false
            return
        }
        val step = (now - lastSampleAt).coerceIn(0L, MAX_STEP_MS)
        lastSampleAt = now
        if (step <= 0L) return
        playedThisTrack += step

        val length = durationMs.takeIf { it > 0 } ?: song.durationMillis()
        val threshold = if (length > 0) {
            min(length / 2, PLAY_CEILING_MS).coerceAtLeast(PLAY_FLOOR_MS)
        } else {
            PLAY_FLOOR_MS
        }
        val counts = !playCounted && playedThisTrack >= threshold
        if (counts) playCounted = true

        ListeningStats.record(enriched(song), step, counts)

        if (++samplesSinceFlush >= FLUSH_EVERY) {
            samplesSinceFlush = 0
            ListeningStats.flush()
        }
    }

    @Synchronized
    fun onStopped() {
        currentId = null
        lastSampleAt = 0L
        playedThisTrack = 0L
        playCounted = false
        samplesSinceFlush = 0
        ListeningStats.flush()
    }

    @Synchronized
    fun onPlaybackStopped() {
        onStopped()
    }

    private fun enriched(song: Song): Song {
        extras[song.videoId]?.let { extra ->
            return song.copy(
                artistId = song.artistId ?: extra.artistId,
                albumId = song.albumId ?: extra.albumId,
                albumName = song.albumName ?: extra.albumName,
            )
        }
        if (song.albumName != null && song.artistId != null) return song
        if (song.localUri != null || song.videoId.length != YOUTUBE_ID_LENGTH) return song
        if (asked.add(song.videoId)) {
            scope.launch {
                YtMusicRepository.trackLinks(song.videoId).getOrNull()?.let {
                    extras[song.videoId] = it
                }
            }
        }
        return song
    }
}
