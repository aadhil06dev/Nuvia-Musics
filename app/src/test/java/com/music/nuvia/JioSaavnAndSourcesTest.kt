package com.music.nuvia

import com.music.nuvia.data.jiosaavn.RawMoreInfo
import com.music.nuvia.data.jiosaavn.RawSongItem
import com.music.nuvia.data.sources.SourceKind
import com.music.nuvia.data.sources.SourceResolver
import com.music.nuvia.data.sources.StreamFormat
import com.music.nuvia.playback.smart.LocalAudioSource
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnAndSourcesTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ---- SourceKind ordering and capabilities -------------------------------

    @Test
    fun `source kinds are ordered correctly`() {
        assertTrue(SourceKind.CUSTOM_MODULE.ordinal < SourceKind.MODULE.ordinal)
        assertTrue(SourceKind.MODULE.ordinal < SourceKind.JIOSAAVN.ordinal)
        assertTrue(SourceKind.JIOSAAVN.ordinal < SourceKind.YOUTUBE.ordinal)
    }

    @Test
    fun `jiosaavn source kind properties are configured as expected`() {
        val jiosaavn = SourceKind.JIOSAAVN
        assertFalse(jiosaavn.needsServer)
        assertFalse(jiosaavn.canServeLossless)
        assertTrue(jiosaavn.worthPrefetching)
        assertEquals("JioSaavn", jiosaavn.label)
    }

    // ---- SourceResolver format comparison rules -----------------------------

    @Test
    fun `isBetter prefers lossless over lossy regardless of bitrate`() {
        val lossless = StreamFormat(codec = "flac", kbps = 900)
        val highLossy = StreamFormat(codec = "aac", kbps = 320)

        assertTrue(SourceResolver.isBetter(lossless, highLossy))
        assertFalse(SourceResolver.isBetter(highLossy, lossless))
    }

    @Test
    fun `isBetter prefers higher bitrate when both are lossy`() {
        val aac320 = StreamFormat(codec = "aac", kbps = 320)
        val opus160 = StreamFormat(codec = "opus", kbps = 160)

        assertTrue(SourceResolver.isBetter(aac320, opus160))
        assertFalse(SourceResolver.isBetter(opus160, aac320))
    }

    @Test
    fun `isBetter treats null current as beaten by any candidate`() {
        val anyFormat = StreamFormat(codec = "opus", kbps = 160)
        assertTrue(SourceResolver.isBetter(anyFormat, null))
    }

    @Test
    fun `beatsYouTubeAac requires exceeding YouTube AAC threshold`() {
        assertTrue(SourceResolver.beatsYouTubeAac(StreamFormat(codec = "aac", kbps = 320)))
        assertFalse(SourceResolver.beatsYouTubeAac(StreamFormat(codec = "aac", kbps = 256)))
        assertFalse(SourceResolver.beatsYouTubeAac(StreamFormat(codec = "opus", kbps = 160)))
        assertFalse(SourceResolver.beatsYouTubeAac(StreamFormat(codec = "aac", kbps = null)))
    }

    @Test
    fun `sameRecordingAs enforces strict duration drift window`() {
        assertTrue(SourceResolver.sameRecordingAs(200, 200))
        assertTrue(SourceResolver.sameRecordingAs(202, 200))
        assertTrue(SourceResolver.sameRecordingAs(198, 200))

        assertFalse(SourceResolver.sameRecordingAs(203, 200))
        assertFalse(SourceResolver.sameRecordingAs(195, 200))
        assertFalse(SourceResolver.sameRecordingAs(null, 200))
        assertFalse(SourceResolver.sameRecordingAs(200, null))
    }

    // ---- LocalAudioSource detection -----------------------------------------

    @Test
    fun `local audio source correctly identifies local schemes`() {
        assertTrue(LocalAudioSource.isLocalScheme("file"))
        assertTrue(LocalAudioSource.isLocalScheme("content"))
        assertTrue(LocalAudioSource.isLocalScheme("FILE"))
        assertTrue(LocalAudioSource.isLocalScheme("CONTENT"))

        assertFalse(LocalAudioSource.isLocalScheme("https"))
        assertFalse(LocalAudioSource.isLocalScheme("http"))
        assertFalse(LocalAudioSource.isLocalScheme("bitchord"))
        assertFalse(LocalAudioSource.isLocalScheme(null))
        assertFalse(LocalAudioSource.isLocalScheme(""))
    }

    // ---- JioSaavn data model parsing ----------------------------------------

    @Test
    fun `decodes raw song item and detects 320kbps support`() {
        val rawJson = """
            {
                "id": "song_123",
                "title": "Chaleya",
                "image": "https://c.saavncdn.com/123/Chaleya-500x500.jpg",
                "more_info": {
                    "album": "Jawan",
                    "duration": "200",
                    "320kbps": "true",
                    "encrypted_media_url": "dummy_encrypted_url"
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<RawSongItem>(rawJson)
        assertEquals("song_123", parsed.id)
        assertEquals("Chaleya", parsed.title)
        assertTrue(parsed.moreInfo.supports320)
    }

    @Test
    fun `more info handles false or missing 320kbps flag`() {
        val raw1 = RawMoreInfo(has320 = "false")
        assertFalse(raw1.supports320)

        val raw2 = RawMoreInfo(has320 = "")
        assertFalse(raw2.supports320)
    }
}
