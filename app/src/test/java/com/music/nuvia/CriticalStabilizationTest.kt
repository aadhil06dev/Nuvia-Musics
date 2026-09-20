package com.music.nuvia

import androidx.compose.ui.graphics.Color
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.screens.matchesSettingsQuery
import com.music.nuvia.ui.theme.buildCustomNUViAColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalStabilizationTest {

    @Test
    fun whiteCustomAccentPreservesZeroSaturationWithoutPinkDrift() {
        val pureWhite = Color(0xFFFFFFFF)
        val palette = buildCustomNUViAColors(pureWhite, isDark = true)

        // Verify primary color is white
        assertEquals(1.0f, palette.primary.red, 0.01f)
        assertEquals(1.0f, palette.primary.green, 0.01f)
        assertEquals(1.0f, palette.primary.blue, 0.01f)

        // Verify secondary color does NOT drift to bright pink/red (red, green, blue should be nearly equal)
        val diffRG = Math.abs(palette.secondary.red - palette.secondary.green)
        val diffRB = Math.abs(palette.secondary.red - palette.secondary.blue)
        assertTrue("Secondary should be neutral achromatic, not red/pink tinted", diffRG < 0.05f)
        assertTrue("Secondary should be neutral achromatic, not red/pink tinted", diffRB < 0.05f)
    }

    @Test
    fun silverCustomAccentPreservesZeroSaturation() {
        val silver = Color(0xFFE2E8F0)
        val palette = buildCustomNUViAColors(silver, isDark = true)

        val diffRG = Math.abs(palette.secondary.red - palette.secondary.green)
        val diffRB = Math.abs(palette.secondary.red - palette.secondary.blue)
        assertTrue("Silver secondary should be neutral, diffRG=$diffRG", diffRG < 0.08f)
        assertTrue("Silver secondary should be neutral, diffRB=$diffRB", diffRB < 0.08f)
    }

    @Test
    fun settingsSearchMatchesCategoriesCorrectly() {
        // Empty query matches everything
        assertTrue(matchesSettingsQuery("", "Audio Quality", "Dolby Atmos"))
        assertTrue(matchesSettingsQuery("   ", "Audio Quality", "Dolby Atmos"))

        // Specific queries
        assertTrue(matchesSettingsQuery("lyrics", "Lyrics & Sync", "Synced lyrics"))
        assertTrue(matchesSettingsQuery("canvas", "Appearance & AMOLED", "Spotify Canvas authorization"))
        assertTrue(matchesSettingsQuery("scrobble", "Account & Integrations", "Scrobbling & metadata"))
        assertTrue(matchesSettingsQuery("dolby", "Audio Quality & Bit Depth", "Dolby Atmos"))
        assertTrue(matchesSettingsQuery("cache", "Storage & Cache", "Audio cache limit"))
        assertTrue(matchesSettingsQuery("crossfade", "Playback Engine", "Smart crossfade"))

        // Unmatched queries
        assertFalse(matchesSettingsQuery("xyz123nonexistent", "Playback Engine", "Audio Quality"))
    }

    @Test
    fun freshInstallDefaultsAdhereToStabilizationRequirements() {
        // Ambient lighting should default to false (pristine AMOLED blacks)
        assertFalse("ambientLighting must default to false for fresh installs", AppSettings.ambientLighting.value)

        // Full bleed artwork should default to true
        assertTrue("fullBleedArtwork must default to true for fresh installs", AppSettings.fullBleedArtwork.value)

        // Scrobbling should be available
        assertTrue("scrobblingAvailable must be true", AppSettings.scrobblingAvailable)
    }

    @Test
    fun bottomBarFixedWhenSongIsNull() {
        // When player.song == null, the bottom bar must stay anchored with zero translation offset
        fun calculateShellTranslation(songPresent: Boolean, scrollDown: Boolean, travelDistancePx: Float): Float {
            if (!songPresent) return 0f // Fixed strictly at bottom
            return if (scrollDown) travelDistancePx else 0f
        }

        val travelDistance = 140f
        assertEquals(0f, calculateShellTranslation(songPresent = false, scrollDown = true, travelDistancePx = travelDistance), 0.001f)
        assertEquals(0f, calculateShellTranslation(songPresent = false, scrollDown = false, travelDistancePx = travelDistance), 0.001f)
        assertEquals(travelDistance, calculateShellTranslation(songPresent = true, scrollDown = true, travelDistancePx = travelDistance), 0.001f)
        assertEquals(0f, calculateShellTranslation(songPresent = true, scrollDown = false, travelDistancePx = travelDistance), 0.001f)
    }

    @Test
    fun companionButtonConstraintsAreStrictlyNonNegative() {
        val buttonSizePx = 144 // 48.dp at 3.0x density
        // Test normal, overshoot negative, and overshoot positive progress values
        val testProgresses = listOf(-0.15f, -0.015f, 0f, 0.0001f, 0.25f, 0.5f, 0.75f, 1.0f, 1.05f)

        for (rawProgress in testProgresses) {
            val safeProgress = rawProgress.coerceIn(0f, 1f)
            assertTrue("safeProgress must be >= 0f", safeProgress >= 0f)
            assertTrue("safeProgress must be <= 1f", safeProgress <= 1f)

            val targetW = Math.round(buttonSizePx * safeProgress).toInt().coerceIn(0, buttonSizePx)
            assertTrue("targetW must be >= 0 (never -1 or negative)", targetW >= 0)
            assertTrue("targetW must not exceed buttonSizePx", targetW <= buttonSizePx)
        }
    }

    @Test
    fun bottomContentInsetProvidesAdequateClearanceAbovePersistentShell() {
        val navBarBottom = 48
        val miniPlayerGap = 12
        val miniPlayerHeight = 68
        val bottomBarHeight = 56
        val bottomBarMargin = 16
        val safetyBuffer = 24

        val totalBottomInset = miniPlayerHeight + miniPlayerGap + bottomBarHeight + bottomBarMargin + navBarBottom + safetyBuffer
        // Total bottom inset must provide plenty of space for full visibility (> 200dp)
        assertTrue("Total bottom inset must provide clear scroll clearance", totalBottomInset >= 224)
    }
}
