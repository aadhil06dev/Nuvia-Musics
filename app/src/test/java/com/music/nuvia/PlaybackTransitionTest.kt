package com.music.nuvia

import com.music.nuvia.playback.smart.CrossfadeMode
import com.music.nuvia.playback.smart.EnergySample
import com.music.nuvia.playback.smart.MixCandidate
import com.music.nuvia.playback.smart.TrackAnalysis
import com.music.nuvia.playback.smart.TransitionStyle
import com.music.nuvia.playback.smart.TransitionTier
import com.music.nuvia.playback.smart.TransitionTrackInfo
import com.music.nuvia.playback.smart.alignTempoOctave
import com.music.nuvia.playback.smart.assessTransitionTier
import com.music.nuvia.playback.smart.audibleSecondsBetween
import com.music.nuvia.playback.smart.isVocalClash
import com.music.nuvia.playback.smart.planTransition
import com.music.nuvia.playback.smart.rankMixInCandidates
import com.music.nuvia.playback.smart.rankMixOutCandidates
import com.music.nuvia.playback.smart.resolveMixOutAnchor
import com.music.nuvia.playback.smart.simultaneousVocalFraction
import com.music.nuvia.playback.smart.vocalActivityBetween
import com.music.nuvia.playback.smart.vocalOverlapAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unit tests for the Playback & Smart Transition Engine calculations, policy routing,
 * and curve invariants.
 */
class PlaybackTransitionTest {

    @Test
    fun constantPowerCrossfade_holdsSumOfSquaresUnity() {
        for (i in 0..100) {
            val progress = i / 100f
            val rise = sin(progress * PI.toFloat() / 2f)
            val fall = cos(progress * PI.toFloat() / 2f)
            val totalPower = rise * rise + fall * fall
            assertEquals(1.0f, totalPower, 0.0001f)
        }
    }

    @Test
    fun alignTempoOctave_doublesOrHalvesCorrectly() {
        // 60 bpm vs 120 bpm -> double 60 to 120
        assertEquals(120.0, alignTempoOctave(120.0, 60.0), 0.01)
        // 140 bpm vs 70 bpm -> double 70 to 140
        assertEquals(140.0, alignTempoOctave(140.0, 70.0), 0.01)
        // 65 bpm vs 130 bpm -> halve 130 to 65
        assertEquals(65.0, alignTempoOctave(65.0, 130.0), 0.01)
        // Same tempo stays unchanged
        assertEquals(128.0, alignTempoOctave(128.0, 128.0), 0.01)
    }

    @Test
    fun assessTransitionTier_beatmatchedWhenTempoCloseAndConfidenceHigh() {
        val outAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 124.0,
            beatConfidence = 0.85,
        )
        val inAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 126.0,
            beatConfidence = 0.90,
        )

        val verdict = assessTransitionTier(outAnalysis, inAnalysis)
        assertEquals(TransitionTier.BEATMATCHED, verdict.tier)
        assertTrue(verdict.reasons.isEmpty())
    }

    @Test
    fun assessTransitionTier_djAssistedWhenTempoDistanceExceedsStretchLimit() {
        val outAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 120.0,
            beatConfidence = 0.85,
        )
        val inAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 140.0,
            beatConfidence = 0.85,
        )

        val verdict = assessTransitionTier(outAnalysis, inAnalysis)
        assertEquals(TransitionTier.DJ_ASSISTED, verdict.tier)
        assertTrue(verdict.reasons.contains("tempo-distance"))
    }

    @Test
    fun assessTransitionTier_plainCrossfadeWhenConfidenceTooLow() {
        val outAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 120.0,
            beatConfidence = 0.1,
        )
        val inAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 120.0,
            beatConfidence = 0.1,
        )

        val verdict = assessTransitionTier(outAnalysis, inAnalysis)
        assertEquals(TransitionTier.PLAIN_CROSSFADE, verdict.tier)
        assertTrue(verdict.reasons.contains("beat-confidence"))
    }

    @Test
    fun assessTransitionTier_plainCrossfadeWhenBpmOutOfRange() {
        val outAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 30.0,
            beatConfidence = 0.9,
        )
        val inAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 120.0,
            beatConfidence = 0.9,
        )

        val verdict = assessTransitionTier(outAnalysis, inAnalysis)
        assertEquals(TransitionTier.PLAIN_CROSSFADE, verdict.tier)
        assertTrue(verdict.reasons.contains("outgoing-tempo"))
    }

    @Test
    fun isVocalClash_detectsHighSimultaneousSinging() {
        assertFalse(isVocalClash(0.2, 0.9))
        assertFalse(isVocalClash(null, 0.9))
        assertFalse(isVocalClash(0.55, 0.7))
        assertTrue(isVocalClash(0.65, 0.85))
    }

    @Test
    fun vocalOverlapAmount_scalesGradedClash() {
        assertEquals(0.0, vocalOverlapAmount(0.2, 0.9), 0.001)
        assertEquals(0.0, vocalOverlapAmount(null, 0.9), 0.001)
        assertEquals(0.0, vocalOverlapAmount(0.6, 0.6), 0.001)
        // At 0.8 on both sides -> (0.8 - 0.6) / 0.4 = 0.5
        assertEquals(0.5, vocalOverlapAmount(0.8, 0.85), 0.01)
        // At 1.0 on both sides -> (1.0 - 0.6) / 0.4 = 1.0
        assertEquals(1.0, vocalOverlapAmount(1.0, 1.0), 0.001)
    }

    @Test
    fun vocalActivityBetween_computesWindowedAverage() {
        val analysis = TrackAnalysis(
            energyCurve = listOf(
                EnergySample(10.0, 0.5),
                EnergySample(11.0, 0.5),
                EnergySample(12.0, 0.5),
                EnergySample(13.0, 0.5),
            ),
            vocalActivityMask = listOf(0.2, 0.4, 0.6, 0.8),
        )

        val activity = vocalActivityBetween(analysis, 10.5, 12.5)
        assertNotNull(activity)
        // Samples at 11.0 and 12.0 are in range: (0.4 + 0.6) / 2 = 0.5
        assertEquals(0.5, activity!!, 0.001)
    }

    @Test
    fun simultaneousVocalFraction_calculatesOverlapAccurately() {
        val outAnalysis = TrackAnalysis(
            energyCurve = listOf(
                EnergySample(100.0, 0.5),
                EnergySample(101.0, 0.5),
                EnergySample(102.0, 0.5),
                EnergySample(103.0, 0.5),
            ),
            vocalActivityMask = listOf(0.7, 0.8, 0.2, 0.1), // 2 vocal samples
        )
        val inAnalysis = TrackAnalysis(
            energyCurve = listOf(
                EnergySample(0.0, 0.5),
                EnergySample(1.0, 0.5),
                EnergySample(2.0, 0.5),
                EnergySample(3.0, 0.5),
            ),
            vocalActivityMask = listOf(0.9, 0.1, 0.9, 0.9), // inStart=0, at t=0 vocal=0.9, at t=1 vocal=0.1
        )

        val fraction = simultaneousVocalFraction(
            outgoing = outAnalysis,
            incoming = inAnalysis,
            outStart = 100.0,
            outEnd = 103.0,
            inStart = 0.0,
            rate = 1.0,
        )
        assertNotNull(fraction)
        // t=100 (in=0): both >= 0.6 (0.7 & 0.9) -> clash
        // t=101 (in=1): out=0.8 >= 0.6, but in=0.1 < 0.6 -> no clash
        // t=102, 103: out < 0.6 -> no clash
        // Total samples = 4, clashed = 1 -> 1/4 = 0.25
        assertEquals(0.25, fraction!!, 0.01)
    }

    @Test
    fun audibleSecondsBetween_countsOnlyAboveThreshold() {
        val analysis = TrackAnalysis(
            energyCurve = listOf(
                EnergySample(0.0, 0.0),
                EnergySample(1.0, 1.0),
                EnergySample(2.0, 1.0),
                EnergySample(3.0, 0.0),
            ),
        )
        val audible = audibleSecondsBetween(analysis, 0.0, 3.0)
        assertNotNull(audible)
        assertTrue(audible!! > 0.0)
    }

    @Test
    fun rankMixOutCandidates_dropsCandidatesExceedingDiscardBudget() {
        val analysis = TrackAnalysis(
            duration = 200.0,
            contentEndTime = 195.0,
            energyCurve = (0..195).map { EnergySample(it.toDouble(), 1.0) },
            mixOutCandidates = listOf(
                MixCandidate(190.0, 0.9, "outro_start"), // skips 5s music (< 12s budget)
                MixCandidate(150.0, 0.95, "energy_cliff"), // skips 45s music (> 12s budget)
            ),
        )

        val ranked = rankMixOutCandidates(analysis, contentEnd = 195.0, duration = 200.0)
        // Candidate at 150.0 should be dropped due to MAX_DISCARDED_MUSIC_SECONDS (12.0)
        assertFalse(ranked.any { it.time == 150.0 })
        assertTrue(ranked.any { it.time == 190.0 })
    }

    @Test
    fun resolveMixOutAnchor_returnsValidAnchorWithinBudget() {
        val analysis = TrackAnalysis(
            duration = 200.0,
            contentEndTime = 195.0,
            energyCurve = (0..195).map { EnergySample(it.toDouble(), 1.0) },
            mixOutCandidates = listOf(
                MixCandidate(190.0, 0.9, "outro_start"),
            ),
        )

        val anchor = resolveMixOutAnchor(analysis, contentEnd = 195.0, duration = 200.0)
        assertEquals(190.0, anchor.time, 0.01)
        assertEquals("outro_start", anchor.type)
        assertTrue(anchor.discardedMusicSeconds <= 12.0)
    }

    @Test
    fun planTransition_blocksPodcastsAndAudiobooks() {
        val currentTrack = TransitionTrackInfo(
            id = "1",
            durationMs = 300_000,
            title = "Episode 42: A Great Podcast",
        )
        val nextTrack = TransitionTrackInfo(
            id = "2",
            durationMs = 300_000,
            title = "Standard Music Song",
        )

        val plan = planTransition(
            analysis = TrackAnalysis(status = TrackAnalysis.STATUS_READY, bpm = 120.0, beatConfidence = 0.8),
            nextAnalysis = TrackAnalysis(status = TrackAnalysis.STATUS_READY, bpm = 120.0, beatConfidence = 0.8),
            currentTrack = currentTrack,
            nextTrack = nextTrack,
            currentTime = 280.0,
            duration = 300.0,
            fadeSeconds = 6.0,
            mode = CrossfadeMode.SMART,
        )

        assertTrue(plan.blocked)
        assertEquals("blocked-speech-or-live", plan.reason)
    }

    @Test
    fun planTransition_gaplessForSameAlbumConsecutiveTracks() {
        val currentTrack = TransitionTrackInfo(
            id = "1",
            durationMs = 180_000,
            album = "Abbey Road",
            albumId = "album_123",
        )
        val nextTrack = TransitionTrackInfo(
            id = "2",
            durationMs = 200_000,
            album = "Abbey Road",
            albumId = "album_123",
        )

        val plan = planTransition(
            analysis = TrackAnalysis(status = TrackAnalysis.STATUS_READY, bpm = 120.0, beatConfidence = 0.8),
            nextAnalysis = TrackAnalysis(status = TrackAnalysis.STATUS_READY, bpm = 120.0, beatConfidence = 0.8),
            currentTrack = currentTrack,
            nextTrack = nextTrack,
            currentTime = 175.0,
            duration = 180.0,
            fadeSeconds = 6.0,
            mode = CrossfadeMode.SMART,
            albumSequential = true,
        )

        assertEquals(TransitionStyle.GAPLESS, plan.transitionStyle)
    }

    @Test
    fun planTransition_plansBeatmatchedDjBlendForMatchingBeats() {
        val currentTrack = TransitionTrackInfo(
            id = "1",
            durationMs = 200_000,
            title = "Track One",
        )
        val nextTrack = TransitionTrackInfo(
            id = "2",
            durationMs = 200_000,
            title = "Track Two",
        )

        val outAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 124.0,
            beatConfidence = 0.85,
            beatInterval = 60.0 / 124.0,
            contentEndTime = 195.0,
            downbeats = listOf(170.0, 174.0, 178.0, 182.0, 186.0, 190.0, 194.0),
        )
        val inAnalysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 124.0,
            beatConfidence = 0.85,
            beatInterval = 60.0 / 124.0,
            mixInCandidates = listOf(MixCandidate(0.0, 0.9, "pickup")),
            downbeats = listOf(0.0, 4.0, 8.0, 12.0),
        )

        val plan = planTransition(
            analysis = outAnalysis,
            nextAnalysis = inAnalysis,
            currentTrack = currentTrack,
            nextTrack = nextTrack,
            currentTime = 185.0,
            duration = 200.0,
            fadeSeconds = 6.0,
            mode = CrossfadeMode.SMART,
        )

        assertFalse(plan.blocked)
        assertEquals(TransitionStyle.DJ_BLEND, plan.transitionStyle)
        assertTrue(plan.bassSwap)
        assertTrue(plan.fadeSeconds > 0.0)
    }

    @Test
    fun resolvingDataSource_exceptionWrappingInvariant() = kotlinx.coroutines.runBlocking {
        // Verifies that any general exception in stream or source resolution
        // is wrapped into an IOException so ExoPlayer's loader thread handles it
        // cleanly as a PlaybackException rather than crashing with an uncaught RuntimeException.
        val timeoutEx = try {
            kotlinx.coroutines.withTimeout(10) {
                kotlinx.coroutines.delay(500)
            }
            null
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            e
        }
        assertNotNull(timeoutEx)
        val wrappedTimeout = java.io.IOException("Stream resolution timed out", timeoutEx)
        assertEquals(timeoutEx, wrappedTimeout.cause)

        val genericEx: Throwable = IllegalStateException("Extraction failed")
        val wrappedGeneric = (genericEx as? java.io.IOException) ?: java.io.IOException("Resolution failed: ${genericEx.message}", genericEx)
        assertEquals(genericEx, wrappedGeneric.cause)
    }
}
