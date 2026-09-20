package com.music.nuvia.playback

import android.os.SystemClock
import androidx.media3.common.Player
import com.music.nuvia.data.DebugLog as Log
import com.music.nuvia.data.listentogether.ListenTogether
import com.music.nuvia.data.listentogether.PartyTrack
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.sources.TrackMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Makes the player obey the party, and the party obey this player.
 *
 * Runs in [PlaybackService] to maintain sync even when backgrounded.
 */
class PartySync(
    private val scope: CoroutineScope,
    private val player: () -> Player?,
) {

    private val jobs = mutableListOf<Job>()
    private var publishJob: Job? = null
    private var startJob: Job? = null

    private var reconcileQuietUntilMs = 0L
    private var awaitSeq = Long.MAX_VALUE
    private var loadingVideoId: String? = null
    private var alignedSeq = -1L

    @Volatile
    private var deferredPlayPending = false

    @Volatile
    private var focusLost = false

    @Volatile
    private var rejoining = false

    private var driftStrikes = 0
    private var driftCooldownUntilMs = 0L

    fun start() {
        jobs += scope.launch {
            ListenTogether.state
                .map { Triple(it.playback.seq, it.queue.seq, it.code) }
                .distinctUntilChanged()
                .collect { (seq, _, _) ->
                    if (seq >= awaitSeq) reconcileQuietUntilMs = 0L
                    reconcile()
                }
        }
        jobs += scope.launch {
            while (true) {
                delay(TICK_MS)
                if (ListenTogether.state.value.inParty) ListenTogether.ensureConnected()
                reconcile()
            }
        }
    }

    fun stop() {
        jobs.forEach(Job::cancel)
        jobs.clear()
        publishJob?.cancel()
        startJob?.cancel()
    }

    fun onLocalIntent() {
        val party = ListenTogether.state.value
        if (!party.inParty) return
        if (focusLost) {
            Log.i(TAG, "rejoining the party after losing the audio")
            focusLost = false
            rejoining = true
        }
        reconcileQuietUntilMs = SystemClock.elapsedRealtime() + INTENT_QUIET_MS
        awaitSeq = Long.MAX_VALUE
        publishJob?.cancel()
        publishJob = scope.launch {
            delay(PUBLISH_DEBOUNCE_MS)
            publish()
        }
    }

    fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            focusLost = false
            return
        }
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS &&
            ListenTogether.state.value.inParty
        ) {
            Log.i(TAG, "another app took the audio; dropping out of the party until asked back")
            focusLost = true
            deferredPlayPending = false
            startJob?.cancel()
        }
    }

    fun shouldDeferPlay(): Boolean {
        val party = ListenTogether.state.value
        if (!party.inParty || party.connection != ListenTogether.Connection.LIVE) return false
        if (!party.clockSynced) return false
        deferredPlayPending = true
        deferredPlayFallback()
        return true
    }

    private fun deferredPlayFallback() {
        startJob?.cancel()
        startJob = scope.launch {
            delay(DEFERRED_PLAY_TIMEOUT_MS)
            val exo = player() ?: return@launch
            if (deferredPlayPending && !exo.playWhenReady) {
                Log.w(TAG, "party never acknowledged the resume; starting locally")
                deferredPlayPending = false
                exo.play()
            }
        }
    }

    private fun reconcile() {
        val party = ListenTogether.state.value
        if (!party.inParty) {
            loadingVideoId = null
            focusLost = false
            rejoining = false
            return
        }
        if (SystemClock.elapsedRealtime() < reconcileQuietUntilMs) return
        if (focusLost) return
        val target = party.playback
        val track = target.track ?: return
        val exo = player() ?: return

        if (exo.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) return
        if (exo.currentMediaItem?.toSong()?.isDeviceFile() == true) return
        if (target.isPlaying && !party.clockSynced) return

        if (exo.currentMediaItem?.mediaId != track.videoId) {
            load(party)
            return
        }
        loadingVideoId = null

        if (!target.isPlaying) {
            deferredPlayPending = false
            if (exo.playWhenReady) exo.pause()
            if (abs(exo.currentPosition - target.positionMs) > PAUSED_TOLERANCE_MS) {
                exo.seekTo(target.positionMs)
            }
            return
        }

        val want = ListenTogether.partyPositionMs()
        if (!exo.playWhenReady) {
            val wait = ListenTogether.msUntilStart()
            if (wait > 0) {
                startJob?.cancel()
                startJob = scope.launch {
                    delay(wait)
                    reconcile()
                }
                return
            }
            if (want != null) exo.seekTo(want)
            deferredPlayPending = false
            startJob?.cancel()
            exo.play()
            alignedSeq = target.seq
            return
        }

        if (want == null) return

        if (exo.playbackState != Player.STATE_READY) {
            driftStrikes = 0
            return
        }

        val drift = exo.currentPosition - want
        if (target.seq != alignedSeq) {
            alignedSeq = target.seq
            if (abs(drift) > ALIGN_TOLERANCE_MS) {
                Log.i(TAG, "aligning ${drift}ms onto party control ${target.seq}")
                exo.seekTo(want)
            }
            return
        }
        if (abs(drift) <= DRIFT_LIMIT_MS) {
            driftStrikes = 0
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now < driftCooldownUntilMs) return
        if (++driftStrikes < DRIFT_STRIKES) return
        Log.i(TAG, "correcting ${drift}ms of drift against the party")
        driftStrikes = 0
        driftCooldownUntilMs = now + DRIFT_COOLDOWN_MS
        exo.seekTo(want)
    }

    private fun load(party: ListenTogether.State) {
        val track = party.playback.track ?: return
        if (loadingVideoId == track.videoId) return
        loadingVideoId = track.videoId

        scope.launch {
            val partyQueue = party.queue.items
            val index = partyQueue.indexOfFirst { it.videoId == track.videoId }
            val queue = if (index >= 0) partyQueue else listOf(track)
            val startIndex = if (index >= 0) index else 0
            val items = withContext(Dispatchers.Default) { queue.map { it.toSong().toMediaItem() } }
            val exo = player()
            if (exo == null) {
                loadingVideoId = null
                return@launch
            }
            val startAt = ListenTogether.partyPositionMs() ?: party.playback.positionMs
            exo.setMediaItems(items, startIndex, startAt)
            exo.prepare()
            if (party.playback.isPlaying && ListenTogether.msUntilStart() <= 0L) exo.play()
            reconcile()
        }
    }

    private fun publish() {
        val party = ListenTogether.state.value
        if (!party.inParty) return
        if (rejoining) {
            rejoining = false
            reconcileQuietUntilMs = 0L
            return
        }
        val exo = player() ?: return
        val song = exo.currentMediaItem?.toSong() ?: return
        if (song.isDeviceFile()) return
        val track = song.toPartyTrack(exo.duration)
        val position = exo.currentPosition.coerceAtLeast(0L)
        val wantsPlaying = deferredPlayPending || exo.playWhenReady
        val base = party.playback.seq
        var controls = 0

        val localIds = (0 until exo.mediaItemCount)
            .take(MAX_PUBLISHED_QUEUE)
            .map { exo.getMediaItemAt(it).mediaId }
            .filterNot { it.startsWith("content://") || it.startsWith("file://") }

        if (localIds != party.queue.items.map(PartyTrack::videoId)) {
            val queue = (0 until exo.mediaItemCount)
                .take(MAX_PUBLISHED_QUEUE)
                .map { exo.getMediaItemAt(it).toSong() }
                .filterNot(Song::isDeviceFile)
                .map { it.toPartyTrack(0L) }
            ListenTogether.setQueue(queue, localIds.indexOf(track.videoId))
            controls++
        }

        when {
            party.playback.track?.videoId != track.videoId -> {
                ListenTogether.setTrack(track, position, wantsPlaying)
                controls++
            }
            party.playback.isPlaying != wantsPlaying -> {
                if (wantsPlaying) ListenTogether.play(position) else ListenTogether.pause(position)
                controls++
            }
            else -> {
                val partyPosition = ListenTogether.partyPositionMs()
                if (partyPosition == null || abs(position - partyPosition) > SEEK_REPORT_FLOOR_MS) {
                    ListenTogether.seek(position)
                    controls++
                }
            }
        }

        awaitSeq = base + controls
        if (controls == 0) reconcileQuietUntilMs = 0L
    }

    private companion object {
        const val TAG = "PartySync"
        const val TICK_MS = 700L
        const val DRIFT_LIMIT_MS = 1_200L
        const val DRIFT_STRIKES = 2
        const val DRIFT_COOLDOWN_MS = 6_000L
        const val PAUSED_TOLERANCE_MS = 400L
        const val ALIGN_TOLERANCE_MS = 120L
        const val SEEK_REPORT_FLOOR_MS = 1_000L
        const val DEFERRED_PLAY_TIMEOUT_MS = 1_800L
        const val PUBLISH_DEBOUNCE_MS = 120L
        const val INTENT_QUIET_MS = 2_500L
        const val MAX_PUBLISHED_QUEUE = 500
    }
}

private fun Song.isDeviceFile(): Boolean =
    videoId.startsWith("content://") || videoId.startsWith("file://")

private fun Song.toPartyTrack(playerDurationMs: Long): PartyTrack = PartyTrack(
    videoId = videoId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = playerDurationMs.takeIf { it > 0L }
        ?: TrackMatcher.secondsOf(durationText)?.let { it * 1000L },
)

private fun PartyTrack.toSong(): Song = Song(
    videoId = videoId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationText = durationMs?.let { ms ->
        val total = ms / 1000
        "%d:%02d".format(total / 60, total % 60)
    },
)

