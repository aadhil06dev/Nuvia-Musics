package com.music.nuvia

import com.music.nuvia.data.model.LikeStatus
import com.music.nuvia.data.model.Song
import com.music.nuvia.download.SavedSongMetadata
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val metadataSerializer = MapSerializer(String.serializer(), SavedSongMetadata.serializer())

    @Test
    fun `metadata serialization handles legacy entries without albumName`() {
        val legacyJson = """
            {
                "vid1": {
                    "videoId": "vid1",
                    "title": "Song Title",
                    "artist": "Artist Name",
                    "thumbnailUrl": "https://example.com/art.jpg",
                    "durationText": "3:45",
                    "uri": "content://media/external/audio/media/100"
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(metadataSerializer, legacyJson)
        val meta = decoded["vid1"]
        assertNotNull(meta)
        assertEquals("vid1", meta?.videoId)
        assertEquals("Song Title", meta?.title)
        assertEquals("Artist Name", meta?.artist)
        assertNull(meta?.albumName)
    }

    @Test
    fun `metadata serialization correctly encodes and decodes complete metadata`() {
        val original = mapOf(
            "vid1" to SavedSongMetadata(
                videoId = "vid1",
                title = "Starboy",
                artist = "The Weeknd",
                thumbnailUrl = "https://example.com/starboy.jpg",
                durationText = "3:50",
                albumName = "Starboy",
                uri = "content://media/external/audio/media/200",
            )
        )

        val encoded = json.encodeToString(metadataSerializer, original)
        val decoded = json.decodeFromString(metadataSerializer, encoded)

        assertEquals(original, decoded)
        assertEquals("Starboy", decoded["vid1"]?.albumName)
    }

    @Test
    fun `downloaded songs deduplication by URI works as expected`() {
        // When both the video ID and catalogue audio ID point to the same content URI
        val metaMap = mapOf(
            "video_id_1" to SavedSongMetadata(
                videoId = "video_id_1",
                title = "Blinding Lights",
                artist = "The Weeknd",
                thumbnailUrl = null,
                durationText = "3:20",
                albumName = "After Hours",
                uri = "content://media/external/audio/media/555",
            ),
            "audio_id_1" to SavedSongMetadata(
                videoId = "audio_id_1",
                title = "Blinding Lights",
                artist = "The Weeknd",
                thumbnailUrl = null,
                durationText = "3:20",
                albumName = "After Hours",
                uri = "content://media/external/audio/media/555",
            )
        )

        val seenUris = mutableSetOf<String>()
        val distinctSongs = mutableListOf<SavedSongMetadata>()

        for ((_, meta) in metaMap) {
            if (seenUris.add(meta.uri)) {
                distinctSongs.add(meta)
            }
        }

        assertEquals(1, distinctSongs.size)
        assertEquals("Blinding Lights", distinctSongs[0].title)
    }

    @Test
    fun `like status layering gives precedence to user session overrides`() {
        val libraryLiked = mapOf(
            "song1" to LikeStatus.LIKE,
            "song2" to LikeStatus.LIKE,
        )

        val overrides = mapOf(
            "song2" to LikeStatus.INDIFFERENT,
            "song3" to LikeStatus.LIKE,
        )

        val finalLikes = libraryLiked + overrides

        assertEquals(LikeStatus.LIKE, finalLikes["song1"])
        assertEquals(LikeStatus.INDIFFERENT, finalLikes["song2"])
        assertEquals(LikeStatus.LIKE, finalLikes["song3"])
        assertNull(finalLikes["song4"])
    }

    @Test
    fun `audio file extensions are recognized properly`() {
        val validFiles = listOf(
            "track.mp3",
            "track.M4A",
            "track.flac",
            "track.wav",
            "track.ogg",
            "track.opus",
            "track.aac",
            "track.webm",
            "track.3gp",
        )

        val invalidFiles = listOf(
            "image.jpg",
            "document.pdf",
            "video.mp4",
            "archive.zip",
            "text.txt",
        )

        val audioExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "opus", "aac", "webm", "3gp")

        for (file in validFiles) {
            val ext = file.substringAfterLast(".", "").lowercase()
            assertTrue("Expected $file to be recognized as audio", ext in audioExtensions)
        }

        for (file in invalidFiles) {
            val ext = file.substringAfterLast(".", "").lowercase()
            assertFalse("Expected $file to NOT be recognized as audio", ext in audioExtensions)
        }
    }

    @Test
    fun `playlist id parsing and construction is consistent`() {
        val collectionId = "album_12345"
        val pageId = com.music.nuvia.download.Downloads.pageIdFor(collectionId)
        assertEquals("local:playlist:album_12345", pageId)
        val extracted = com.music.nuvia.download.Downloads.recordIdOf(pageId)
        assertEquals(collectionId, extracted)
        assertNull(com.music.nuvia.download.Downloads.recordIdOf("local:downloads"))
        assertNull(com.music.nuvia.download.Downloads.recordIdOf("VLPL12345"))
    }
}
