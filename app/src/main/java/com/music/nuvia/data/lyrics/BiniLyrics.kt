package com.music.nuvia.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Apple Music TTML from BiniLyrics host, indexed by recording and ISRC.
 */
object BiniLyrics {

    private const val BASE = "https://lyrics-api.binimum.org/"

    data class Match(val isrc: String?, val lines: List<LyricLine>)

    internal suspend fun identify(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        isrc: String? = null,
    ): Hit? = withContext(Dispatchers.IO) {
        val url = BASE.toHttpUrl().newBuilder()
            .apply {
                if (!isrc.isNullOrBlank()) {
                    addQueryParameter("isrc", isrc)
                } else {
                    addQueryParameter("track", title)
                    addQueryParameter("artist", artist)
                    if (!album.isNullOrBlank()) addQueryParameter("album", album)
                    val seconds = durationMs / 1000
                    if (seconds > 0) addQueryParameter("duration", seconds.toString())
                }
            }
            .build()

        val body = lyricsGet(url.toString()) ?: return@withContext null
        val response = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()
            ?: return@withContext null
        response.results?.firstOrNull()
    }

    internal suspend fun lyricsFor(hit: Hit): Match? = withContext(Dispatchers.IO) {
        val document = hit.lyricsUrl?.takeIf { it.isNotBlank() } ?: return@withContext null
        val ttml = lyricsGet(document) ?: return@withContext null
        val lines = TtmlLyrics.parse(ttml).takeIf { it.isNotEmpty() } ?: return@withContext null
        Match(hit.isrc?.takeIf { it.isNotBlank() }, lines)
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        isrc: String? = null,
    ): Match? = identify(title, artist, durationMs, album, isrc)?.let { lyricsFor(it) }

    @Serializable
    internal data class Response(
        val total: Int? = null,
        val source: String? = null,
        val results: List<Hit>? = null,
    )

    @Serializable
    internal data class Hit(
        @SerialName("track_name") val trackName: String? = null,
        @SerialName("artist_name") val artistName: String? = null,
        @SerialName("album_name") val albumName: String? = null,
        val duration: Int? = null,
        val isrc: String? = null,
        @SerialName("timing_type") val timingType: String? = null,
        val lyricsUrl: String? = null,
    )
}

