package com.music.nuvia.data.history

import android.content.Context
import android.content.SharedPreferences
import com.music.nuvia.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
internal data class HistoryEntry(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val localUri: String? = null,
    val localPath: String? = null,
    val playedAt: Long = System.currentTimeMillis(),
)

/**
 * On-device persistent chronological listening history for NUViA.
 * Tracks every played song offline and online, deduplicating sensibly by bumping
 * re-listened songs to the head of the chronological timeline.
 */
object LocalHistoryStore {

    private const val PREFS_NAME = "nuvia_listening_history"
    private const val KEY_HISTORY = "history_entries_json"
    private const val MAX_HISTORY_ENTRIES = 500

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history.asStateFlow()

    private var rawEntries: MutableList<HistoryEntry> = mutableListOf()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val raw = prefs.getString(KEY_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(HistoryEntry.serializer()), raw)
            }.getOrDefault(emptyList())
            rawEntries = list.sortedByDescending { it.playedAt }.toMutableList()
        } else {
            rawEntries = mutableListOf()
        }
        publish()
    }

    private fun publish() {
        _history.value = rawEntries.map { entry ->
            Song(
                videoId = entry.videoId,
                title = entry.title,
                artist = entry.artist,
                thumbnailUrl = entry.thumbnailUrl,
                durationText = entry.durationText,
                albumName = entry.albumName,
                albumId = entry.albumId,
                artistId = entry.artistId,
                localUri = entry.localUri,
                localPath = entry.localPath,
            )
        }
    }

    @Synchronized
    fun record(song: Song) {
        if (song.videoId.isBlank()) return
        val now = System.currentTimeMillis()
        // Deduplicate: remove older occurrence of same track
        rawEntries.removeAll { it.videoId == song.videoId }
        // Prepend new entry
        rawEntries.add(
            0,
            HistoryEntry(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationText = song.durationText,
                albumName = song.albumName,
                albumId = song.albumId,
                artistId = song.artistId,
                localUri = song.localUri,
                localPath = song.localPath,
                playedAt = now,
            )
        )
        // Bound storage size
        if (rawEntries.size > MAX_HISTORY_ENTRIES) {
            rawEntries = rawEntries.take(MAX_HISTORY_ENTRIES).toMutableList()
        }
        publish()
        persist()
    }

    @Synchronized
    fun remove(videoId: String) {
        rawEntries.removeAll { it.videoId == videoId }
        publish()
        persist()
    }

    @Synchronized
    fun clear() {
        rawEntries.clear()
        publish()
        persist()
    }

    private fun persist() {
        val snapshot = ArrayList(rawEntries)
        scope.launch {
            if (::prefs.isInitialized) {
                val encoded = runCatching {
                    json.encodeToString(ListSerializer(HistoryEntry.serializer()), snapshot)
                }.getOrNull()
                prefs.edit().putString(KEY_HISTORY, encoded).apply()
            }
        }
    }
}

