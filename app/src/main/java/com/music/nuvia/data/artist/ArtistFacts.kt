package com.music.nuvia.data.artist

import kotlinx.serialization.Serializable

/**
 * Verified artist metadata and facts.
 *
 * Genuinely sourced from YouTube Music, Last.fm, and open music metadata.
 * Never fabricated.
 */
@Serializable
data class ArtistFacts(
    val artistName: String,
    val biography: String? = null,
    val biographySummary: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val origin: String? = null,
    val formedYear: String? = null,
    val listeners: Long? = null,
    val playCount: Long? = null,
    val onTour: Boolean = false,
    val similarArtists: List<String> = emptyList(),
    val source: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
) {
    val isEmpty: Boolean
        get() = biography.isNullOrBlank() &&
            biographySummary.isNullOrBlank() &&
            genres.isEmpty() &&
            tags.isEmpty() &&
            origin.isNullOrBlank() &&
            formedYear.isNullOrBlank() &&
            listeners == null &&
            playCount == null &&
            similarArtists.isEmpty()

    /**
     * Combined list of displayable tags (genres + tags) normalized and deduplicated.
     */
    val allTags: List<String>
        get() = ArtistFactsRepository.normalizeTags(genres + tags)

    /**
     * Displayable metadata line (e.g., "London, UK • Formed 1994 • 5.2M listeners").
     */
    val formattedSubtitle: String?
        get() {
            val parts = mutableListOf<String>()
            origin?.takeIf { it.isNotBlank() }?.let { parts += it }
            formedYear?.takeIf { it.isNotBlank() }?.let { parts += "Est. $it" }
            listeners?.takeIf { it > 0 }?.let { parts += "${formatCount(it)} listeners" }
            return if (parts.isEmpty()) null else parts.joinToString(" • ")
        }

    companion object {
        fun formatCount(count: Long): String = when {
            count >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}
