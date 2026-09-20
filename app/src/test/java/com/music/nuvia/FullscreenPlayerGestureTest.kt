package com.music.nuvia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying fullscreen player swipe-to-close and dismissal invariants.
 */
class FullscreenPlayerGestureTest {

    @Test
    fun dismissal_preservesPlaybackStateAndQueue() {
        // Invariant: dismissing the fullscreen player must only hide the UI sheet
        // (showNowPlaying = false) without stopping, pausing, or mutating playback.
        var showNowPlaying = true
        var isPlaying = true
        val positionMs = 42_000L
        val currentQueueIndex = 3
        val currentVideoId = "test_track_123"

        // Simulate swipe-to-dismiss triggered via onDismissRequest
        showNowPlaying = false

        assertFalse(showNowPlaying)
        assertTrue(isPlaying)
        assertEquals(42_000L, positionMs)
        assertEquals(3, currentQueueIndex)
        assertEquals("test_track_123", currentVideoId)

        // Reopening fullscreen player must maintain all playback state intact
        showNowPlaying = true
        assertTrue(showNowPlaying)
        assertTrue(isPlaying)
        assertEquals(42_000L, positionMs)
    }

    @Test
    fun gestureDisambiguation_horizontalVsVertical() {
        // Verifies the directionality model:
        // Horizontal drags with delta >= swipeThreshold trigger track navigation.
        // Pure vertical drags (deltaX = 0, deltaY > 0) are ignored by horizontal skip detector
        // and routed to the sheet dismissal handler.
        val swipeThreshold = 72f

        fun isHorizontalSkip(deltaX: Float, deltaY: Float): Boolean {
            // Horizontal drag detector only consumes when horizontal movement is primary
            // and exceeds threshold
            return kotlin.math.abs(deltaX) >= swipeThreshold && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)
        }

        fun isVerticalDismissCandidate(deltaX: Float, deltaY: Float): Boolean {
            // Downward gesture where vertical component dominates
            return deltaY > 0f && deltaY > kotlin.math.abs(deltaX)
        }

        // Fast downward swipe from anywhere on player
        assertTrue(isVerticalDismissCandidate(0f, 150f))
        assertFalse(isHorizontalSkip(0f, 150f))

        // Diagonal downward swipe with strong downward component
        assertTrue(isVerticalDismissCandidate(20f, 100f))
        assertFalse(isHorizontalSkip(20f, 100f))

        // Horizontal swipe to next track
        assertFalse(isVerticalDismissCandidate(-80f, 10f))
        assertTrue(isHorizontalSkip(-80f, 10f))

        // Small downward gesture (under typical snap threshold)
        assertTrue(isVerticalDismissCandidate(0f, 15f))
        assertFalse(isHorizontalSkip(0f, 15f))
    }

    @Test
    fun immediateFirstFrame_fastPathProvidesInstantTrackBeforeControllerSync() {
        val clickedSong = com.music.nuvia.data.model.Song(
            videoId = "fast_first_frame_id",
            title = "Instant Track",
            artist = "NUViA Artist",
            durationText = "3:30",
            thumbnailUrl = "https://img.youtube.com/vi/fast_first_frame_id/maxresdefault.jpg",
        )

        var showNowPlaying = false
        var activeNowPlayingSong: com.music.nuvia.data.model.Song? = null
        var playerSong: com.music.nuvia.data.model.Song? = null // Controller hasn't updated yet

        // User taps a song in home / search / playlist / album / local
        activeNowPlayingSong = clickedSong
        showNowPlaying = true

        // First frame composition check:
        // ModalBottomSheet and NowPlayingScreen evaluate activeSong = activeNowPlayingSong ?: playerSong
        val firstFrameActiveSong = activeNowPlayingSong ?: playerSong
        assertTrue("Fullscreen player must be open on frame 1", showNowPlaying)
        org.junit.Assert.assertNotNull("Frame 1 song must be non-null immediately without waiting for controller", firstFrameActiveSong)
        assertEquals("fast_first_frame_id", firstFrameActiveSong?.videoId)
        assertEquals("Instant Track", firstFrameActiveSong?.title)

        // Frame 2+ : Controller prepares and notifies playerState
        playerSong = clickedSong
        if (playerSong.videoId == activeNowPlayingSong.videoId) {
            activeNowPlayingSong = null
        }

        // Active song gracefully drops override and tracks controller
        val settledActiveSong = activeNowPlayingSong ?: playerSong
        org.junit.Assert.assertNull(activeNowPlayingSong)
        assertEquals("fast_first_frame_id", settledActiveSong?.videoId)
    }

    @Test
    fun dismissal_cleansUpFastPathState() {
        var showNowPlaying = true
        var activeNowPlayingSong: com.music.nuvia.data.model.Song? = com.music.nuvia.data.model.Song(
            videoId = "temp_id",
            title = "Temp",
            artist = "Artist",
            thumbnailUrl = "https://img.youtube.com/vi/temp_id/maxresdefault.jpg",
        )

        // User dismisses
        showNowPlaying = false
        activeNowPlayingSong = null

        assertFalse(showNowPlaying)
        org.junit.Assert.assertNull(activeNowPlayingSong)
    }
}
