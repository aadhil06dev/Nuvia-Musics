package com.music.nuvia.data.playlist

import android.content.Context
import android.content.SharedPreferences
import com.music.nuvia.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val albumName: String? = null,
)

@Serializable
internal data class StoredPlaylist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val tracks: List<StoredTrack> = emptyList(),
)

data class LocalPlaylist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tracks: List<Song> = emptyList(),
)

object LocalPlaylistStore {

    private const val PREFS_NAME = "nuvia_local_playlists"
    private const val KEY_PLAYLISTS = "playlists_json"

    private lateinit var prefs: SharedPreferences
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists: StateFlow<List<LocalPlaylist>> = _playlists.asStateFlow()

    private val lock = Any()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = loadFromPrefs()
        _playlists.value = loaded
    }

    fun getAll(): List<LocalPlaylist> = _playlists.value

    fun get(id: String): LocalPlaylist? {
        val normalized = id.removePrefix("VL")
        return _playlists.value.firstOrNull { it.id == id || it.id.removePrefix("VL") == normalized }
    }

    fun save(playlist: LocalPlaylist) {
        synchronized(lock) {
            val current = _playlists.value.toMutableList()
            val index = current.indexOfFirst { it.id == playlist.id }
            if (index >= 0) {
                current[index] = playlist.copy(updatedAt = System.currentTimeMillis())
            } else {
                current.add(playlist)
            }
            _playlists.value = current
            persist(current)
        }
    }

    fun delete(id: String) {
        synchronized(lock) {
            val current = _playlists.value.filterNot { it.id == id }
            _playlists.value = current
            persist(current)
        }
    }

    fun restorePlaylists(incoming: List<LocalPlaylist>, overwrite: Boolean) {
        synchronized(lock) {
            val updated = if (overwrite) {
                incoming
            } else {
                val current = _playlists.value.toMutableList()
                val existingIds = current.map { it.id }.toSet()
                for (item in incoming) {
                    if (item.id !in existingIds) {
                        current.add(item)
                    }
                }
                current
            }
            _playlists.value = updated
            persist(updated)
        }
    }

    private fun persist(list: List<LocalPlaylist>) {
        if (!::prefs.isInitialized) return
        val stored = list.map { it.toStored() }
        val rawJson = json.encodeToString(ListSerializer(StoredPlaylist.serializer()), stored)
        prefs.edit().putString(KEY_PLAYLISTS, rawJson).apply()
    }

    private fun loadFromPrefs(): List<LocalPlaylist> {
        if (!::prefs.isInitialized) return emptyList()
        val rawJson = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val stored = json.decodeFromString(ListSerializer(StoredPlaylist.serializer()), rawJson)
            stored.map { it.toLocal() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun LocalPlaylist.toStored(): StoredPlaylist = StoredPlaylist(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tracks = tracks.map { song ->
            StoredTrack(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationText = song.durationText,
                albumName = song.albumName,
            )
        },
    )

    private fun StoredPlaylist.toLocal(): LocalPlaylist = LocalPlaylist(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tracks = tracks.map { track ->
            Song(
                videoId = track.videoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationText = track.durationText,
                albumName = track.albumName,
            )
        },
    )
}
