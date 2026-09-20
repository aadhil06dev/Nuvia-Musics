package com.music.nuvia.data.stats

import android.content.Context
import android.util.Log
import com.music.nuvia.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * On-device listening aggregates and statistics engine for NUViA.
 * Tracks playback in monthly JSON buckets without unconstrained database growth.
 */
object ListeningStats {

    private lateinit var directory: File

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var open: OpenBucket? = null
    private val lock = Any()
    private var dirty = false

    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    @Volatile
    private var version = 0L

    private var cached: Cached? = null

    private class Cached(
        val period: ReplayPeriod,
        val version: Long,
        val facts: Int,
        val day: LocalDate,
        val summary: ReplaySummary,
    )

    fun init(context: Context) {
        directory = File(context.filesDir, DIRECTORY)
        writer.launch { synchronized(lock) { bucketFor(YearMonth.now()) } }
    }

    private val ready: Boolean get() = this::directory.isInitialized

    fun record(song: Song, playedMs: Long, countsAsPlay: Boolean) {
        if (!ready) return
        if (playedMs <= 0 && !countsAsPlay) return
        if (song.videoId.isBlank()) return
        val now = System.currentTimeMillis()
        val at = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        synchronized(lock) {
            val bucket = bucketFor(YearMonth.from(at))
            val track = bucket.tracks.getOrPut(song.videoId) {
                TrackEntry(
                    id = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    album = song.albumName,
                    albumId = song.albumId,
                    artistId = song.artistId,
                    art = song.thumbnailUrl,
                )
            }
            track.ms += playedMs
            track.last = now
            if (track.album == null) track.album = song.albumName
            if (track.albumId == null) track.albumId = song.albumId
            if (track.artistId == null) track.artistId = song.artistId
            if (track.art == null) track.art = song.thumbnailUrl
            if (countsAsPlay) track.plays++

            primaryArtist(song.artist)?.let { name ->
                ArtistFacts.noticed(name)
                val artist = bucket.artists.getOrPut(name.lowercase(Locale.ROOT)) {
                    NameEntry(name = name, art = song.thumbnailUrl)
                }
                artist.ms += playedMs
                if (countsAsPlay) artist.plays++
                if (artist.id == null) artist.id = song.artistId
            }

            song.albumName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                val album = bucket.albums.getOrPut(albumKey(name, song.artist)) {
                    NameEntry(name = name, sub = song.artist, art = song.thumbnailUrl)
                }
                album.ms += playedMs
                if (countsAsPlay) album.plays++
                if (album.id == null) album.id = song.albumId
            }

            bucket.hours[at.hour] += playedMs
            val day = at.dayOfMonth
            bucket.days[day] = (bucket.days[day] ?: 0L) + playedMs
            dirty = true
        }
    }

    fun flush() {
        pending()?.let { (key, snapshot) -> writer.launch { write(key, snapshot) } }
    }

    private suspend fun flushAndAwait() {
        pending()?.let { (key, snapshot) -> writer.launch { write(key, snapshot) }.join() }
    }

    private fun pending(): Pair<String, StoredBucket>? {
        if (!ready) return null
        return synchronized(lock) {
            if (!dirty) return null
            val bucket = open ?: return null
            dirty = false
            bucket.key to bucket.snapshot()
        }
    }

    private suspend fun write(key: String, snapshot: StoredBucket) = writeLock.withLock {
        runCatching {
            directory.mkdirs()
            val file = File(directory, "$key.json")
            val temporary = File(directory, "$key.json.tmp")
            temporary.writeText(json.encodeToString(StoredBucket.serializer(), snapshot))
            if (!temporary.renameTo(file)) {
                if (file.delete() && temporary.renameTo(file)) Unit else temporary.delete()
            }
            version++
        }.onFailure { Log.w(TAG, "Could not write listening bucket $key", it) }
    }

    private fun bucketFor(month: YearMonth): OpenBucket {
        val key = month.toString()
        open?.takeIf { it.key == key }?.let { return it }
        open?.let { previous ->
            val stale = previous.snapshot()
            writer.launch { write(previous.key, stale) }
        }
        val loaded = read(key) ?: StoredBucket(month = key)
        return OpenBucket.of(key, loaded).also {
            open = it
            prune()
        }
    }

    suspend fun summary(period: ReplayPeriod): ReplaySummary = withContext(Dispatchers.IO) {
        flushAndAwait()
        val today = LocalDate.now()
        val facts = ArtistFacts.revision.value
        cached?.takeIf {
            it.period == period && it.version == version && it.facts == facts && it.day == today
        }?.let { return@withContext it.summary }

        val merged = MergedBucket()
        months().filter { period.covers(it, today) }
            .forEach { month -> read(month.toString())?.let(merged::add) }
        merged.toSummary(period, today).also {
            cached = Cached(period, version, facts, today, it)
        }
    }

    fun months(): List<YearMonth> {
        if (!ready) return emptyList()
        val files = directory.listFiles() ?: return emptyList()
        return files.mapNotNull { file ->
            file.name.removeSuffix(".json").takeIf { it != file.name }
                ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        }.sorted()
    }

    private fun read(key: String): StoredBucket? {
        val file = File(directory, "$key.json")
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(StoredBucket.serializer(), file.readText()) }
            .onFailure { Log.w(TAG, "Discarding unreadable listening bucket $key", it) }
            .getOrNull()
    }

    private fun prune() {
        val existing = months()
        if (existing.size > KEEP_MONTHS) {
            existing.take(existing.size - KEEP_MONTHS).forEach {
                File(directory, "$it.json").delete()
            }
        }
        val bucket = open ?: return
        bucket.tracks.trimTo(MAX_TRACKS) { it.ms }
        bucket.artists.trimTo(MAX_NAMES) { it.ms }
        bucket.albums.trimTo(MAX_NAMES) { it.ms }
    }

    private inline fun <T : Any> List<T>.mergedBy(
        into: LinkedHashMap<String, T>,
        key: (T) -> String,
    ): LinkedHashMap<String, T> {
        forEach { entry ->
            val k = key(entry)
            val existing = into[k]
            if (existing == null) {
                into[k] = entry
            } else {
                @Suppress("UNCHECKED_CAST")
                (existing as NameEntry).absorb(entry as NameEntry)
            }
        }
        return into
    }

    private inline fun <K, V> MutableMap<K, V>.trimTo(limit: Int, crossinline weight: (V) -> Long) {
        if (size <= limit) return
        entries.sortedBy { weight(it.value) }
            .take(size - limit)
            .forEach { remove(it.key) }
    }

    suspend fun exportAll(): List<StoredBucket> {
        if (!ready) return emptyList()
        flushAndAwait()
        return months().mapNotNull { read(it.toString()) }
    }

    fun importAll(buckets: List<StoredBucket>) {
        if (!ready) return
        synchronized(lock) {
            open = null
            dirty = false
            directory.listFiles()?.forEach { it.delete() }
            directory.mkdirs()
            version++
            cached = null
            buckets.forEach { bucket ->
                val key = runCatching { YearMonth.parse(bucket.month).toString() }.getOrNull()
                    ?: return@forEach
                runCatching {
                    File(directory, "$key.json")
                        .writeText(json.encodeToString(StoredBucket.serializer(), bucket))
                }.onFailure { Log.w(TAG, "Could not import bucket $key", it) }
            }
        }
    }

    private class OpenBucket(
        val key: String,
        val tracks: MutableMap<String, TrackEntry>,
        val artists: MutableMap<String, NameEntry>,
        val albums: MutableMap<String, NameEntry>,
        val hours: LongArray,
        val days: MutableMap<Int, Long>,
    ) {
        fun snapshot() = StoredBucket(
            month = key,
            tracks = tracks.values.toList(),
            artists = artists.map { (key, entry) -> entry.copy(key = key) },
            albums = albums.map { (key, entry) -> entry.copy(key = key) },
            hours = hours.toList(),
            days = days.toMap(),
        )

        companion object {
            fun of(key: String, stored: StoredBucket) = OpenBucket(
                key = key,
                tracks = stored.tracks.associateByTo(LinkedHashMap()) { it.id },
                artists = stored.artists
                    .map { it.copy(name = primaryArtist(it.name) ?: it.name) }
                    .mergedBy(LinkedHashMap()) { it.name.lowercase(Locale.ROOT) },
                albums = stored.albums.mergedBy(LinkedHashMap()) { albumKey(it.name, it.sub.orEmpty()) },
                hours = LongArray(24) { stored.hours.getOrElse(it) { 0L } },
                days = stored.days.toMutableMap(),
            )
        }
    }

    private class MergedBucket {
        val tracks = HashMap<String, TrackEntry>()
        val artists = HashMap<String, NameEntry>()
        val albums = HashMap<String, NameEntry>()
        val hours = LongArray(24)
        val days = HashMap<String, Long>()
        var earliest: String? = null

        fun add(bucket: StoredBucket) {
            bucket.tracks.forEach { entry ->
                tracks.merge(entry.id, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.artists.forEach { entry ->
                val lead = entry.copy(name = primaryArtist(entry.name) ?: entry.name)
                artists.merge(lead.name.lowercase(Locale.ROOT), lead) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.albums.forEach { entry ->
                val key = albumKey(entry.name, entry.sub.orEmpty())
                albums.merge(key, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            repeat(24) { hours[it] += bucket.hours.getOrElse(it) { 0L } }
            bucket.days.forEach { (day, ms) ->
                val date = "${bucket.month}-%02d".format(day)
                days[date] = (days[date] ?: 0L) + ms
            }
            if (earliest == null || bucket.month < earliest!!) earliest = bucket.month
        }

        fun toSummary(period: ReplayPeriod, today: LocalDate): ReplaySummary {
            val rankedSongs = tracks.values
                .sortedWith(compareByDescending<TrackEntry> { it.ms }.thenByDescending { it.plays })
                .map {
                    RankedSong(
                        song = Song(
                            videoId = it.id,
                            title = it.title,
                            artist = it.artist,
                            thumbnailUrl = it.art,
                            artistId = it.artistId,
                            albumId = it.albumId,
                            albumName = it.album,
                        ),
                        ms = it.ms,
                        plays = it.plays,
                    )
                }
            val rankedArtists = artists.values
                .sortedWith(compareByDescending<NameEntry> { it.ms }.thenByDescending { it.plays })
                .map {
                    RankedEntry(
                        title = it.name,
                        subtitle = it.sub,
                        artworkUrl = ArtistFacts.imageFor(it.name) ?: it.art,
                        browseId = it.id ?: ArtistFacts.browseIdFor(it.name),
                        ms = it.ms,
                        plays = it.plays,
                    )
                }
            val rankedAlbums = albums.values
                .sortedWith(compareByDescending<NameEntry> { it.ms }.thenByDescending { it.plays })
                .map { RankedEntry(it.name, it.sub, it.art, it.id, it.ms, it.plays) }

            val genreMs = LinkedHashMap<String, Long>()
            val genrePlays = LinkedHashMap<String, Int>()
            val genreArt = LinkedHashMap<String, String?>()
            rankedArtists.forEach { artist ->
                ArtistFacts.genresFor(artist.title).forEach { genre ->
                    genreMs[genre] = (genreMs[genre] ?: 0L) + artist.ms
                    genrePlays[genre] = (genrePlays[genre] ?: 0) + artist.plays
                    if (genreArt[genre] == null) genreArt[genre] = artist.artworkUrl
                }
            }
            rankedArtists.take(ARTISTS_TO_RESOLVE)
                .filter { it.artworkUrl == null || it.browseId == null }
                .forEach { ArtistFacts.noticed(it.title) }

            val rankedGenres = genreMs.entries
                .sortedByDescending { it.value }
                .map {
                    RankedEntry(
                        title = it.key,
                        subtitle = null,
                        artworkUrl = genreArt[it.key],
                        browseId = null,
                        ms = it.value,
                        plays = genrePlays[it.key] ?: 0,
                    )
                }

            val busiest = days.entries.maxByOrNull { it.value }
            return ReplaySummary(
                period = period,
                label = period.label(today),
                totalMs = tracks.values.sumOf { it.ms },
                totalPlays = tracks.values.sumOf { it.plays },
                songs = rankedSongs,
                artists = rankedArtists,
                albums = rankedAlbums,
                genres = rankedGenres,
                hourOfDay = hours.toList(),
                busiestDay = busiest?.key,
                busiestDayMs = busiest?.value ?: 0L,
                distinctSongs = tracks.size,
                distinctArtists = artists.size,
                distinctAlbums = albums.size,
                since = earliest,
            )
        }
    }

    private fun albumKey(name: String, artist: String): String =
        name.lowercase(Locale.ROOT) + ALBUM_KEY_SEPARATOR + artist.lowercase(Locale.ROOT)

    private val ALBUM_KEY_SEPARATOR = Char(UNIT_SEPARATOR).toString()
    private const val UNIT_SEPARATOR = 31

    fun primaryArtist(credit: String): String? {
        lastCredit?.let { (raw, name) -> if (raw == credit) return name }
        val name = credit.split(CREDIT_SEPARATORS)
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        lastCredit = credit to name
        return name
    }

    @Volatile
    private var lastCredit: Pair<String, String?>? = null

    private val CREDIT_SEPARATORS = Regex(
        """\s*,\s*|\s+&\s+|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+|\s+featuring\s+""",
        RegexOption.IGNORE_CASE,
    )

    private const val TAG = "NUViAListening"
    private const val DIRECTORY = "listening"
    private const val ARTISTS_TO_RESOLVE = 15
    private const val KEEP_MONTHS = 36
    private const val MAX_TRACKS = 600
    private const val MAX_NAMES = 400
}

@Serializable
data class TrackEntry(
    val id: String,
    val title: String = "",
    val artist: String = "",
    var album: String? = null,
    var albumId: String? = null,
    var artistId: String? = null,
    var art: String? = null,
    var ms: Long = 0L,
    var plays: Int = 0,
    var last: Long = 0L,
) {
    fun absorb(other: TrackEntry) {
        ms += other.ms
        plays += other.plays
        last = maxOf(last, other.last)
        if (album == null) album = other.album
        if (albumId == null) albumId = other.albumId
        if (artistId == null) artistId = other.artistId
        if (art == null) art = other.art
    }
}

@Serializable
data class NameEntry(
    val name: String = "",
    val sub: String? = null,
    var art: String? = null,
    var id: String? = null,
    var ms: Long = 0L,
    var plays: Int = 0,
    val key: String? = null,
) {
    fun absorb(other: NameEntry) {
        ms += other.ms
        plays += other.plays
        if (art == null) art = other.art
        if (id == null) id = other.id
    }
}

@Serializable
data class StoredBucket(
    val version: Int = 1,
    val month: String,
    val tracks: List<TrackEntry> = emptyList(),
    val artists: List<NameEntry> = emptyList(),
    val albums: List<NameEntry> = emptyList(),
    val hours: List<Long> = List(24) { 0L },
    val days: Map<Int, Long> = emptyMap(),
)

data class RankedEntry(
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val browseId: String?,
    val ms: Long,
    val plays: Int,
)

data class RankedSong(val song: Song, val ms: Long, val plays: Int)

enum class ReplayPeriod(val chip: String) {
    THIS_MONTH("This month"),
    THIS_YEAR("This year"),
    ALL_TIME("All time"),
    ;

    fun covers(month: YearMonth, today: LocalDate): Boolean = when (this) {
        THIS_MONTH -> month == YearMonth.from(today)
        THIS_YEAR -> month.year == today.year
        ALL_TIME -> true
    }

    fun label(today: LocalDate): String = when (this) {
        THIS_MONTH -> YearMonth.from(today).month.name.lowercase(Locale.ROOT)
            .replaceFirstChar { it.uppercase(Locale.ROOT) } + " ${today.year}"
        THIS_YEAR -> today.year.toString()
        ALL_TIME -> "All time"
    }
}

data class ReplaySummary(
    val period: ReplayPeriod,
    val label: String,
    val totalMs: Long,
    val totalPlays: Int,
    val songs: List<RankedSong>,
    val artists: List<RankedEntry>,
    val albums: List<RankedEntry>,
    val genres: List<RankedEntry>,
    val hourOfDay: List<Long>,
    val busiestDay: String?,
    val busiestDayMs: Long,
    val distinctSongs: Int,
    val distinctArtists: Int,
    val distinctAlbums: Int,
    val since: String?,
) {
    val minutes: Long get() = totalMs / 60_000
    val hours: Long get() = totalMs / 3_600_000
    val isEmpty: Boolean get() = songs.isEmpty()
    val peakHour: Int? get() = hourOfDay.withIndex()
        .filter { it.value > 0 }
        .maxByOrNull { it.value }
        ?.index
}

