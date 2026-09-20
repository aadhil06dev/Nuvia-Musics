package com.music.nuvia

import com.music.nuvia.data.backup.BackupSummary
import com.music.nuvia.data.backup.BackupValidator
import com.music.nuvia.data.backup.RestoreOptions
import com.music.nuvia.data.backup.RestoreStrategy
import com.music.nuvia.data.backup.ValidationResult
import com.music.nuvia.data.backup.model.NuviaBackup
import com.music.nuvia.data.backup.model.PlaylistBackup
import com.music.nuvia.data.backup.model.SettingsBackup
import com.music.nuvia.data.backup.model.TrackMetadataBackup
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.AudioQuality
import com.music.nuvia.data.settings.DownloadQuality
import com.music.nuvia.data.settings.SearchHistory
import com.music.nuvia.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupAndRestoreTest {

    @Before
    fun setUp() {
        AppSettings.pinnedPlaylists.value = emptyList()
        SearchHistory.clear()
    }

    @Test
    fun `serialization produces valid versioned JSON without secrets`() {
        val backup = NuviaBackup(
            formatVersion = 1,
            app = "NUViA",
            appVersion = "1.0",
            exportedAt = 1700000000000L,
            settings = SettingsBackup(
                audioQualityWifi = "HIGH",
                themeMode = "DARK",
                crossfadeSeconds = 6,
                losslessAudio = true,
            ),
            playlists = listOf(
                PlaylistBackup(
                    id = "pl_synthwave",
                    title = "Synthwave Nights",
                    subtitle = "Retro vibes",
                    isPinned = true,
                    tracks = listOf(
                        TrackMetadataBackup(
                            videoId = "v_123",
                            title = "Resonance",
                            artist = "HOME",
                            albumName = "Odyssey",
                            durationText = "3:32",
                        )
                    )
                )
            ),
            pinnedPlaylists = listOf("pl_synthwave"),
            searchHistory = listOf("Daft Punk", "Lorn"),
        )

        val jsonString = BackupValidator.json.encodeToString(NuviaBackup.serializer(), backup)

        assertTrue(jsonString.contains("\"app\": \"NUViA\""))
        assertTrue(jsonString.contains("\"formatVersion\": 1"))
        assertTrue(jsonString.contains("Synthwave Nights"))
        assertTrue(jsonString.contains("Resonance"))
        assertTrue(jsonString.contains("Daft Punk"))

        // Strictly verify NO auth tokens, cookies, or secrets exist
        assertFalse(jsonString.contains("sessionKey"))
        assertFalse(jsonString.contains("apiKey"))
        assertFalse(jsonString.contains("token"))
        assertFalse(jsonString.contains("cookie"))
    }

    @Test
    fun `deserialization accurately reconstructs backup model`() {
        val jsonString = """
        {
            "formatVersion": 1,
            "app": "NUViA",
            "appVersion": "1.2.0",
            "exportedAt": 1700000000000,
            "settings": {
                "audioQualityWifi": "HIGH",
                "themeMode": "DARK",
                "crossfadeSeconds": 5,
                "losslessAudio": true
            },
            "playlists": [
                {
                    "id": "pl_ambient",
                    "title": "Ambient Space",
                    "isPinned": true,
                    "tracks": [
                        {
                            "videoId": "vid_ambient_1",
                            "title": "Weightless",
                            "artist": "Marconi Union"
                        }
                    ]
                }
            ],
            "pinnedPlaylists": ["pl_ambient"],
            "searchHistory": ["Ambient", "Chill"]
        }
        """.trimIndent()

        val result = BackupValidator.validate(jsonString)
        assertTrue("Expected Success validation result", result is ValidationResult.Success)

        val success = result as ValidationResult.Success
        assertEquals(1, success.backup.formatVersion)
        assertEquals("NUViA", success.backup.app)
        assertEquals("1.2.0", success.backup.appVersion)
        assertEquals(1, success.backup.playlists.size)
        assertEquals("Ambient Space", success.backup.playlists[0].title)
        assertEquals(1, success.backup.playlists[0].tracks.size)
        assertEquals("Weightless", success.backup.playlists[0].tracks[0].title)
        assertEquals(listOf("pl_ambient"), success.backup.pinnedPlaylists)
        assertEquals(listOf("Ambient", "Chill"), success.backup.searchHistory)
        assertEquals(1, success.summary.playlistCount)
        assertEquals(1, success.summary.totalTrackCount)
        assertEquals(1, success.summary.pinnedPlaylistCount)
        assertEquals(2, success.summary.searchHistoryCount)
    }

    @Test
    fun `corrupted and malformed JSON returns descriptive error without crashing`() {
        // Truncated / malformed JSON
        val malformedJson = """{ "formatVersion": 1, "app": "NUViA", "playli """
        val result1 = BackupValidator.validate(malformedJson)
        assertTrue(result1 is ValidationResult.Error)
        val err1 = result1 as ValidationResult.Error
        assertTrue(err1.message.contains("Malformed or corrupted JSON", ignoreCase = true))

        // Empty file
        val emptyResult = BackupValidator.validate("   ")
        assertTrue(emptyResult is ValidationResult.Error)
        val errEmpty = emptyResult as ValidationResult.Error
        assertTrue(errEmpty.message.contains("empty", ignoreCase = true))

        // Random non-JSON binary-like text
        val randomTextResult = BackupValidator.validate("<<BINARY_GARBAGE_BYTES>>")
        assertTrue(randomTextResult is ValidationResult.Error)
    }

    @Test
    fun `unknown fields from future versions are safely ignored`() {
        val futureJson = """
        {
            "formatVersion": 1,
            "app": "NUViA",
            "appVersion": "2.0.0",
            "exportedAt": 1700000000000,
            "futureSuperAiSetting": {
                "quantumEqualizer": true,
                "hologramMode": "active"
            },
            "newThemeAccent": "#FF00FF",
            "playlists": [],
            "pinnedPlaylists": []
        }
        """.trimIndent()

        val result = BackupValidator.validate(futureJson)
        assertTrue("Future fields must be safely tolerated", result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(1, success.backup.formatVersion)
        assertEquals("NUViA", success.backup.app)
    }

    @Test
    fun `version handling rejects future format versions and incompatible apps`() {
        // Unsupported future format version (formatVersion = 2 when current is 1)
        val futureVersionJson = """
        {
            "formatVersion": 99,
            "app": "NUViA",
            "appVersion": "99.0",
            "exportedAt": 1700000000000
        }
        """.trimIndent()

        val versionResult = BackupValidator.validate(futureVersionJson)
        assertTrue(versionResult is ValidationResult.Error)
        val versionErr = versionResult as ValidationResult.Error
        assertTrue(versionErr.message.contains("newer version", ignoreCase = true))

        // Incompatible app identifier
        val wrongAppJson = """
        {
            "formatVersion": 1,
            "app": "SomeOtherMusicApp",
            "appVersion": "1.0",
            "exportedAt": 1700000000000
        }
        """.trimIndent()

        val appResult = BackupValidator.validate(wrongAppJson)
        assertTrue(appResult is ValidationResult.Error)
        val appErr = appResult as ValidationResult.Error
        assertTrue(appErr.message.contains("Incompatible backup", ignoreCase = true))
    }

    @Test
    fun `duplicate handling identifies internal duplicates and conflicts with existing library`() {
        val existingIds = setOf("pl_existing_1", "pl_chill")
        val existingTitles = setOf("My Favorites", "Chill Beats")

        val backupJson = """
        {
            "formatVersion": 1,
            "app": "NUViA",
            "appVersion": "1.0",
            "exportedAt": 1700000000000,
            "playlists": [
                {
                    "id": "pl_new",
                    "title": "Workout Energetic",
                    "tracks": []
                },
                {
                    "id": "pl_existing_1",
                    "title": "Something Else",
                    "tracks": []
                },
                {
                    "id": "pl_different_id",
                    "title": "Chill Beats",
                    "tracks": []
                },
                {
                    "id": "pl_new",
                    "title": "Workout Duplicate",
                    "tracks": []
                }
            ]
        }
        """.trimIndent()

        val result = BackupValidator.validate(backupJson, existingIds, existingTitles)
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success

        // Should detect:
        // 1. pl_existing_1 (ID conflict with library)
        // 2. Chill Beats (Title conflict with library)
        // 3. pl_new second occurrence (Internal duplicate within backup)
        assertTrue(success.duplicatePlaylists.size >= 3)
        assertTrue(success.duplicatePlaylists.any { it.id == "pl_existing_1" })
        assertTrue(success.duplicatePlaylists.any { it.title == "Chill Beats" })
        assertTrue(success.duplicatePlaylists.any { it.reason.contains("Duplicate playlist ID") })
    }

    @Test
    fun `pinned playlist restoration respects limits and merges correctly`() {
        AppSettings.pinnedPlaylists.value = listOf("pin_1", "pin_2")

        // 1. Merge strategy with new pins
        val backupPins = listOf("pin_2", "pin_3", "pin_4", "pin_5", "pin_6", "pin_7")
        val cleanPins = backupPins.filter { it.isNotBlank() }.distinct()

        val merged = (AppSettings.pinnedPlaylists.value + cleanPins)
            .distinct()
            .take(AppSettings.MAX_PINNED_PLAYLISTS)

        AppSettings.setPinnedPlaylists(merged)

        // Should enforce MAX_PINNED_PLAYLISTS = 5
        assertEquals(5, AppSettings.pinnedPlaylists.value.size)
        assertEquals(listOf("pin_1", "pin_2", "pin_3", "pin_4", "pin_5"), AppSettings.pinnedPlaylists.value)

        // 2. Overwrite strategy
        val overwritePins = listOf("pin_A", "pin_B").take(AppSettings.MAX_PINNED_PLAYLISTS)
        AppSettings.setPinnedPlaylists(overwritePins)
        assertEquals(listOf("pin_A", "pin_B"), AppSettings.pinnedPlaylists.value)
    }

    @Test
    fun `settings restoration applies valid values and safely falls back on unknown values`() {
        val s = SettingsBackup(
            audioQualityWifi = "HIGH",
            audioQualityCellular = "LOW",
            downloadQuality = "LOSSLESS",
            themeMode = "DARK",
            crossfadeSeconds = 8,
            losslessAudio = true,
            syncedLyrics = true,
        )

        // Apply audio quality
        s.audioQualityWifi?.let { name ->
            AudioQuality.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.let { AppSettings.audioQualityWifi.value = it }
        }
        assertEquals(AudioQuality.HIGH, AppSettings.audioQualityWifi.value)

        // Apply theme mode
        s.themeMode?.let { name ->
            ThemeMode.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.let { AppSettings.themeMode.value = it }
        }
        assertEquals(ThemeMode.DARK, AppSettings.themeMode.value)

        // Apply crossfade and lossless
        s.crossfadeSeconds?.let { AppSettings.crossfadeSeconds.value = it }
        assertEquals(8, AppSettings.crossfadeSeconds.value)

        s.losslessAudio?.let { AppSettings.losslessAudio.value = it }
        assertTrue(AppSettings.losslessAudio.value)

        // Unknown enum value must safely not crash or corrupt state
        val unknownQuality = "SUPER_ULTRA_EXTREME_QUALITY"
        val fallbackQuality = AudioQuality.entries.firstOrNull { it.name.equals(unknownQuality, ignoreCase = true) }
        // Should be null, so AppSettings is untouched
        assertEquals(null, fallbackQuality)
        assertEquals(AudioQuality.HIGH, AppSettings.audioQualityWifi.value)
    }

    @Test
    fun `search history restoration merges and deduplicates correctly`() {
        SearchHistory.record("Queen")
        SearchHistory.record("Pink Floyd")

        val backupQueries = listOf("pink floyd", "The Beatles", "Led Zeppelin")
        SearchHistory.restore(backupQueries, overwrite = false)

        val recent = SearchHistory.recent.value
        assertEquals(4, recent.size)
        assertTrue(recent.contains("Queen"))
        assertTrue(recent.any { it.equals("Pink Floyd", ignoreCase = true) })
        assertTrue(recent.contains("The Beatles"))
        assertTrue(recent.contains("Led Zeppelin"))

        // Overwrite
        SearchHistory.restore(listOf("Radiohead", "Nirvana"), overwrite = true)
        assertEquals(listOf("Radiohead", "Nirvana"), SearchHistory.recent.value)
    }
}
