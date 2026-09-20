package com.music.nuvia.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * A community-submitted lyrics database.
 */
object Unison {

    private const val BASE = "https://unison.boidu.dev/lyrics"

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("song", title)
            .addQueryParameter("artist", artist)
            .apply {
                if (!album.isNullOrBlank()) addQueryParameter("album", album)
                val seconds = durationMs / 1000
                if (seconds > 0) addQueryParameter("duration", seconds.toString())
            }
            .build()

        val body = lyricsGet(url.toString()) ?: return@withContext null
        val response = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()
            ?: return@withContext null
        if (response.success != true) return@withContext null
        linesOf(response.data ?: return@withContext null)
    }

    internal fun linesOf(data: Entry): List<LyricLine>? {
        val text = data.lyrics?.takeIf { it.isNotBlank() } ?: return null
        val lines = when {
            data.format.equals(TTML, ignoreCase = true) -> TtmlLyrics.parse(text)
            data.syncType.equals(PLAIN, ignoreCase = true) -> plain(text)
            else -> EnhancedLrc.parse(text).takeIf { it.isNotEmpty() } ?: LrcLib.parseLrc(text)
        }
        return lines.takeIf { it.isNotEmpty() }
    }

    private fun plain(text: String): List<LyricLine> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { LyricLine(timeMs = 0, text = it) }
        .toList()

    private const val TTML = "ttml"
    private const val PLAIN = "plain"

    @Serializable
    internal data class Response(
        val success: Boolean? = null,
        val data: Entry? = null,
    )

    @Serializable
    internal data class Entry(
        val song: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        val format: String? = null,
        val syncType: String? = null,
        val confidence: String? = null,
        val voteCount: Int? = null,
    )
}

