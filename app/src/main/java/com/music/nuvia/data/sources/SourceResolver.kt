package com.music.nuvia.data.sources

import android.net.Uri
import android.util.Log
import com.music.nuvia.data.TrackLog
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.AudioQuality
import com.music.nuvia.data.settings.DownloadQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

/**
 * Turns a queued track into an openable stream, using whichever source can
 * best serve it.
 *
 * Two things happen here that don't happen in any single [MusicSource]:
 *
 *  1. **The quality question is answered once**, from the connection in hand
 *     and the user's ceiling for it — see [requestForNow], or
 *     [requestForDownload] for the one caller whose answer becomes a file
 *     rather than a stream. Sources are told what to serve; they don't each
 *     re-derive it.
 *
 *  2. **The order is applied.** A track is pinned to the source that produced
 *     it, but a pin is a starting point, not a cage: with lossless on, a
 *     higher-priority source that can serve the same recording bit-exact gets
 *     asked first, and a source that fails gets stepped over rather than
 *     failing the track.
 *
 * Whether another source *has* the same recording is [TrackMatcher]'s question,
 * not this one's. Everything here does with a candidate list is ask that, and
 * everything a source is asked for comes from the same place — so the library,
 * a playlist, radio, search and the home feed all substitute on identical
 * terms, whichever of them a track was queued from.
 */
object SourceResolver {

    private const val TAG = "NUViA"

    /**
     * What to ask a source for, right now.
     *
     * The lossless switch is a preference, not an override — it loses to the
     * connection's own ceiling, which is the setting someone reached for
     * specifically to protect a data plan. A capped connection gets a capped
     * transcode whether or not lossless is on, because the alternative is a
     * switch in one part of Settings quietly undoing a switch in another, and
     * the one being undone is the one attached to a bill.
     */
    fun requestForNow(): StreamRequest {
        val ceiling = AppSettings.effectiveAudioQuality
        return if (ceiling != AudioQuality.HIGH) {
            StreamRequest.Capped(ceiling.maxKbps)
        } else {
            StreamRequest.Lossless
        }
    }

    /**
     * What to ask a source for on behalf of a file being kept.
     *
     * Reads [AppSettings.downloadQuality] and nothing else — not the ceilings,
     * not the lossless switch. Both of those are about what a *stream* costs on
     * the connection in hand, and this is the one request whose answer outlives
     * the connection: it becomes a file.
     *
     * @param quality defaults to the setting as it stands, which is what a
     *   caller with no download in flight wants. A caller that is already
     *   fetching one passes the value it started with.
     */
    fun requestForDownload(
        quality: DownloadQuality = AppSettings.downloadQuality.value,
    ): StreamRequest = when {
        quality.keepsLossless -> StreamRequest.Lossless
        quality.maxKbps == Int.MAX_VALUE -> StreamRequest.Best
        else -> StreamRequest.Capped(quality.maxKbps)
    }

    /**
     * @param uri a `bitchord://source?...` URI as built by [SourceRegistry.trackUri].
     * @return the stream, or null when nothing enabled could serve the track.
     */
    suspend fun resolve(uri: Uri): SourceStream? {
        val configId = uri.getQueryParameter("s") ?: return null
        val trackId = uri.getQueryParameter("t") ?: return null
        return resolve(
            configId = configId,
            trackId = trackId,
            target = targetIn(uri),
        )
    }

    /**
     * The recording a playback URI describes, for matching it elsewhere.
     *
     * Title, artist and runtime ride in the URI because they are what a
     * cross-source match is made on, and the resolver runs on ExoPlayer's
     * loader thread with nothing but a DataSpec in hand.
     */
    fun targetIn(uri: Uri) = TrackMatcher.Target(
        title = uri.getQueryParameter("n").orEmpty(),
        artist = uri.getQueryParameter("a").orEmpty(),
        durationSec = uri.getQueryParameter("d")?.toIntOrNull(),
    )

    /**
     * @param target is what a cross-source match is made on. Without it the
     *   only possible behaviour is "the pinned source or nothing", which is
     *   still a correct outcome — just a worse one.
     */
    suspend fun resolve(
        configId: String,
        trackId: String,
        target: TrackMatcher.Target,
    ): SourceStream? {
        val request = requestForNow()
        val pinned = SourceRegistry.instance(configId)
        val active = SourceRegistry.active()

        // The upgrade path: with lossless asked for and the pinned source
        // unable to serve it, anything ranked above it that can is worth
        // asking first. This is the whole reason the list is ordered — it is
        // what makes "my own FLAC of this, if I have one, else stream it"
        // expressible.
        if (request is StreamRequest.Lossless && pinned?.kind?.canServeLossless != true) {
            for (source in rankedAbove(configId, active)) {
                if (!source.kind.canServeLossless) continue
                val upgraded = matchAndStream(source, target, request) ?: continue
                if (upgraded.format.isLossless != true) continue
                TrackLog.d(TAG, "lossless upgrade: '${target.title}' served by ${source.displayName}")
                return upgraded
            }
        }

        if (pinned != null) {
            attempt(pinned) { pinned.stream(trackId, request) }?.let { return it }
        }

        // Last resort. A track whose own source is down is still a track the
        // user asked for, and another source having it is not unlikely.
        val (fallbackSource, stream) =
            bestAcross(active.filterNot { it.configId == configId }, target, request) ?: return null
        TrackLog.d(TAG, "fallback: '${target.title}' served by ${fallbackSource.displayName}")
        return stream
    }

    /**
     * The stream for a YouTube track from a source the user ranked above
     * YouTube, or null when none of them has the recording.
     *
     * Every ranked source is asked at once and the first playable answer is
     * taken — see [bestAcross].
     */
    suspend fun substituteForYouTube(target: TrackMatcher.Target): SourceStream? {
        if (target.title.isBlank()) return null
        val active = SourceRegistry.active()
        val youtube = active.firstOrNull { it.kind == SourceKind.YOUTUBE } ?: return null
        val request = requestForNow()
        val (source, stream) = bestAcross(rankedAbove(youtube.configId, active), target, request)
            ?: return null
        TrackLog.d(
            TAG,
            "substituted: '${target.title}' served by ${source.displayName} over YouTube" +
                " at ${stream.format.summary}" + if (stream.belowRequest) " (below request)" else "",
        )
        return stream
    }

    /**
     * The copy of [target] held by a source quick enough to ask about *before*
     * the track is played — or null when no such source is enabled, or none of
     * them has it.
     */
    suspend fun prefetchSubstitute(target: TrackMatcher.Target): SourceStream? {
        if (target.title.isBlank()) return null
        val active = SourceRegistry.active()
        val youtube = active.firstOrNull { it.kind == SourceKind.YOUTUBE } ?: return null
        val quick = rankedAbove(youtube.configId, active).filter { it.kind.worthPrefetching }
        if (quick.isEmpty()) return null
        val (source, stream) = bestAcross(quick, target, requestForNow()) ?: return null
        TrackLog.d(
            TAG,
            "warmed: '${target.title}' from ${source.displayName} at ${stream.format.summary}",
        )
        return stream
    }

    /**
     * A stream that genuinely satisfies the current request, for a track that
     * is already playing on one that doesn't — or null if there isn't one.
     */
    suspend fun upgradeFor(
        target: TrackMatcher.Target,
        playing: StreamFormat? = null,
    ): SourceStream? {
        if (target.title.isBlank() || target.durationSec == null) return null
        val request = requestForNow()
        val active = SourceRegistry.active()
        val youtube = active.firstOrNull { it.kind == SourceKind.YOUTUBE } ?: return null

        val (source, chosen) = bestAcross(
            rankedAbove(youtube.configId, active),
            target,
            request,
            waitForAll = true,
            strictLength = true,
        ) { candidate, stream ->
            worthSwapping(stream.format, playing).also { worth ->
                if (!worth) {
                    TrackLog.d(
                        TAG,
                        "${candidate.displayName}'s ${stream.format.summary} isn't worth swapping " +
                            "'${target.title}' off ${playing?.summary ?: "an unmeasured stream"}",
                    )
                }
            }
        } ?: return null
        TrackLog.d(TAG, "upgrade found: '${target.title}' at ${chosen.format.summary} from ${source.displayName}")
        return chosen
    }

    /**
     * The copy of a track about to be downloaded that is worth keeping over
     * YouTube's, or null when nothing configured has one.
     */
    suspend fun forDownload(
        target: TrackMatcher.Target,
        request: StreamRequest = requestForDownload(),
    ): SourceStream? = coroutineScope {
        if (target.title.isBlank()) return@coroutineScope null
        val wantsLossless = when (request) {
            is StreamRequest.Lossless -> true
            is StreamRequest.Best -> false
            is StreamRequest.Capped -> return@coroutineScope null
        }
        val active = SourceRegistry.active()
        val youtubeId = active.firstOrNull { it.kind == SourceKind.YOUTUBE }?.configId
        val ranked = rankedAbove(youtubeId.orEmpty(), active)
        val strictLength = target.durationSec != null

        val bitExact = if (wantsLossless) ranked.filter { it.kind.canServeLossless } else emptyList()
        val elsewhere = ranked - bitExact.toSet()

        val elsewhereBest = async {
            bestAcross(
                elsewhere,
                target,
                StreamRequest.Best,
                waitForAll = true,
                strictLength = strictLength,
            ) { source, stream ->
                val ok = wantsLossless || stream.format.isLossless != true
                if (!ok) {
                    TrackLog.d(
                        TAG,
                        "${source.displayName} offered ${stream.format.summary}; " +
                            "more than this download asked for",
                    )
                }
                ok
            }
        }

        var best: Pair<MusicSource, SourceStream>? = null
        for (source in bitExact) {
            val stream = matchAndStream(
                source,
                target,
                request,
                waitForAll = true,
                strictLength = strictLength,
            ) ?: continue
            if (stream.format.isLossless == true) {
                TrackLog.d(
                    TAG,
                    "download: '${target.title}' from ${source.displayName} at ${stream.format.summary}",
                )
                elsewhereBest.cancel()
                return@coroutineScope stream
            }
            TrackLog.d(
                TAG,
                "${source.displayName} offered ${stream.format.summary} to download; not bit-exact",
            )
            if (isBetter(stream.format, best?.second?.format)) best = source to stream
        }

        elsewhereBest.await()?.let { (source, stream) ->
            if (isBetter(stream.format, best?.second?.format)) best = source to stream
        }
        val (winner, chosen) = best ?: return@coroutineScope null
        if (!beatsYouTubeAac(chosen.format)) {
            TrackLog.d(
                TAG,
                "best offered for '${target.title}' is ${winner.displayName}'s " +
                    "${chosen.format.summary}; taking YouTube's AAC over that",
            )
            return@coroutineScope null
        }
        TrackLog.d(
            TAG,
            "download: '${target.title}' from ${winner.displayName} at ${chosen.format.summary}, " +
                "over YouTube's AAC",
        )
        chosen
    }

    /**
     * Whether a lossy [candidate] is worth keeping as a file over whatever
     * YouTube's own AAC ladder would have given for the same track.
     */
    internal fun beatsYouTubeAac(candidate: StreamFormat): Boolean =
        (candidate.kbps ?: 0) > YOUTUBE_BEST_AAC_KBPS

    /**
     * Whether [candidate] is enough better than [playing] to be worth the
     * break in the audio that swapping to it costs.
     */
    internal fun worthSwapping(candidate: StreamFormat, playing: StreamFormat?): Boolean {
        if (candidate.isLossless == true) return true
        val gain = (candidate.kbps ?: return false) - (playing?.kbps ?: return false)
        return gain >= UPGRADE_MIN_GAIN_KBPS
    }

    /**
     * Whether two runtimes are close enough to be the same recording, for a
     * swap into a track that is already playing.
     */
    fun sameRecordingAs(candidateSec: Int?, playingSec: Int?): Boolean {
        if (candidateSec == null || playingSec == null) return false
        return kotlin.math.abs(candidateSec - playingSec) <= UPGRADE_DRIFT_SEC
    }

    /**
     * Whether anything outranks YouTube right now — i.e. whether a YouTube
     * track is worth offering around before it is resolved.
     */
    fun canSubstituteForYouTube(): Boolean =
        SourceRegistry.active().indexOfFirst { it.kind == SourceKind.YOUTUBE } > 0

    /**
     * The sources ranked above [configId], in order.
     */
    private fun rankedAbove(configId: String, active: List<MusicSource>): List<MusicSource> =
        active.indexOfFirst { it.configId == configId }
            .let { if (it < 0) active.size else it }
            .let { active.take(it) }

    /**
     * The first stream any of [sources] can serve for [target] — **all of them
     * asked at once** — or null if none of them has the recording.
     */
    internal suspend fun bestAcross(
        sources: List<MusicSource>,
        target: TrackMatcher.Target,
        request: StreamRequest,
        waitForAll: Boolean = false,
        strictLength: Boolean = false,
        accept: (MusicSource, SourceStream) -> Boolean = { _, _ -> true },
    ): Pair<MusicSource, SourceStream>? = coroutineScope {
        val running: MutableList<Deferred<Pair<MusicSource, SourceStream?>>> = sources
            .map { source ->
                async { source to matchAndStream(source, target, request, waitForAll, strictLength) }
            }
            .toMutableList()
        var best: Pair<MusicSource, SourceStream>? = null
        try {
            while (running.isNotEmpty()) {
                val first = select {
                    running.forEach { candidate -> candidate.onAwait { candidate } }
                }
                val ready = listOf(first) + running.filter { it !== first && it.isCompleted }
                running -= ready.toSet()
                for (done in ready) {
                    val (source, stream) = done.await()
                    if (stream == null) continue
                    if (!accept(source, stream)) continue
                    if (isBetter(stream.format, best?.second?.format)) best = source to stream
                }
                if (best != null) break
            }
        } finally {
            running.forEach { it.cancel() }
        }
        best
    }

    /**
     * Searches [source] for the recording in [target] and streams it if one of
     * the answers really is that recording — see [TrackMatcher].
     */
    private suspend fun matchAndStream(
        source: MusicSource,
        target: TrackMatcher.Target,
        request: StreamRequest,
        waitForAll: Boolean = false,
        strictLength: Boolean = false,
    ): SourceStream? {
        for (query in TrackMatcher.queries(target)) {
            val candidates = attempt(source) {
                source.search(query, limit = MATCH_CANDIDATES, waitForAll = waitForAll)
            } ?: return null
            var matches = TrackMatcher.ranked(candidates, target)
            if (strictLength) {
                matches = matches.filter { TrackMatcher.withinSeconds(it, target, UPGRADE_DRIFT_SEC) }
            }
            if (matches.isEmpty()) continue
            return streamBest(source, matches, target, request)
        }
        return null
    }

    internal fun preferred(
        matches: List<Song>,
        target: TrackMatcher.Target,
        wantsLossless: Boolean,
    ): List<Song> {
        val sameLength = matches.filter { TrackMatcher.withinSeconds(it, target, SAME_RECORDING_SEC) }
        val eligible = sameLength.ifEmpty { matches }
        if (!wantsLossless) return eligible
        return eligible.sortedByDescending { it.sourceQuality == ModuleSource.LOSSLESS }
    }

    private suspend fun streamBest(
        source: MusicSource,
        matches: List<Song>,
        target: TrackMatcher.Target,
        request: StreamRequest,
    ): SourceStream? {
        val wantsLossless = request is StreamRequest.Lossless
        val ordered = preferred(matches, target, wantsLossless)
        var settleFor: SourceStream? = null
        for (match in ordered.take(STREAM_ATTEMPTS)) {
            val trackId = SourceRegistry.parseTrackKey(match.videoId)?.second ?: match.videoId
            val opened = attempt(source) { source.stream(trackId, request) } ?: continue
            val stream = opened.copy(durationSec = TrackMatcher.secondsOf(match.durationText))
            val served = stream.format
            if (!wantsLossless || served.isLossless == true || served.statesNothingLossy) {
                TrackLog.d(
                    TAG,
                    "${source.displayName} matched '${match.title}' by '${match.artist}' → ${served.summary}",
                )
                return stream
            }
            TrackLog.d(TAG, "${source.displayName} offered ${served.summary} for '${match.title}'; looking further")
            settleFor = betterOf(settleFor, stream.copy(belowRequest = true))
        }
        return settleFor
    }

    /**
     * Whether [candidate] is a better rendition than [current], by codec first
     * and bitrate second. A null [current] is beaten by anything.
     */
    internal fun isBetter(candidate: StreamFormat, current: StreamFormat?): Boolean {
        if (current == null) return true
        if (candidate.isLossless != current.isLossless) return candidate.isLossless == true
        return (candidate.kbps ?: 0) > (current.kbps ?: 0)
    }

    /** The higher-quality of two streams — see [isBetter]. */
    private fun betterOf(current: SourceStream?, candidate: SourceStream): SourceStream =
        if (current == null || isBetter(candidate.format, current.format)) candidate else current

    /**
     * Whether a format has said nothing that rules lossless out.
     */
    private val StreamFormat.statesNothingLossy: Boolean
        get() = isLossless == null && kbps == null

    /**
     * Runs [block], turning any failure into null and a log line.
     */
    private suspend fun <T> attempt(source: MusicSource, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TrackLog.w(TAG, "${source.displayName} failed: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

    private const val MATCH_CANDIDATES = 15
    private const val STREAM_ATTEMPTS = 3
    private const val UPGRADE_DRIFT_SEC = 2
    private const val SAME_RECORDING_SEC = 3
    private const val YOUTUBE_BEST_AAC_KBPS = 256
    private const val UPGRADE_MIN_GAIN_KBPS = 96
}
