package com.music.nuvia.data.lyrics

/**
 * A YouTube title, as a lyrics database would have indexed it.
 *
 * Every source here but [SimpMusicLyrics] is asked for a name, and the name
 * this app has is YouTube's, which is not the name anyone catalogued. "Dracula
 * (feat. JENNIE)" is filed by Apple, LRCLIB and everyone else as "Dracula", and
 * asked for verbatim it misses all of them.
 *
 * Only credits and packaging are removed: who else is on the record, and how the upload was
 * labelled ("(Official Music Video)", "(Lyric Video)", "[HD]").
 *
 * Anything that names a different recording stays — "(Remix)", "(Acoustic)",
 * "(Live)", "(Remastered)", "(Sped Up)".
 */
object LyricsQuery {
    fun forLyricsSearch(title: String): String = title.forLyricsSearch()
    fun artistForLyricsSearch(artist: String): String = artist.artistForLyricsSearch()
}

internal fun String.forLyricsSearch(): String {
    var name = this
    CREDITS.forEach { pattern -> name = pattern.replace(name, " ") }
    return name.replace(WHITESPACE, " ").trim().trimEnd(',', '-', '–', '—').trim()
        .ifBlank { trim() }
}

/**
 * Trims " - Topic" off an auto-generated channel name.
 *
 * YouTube's own artist channels for licensed music are named this way, and it
 * reaches the player as the artist on anything played from one.
 */
internal fun String.artistForLyricsSearch(): String =
    removeSuffix(" - Topic").trim().ifBlank { trim() }

private val WHITESPACE = Regex("""\s+""")

private val CREDITS = listOf(
    // Bracketed credits: (feat. X), [ft. X], (with X).
    Regex("""\s*[(\[]\s*(feat|ft|featuring|with)\b[^)\]]*[)\]]""", RegexOption.IGNORE_CASE),
    // The same, unbracketed and running to the end of the title.
    Regex("""\s+(feat|ft|featuring)\.?\s+.*$""", RegexOption.IGNORE_CASE),
    // How the upload was labelled, not what was recorded.
    Regex(
        """\s*[(\[]\s*(official\s*)?(music\s*)?""" +
            """(video|audio|visuali[sz]er|lyrics?\s*video|lyrics?|m/?v|hd|hq|4k|full\s*song)""" +
            """\s*[)\]]""",
        RegexOption.IGNORE_CASE,
    ),
    Regex("""\s*[(\[]\s*official\s*[)\]]""", RegexOption.IGNORE_CASE),
)
