package com.music.nuvia.playback

enum class EqualizerPreset(vararg val bandsDb: Float) {
    FLAT(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    ACOUSTIC(3f, 1.5f, 0f, 1.5f, 2.5f, 2f, 1f),
    BASS_BOOST(6f, 4f, 1.5f, 0f, 0f, 0f, 0f),
    BASS_CUT(-6f, -4f, -1.5f, 0f, 0f, 0f, 0f),
    VOCAL(-3f, -1.5f, 1f, 3.5f, 3f, 1f, -1f),
    TREBLE_BOOST(0f, 0f, 0f, 0f, 1.5f, 3.5f, 5f),
    TREBLE_CUT(0f, 0f, 0f, 0f, -1.5f, -3.5f, -5f),
    LOUDNESS(6f, 3.5f, 0f, -1.5f, -1f, 2f, 5f),
    SPOKEN_WORD(-5f, -2.5f, 1.5f, 4f, 3.5f, 1.5f, -2f),
    ELECTRONIC(5f, 3f, -1f, 0f, 1f, 3f, 4f),
    ROCK(4f, 2.5f, -1f, -1.5f, 1f, 3f, 3.5f),
    HIP_HOP(6f, 4f, 0.5f, -1f, 0.5f, 2f, 2.5f),
    JAZZ(3f, 1.5f, 0f, 1f, 1.5f, 2f, 2.5f),
    CLASSICAL(3f, 2f, 0f, 0f, 1f, 2.5f, 3f),
    SMALL_SPEAKERS(5f, 4f, 2f, 0.5f, 0f, -1f, -2f),
    LATE_NIGHT(3f, 1f, 0f, 1.5f, 1f, -1f, -3f),
    CUSTOM,
    ;

    val bands: List<Float> get() = bandsDb.toList()

    companion object {
        fun matching(bandsDb: List<Float>): EqualizerPreset = entries.firstOrNull { preset ->
            preset != CUSTOM && preset.bandsDb.size == bandsDb.size &&
                preset.bandsDb.indices.all { kotlin.math.abs(preset.bandsDb[it] - bandsDb[it]) < 0.05f }
        } ?: CUSTOM
    }
}

