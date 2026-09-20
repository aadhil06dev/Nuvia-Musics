package com.music.nuvia.download

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.music.nuvia.data.DebugLog as Log
import androidx.core.content.ContextCompat
import com.music.nuvia.data.YtMusicRepository
import com.music.nuvia.data.innertube.StreamResolver
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.DownloadQuality
import com.music.nuvia.data.sources.SourceResolver
import com.music.nuvia.data.sources.SourceStream
import com.music.nuvia.data.sources.TrackMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.Locale

/** Where a track is between "not on this device" and "on it". */
sealed interface DownloadState {

    /** Accepted, waiting for the one in front of it. */
    data object Queued : DownloadState

    /** [fraction] is 0f until the length is known, which is the first thing asked for. */
    data class Running(val fraction: Float) : DownloadState

    data class Failed(val reason: String) : DownloadState
}

/**
 * The download queue, and the record of what came out of it.
 *
 * Split deliberately into two pieces of state that look similar and behave
 * nothing alike:
 *
 *  - [active] is what is happening now — queued, running, just failed. It lives
 *    in memory, is driven by [DownloadService], and is empty on a cold start
 *    because a download interrupted by the process dying did not happen.
 *  - [saved] is what exists on disk, keyed by videoId and remembered across
 *    launches. It is the only way the app can answer "do I already have this?"
 *    without a media-store query per row, and the only way it knows *which*
 *    file a track corresponds to when asked to delete it.
 *
 * [saved] is a claim about a folder this app does not own. The user is expected
 * to manage Downloads with a file manager, so an entry here can outlive the
 * file it names — which is why every read of it goes through [savedUri], and
 * why that verifies before it answers.
 */
object Downloads {

    private const val TAG = "NUViA"
    private const val KEY_SAVED_METADATA = "downloaded_tracks_metadata"
    private const val KEY_SAVED_COLLECTIONS = "downloaded_collections"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private val metadataSerializer = MapSerializer(String.serializer(), SavedSongMetadata.serializer())
    private val collectionSerializer = MapSerializer(String.serializer(), SavedCollection.serializer())

    private val _active = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val active: StateFlow<Map<String, DownloadState>> = _active.asStateFlow()

    /**
     * Ids asked for as part of a release's own download tap, by the browseId
     * that was tapped.
     */
    private val _requested = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val requested: StateFlow<Map<String, Set<String>>> = _requested.asStateFlow()

    /** Record that [videoIds] were asked for as [browseId]'s own release. */
    fun markRequested(browseId: String, videoIds: Collection<String>) {
        if (videoIds.isEmpty()) return
        _requested.update { it + (browseId to (it[browseId].orEmpty() + videoIds)) }
    }

    private val _saved = MutableStateFlow<Map<String, String>>(emptyMap())

    /** videoId to the uri of the file saved for it. */
    val saved: StateFlow<Map<String, String>> = _saved.asStateFlow()

    private val _savedMetadata = MutableStateFlow<Map<String, SavedSongMetadata>>(emptyMap())

    val savedMetadata: StateFlow<Map<String, SavedSongMetadata>> = _savedMetadata.asStateFlow()

    private val _collections = MutableStateFlow<Map<String, SavedCollection>>(emptyMap())

    /**
     * The releases that were downloaded *as* releases, by the id they were asked
     * for under.
     */
    val collections: StateFlow<Map<String, SavedCollection>> = _collections.asStateFlow()

    /** Waiting, in the order asked for. Guarded by [lock]. */
    private val pending = LinkedHashMap<String, Song>()

    private val lock = Any()

    /**
     * The tracks taken off the queue and not yet finished, to the job fetching
     * each — null in the gap between a worker claiming a track and its job
     * existing. Guarded by [lock].
     */
    private val running = LinkedHashMap<String, Job?>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("bitchord_settings", Context.MODE_PRIVATE)
        _saved.value = runCatching {
            json.decodeFromString(serializer, prefs.getString(KEY_SAVED, null) ?: "{}")
        }.getOrDefault(emptyMap())
        _savedMetadata.value = runCatching {
            json.decodeFromString(metadataSerializer, prefs.getString(KEY_SAVED_METADATA, null) ?: "{}")
        }.getOrDefault(emptyMap())
        _collections.value = runCatching {
            json.decodeFromString(collectionSerializer, prefs.getString(KEY_SAVED_COLLECTIONS, null) ?: "{}")
        }.getOrDefault(emptyMap())
    }

    // ---- Asking -------------------------------------------------------------

    /**
     * Queue [song], and make sure something is draining the queue.
     *
     * @param from what release this track was asked for as part of, when it was
     *   one of many. Carried no further than [DownloadSession].
     */
    fun enqueue(context: Context, song: Song, from: String? = null) {
        val id = song.videoId
        if (!AppSettings.downloadsAllowedNow) {
            val inFlight = _active.value[id]
            if (inFlight !is DownloadState.Queued && inFlight !is DownloadState.Running) {
                DownloadSession.queued(song, from)
                fail(id, WIFI_ONLY_REFUSAL)
            }
            return
        }
        synchronized(lock) {
            if (id in pending || id in running) return
            pending[id] = song
        }
        _active.update { it + (id to DownloadState.Queued) }
        DownloadSession.queued(song, from)

        val app = context.applicationContext
        runCatching {
            ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        }.onFailure {
            Log.w(TAG, "could not start the download service: ${it.message}")
            synchronized(lock) { pending.remove(id) }
            fail(id, "Downloads can't start right now")
        }
    }

    /**
     * Drop [videoId] from the queue, or stop it if it is one of the ones running.
     */
    fun cancel(videoId: String) {
        val job = synchronized(lock) {
            pending.remove(videoId)
            if (videoId !in running) return@synchronized null
            running.remove(videoId)
        }
        job?.cancel()
        clear(videoId)
        DownloadSession.forget(videoId)
    }

    // ---- The record ---------------------------------------------------------

    /**
     * The file saved for [videoId], or null — pruning the record if the file
     * has been deleted from under it.
     */
    suspend fun savedUri(context: Context, videoId: String): Uri? = withContext(Dispatchers.IO) {
        val recorded = _saved.value[videoId] ?: return@withContext null
        val uri = recorded.toUri()
        if (DownloadStore.exists(context, uri)) return@withContext uri
        Log.d(TAG, "$videoId was downloaded but the file is gone; forgetting it")
        forget(videoId)
        null
    }

    /** True when [uriString] names a `file://` path that is not there. */
    fun isMissingLocalFile(uriString: String): Boolean {
        if (!uriString.startsWith("file://")) return false
        val path = runCatching { uriString.toUri().path }.getOrNull() ?: return false
        return !File(path).exists()
    }

    /** As [savedUri], but synchronous and without a [Context] parameter. */
    fun verifiedSavedUri(videoId: String): String? {
        val recorded = _saved.value[videoId] ?: return null
        if (!isMissingLocalFile(recorded)) return recorded
        Log.d(TAG, "$videoId was downloaded but the file is gone; forgetting it")
        forget(videoId)
        return null
    }

    /** Delete the file saved for [videoId] and forget it. */
    suspend fun delete(context: Context, videoId: String): Boolean = withContext(Dispatchers.IO) {
        val targetUriStr = _saved.value[videoId]
            ?: _saved.value.entries.firstOrNull { it.value == videoId }?.value
            ?: _savedMetadata.value.values.firstOrNull { it.uri == videoId }?.uri
            ?: if (videoId.startsWith("content://") || videoId.startsWith("file://")) videoId else null
            ?: return@withContext false

        val uri = targetUriStr.toUri()
        val deleted = DownloadStore.delete(context, uri)
        val matchingIds = _saved.value.filter { it.value == targetUriStr }.keys +
            _savedMetadata.value.filter { it.value.uri == targetUriStr }.keys +
            videoId
        matchingIds.forEach { forget(it) }
        deleted
    }

    /** Delete all downloaded tracks and clear metadata records. */
    suspend fun clearAll(context: Context): Int = withContext(Dispatchers.IO) {
        val uris = _saved.value.values.toSet() + _savedMetadata.value.values.map { it.uri }.toSet()
        var count = 0
        uris.forEach { uriStr ->
            runCatching {
                if (DownloadStore.delete(context, uriStr.toUri())) count++
            }
        }
        _saved.update { emptyMap() }
        _savedMetadata.update { emptyMap() }
        _collections.update { emptyMap() }
        if (::prefs.isInitialized) {
            synchronized(recordLock) {
                prefs.edit()
                    .putString(KEY_SAVED, "{}")
                    .putString(KEY_SAVED_METADATA, "{}")
                    .putString(KEY_SAVED_COLLECTIONS, "{}")
                    .apply()
            }
        }
        count
    }

    /** Sum of file sizes of all downloaded tracks currently saved. */
    suspend fun getDownloadedStorageBytes(context: Context): Long = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val uris = _saved.value.values.toSet() + _savedMetadata.value.values.map { it.uri }.toSet()
        for (uriStr in uris) {
            val uri = uriStr.toUri()
            runCatching {
                if (uri.scheme == "file") {
                    val file = uri.path?.let { File(it) }
                    if (file != null && file.exists()) {
                        totalBytes += file.length()
                    }
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        totalBytes += pfd.statSize.coerceAtLeast(0)
                    }
                }
            }
        }
        totalBytes
    }

    private fun forget(videoId: String) {
        record(saved = { it - videoId }, meta = { it - videoId })
    }

    /** Drop the record for [videoId] because a read of the file it names has failed. */
    fun forgetMissing(videoId: String) {
        if (videoId !in _saved.value) return
        Log.d(TAG, "$videoId could not be opened; forgetting the download")
        forget(videoId)
    }

    // ---- Releases -----------------------------------------------------------

    /**
     * Remember that [songs] were asked for as one release rather than one at a time.
     */
    fun rememberCollection(target: DownloadTarget, songs: List<Song>) {
        if (songs.isEmpty()) return
        val existing = _collections.value[target.id]
        val ids = songs.map { it.videoId }.distinct()
        val record = SavedCollection(
            id = target.id,
            title = target.title,
            subtitle = target.subtitle,
            thumbnailUrl = target.thumbnailUrl ?: existing?.thumbnailUrl,
            playlist = target.playlist,
            videoIds = (ids + (existing?.videoIds ?: emptyList())).distinct(),
        )
        recordCollections(_collections.value + (target.id to record))
    }

    /** Drop a release from the record without touching the files under it. */
    fun forgetCollection(id: String) {
        if (id !in _collections.value) return
        recordCollections(_collections.value - id)
    }

    /** Delete every file downloaded for release [id] and drop the record of it. */
    suspend fun deleteCollection(context: Context, id: String): Boolean {
        val record = _collections.value[id] ?: return false
        var any = false
        record.videoIds.forEach { videoId -> if (delete(context, videoId)) any = true }
        forgetCollection(id)
        return any
    }

    const val PLAYLIST_PREFIX = "local:playlist:"

    fun pageIdFor(id: String): String = PLAYLIST_PREFIX + id

    fun recordIdOf(browseId: String): String? =
        browseId.removePrefix(PLAYLIST_PREFIX).takeIf { it != browseId && it.isNotEmpty() }

    fun savedPlaylists(onDisk: Map<String, String> = _saved.value): List<SavedCollection> {
        if (_collections.value.isEmpty()) return emptyList()
        return _collections.value.values
            .filter { record -> record.playlist && record.videoIds.any { it in onDisk } }
            .sortedBy { it.title.lowercase(Locale.ROOT) }
    }

    fun collectionsAmong(songs: List<Song>): List<DownloadedCollection> {
        if (songs.isEmpty() || _collections.value.isEmpty()) return emptyList()
        val byId = songs.associateBy { it.videoId }
        val byUri = songs.mapNotNull { song -> song.localUri?.let { it to song } }.toMap()
        val uris = _saved.value
        return _collections.value.values
            .mapNotNull { record ->
                val tracks = record.videoIds
                    .mapNotNull { id -> byId[id] ?: uris[id]?.let(byUri::get) }
                    .distinctBy { it.localUri ?: it.videoId }
                if (tracks.isEmpty()) {
                    null
                } else {
                    DownloadedCollection(
                        id = record.id,
                        title = record.title,
                        subtitle = record.subtitle,
                        thumbnailUrl = record.thumbnailUrl ?: tracks.firstNotNullOfOrNull { it.thumbnailUrl },
                        playlist = record.playlist,
                        songs = tracks,
                    )
                }
            }
            .sortedBy { it.title.lowercase(Locale.ROOT) }
    }

    private fun recordCollections(map: Map<String, SavedCollection>) {
        _collections.value = map
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_SAVED_COLLECTIONS, json.encodeToString(collectionSerializer, map))
                .apply()
        }
    }

    /**
     * Restores collections (playlists) and song metadata from backup.
     * Safely updates SharedPreferences without fabricating binary audio files.
     */
    fun restoreCollectionsAndMetadata(
        newCollections: Map<String, SavedCollection>,
        newMetadata: Map<String, SavedSongMetadata>,
        overwrite: Boolean,
    ) {
        val mergedCollections = if (overwrite) newCollections else _collections.value + newCollections
        val mergedMeta = if (overwrite) newMetadata else _savedMetadata.value + newMetadata
        _collections.value = mergedCollections
        _savedMetadata.value = mergedMeta
        if (::prefs.isInitialized) {
            synchronized(recordLock) {
                prefs.edit()
                    .putString(KEY_SAVED_COLLECTIONS, json.encodeToString(collectionSerializer, mergedCollections))
                    .putString(KEY_SAVED_METADATA, json.encodeToString(metadataSerializer, mergedMeta))
                    .apply()
            }
        }
    }

    private fun remember(asked: Song, fetched: Song, uri: Uri) {
        val ids = setOf(asked.videoId, fetched.videoId)
        val album = fetched.albumName?.takeIf { it.isNotBlank() }
            ?: asked.albumName?.takeIf { it.isNotBlank() }
        val metaAsked = SavedSongMetadata(
            videoId = asked.videoId,
            title = asked.title,
            artist = asked.artist,
            thumbnailUrl = asked.thumbnailUrl,
            durationText = asked.durationText,
            albumName = album,
            uri = uri.toString(),
        )
        val metaFetched = SavedSongMetadata(
            videoId = fetched.videoId,
            title = fetched.title,
            artist = fetched.artist,
            thumbnailUrl = fetched.thumbnailUrl,
            durationText = fetched.durationText,
            albumName = album,
            uri = uri.toString(),
        )
        record(
            saved = { it + ids.associateWith { id -> uri.toString() } },
            meta = {
                it + mapOf(asked.videoId to metaAsked, fetched.videoId to metaFetched)
            },
        )
    }

    private fun record(
        saved: (Map<String, String>) -> Map<String, String>,
        meta: (Map<String, SavedSongMetadata>) -> Map<String, SavedSongMetadata>,
    ) {
        val savedMap = _saved.updateAndGet(saved)
        val metaMap = _savedMetadata.updateAndGet(meta)
        if (!::prefs.isInitialized) return
        synchronized(recordLock) {
            prefs.edit()
                .putString(KEY_SAVED, json.encodeToString(serializer, savedMap))
                .putString(KEY_SAVED_METADATA, json.encodeToString(metadataSerializer, metaMap))
                .apply()
        }
    }

    private val recordLock = Any()

    /** Returns all downloaded songs whose files still exist on disk. */
    suspend fun getDownloadedSongs(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val metaMap = _savedMetadata.value
        val result = mutableListOf<Song>()
        val seenUris = mutableSetOf<String>()

        for ((videoId, meta) in metaMap) {
            val uri = meta.uri.toUri()
            if (DownloadStore.exists(context, uri)) {
                if (seenUris.add(meta.uri)) {
                    result.add(
                        Song(
                            videoId = meta.videoId,
                            title = meta.title,
                            artist = meta.artist,
                            thumbnailUrl = meta.thumbnailUrl,
                            durationText = meta.durationText,
                            albumName = meta.albumName,
                            localUri = meta.uri,
                        )
                    )
                }
            } else {
                forget(videoId)
            }
        }
        result
    }

    private fun String.toUri(): Uri = Uri.parse(this)

    // ---- Driven by DownloadService -----------------------------------------

    internal fun takeNext(): Song? = synchronized(lock) {
        val entry = pending.entries.firstOrNull() ?: return null
        pending.remove(entry.key)
        running[entry.key] = null
        entry.value
    }

    internal fun onRunning(videoId: String, job: Job) {
        val cancelled = synchronized(lock) {
            if (videoId !in running) return@synchronized true
            running[videoId] = job
            false
        }
        if (cancelled) job.cancel()
    }

    internal fun onIdle(videoId: String) {
        synchronized(lock) { running.remove(videoId) }
    }

    internal fun busy(): Boolean = synchronized(lock) { pending.isNotEmpty() || running.isNotEmpty() }

    internal fun onStopped() {
        synchronized(lock) { running.clear() }
    }

    internal suspend fun run(context: Context, song: Song) = withContext(Dispatchers.IO) {
        val id = song.videoId
        _active.update { it + (id to DownloadState.Running(0f)) }
        DownloadSession.running(id, 0f)

        try {
            val plan = prepare(context, song)
            DownloadSession.retitle(id, plan.track)
            transfer(context, song, plan)
        } catch (e: CancellationException) {
            clear(id)
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "download failed for $id: ${e.message}", e)
            fail(id, e.friendly())
        }
    }

    internal suspend fun prepare(context: Context, song: Song): Prepared = withContext(Dispatchers.IO) {
        val track = runCatching { YtMusicRepository.resolveAudio(song) }.getOrDefault(song)
        val quality = AppSettings.downloadQuality.value

        if (quality.keepsLossless) {
            LOSSLESS_EXTENSIONS.firstNotNullOfOrNull { extension ->
                DownloadStore.existing(context, DownloadStore.fileNameFor(track, extension))
            }?.let { uri ->
                return@withContext Prepared(song.videoId, track, route = null, alreadyAt = uri)
            }
        }

        val route = routeFor(track, quality)
        Log.d(TAG, "downloading ${song.videoId} as .${route.extension} (${route.describe}, ${quality.label})")
        Prepared(song.videoId, track, route = route, alreadyAt = null)
    }

    internal class Prepared(
        val videoId: String,
        val track: Song,
        val route: Route?,
        val alreadyAt: Uri?,
    )

    private suspend fun transfer(context: Context, song: Song, plan: Prepared) {
        val id = song.videoId
        val track = plan.track

        plan.alreadyAt?.let { uri ->
            remember(song, track, uri)
            DownloadSession.done(id)
            clear(id)
            return
        }
        val route = plan.route ?: error("Nothing to download")

        var pending: DownloadStore.Pending? = null
        var lyrics: Deferred<LyricsTag.Embeddable?>? = null
        try {
            coroutineScope {
                if (MediaTagger.carriesTags(route.extension)) {
                    lyrics = async { LyricsTag.forTrack(track) }
                }

                val name = DownloadStore.fileNameFor(track, route.extension)
                val alreadyThere = DownloadStore.existing(context, name)
                if (alreadyThere != null) {
                    Log.d(TAG, "$name is already in Music; adopting it")
                    remember(song, track, alreadyThere)
                    DownloadSession.done(id)
                    clear(id)
                    return@coroutineScope
                }

                val destination = DownloadStore.begin(context, name, route.mimeType)
                pending = destination
                destination.openStream().use { sink ->
                    route.write(sink) { written, total ->
                        val fraction = written.toFloat() / total
                        _active.update { it + (id to DownloadState.Running(fraction)) }
                        DownloadSession.running(id, fraction)
                    }
                }
                val words = lyrics?.await()
                val savedUri = destination.commit()
                pending = null
                MediaTagger.embed(context, savedUri, track, route.extension, words)
                remember(song, track, savedUri)
                DownloadSession.done(id)
                clear(id)
                Log.d(TAG, "saved $name")
            }
        } catch (e: Throwable) {
            pending?.abort()
            throw e
        } finally {
            lyrics?.cancel()
        }
    }

    internal class Route(
        val extension: String,
        val mimeType: String,
        val describe: String,
        val write: suspend (OutputStream, (written: Long, total: Long) -> Unit) -> Unit,
    )

    private suspend fun routeFor(track: Song, quality: DownloadQuality): Route {
        fromSources(track, quality)?.let { (stream, storable) ->
            return Route(
                extension = storable.extension,
                mimeType = storable.mimeType,
                describe = stream.format.summary,
                write = { sink, onProgress ->
                    Downloader.fetchDirect(stream.url, stream.headers, sink, onProgress)
                },
            )
        }
        val stream = StreamResolver.resolveForDownload(track.videoId, quality.maxKbps)
        return Route(
            extension = stream.downloadExtension,
            mimeType = stream.downloadMimeType,
            describe = "${stream.kbps}kbps ${stream.mimeType}",
            write = { sink, onProgress ->
                Downloader.fetch(track.videoId, stream, quality.maxKbps, sink, onProgress)
            },
        )
    }

    private suspend fun fromSources(
        track: Song,
        quality: DownloadQuality,
    ): Pair<SourceStream, DownloadStore.Storable>? {
        val stream = withTimeoutOrNull(SOURCE_LOOKUP_MS) {
            try {
                SourceResolver.forDownload(
                    TrackMatcher.targetOf(track),
                    SourceResolver.requestForDownload(quality),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "source lookup failed for ${track.videoId}: ${e.message}")
                null
            }
        } ?: return null

        val storable = DownloadStore.storable(stream.format.codec)
        if (storable == null) {
            Log.d(TAG, "nothing to file a '${stream.format.codec}' as; taking YouTube for ${track.videoId}")
            return null
        }
        return stream to storable
    }

    private fun clear(videoId: String) {
        _active.update { it - videoId }
    }

    private fun fail(videoId: String, reason: String) {
        _active.update { it + (videoId to DownloadState.Failed(reason)) }
        DownloadSession.failed(videoId, reason)
    }

    private fun Exception.friendly(): String = when {
        (this is IllegalStateException || this is IllegalArgumentException) &&
            !message.isNullOrBlank() -> message!!
        else -> "Download failed — check your connection"
    }

    private const val SOURCE_LOOKUP_MS = 20_000L

    private val LOSSLESS_EXTENSIONS = listOf("flac", "wav")

    internal const val WIFI_ONLY_REFUSAL = "Downloads are set to Wi-Fi only"

    fun dismissFailure(videoId: String) {
        if (_active.value[videoId] is DownloadState.Failed) clear(videoId)
    }

    private const val KEY_SAVED = "downloaded_tracks"
}

@kotlinx.serialization.Serializable
data class SavedSongMetadata(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val albumName: String? = null,
    val uri: String,
)

data class DownloadTarget(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val playlist: Boolean = false,
)

@kotlinx.serialization.Serializable
data class SavedCollection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val playlist: Boolean = false,
    val videoIds: List<String> = emptyList(),
)

data class DownloadedCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val playlist: Boolean,
    val songs: List<Song>,
)
