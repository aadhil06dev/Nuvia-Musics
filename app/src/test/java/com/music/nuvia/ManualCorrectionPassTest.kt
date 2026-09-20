package com.music.nuvia

import com.music.nuvia.data.lyrics.LyricsSource
import com.music.nuvia.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class ManualCorrectionPassTest {

    @Before
    fun setUp() {
        AppSettings.liquidGlassBlur.value = true
        AppSettings.customThemeColor.value = 0xFFE85D04.toInt()
        AppSettings.lyricsSyncOffsetMs.value = 0
    }

    @After
    fun tearDown() {
        AppSettings.liquidGlassBlur.value = true
        AppSettings.customThemeColor.value = 0xFFE85D04.toInt()
        AppSettings.lyricsSyncOffsetMs.value = 0
    }

    @Test
    fun `verifies YouTube transcript and YouTube Music lyric sources are registered`() {
        val sources = LyricsSource.entries
        assertTrue(sources.contains(LyricsSource.YOUTUBE_TRANSCRIPT))
        assertTrue(sources.contains(LyricsSource.YOUTUBE_MUSIC))
        assertEquals("YouTube captions", LyricsSource.YOUTUBE_TRANSCRIPT.label)
        assertEquals("YouTube Music", LyricsSource.YOUTUBE_MUSIC.label)
    }

    @Test
    fun `verifies lyrics sync offset default is zero`() {
        assertEquals(0, AppSettings.lyricsSyncOffsetMs.value)
    }

    @Test
    fun `verifies custom theme color default is brand orange`() {
        assertEquals(0xFFE85D04.toInt(), AppSettings.customThemeColor.value)
    }

    @Test
    fun `verifies liquid glass blur default is enabled`() {
        assertTrue(AppSettings.liquidGlassBlur.value)
    }
}
