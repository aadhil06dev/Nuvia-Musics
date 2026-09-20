package com.music.nuvia.ui.screens

import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.ShelfItem

/**
 * Canonical presentation identity for Home feed items.
 *
 * Disambiguates items by their primary identifier:
 * - Tracks use their videoId (e.g. "track:dQw4w9WgXcQ", "track:jiosaavn:12345")
 * - Albums, artists, and playlists use their browseId (e.g. "browse:MPREb_xxx", "browse:UCxxx")
 * - Items lacking IDs fall back to normalized title and subtitle
 */
fun ShelfItem.canonicalKey(): String = when {
    !videoId.isNullOrBlank() -> "track:$videoId"
    !browseId.isNullOrBlank() -> "browse:$browseId"
    else -> "text:${title.trim().lowercase()}|${subtitle.trim().lowercase()}"
}

val ShelfItem.isTrack: Boolean
    get() = !videoId.isNullOrBlank()

/**
 * Checks whether a shelf represents a playlist, mix, or community collection
 * which must not be converted into a cinematic hero spotlight.
 */
fun isPlaylistOrCommunityShelf(shelf: HomeShelf): Boolean {
    val titleLower = shelf.title.lowercase()
    return titleLower.contains("community") ||
        titleLower.contains("playlist") ||
        titleLower.contains("mix") ||
        titleLower.contains("trending")
}

/**
 * Deduplicates items within a single collection, preserving original order.
 * Ensures no duplicate track, artist, album, or playlist card exists in a single shelf.
 */
fun deduplicateShelfItems(items: List<ShelfItem>): List<ShelfItem> {
    val seen = HashSet<String>(items.size)
    val result = ArrayList<ShelfItem>(items.size)
    for (item in items) {
        if (seen.add(item.canonicalKey())) {
            result.add(item)
        }
    }
    return result
}

/**
 * Curated presentation state for the Home screen.
 */
data class HomeCuratedFeed(
    val quickItems: List<ShelfItem>,
    val contentShelves: List<HomeShelf>,
    val leadShelf: HomeShelf?,
)

/**
 * Keywords associated with each mood / vibe filter.
 */
fun getVibeKeywords(vibe: String): List<String> = when (vibe.lowercase()) {
    "atmosphere" -> listOf(
        "atmosphere", "ambient", "drift", "soundscape", "drone", "space",
        "night", "lo-fi", "lofi", "instrumental", "sleep", "deep", "calm",
        "peaceful", "wave", "ethereal", "vibes", "cinematic", "meditation",
        "nature", "piano", "chill", "relax", "dream", "cloud"
    )
    "chill" -> listOf(
        "chill", "relax", "acoustic", "coffee", "lazy", "smooth", "unwind",
        "easy", "mellow", "lounge", "soft", "slow", "breeze", "r&b", "soul",
        "bedroom", "indie", "cozy", "folk", "quiet", "sunset", "warm"
    )
    "energy" -> listOf(
        "energy", "workout", "upbeat", "dance", "edm", "hype", "power",
        "cardio", "gym", "party", "rock", "electronic", "trap", "rap",
        "fast", "bass", "running", "club", "pop", "fitness", "boost",
        "metal", "active", "speed", "pump", "banger"
    )
    "focus" -> listOf(
        "focus", "study", "work", "flow", "concentration", "read", "coding",
        "minimal", "classical", "brain", "productivity", "lofi", "ambient",
        "peaceful", "instrumental", "piano", "deep", "task", "thinking"
    )
    "night" -> listOf(
        "night", "midnight", "dark", "after hours", "late", "sleep", "stars",
        "dusk", "nocturnal", "evening", "dream", "shadow", "moon", "lights",
        "club", "slowed", "reverb"
    )
    else -> emptyList()
}

/**
 * Scores an item against the active vibe keywords.
 */
fun itemVibeScore(item: ShelfItem, keywords: List<String>): Int {
    if (keywords.isEmpty()) return 0
    val text = "${item.title} ${item.subtitle}".lowercase()
    return keywords.count { kw -> text.contains(kw) }
}

/**
 * Scores a shelf against the active vibe keywords based on title and member items.
 */
fun shelfVibeScore(shelf: HomeShelf, keywords: List<String>): Int {
    if (keywords.isEmpty()) return 0
    val titleScore = keywords.count { kw -> shelf.title.lowercase().contains(kw) } * 4
    val itemsScore = shelf.items.sumOf { itemVibeScore(it, keywords) }
    return titleScore + itemsScore
}

/**
 * Deterministic recommendation curation and global deduplication engine.
 *
 * Execution Order:
 * 1. Source Data Normalization: Intra-shelf deduplication per collection.
 * 2. Vibe Ranking: Arrange shelves and items according to the active mood filter.
 * 3. Section Priority & Cross-Section Track Deduplication:
 *    - Priority 1: Spotlight / Hero Showcase (leadShelf)
 *    - Priority 2: Quick Picks / Shortcuts Grid (quickItems, up to 6 unique tracks)
 *    - Priority 3 & 4: Diverse Recommendation Shelves (contentShelves)
 *
 * Rules:
 * - Once a track is claimed by a higher-priority section, all subsequent sections exclude it.
 * - Tracks with different canonical IDs (even if sharing titles) are preserved.
 * - Insufficient items never cause duplicated filler cards; sections gracefully adapt or omit.
 */
fun curateHomeFeed(
    shelves: List<HomeShelf>,
    vibe: String,
): HomeCuratedFeed {
    if (shelves.isEmpty()) {
        return HomeCuratedFeed(emptyList(), emptyList(), null)
    }

    // Step 1: Intra-shelf deduplication for every incoming shelf
    val cleanShelves = ArrayList<HomeShelf>(shelves.size)
    for (shelf in shelves) {
        val distinctItems = deduplicateShelfItems(shelf.items)
        if (distinctItems.isNotEmpty()) {
            cleanShelves.add(shelf.copy(items = distinctItems))
        }
    }

    if (cleanShelves.isEmpty()) {
        return HomeCuratedFeed(emptyList(), emptyList(), null)
    }

    // Step 2: Vibe Ranking
    val isAllVibe = vibe.equals("All", ignoreCase = true)
    val keywords = if (isAllVibe) emptyList() else getVibeKeywords(vibe)
    val rankedShelves = if (isAllVibe) {
        cleanShelves
    } else {
        cleanShelves.sortedByDescending { shelfVibeScore(it, keywords) }
    }

    // Global trackers for claimed tracks and entities across sections
    val claimedTrackKeys = HashSet<String>()
    val claimedEntityKeys = HashSet<String>()

    // Step 3: Priority 1 — Spotlight / Hero Showcase
    val rawLead = if (isAllVibe) {
        rankedShelves.firstOrNull { !isPlaylistOrCommunityShelf(it) && it.items.isNotEmpty() }
    } else {
        rankedShelves.firstOrNull { !isPlaylistOrCommunityShelf(it) && shelfVibeScore(it, keywords) > 0 && it.items.isNotEmpty() }
    }

    val leadShelf: HomeShelf? = if (rawLead != null) {
        val leadItems = if (!isAllVibe) {
            rawLead.items.sortedByDescending { itemVibeScore(it, keywords) }
        } else {
            rawLead.items
        }.take(8) // Limit hero showcase to top 8 spotlight items

        // Claim all items of the raw lead shelf to prevent any of its songs/entities repeating
        for (item in rawLead.items) {
            if (item.isTrack) {
                claimedTrackKeys.add(item.canonicalKey())
            } else {
                claimedEntityKeys.add(item.canonicalKey())
            }
        }
        rawLead.copy(items = leadItems)
    } else {
        null
    }

    // Step 4: Priority 2 — Quick Picks Grid (up to 6 unique playable tracks)
    val remainingShelvesForPicks = rankedShelves.filter { it != rawLead }

    val quickCandidates = ArrayList<ShelfItem>()
    val quickSeenKeys = HashSet<String>()

    fun collectEligibleQuickItems(items: List<ShelfItem>, predicate: (ShelfItem) -> Boolean) {
        for (item in items) {
            if (quickCandidates.size >= 6) break
            val key = item.canonicalKey()
            val isClaimed = if (item.isTrack) claimedTrackKeys.contains(key) else claimedEntityKeys.contains(key)
            if (!isClaimed && quickSeenKeys.add(key) && predicate(item)) {
                quickCandidates.add(item)
            }
        }
    }

    if (!isAllVibe) {
        // Collect vibe-matching tracks first
        for (shelf in remainingShelvesForPicks) {
            collectEligibleQuickItems(shelf.items) { it.isTrack && itemVibeScore(it, keywords) > 0 }
            if (quickCandidates.size >= 6) break
        }
        // Then fallback tracks if under 6
        if (quickCandidates.size < 6) {
            for (shelf in remainingShelvesForPicks) {
                collectEligibleQuickItems(shelf.items) { it.isTrack }
                if (quickCandidates.size >= 6) break
            }
        }
        // Then any other playable collection if still under 6
        if (quickCandidates.size < 6) {
            for (shelf in remainingShelvesForPicks) {
                collectEligibleQuickItems(shelf.items) { true }
                if (quickCandidates.size >= 6) break
            }
        }
    } else {
        // "All" vibe: collect tracks from remaining shelves in order
        for (shelf in remainingShelvesForPicks) {
            collectEligibleQuickItems(shelf.items) { it.isTrack }
            if (quickCandidates.size >= 6) break
        }
        // If still under 6, collect other playable collections
        if (quickCandidates.size < 6) {
            for (shelf in remainingShelvesForPicks) {
                collectEligibleQuickItems(shelf.items) { true }
                if (quickCandidates.size >= 6) break
            }
        }
    }

    // Register all Quick Picks items in the claimed set
    for (item in quickCandidates) {
        if (item.isTrack) {
            claimedTrackKeys.add(item.canonicalKey())
        } else {
            claimedEntityKeys.add(item.canonicalKey())
        }
    }

    // Step 5: Priority 3 & 4 — Diverse Recommendation Shelves
    val contentShelves = ArrayList<HomeShelf>()
    for (shelf in rankedShelves) {
        if (shelf == rawLead) continue

        val shelfItems = ArrayList<ShelfItem>(shelf.items.size)
        val shelfInternalKeys = HashSet<String>()

        for (item in shelf.items) {
            val key = item.canonicalKey()
            // Intra-shelf deduplication check
            if (!shelfInternalKeys.add(key)) continue

            if (item.isTrack) {
                // Cross-section deduplication check: exclude if already claimed by Spotlight, Quick Picks, or an earlier shelf
                if (claimedTrackKeys.contains(key)) continue

                // Claim track for this shelf and include it
                claimedTrackKeys.add(key)
                shelfItems.add(item)
            } else {
                // Non-track items (albums, artists, playlists):
                // Exclude if already presented in Spotlight hero or Quick Picks
                if (claimedEntityKeys.contains(key)) continue
                shelfItems.add(item)
            }
        }

        // Gracefully drop shelves that have no remaining items
        if (shelfItems.isNotEmpty()) {
            contentShelves.add(shelf.copy(items = shelfItems))
        }
    }

    return HomeCuratedFeed(
        quickItems = quickCandidates,
        contentShelves = contentShelves,
        leadShelf = leadShelf,
    )
}
