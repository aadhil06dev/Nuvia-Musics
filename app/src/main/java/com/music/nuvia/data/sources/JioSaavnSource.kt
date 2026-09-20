package com.music.nuvia.data.sources

import com.music.nuvia.data.TrackLog
import com.music.nuvia.data.jiosaavn.JioSaavnService
import com.music.nuvia.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "NUViA"

class JioSaavnSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.JIOSAAVN
    override val displayName: String get() = config.label.ifBlank { SourceKind.JIOSAAVN.label }

    override suspend fun health(): SourceHealth = withContext(Dispatchers.IO) {
        val test = JioSaavnService.testConnection()
        if (test.success) {
            SourceHealth.Ok("Connected (${test.latencyMs}ms • ${test.streamBitrate ?: "320kbps"})")
        } else {
            SourceHealth.Unreachable(test.errorMessage ?: "Connection failed")
        }
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> {
        TrackLog.d(TAG, "▶ JioSaavn searchSongs() query=\"$query\" limit=$limit")
        val results = JioSaavnService.searchSongs(query)
        TrackLog.d(TAG, "  ✓ JioSaavn returned ${results.size} tracks" + results.take(3)
            .joinToString(prefix = ": ", separator = "; ") { "'${it.title}' by '${
                it.moreInfo.artistMap.primaryArtists.joinToString(", ") { a -> a.name }
            }' ${it.moreInfo.duration}s" }.takeIf { results.isNotEmpty() }.orEmpty())

        return results.take(limit).map { raw ->
            val primaryArtists = raw.moreInfo.artistMap.primaryArtists.joinToString(", ") { it.name }
            val artistName = primaryArtists.ifBlank { "Unknown Artist" }
            
            val thumbnail = raw.image
                .replace(Regex("150x150|50x50"), "500x500")
                .replace(Regex("^http://"), "https://")

            Song(
                videoId = SourceRegistry.trackKey(config.id, raw.id),
                title = raw.title,
                artist = artistName,
                thumbnailUrl = thumbnail,
                durationText = raw.moreInfo.duration.toIntOrNull()?.let { seconds ->
                    val m = seconds / 60
                    val s = seconds % 60
                    String.format("%d:%02d", m, s)
                },
                sourceQuality = "HIGH"
            )
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        TrackLog.d(TAG, "▶ JioSaavn getStreamUrl() trackId=$trackId request=$request")
        val stream = JioSaavnService.getStreamUrl(trackId)
        if (stream == null || stream.url.isBlank()) {
            TrackLog.w(TAG, "  ✗ JioSaavn had no stream URL for $trackId")
            return null
        }

        if (stream.kbps != null && stream.kbps <= MIN_USABLE_KBPS) {
            TrackLog.w(
                TAG,
                "  ✗ JioSaavn only offered ${stream.kbps}kbps for $trackId; not worth playing",
            )
            return null
        }

        TrackLog.d(TAG, "  ✓ JioSaavn ${stream.kbps ?: "?"}kbps ${stream.url.take(96)}")
        return SourceStream(
            url = stream.url,
            format = StreamFormat(codec = "mp4", kbps = stream.kbps),
        )
    }

    private companion object {
        const val MIN_USABLE_KBPS = 96
    }
}
