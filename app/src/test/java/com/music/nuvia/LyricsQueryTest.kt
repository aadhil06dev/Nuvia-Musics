package com.music.nuvia

import com.music.nuvia.data.lyrics.LyricsQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsQueryTest {

    @Test
    fun stripsVideoTagsFromTitle() {
        assertEquals(
            "Starboy",
            LyricsQuery.forLyricsSearch("Starboy (Official Music Video)"),
        )
        assertEquals(
            "Blinding Lights",
            LyricsQuery.forLyricsSearch("Blinding Lights [Official Audio]"),
        )
        assertEquals(
            "Shape of You",
            LyricsQuery.forLyricsSearch("Shape of You (Lyric Video)"),
        )
        assertEquals(
            "Levitating",
            LyricsQuery.forLyricsSearch("Levitating (feat. DaBaby) [Official Video]"),
        )
        assertEquals(
            "golden hour",
            LyricsQuery.forLyricsSearch("golden hour"),
        )
    }

    @Test
    fun preservesVersionModifiersInTitle() {
        assertEquals(
            "Save Your Tears (Remix)",
            LyricsQuery.forLyricsSearch("Save Your Tears (Remix) (Official Video)"),
        )
        assertEquals(
            "Hotel California (Live)",
            LyricsQuery.forLyricsSearch("Hotel California (Live) [Official Audio]"),
        )
        assertEquals(
            "Layla (Acoustic)",
            LyricsQuery.forLyricsSearch("Layla (Acoustic) (Visualizer)"),
        )
        assertEquals(
            "Dracula (JENNIE Remix)",
            LyricsQuery.forLyricsSearch("Dracula (JENNIE Remix)"),
        )
        assertEquals(
            "Bohemian Rhapsody (Remastered 2011)",
            LyricsQuery.forLyricsSearch("Bohemian Rhapsody (Remastered 2011)"),
        )
    }

    @Test
    fun handlesPackagingOnlyTitle() {
        // A title that is nothing but packaging keeps what it had rather than blank
        assertEquals("(Official Video)", LyricsQuery.forLyricsSearch("(Official Video)"))
    }

    @Test
    fun dropsTopicSuffixFromArtist() {
        assertEquals(
            "Dua Lipa",
            LyricsQuery.artistForLyricsSearch("Dua Lipa - Topic"),
        )
        assertEquals(
            "Tame Impala",
            LyricsQuery.artistForLyricsSearch("Tame Impala - Topic"),
        )
        assertEquals(
            "The Weeknd",
            LyricsQuery.artistForLyricsSearch("The Weeknd"),
        )
    }
}

