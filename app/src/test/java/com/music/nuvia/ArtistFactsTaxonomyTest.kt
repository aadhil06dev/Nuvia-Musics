package com.music.nuvia

import com.music.nuvia.data.stats.ArtistFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistFactsTaxonomyTest {

    @Test
    fun canonicalNormalizesStandardGenres() {
        assertEquals("Pop", ArtistFacts.canonical("pop"))
        assertEquals("Rock", ArtistFacts.canonical("rock"))
        assertEquals("Hip-Hop", ArtistFacts.canonical("hip-hop"))
        assertEquals("Hip-Hop", ArtistFacts.canonical("hip hop"))
        assertEquals("Hip-Hop", ArtistFacts.canonical("hiphop"))
        assertEquals("R&B", ArtistFacts.canonical("r&b"))
        assertEquals("R&B", ArtistFacts.canonical("rnb"))
        assertEquals("Lo-Fi", ArtistFacts.canonical("lo-fi"))
        assertEquals("Lo-Fi", ArtistFacts.canonical("chillhop"))
        assertEquals("Electronic", ArtistFacts.canonical("electronic"))
        assertEquals("Drum & Bass", ArtistFacts.canonical("dnb"))
        assertEquals("Soundtrack", ArtistFacts.canonical("ost"))
        assertEquals("Shoegaze", ArtistFacts.canonical("shoegaze"))
    }

    @Test
    fun canonicalRejectsFolksonomyAndNonGenres() {
        assertNull(ArtistFacts.canonical("seen live"))
        assertNull(ArtistFacts.canonical("awesome"))
        assertNull(ArtistFacts.canonical("favorites"))
        assertNull(ArtistFacts.canonical("albums i own"))
        assertNull(ArtistFacts.canonical("spotify"))
        assertNull(ArtistFacts.canonical("2024"))
        assertNull(ArtistFacts.canonical(""))
        assertNull(ArtistFacts.canonical("   "))
    }
}

