package com.music.nuvia.data.artist

import android.content.Context
import com.music.nuvia.data.DebugLog
import com.music.nuvia.data.Http
import com.music.nuvia.data.scrobbling.LastFM
import com.music.nuvia.data.settings.AppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for artist facts, biographies, and metadata enrichment.
 *
 * Guarantees:
 * - Asynchronous execution on Dispatchers.IO
 * - In-flight deduplication: simultaneous requests for the same artist share the same worker
 * - In-memory and disk caching with stale-while-revalidate capability
 * - Offline safety: failed requests never destroy valid existing cache
 * - Conservative tag/genre normalization without fabricating data
 */
object ArtistFactsRepository {
    private const val TAG = "ArtistFactsRepo"
    private const val CACHE_DIR_NAME = "artist_facts_v1"
    private const val MAX_MEMORY_CACHE_ENTRIES = 100

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memoryCache = ConcurrentHashMap<String, ArtistFacts>()
    private val inFlight = ConcurrentHashMap<String, Deferred<ArtistFacts>>()
    private val inFlightMutex = Mutex()

    @Volatile
    private var cacheDir: File? = null

    fun initialize(context: Context) {
        try {
            val dir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            cacheDir = dir
        } catch (e: Exception) {
            DebugLog.w(TAG, "Failed to initialize artist facts cache dir: ${e.message}")
        }
    }

    /**
     * Retrieves cached facts immediately without performing any network requests.
     */
    fun getCached(artistName: String): ArtistFacts? {
        val key = cacheKey(artistName)
        if (key.isBlank()) return null
        return memoryCache[key] ?: readFromDisk(key)?.also {
            memoryCache[key] = it
        }
    }

    /**
     * Fetches artist facts asynchronously with deduplication, multi-tier fallback, and caching.
     *
     * @param artistName The canonical artist name
     * @param ytDescription Official bio or description directly from YouTube Music / Innertube
     * @param ytSubscribers Subscriber or follower count string from YouTube Music
     * @param forceRefresh Force bypassing the memory cache
     */
    suspend fun getArtistFacts(
        artistName: String,
        ytDescription: String? = null,
        ytSubscribers: String? = null,
        forceRefresh: Boolean = false,
    ): ArtistFacts = withContext(Dispatchers.IO) {
        val key = cacheKey(artistName)
        if (key.isBlank()) {
            return@withContext ArtistFacts(artistName = artistName)
        }

        // Return from memory cache if available and refresh not forced
        if (!forceRefresh) {
            memoryCache[key]?.let { return@withContext it }
            readFromDisk(key)?.let {
                memoryCache[key] = it
                return@withContext it
            }
        }

        // Deduplicate in-flight requests for the same artist
        val deferred = inFlightMutex.withLock {
            inFlight[key] ?: scope.async {
                fetchAndMerge(key, artistName, ytDescription, ytSubscribers)
            }.also { inFlight[key] = it }
        }

        try {
            deferred.await()
        } finally {
            inFlightMutex.withLock {
                inFlight.remove(key)
            }
        }
    }

    private suspend fun fetchAndMerge(
        key: String,
        artistName: String,
        ytDescription: String?,
        ytSubscribers: String?,
    ): ArtistFacts {
        // Read existing stale cache to ensure failure never destroys valid data
        val existing = memoryCache[key] ?: readFromDisk(key)

        var bio = existing?.biography ?: ytDescription?.takeIf { it.isNotBlank() }
        var bioSummary = existing?.biographySummary
        val genres = existing?.genres.orEmpty().toMutableList()
        val tags = existing?.tags.orEmpty().toMutableList()
        var origin = existing?.origin
        var formedYear = existing?.formedYear
        var listeners = existing?.listeners
        var playCount = existing?.playCount
        var onTour = existing?.onTour ?: false
        val similar = existing?.similarArtists.orEmpty().toMutableList()
        var source = existing?.source ?: (if (ytDescription != null) "YouTube Music" else null)

        // 1. Try Last.fm for rich artist biography, tags, listeners, playcount
        try {
            val lastFmFacts = queryLastFm(artistName)
            if (lastFmFacts != null) {
                if (bio.isNullOrBlank() && !lastFmFacts.biography.isNullOrBlank()) {
                    bio = lastFmFacts.biography
                }
                if (bioSummary.isNullOrBlank() && !lastFmFacts.biographySummary.isNullOrBlank()) {
                    bioSummary = lastFmFacts.biographySummary
                }
                if (lastFmFacts.tags.isNotEmpty()) {
                    tags.addAll(lastFmFacts.tags)
                }
                if (listeners == null) listeners = lastFmFacts.listeners
                if (playCount == null) playCount = lastFmFacts.playCount
                if (lastFmFacts.onTour) onTour = true
                if (lastFmFacts.similarArtists.isNotEmpty()) {
                    similar.addAll(lastFmFacts.similarArtists)
                }
                source = "Last.fm"
            }
        } catch (e: Exception) {
            DebugLog.d(TAG, "Last.fm artist info query failed for $artistName: ${e.message}")
        }

        // 2. If origin or formedYear or genres are still missing, try MusicBrainz
        if (origin.isNullOrBlank() || formedYear.isNullOrBlank() || (genres.isEmpty() && tags.isEmpty())) {
            try {
                val mbFacts = queryMusicBrainz(artistName)
                if (mbFacts != null) {
                    if (origin.isNullOrBlank()) origin = mbFacts.origin
                    if (formedYear.isNullOrBlank()) formedYear = mbFacts.formedYear
                    if (mbFacts.genres.isNotEmpty()) genres.addAll(mbFacts.genres)
                    if (mbFacts.tags.isNotEmpty()) tags.addAll(mbFacts.tags)
                    if (source == null) source = "MusicBrainz"
                }
            } catch (e: Exception) {
                DebugLog.d(TAG, "MusicBrainz artist query failed for $artistName: ${e.message}")
            }
        }

        // Fallback: If biography still has HTML or trailing license notices, clean it
        val cleanedBio = bio?.let(::cleanBiography)
        val cleanedSummary = bioSummary?.let(::cleanBiography) ?: cleanedBio?.take(280)

        val result = ArtistFacts(
            artistName = artistName,
            biography = cleanedBio,
            biographySummary = cleanedSummary,
            genres = normalizeTags(genres),
            tags = normalizeTags(tags),
            origin = origin?.trim()?.takeIf { it.isNotBlank() },
            formedYear = formedYear?.trim()?.takeIf { it.isNotBlank() },
            listeners = listeners,
            playCount = playCount,
            onTour = onTour,
            similarArtists = similar.distinct().take(8),
            source = source,
            cachedAt = System.currentTimeMillis(),
        )

        // Cache the result in memory and on disk
        if (!result.isEmpty) {
            if (memoryCache.size >= MAX_MEMORY_CACHE_ENTRIES) {
                memoryCache.clear()
            }
            memoryCache[key] = result
            writeToDisk(key, result)
        } else if (existing != null) {
            // Keep stale cache if the network returned empty
            return existing
        }

        return result
    }

    /**
     * Queries Last.fm artist.getInfo endpoint.
     */
    private fun queryLastFm(artistName: String): ArtistFacts? {
        val apiKey = AppSettings.lastfmApiKey.value.ifBlank { LastFM.FALLBACK_COMPAT_API_KEY }
        if (apiKey.isBlank()) return null

        val encoded = URLEncoder.encode(artistName, "UTF-8")
        val endpoint = AppSettings.lastfmEndpoint.value.ifBlank { LastFM.DEFAULT_API_ENDPOINT }
        val url = "$endpoint?method=artist.getInfo&artist=$encoded&api_key=$apiKey&format=json&autocorrect=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NUViA/1.0 (Android)")
            .get()
            .build()

        val response = Http.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null

        if (body.contains("\"error\":")) return null

        val root = json.parseToJsonElement(body).jsonObject
        val artistObj = root["artist"]?.jsonObject ?: return null

        val bioObj = artistObj["bio"]?.jsonObject
        val bioContent = bioObj?.get("content")?.jsonPrimitive?.content
        val bioSummary = bioObj?.get("summary")?.jsonPrimitive?.content

        val statsObj = artistObj["stats"]?.jsonObject
        val listeners = statsObj?.get("listeners")?.jsonPrimitive?.content?.toLongOrNull()
        val playcount = statsObj?.get("playcount")?.jsonPrimitive?.content?.toLongOrNull()
        val onTour = artistObj["ontour"]?.jsonPrimitive?.content == "1"

        val tagsList = mutableListOf<String>()
        val tagsNode = artistObj["tags"]?.jsonObject?.get("tag")
        when (tagsNode) {
            is JsonArray -> {
                for (item in tagsNode) {
                    item.jsonObject["name"]?.jsonPrimitive?.content?.let { tagsList.add(it) }
                }
            }
            is JsonObject -> {
                tagsNode["name"]?.jsonPrimitive?.content?.let { tagsList.add(it) }
            }
            else -> Unit
        }

        val similarList = mutableListOf<String>()
        val similarNode = artistObj["similar"]?.jsonObject?.get("artist")
        if (similarNode is JsonArray) {
            for (item in similarNode) {
                item.jsonObject["name"]?.jsonPrimitive?.content?.let { similarList.add(it) }
            }
        }

        return ArtistFacts(
            artistName = artistObj["name"]?.jsonPrimitive?.content ?: artistName,
            biography = bioContent,
            biographySummary = bioSummary,
            tags = tagsList,
            listeners = listeners,
            playCount = playcount,
            onTour = onTour,
            similarArtists = similarList,
            source = "Last.fm",
        )
    }

    /**
     * Queries the open MusicBrainz API for official origin, formation year, and genres.
     */
    private fun queryMusicBrainz(artistName: String): ArtistFacts? {
        val encoded = URLEncoder.encode(artistName, "UTF-8")
        val url = "https://musicbrainz.org/ws/2/artist/?query=artist:%22$encoded%22&fmt=json&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NUViA/1.0 ( music.nuvia.app )")
            .get()
            .build()

        val response = Http.client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null

        val root = json.parseToJsonElement(body).jsonObject
        val artists = root["artists"]?.jsonArray ?: return null
        val artist = artists.firstOrNull()?.jsonObject ?: return null

        val areaName = artist["area"]?.jsonObject?.get("name")?.jsonPrimitive?.content
        val country = artist["country"]?.jsonPrimitive?.content
        val origin = when {
            areaName != null && country != null && !areaName.equals(country, ignoreCase = true) -> "$areaName, $country"
            areaName != null -> areaName
            country != null -> country
            else -> null
        }

        val lifeSpan = artist["life-span"]?.jsonObject
        val beginYear = lifeSpan?.get("begin")?.jsonPrimitive?.content?.take(4)

        val tagsList = mutableListOf<String>()
        val tagsArr = artist["tags"]?.jsonArray
        if (tagsArr != null) {
            for (item in tagsArr) {
                item.jsonObject["name"]?.jsonPrimitive?.content?.let { tagsList.add(it) }
            }
        }

        return ArtistFacts(
            artistName = artist["name"]?.jsonPrimitive?.content ?: artistName,
            origin = origin,
            formedYear = beginYear,
            tags = tagsList,
            source = "MusicBrainz",
        )
    }

    /**
     * Conservatively normalizes genre and tag strings:
     * - Trims whitespace and boundary punctuation
     * - Capitalizes into standard title-case (preserving standard acronyms like R&B, EDM, OST, UK, US)
     * - Removes blank entries
     * - Deduplicates case-insensitively
     */
    fun normalizeTags(tags: List<String>): List<String> {
        return tags.asSequence()
            .map { it.trim().trim('.', ',', ';', ':', '/', '\\', '"', '\'') }
            .filter { it.isNotBlank() && it.length <= 40 }
            .map { tag ->
                tag.split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        val lower = word.lowercase(Locale.ROOT)
                        when (lower) {
                            "r&b" -> "R&B"
                            "edm" -> "EDM"
                            "ost" -> "OST"
                            "dj" -> "DJ"
                            "uk" -> "UK"
                            "us" -> "US"
                            "usa" -> "USA"
                            "k-pop", "kpop" -> "K-Pop"
                            "j-pop", "jpop" -> "J-Pop"
                            else -> word.replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                            }
                        }
                    }
            }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toList()
    }

    /**
     * Cleans biography strings:
     * - Strips HTML tags
     * - Strips Last.fm user boilerplate and read more links
     * - Collapses redundant line breaks
     */
    fun cleanBiography(raw: String): String {
        var text = raw.replace(Regex("<[^>]*>"), "")
        // Remove Last.fm footer
        val readMoreIdx = text.indexOf("Read more on Last.fm", ignoreCase = true)
        if (readMoreIdx != -1) {
            text = text.substring(0, readMoreIdx).trim()
        }
        val userContributedIdx = text.indexOf("User-contributed text is available", ignoreCase = true)
        if (userContributedIdx != -1) {
            text = text.substring(0, userContributedIdx).trim()
        }
        return text.trim()
    }

    private fun cacheKey(name: String): String {
        return name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_")
    }

    private fun readFromDisk(key: String): ArtistFacts? {
        val dir = cacheDir ?: return null
        return try {
            val file = File(dir, "$key.json")
            if (file.exists() && file.isFile) {
                val content = file.readText(Charsets.UTF_8)
                json.decodeFromString<ArtistFacts>(content)
            } else {
                null
            }
        } catch (e: Exception) {
            DebugLog.d(TAG, "Failed to read cached artist facts for $key: ${e.message}")
            null
        }
    }

    private fun writeToDisk(key: String, facts: ArtistFacts) {
        val dir = cacheDir ?: return
        try {
            val file = File(dir, "$key.json")
            val content = json.encodeToString(ArtistFacts.serializer(), facts)
            file.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            DebugLog.d(TAG, "Failed to write artist facts cache for $key: ${e.message}")
        }
    }

    /**
     * Clears in-memory and disk caches (for testing or storage cleanup).
     */
    fun clearCache() {
        memoryCache.clear()
        cacheDir?.listFiles()?.forEach { it.delete() }
    }
}
