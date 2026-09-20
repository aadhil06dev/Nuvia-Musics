package com.music.nuvia

import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinnedPlaylistsTest {

    @Before
    fun setUp() {
        // Reset pinned playlists state before each test
        AppSettings.pinnedPlaylists.value = emptyList()
    }

    @Test
    fun `pinning playlist adds to pinned list`() {
        val pinned = AppSettings.togglePinnedPlaylist("playlist_1")
        assertTrue(pinned)
        assertEquals(listOf("playlist_1"), AppSettings.pinnedPlaylists.value)
    }

    @Test
    fun `toggling already pinned playlist unpins it`() {
        AppSettings.togglePinnedPlaylist("playlist_1")
        assertEquals(listOf("playlist_1"), AppSettings.pinnedPlaylists.value)

        val pinnedAgain = AppSettings.togglePinnedPlaylist("playlist_1")
        assertFalse(pinnedAgain)
        assertTrue(AppSettings.pinnedPlaylists.value.isEmpty())
    }

    @Test
    fun `toggling recognizes VL prefix variations`() {
        AppSettings.togglePinnedPlaylist("VLPL12345")
        assertEquals(listOf("VLPL12345"), AppSettings.pinnedPlaylists.value)

        // Toggling with prefix removed should unpin the same playlist
        val pinned = AppSettings.togglePinnedPlaylist("PL12345")
        assertFalse(pinned)
        assertTrue(AppSettings.pinnedPlaylists.value.isEmpty())
    }

    @Test
    fun `maximum pin limit is enforced`() {
        assertEquals(5, AppSettings.MAX_PINNED_PLAYLISTS)

        for (i in 1..AppSettings.MAX_PINNED_PLAYLISTS) {
            val result = AppSettings.togglePinnedPlaylist("playlist_$i")
            assertTrue("Pinning playlist_$i should succeed", result)
        }
        assertEquals(5, AppSettings.pinnedPlaylists.value.size)

        // Attempting to pin 6th playlist should be refused
        val sixthResult = AppSettings.togglePinnedPlaylist("playlist_6")
        assertFalse("Pinning past limit should be refused", sixthResult)
        assertEquals(5, AppSettings.pinnedPlaylists.value.size)
        assertFalse("playlist_6 should not be in list", "playlist_6" in AppSettings.pinnedPlaylists.value)
    }

    @Test
    fun `unpinPlaylist removes playlist and prefix variations`() {
        AppSettings.togglePinnedPlaylist("VL_fav_1")
        AppSettings.togglePinnedPlaylist("fav_2")
        assertEquals(listOf("VL_fav_1", "fav_2"), AppSettings.pinnedPlaylists.value)

        AppSettings.unpinPlaylist("_fav_1") // removes VL_fav_1
        assertEquals(listOf("fav_2"), AppSettings.pinnedPlaylists.value)

        AppSettings.unpinPlaylist("VLfav_2") // removes fav_2
        assertTrue(AppSettings.pinnedPlaylists.value.isEmpty())
    }

    @Test
    fun `library deterministic ordering puts pinned items first in pinned order`() {
        // Pin playlists in specific order: P3, P1
        AppSettings.togglePinnedPlaylist("P3")
        AppSettings.togglePinnedPlaylist("P1")

        val pinnedList = AppSettings.pinnedPlaylists.value
        val items = listOf(
            ShelfItem(title = "Playlist One", subtitle = "User", thumbnailUrl = null, videoId = null, browseId = "P1"),
            ShelfItem(title = "Playlist Two", subtitle = "User", thumbnailUrl = null, videoId = null, browseId = "P2"),
            ShelfItem(title = "Playlist Three", subtitle = "User", thumbnailUrl = null, videoId = null, browseId = "P3"),
            ShelfItem(title = "Playlist Four", subtitle = "User", thumbnailUrl = null, videoId = null, browseId = "P4"),
        )
        val shelf = HomeShelf("Playlists", items)

        // Same reordering algorithm as LibraryScreen.kt
        val isItemPinned: (ShelfItem) -> Boolean = { item ->
            val id = item.browseId
            id != null && (id in pinnedList || id.removePrefix("VL") in pinnedList || "VL$id" in pinnedList)
        }

        val (pinned, unpinned) = shelf.items.partition { isItemPinned(it) }
        val sortedPinned = pinned.sortedBy { item ->
            val id = item.browseId.orEmpty()
            val index1 = pinnedList.indexOf(id)
            val index2 = pinnedList.indexOf(id.removePrefix("VL"))
            val index3 = pinnedList.indexOf("VL$id")
            listOf(index1, index2, index3).filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        }
        val result = sortedPinned + unpinned

        // Expect P3 (first pinned), P1 (second pinned), then unpinned P2, P4
        assertEquals(listOf("P3", "P1", "P2", "P4"), result.map { it.browseId })
    }
}
