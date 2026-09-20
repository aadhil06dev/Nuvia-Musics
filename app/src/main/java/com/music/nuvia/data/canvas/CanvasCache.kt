package com.music.nuvia.data.canvas

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Disk cache for canvas clips — the looping video some releases publish
 * alongside a track, played over the cover art by
 * [CanvasArtworkPlayer][com.music.nuvia.ui.player.CanvasArtworkPlayer].
 *
 * Wrapping the upstream in [CacheDataSource] means only the first loop of a
 * clip ever reaches the network; every loop after it, and every replay of
 * the same track later in the session, is served from disk instead.
 */
@UnstableApi
object CanvasCache {

    /**
     * Small on purpose — a clip is a few seconds of video, not a song, and
     * this only needs to outlive one player screen's worth of looping, not
     * a library. [SimpleCache]'s own evictor reclaims the rest.
     */
    private const val CACHE_LIMIT_BYTES = 150L * 1024 * 1024

    private lateinit var cache: SimpleCache

    /** Opened once per process, alongside [com.music.nuvia.playback.AudioCache.init]. */
    fun init(context: Context) {
        cache = SimpleCache(
            File(context.cacheDir, "canvas"),
            LeastRecentlyUsedCacheEvictor(CACHE_LIMIT_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    /**
     * [upstream] wrapped so a clip already on disk never touches the
     * network again — see the class doc for why that is the whole point.
     * A cache write that fails (full disk, evicted mid-write) drops back to
     * plain streaming rather than surfacing as a playback error, the same
     * choice [com.music.nuvia.playback.AudioCache] makes for audio.
     */
    fun dataSourceFactory(upstream: DataSource.Factory): DataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
