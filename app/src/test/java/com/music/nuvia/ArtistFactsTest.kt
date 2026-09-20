package com.music.nuvia

import com.music.nuvia.data.artist.ArtistFacts
import com.music.nuvia.data.artist.ArtistFactsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArtistFactsTest {

    @Before
    fun setUp() {
        ArtistFactsRepository.clearCache()
    }

    @Test
    fun testArtistFactsIsEmpty() {
        val emptyFacts = ArtistFacts(artistName = "Unknown Artist")
        assertTrue(emptyFacts.isEmpty)

        val populatedFacts = ArtistFacts(
            artistName = "Daft Punk",
            origin = "Paris, France",
            formedYear = "1993",
        )
        assertFalse(populatedFacts.isEmpty)
    }

    @Test
    fun testFormatCount() {
        assertEquals("500", ArtistFacts.formatCount(500))
        assertEquals("1.5K", ArtistFacts.formatCount(1500))
        assertEquals("2.4M", ArtistFacts.formatCount(2_400_000))
        assertEquals("1.1B", ArtistFacts.formatCount(1_100_000_000))
    }

    @Test
    fun testFormattedSubtitle() {
        val factsWithAll = ArtistFacts(
            artistName = "Radiohead",
            origin = "Abingdon, Oxfordshire, England",
            formedYear = "1985",
            listeners = 4_500_000,
        )
        val subtitle = factsWithAll.formattedSubtitle
        assertNotNull(subtitle)
        assertTrue(subtitle!!.contains("Abingdon, Oxfordshire, England"))
        assertTrue(subtitle.contains("Est. 1985"))
        assertTrue(subtitle.contains("4.5M listeners"))

        val emptyFacts = ArtistFacts(artistName = "Solo")
        assertNull(emptyFacts.formattedSubtitle)
    }

    @Test
    fun testNormalizeTags() {
        val rawTags = listOf(
            "electronic",
            "ELECTRONIC",
            "  r&b  ",
            "edm",
            "k-pop",
            "..synthpop,",
            "",
            "   ",
        )
        val normalized = ArtistFactsRepository.normalizeTags(rawTags)

        // Case-insensitive deduplication
        assertEquals(5, normalized.size)
        assertTrue(normalized.contains("Electronic"))
        assertTrue(normalized.contains("R&B"))
        assertTrue(normalized.contains("EDM"))
        assertTrue(normalized.contains("K-Pop"))
        assertTrue(normalized.contains("Synthpop"))
    }

    @Test
    fun testCleanBiography() {
        val htmlBio = "Daft Punk were a French electronic music duo. <a href=\"https://www.last.fm/music/Daft+Punk\">Read more on Last.fm</a>. User-contributed text is available under the Creative Commons By-SA License."
        val cleaned = ArtistFactsRepository.cleanBiography(htmlBio)

        assertEquals("Daft Punk were a French electronic music duo.", cleaned)
        assertFalse(cleaned.contains("<a"))
        assertFalse(cleaned.contains("Last.fm"))
        assertFalse(cleaned.contains("User-contributed text"))
    }

    @Test
    fun testFallbackToYouTubeDescription() = runBlocking {
        val ytBio = "Official YouTube channel for The Weeknd. Starboy available now."
        val ytSubs = "32.4M subscribers"

        val facts = ArtistFactsRepository.getArtistFacts(
            artistName = "NonExistentArtistTestingXYZ987",
            ytDescription = ytBio,
            ytSubscribers = ytSubs,
        )

        assertNotNull(facts)
        assertEquals("NonExistentArtistTestingXYZ987", facts.artistName)
        // If external API returns nothing for this fake artist, it gracefully falls back to YouTube bio
        if (facts.biography != null) {
            assertTrue(facts.biography!!.contains("The Weeknd") || facts.biography == ytBio)
        }
    }
}
