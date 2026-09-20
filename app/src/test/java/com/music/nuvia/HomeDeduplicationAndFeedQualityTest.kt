package com.music.nuvia

import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.ui.screens.canonicalKey
import com.music.nuvia.ui.screens.curateHomeFeed
import com.music.nuvia.ui.screens.deduplicateShelfItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDeduplicationAndFeedQualityTest {

    private fun track(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist $id",
        thumb: String? = "https://img.youtube.com/vi/$id/0.jpg",
    ) = ShelfItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = thumb,
        videoId = id,
        browseId = null,
    )

    private fun album(
        id: String,
        title: String = "Album $id",
        artist: String = "Artist $id",
    ) = ShelfItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = null,
        videoId = null,
        browseId = id,
    )

    @Test
    fun `1 - same track appearing in multiple source lists only highest priority section keeps it`() {
        val sharedTrack = track("track_shared", title = "Billie Jean")
        val leadTrack1 = track("track_lead_1")
        val leadTrack2 = track("track_lead_2")
        val otherTrack1 = track("track_other_1")
        val otherTrack2 = track("track_other_2")

        val shelf1 = HomeShelf("Featured Spotlight", listOf(sharedTrack, leadTrack1, leadTrack2))
        val shelf2 = HomeShelf("Quick Picks", listOf(sharedTrack, otherTrack1, otherTrack2))
        val shelf3 = HomeShelf("Trending Songs", listOf(sharedTrack, track("track_trend_1")))

        val feed = curateHomeFeed(listOf(shelf1, shelf2, shelf3), "All")

        // Priority 1: Spotlight (leadShelf) keeps sharedTrack
        assertNotNull(feed.leadShelf)
        assertTrue(feed.leadShelf!!.items.any { it.videoId == "track_shared" })

        // Priority 2: Quick Picks must NOT contain sharedTrack
        assertFalse(feed.quickItems.any { it.videoId == "track_shared" })

        // Priority 3: Remaining content shelves must NOT contain sharedTrack
        val allContentShelfTracks = feed.contentShelves.flatMap { it.items }.mapNotNull { it.videoId }
        assertFalse(allContentShelfTracks.contains("track_shared"))

        // Priority 2 to Priority 3: Track selected for Quick Picks must not appear in content shelves
        val quickIds = feed.quickItems.mapNotNull { it.videoId }.toSet()
        for (contentTrackId in allContentShelfTracks) {
            assertFalse("Track $contentTrackId was in Quick Picks and should not be in content shelves", quickIds.contains(contentTrackId))
        }
    }

    @Test
    fun `2 - same track appearing twice in one shelf results in one entry`() {
        val trackA = track("track_A", title = "Starboy")
        val trackB = track("track_B", title = "Blinding Lights")
        val trackADuplicate = track("track_A", title = "Starboy")

        val rawItems = listOf(trackA, trackB, trackADuplicate)
        val deduplicated = deduplicateShelfItems(rawItems)

        assertEquals(2, deduplicated.size)
        assertEquals("track_A", deduplicated[0].videoId)
        assertEquals("track_B", deduplicated[1].videoId)
    }

    @Test
    fun `3 - same title but different track IDs both remain`() {
        val kidLaroiStay = track("vid_stay_kid_laroi", title = "Stay", artist = "The Kid LAROI")
        val rihannaStay = track("vid_stay_rihanna", title = "Stay", artist = "Rihanna")

        val shelf = HomeShelf("Pop Hits", listOf(kidLaroiStay, rihannaStay))
        val deduplicated = deduplicateShelfItems(shelf.items)

        assertEquals(2, deduplicated.size)
        assertEquals("vid_stay_kid_laroi", deduplicated[0].videoId)
        assertEquals("vid_stay_rihanna", deduplicated[1].videoId)
        assertEquals("Stay", deduplicated[0].title)
        assertEquals("Stay", deduplicated[1].title)
    }

    @Test
    fun `4 - same track from the same provider yields one result`() {
        val yt1 = track("dQw4w9WgXcQ", title = "Never Gonna Give You Up")
        val yt2 = track("dQw4w9WgXcQ", title = "Never Gonna Give You Up")

        val items = listOf(yt1, yt2)
        val deduplicated = deduplicateShelfItems(items)

        assertEquals(1, deduplicated.size)
        assertEquals("dQw4w9WgXcQ", deduplicated[0].videoId)
    }

    @Test
    fun `5 - different providers with distinct IDs do not incorrectly remove unrelated tracks`() {
        val ytTrack = track("yt_rick_astley", title = "Song", artist = "Artist A")
        val jioTrack = track("jiosaavn:987654", title = "Song", artist = "Artist B")

        assertEquals("track:yt_rick_astley", ytTrack.canonicalKey())
        assertEquals("track:jiosaavn:987654", jioTrack.canonicalKey())

        val shelf = HomeShelf("Mixed Sources", listOf(ytTrack, jioTrack))
        val deduplicated = deduplicateShelfItems(shelf.items)

        assertEquals(2, deduplicated.size)
        assertEquals("yt_rick_astley", deduplicated[0].videoId)
        assertEquals("jiosaavn:987654", deduplicated[1].videoId)
    }

    @Test
    fun `6 - vibe filtering and deduplication work seamlessly together`() {
        val chill1 = track("chill_1", title = "Chill Evening Breeze", artist = "Lofi Artist")
        val chill2 = track("chill_2", title = "Acoustic Sunset Melody", artist = "Acoustic Band")
        val chill3 = track("chill_3", title = "Relaxing Coffee Study", artist = "Piano Solo")
        val energy1 = track("energy_1", title = "Workout Upbeat Party", artist = "EDM DJ")

        val shelfChill = HomeShelf("Chill Vibes & Relax", listOf(chill1, chill2, chill3))
        val shelfEnergy = HomeShelf("Workout Power Energy", listOf(energy1, chill1)) // Note chill1 duplicated here

        val feed = curateHomeFeed(listOf(shelfChill, shelfEnergy), "Chill")

        // Spotlight should feature the highest scoring non-playlist shelf
        assertNotNull(feed.leadShelf)
        assertEquals("Chill Vibes & Relax", feed.leadShelf!!.title)

        // Count total occurrences of chill_1 across the whole curated feed
        val totalChill1Count = (listOfNotNull(feed.leadShelf).flatMap { it.items } +
            feed.quickItems +
            feed.contentShelves.flatMap { it.items })
            .count { it.videoId == "chill_1" }

        assertEquals("chill_1 must appear exactly once in the entire curated feed", 1, totalChill1Count)
    }

    @Test
    fun `7 - refresh does not accumulate duplicates`() {
        val shelf = HomeShelf("Daily Rotation", listOf(
            track("t1"), track("t2"), track("t3"), track("t4")
        ))

        // First load
        val feed1 = curateHomeFeed(listOf(shelf), "All")
        val tracks1 = (listOfNotNull(feed1.leadShelf).flatMap { it.items } +
            feed1.quickItems +
            feed1.contentShelves.flatMap { it.items }).mapNotNull { it.videoId }

        // Simulated pull-to-refresh with the same repository response
        val feed2 = curateHomeFeed(listOf(shelf), "All")
        val tracks2 = (listOfNotNull(feed2.leadShelf).flatMap { it.items } +
            feed2.quickItems +
            feed2.contentShelves.flatMap { it.items }).mapNotNull { it.videoId }

        assertEquals(tracks1, tracks2)
        assertEquals(tracks1.toSet().size, tracks1.size)
    }

    @Test
    fun `8 - insufficient unique tracks do not repeat tracks to fill shelf`() {
        // Only 3 unique tracks in total
        val t1 = track("unique_1")
        val t2 = track("unique_2")
        val t3 = track("unique_3")

        val shelf = HomeShelf("Small Library", listOf(t1, t2, t3))

        val feed = curateHomeFeed(listOf(shelf), "All")

        // Lead shelf takes 3 tracks
        assertEquals(3, feed.leadShelf?.items?.size)

        // Quick picks has no remaining tracks to repeat; it must NOT duplicate unique_1, 2, or 3
        assertTrue(feed.quickItems.isEmpty())

        // Content shelves should have no duplicates and drop if empty
        val contentTracks = feed.contentShelves.flatMap { it.items }.mapNotNull { it.videoId }
        assertTrue(contentTracks.isEmpty())

        // When a separate shelf has 2 unique tracks for quick items:
        val quickShelf = HomeShelf("Quick Shelf", listOf(track("q1"), track("q2")))
        val feedWithQuick = curateHomeFeed(listOf(shelf, quickShelf), "All")

        assertEquals(2, feedWithQuick.quickItems.size)
        // Must NOT inflate to 6 by repeating q1 and q2
        assertEquals(listOf("q1", "q2"), feedWithQuick.quickItems.map { it.videoId })
    }

    @Test
    fun `9 - album and artist shelves deduplicate duplicate entities within same shelf`() {
        val album1 = album("MPREb_album1", title = "Album One")
        val album2 = album("MPREb_album2", title = "Album Two")
        val album1Dup = album("MPREb_album1", title = "Album One")

        val shelf = HomeShelf("New Releases", listOf(album1, album2, album1Dup))
        val deduplicated = deduplicateShelfItems(shelf.items)

        assertEquals(2, deduplicated.size)
        assertEquals("MPREb_album1", deduplicated[0].browseId)
        assertEquals("MPREb_album2", deduplicated[1].browseId)
    }
}
