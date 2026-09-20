package com.music.nuvia.data.lyrics

/** `ttm:agent` types, as both TTML and LyricsPlus name them. */
private const val PERSON = "person"
private const val GROUP = "group"
private const val OTHER = "other"

/** Apple's two reserved voices: everyone at once, and the other singer. */
private const val GROUP_AGENT = "v1000"
private const val OTHER_AGENT = "v2000"

/**
 * Past this share of lines on the right, the whole song is flipped — see
 * [lineAlignments]. Not 100%: a chorus or two sung by the group lands on the
 * left and would otherwise be enough to call it a genuine duet.
 */
private const val MOSTLY_RIGHT = 0.85f

/**
 * Which side of the panel each line is sung from, given the voice that sang it.
 */
internal fun lineAlignments(
    singers: List<String?>,
    types: Map<String, String>,
): List<LyricAlignment> {
    var left = true
    var lastVoice: String? = null
    var rightward = 0
    var placed = 0

    val sides = singers.map { singer ->
        if (singer.isNullOrEmpty()) return@map LyricAlignment.Start
        // Apple's two reserved ids carry no declaration of their own.
        val type = types[singer] ?: when (singer) {
            GROUP_AGENT -> GROUP
            OTHER_AGENT -> OTHER
            else -> PERSON
        }
        placed += 1
        if (type == GROUP) return@map LyricAlignment.Start

        when {
            lastVoice == null -> left = type != OTHER
            singer != lastVoice -> left = !left
        }
        lastVoice = singer

        if (!left) rightward += 1
        if (left) LyricAlignment.Start else LyricAlignment.End
    }

    if (placed == 0 || rightward.toFloat() / placed < MOSTLY_RIGHT) return sides
    return sides.map {
        if (it == LyricAlignment.Start) LyricAlignment.End else LyricAlignment.Start
    }
}

