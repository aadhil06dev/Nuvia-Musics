package com.music.nuvia.download

import com.music.nuvia.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** Where one track in the manager's list has got to. */
sealed interface DownloadProgress {

    data object Queued : DownloadProgress

    /** [fraction] is 0f until the length is known, which is one request in. */
    data class Running(val fraction: Float) : DownloadProgress

    data object Done : DownloadProgress

    data class Failed(val reason: String) : DownloadProgress

    /** Neither waiting nor coming back — nothing left for the user to watch. */
    val settled: Boolean get() = this is Done || this is Failed
}

/**
 * Everything downloaded since the app was opened, and whether the user has
 * looked at it yet.
 */
object DownloadSession {

    data class Item(
        /** The id the *tap* used, which is what everything else here is keyed by. */
        val videoId: String,
        val song: Song,
        val progress: DownloadProgress,
        /** What release this was part of, when it was part of one. */
        val from: String? = null,
        /** Ask order, so the list reads the way the queue drains. */
        val sequence: Long,
    )

    data class State(
        val items: List<Item> = emptyList(),
        /** When the user last had the manager open, on [tick]'s clock. */
        val seenAt: Long = 0L,
        /** When the last thing in the queue stopped moving, on [tick]'s clock. */
        val settledAt: Long = 0L,
    ) {
        val waiting: Int get() = items.count { !it.progress.settled }
        val finished: Int get() = items.count { it.progress is DownloadProgress.Done }
        val failed: Int get() = items.count { it.progress is DownloadProgress.Failed }

        /** Whether anything is still queued or running. */
        val busy: Boolean get() = waiting > 0

        /**
         * How far through the whole batch this is, counting a settled track as
         * a whole one whichever way it settled.
         */
        val fraction: Float
            get() {
                if (items.isEmpty()) return 0f
                val total = items.sumOf { item ->
                    when (val progress = item.progress) {
                        is DownloadProgress.Running -> progress.fraction.toDouble()
                        DownloadProgress.Queued -> 0.0
                        else -> 1.0
                    }
                }
                return (total / items.size).toFloat().coerceIn(0f, 1f)
            }

        /** Not "is downloading" but "is unaccounted for". */
        val visible: Boolean get() = items.isNotEmpty() && (busy || seenAt < settledAt)
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val clock = AtomicLong(0L)

    private fun tick(): Long = clock.incrementAndGet()

    fun queued(song: Song, from: String? = null) {
        update { state ->
            val existing = state.items.indexOfFirst { it.videoId == song.videoId }
            val item = Item(
                videoId = song.videoId,
                song = song,
                progress = DownloadProgress.Queued,
                from = from ?: state.items.getOrNull(existing)?.from,
                sequence = if (existing >= 0) state.items[existing].sequence else tick(),
            )
            val items = if (existing >= 0) {
                state.items.toMutableList().also { it[existing] = item }
            } else {
                state.items + item
            }
            state.copy(items = items)
        }
    }

    fun running(videoId: String, fraction: Float) =
        set(videoId, DownloadProgress.Running(fraction))

    fun done(videoId: String) = set(videoId, DownloadProgress.Done)

    fun failed(videoId: String, reason: String) = set(videoId, DownloadProgress.Failed(reason))

    fun retitle(videoId: String, song: Song) {
        update { state ->
            val index = state.items.indexOfFirst { it.videoId == videoId }
            if (index < 0) return@update state
            state.copy(
                items = state.items.toMutableList().also {
                    it[index] = it[index].copy(song = song)
                },
            )
        }
    }

    fun forget(videoId: String) {
        update { state ->
            val items = state.items.filterNot { it.videoId == videoId }
            if (items.size == state.items.size) return@update state
            state.copy(
                items = items,
                settledAt = if (items.none { !it.progress.settled }) tick() else state.settledAt,
            )
        }
    }

    fun markSeen() {
        _state.update { it.copy(seenAt = tick()) }
    }

    fun clear() {
        _state.value = State()
    }

    private fun set(videoId: String, progress: DownloadProgress) {
        update { state ->
            val index = state.items.indexOfFirst { it.videoId == videoId }
            if (index < 0) return@update state
            val items = state.items.toMutableList().also {
                it[index] = it[index].copy(progress = progress)
            }
            val quiet = items.none { !it.progress.settled }
            state.copy(
                items = items,
                settledAt = if (progress.settled && quiet) tick() else state.settledAt,
            )
        }
    }

    private inline fun update(block: (State) -> State) {
        _state.update(block)
    }
}
