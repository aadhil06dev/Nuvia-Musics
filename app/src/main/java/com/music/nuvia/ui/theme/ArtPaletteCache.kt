package com.music.nuvia.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Shared palette seed extracted once from artwork and reused across:
 * - Dynamic NUViA Liquid Glass tokens ([NUViAColors])
 * - Now Playing mesh gradient backdrop ([com.music.nuvia.ui.player.MeshPalette])
 * - Album / Playlist / Artist detail page wash ([ArtworkPalette])
 * - Song action sheets and backdrops
 */
@Immutable
data class ArtPaletteSeed(
    val dominant: Color,
    val vibrant: Color,
    val muted: Color,
    val bottomEdge: Color,
    val meshColors: List<Color>,
)

/**
 * Normalizes artwork URLs by stripping resolution-specific size hints (e.g. `w480-h480`, `w160-h160`).
 * Ensures all display sizes of the same track or album cover resolve to a single canonical palette cache key.
 */
private val SIZE_HINT_REGEX = Regex("""w\d+-h\d+""")

fun normalizeArtworkKey(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return url.replace(SIZE_HINT_REGEX, "size_neutral")
}

/**
 * High-performance, synchronized in-memory LRU cache of derived artwork palette seeds.
 * Keyed by normalized artwork URL to prevent redundant bitmap decoding and color quantization.
 */
object ArtPaletteCache {
    private const val MAX_ENTRIES = 128
    private val lock = Any()
    private val cache = object : LinkedHashMap<String, ArtPaletteSeed>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, ArtPaletteSeed>) = size > MAX_ENTRIES
    }

    fun get(url: String?): ArtPaletteSeed? {
        val key = normalizeArtworkKey(url) ?: return null
        synchronized(lock) {
            return cache[key]
        }
    }

    fun put(url: String?, seed: ArtPaletteSeed) {
        val key = normalizeArtworkKey(url) ?: return
        synchronized(lock) {
            cache[key] = seed
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }
}

val FallbackMeshColors = listOf(
    Color(0xFF2C1004),
    Color(0xFF1E0A03),
    Color(0xFF150702),
    Color(0xFF0D0502),
)

/**
 * Extracts distinct, contrast-guarded swatches and a 4-color mesh palette from a decoded artwork bitmap.
 * Runs on [kotlinx.coroutines.Dispatchers.Default].
 */
fun extractArtPaletteSeed(bitmap: Bitmap): ArtPaletteSeed? {
    val swatches = Palette.from(bitmap)
        .maximumColorCount(28)
        .generate()
        .swatches
        .ifEmpty {
            Palette.from(bitmap).clearFilters().maximumColorCount(28).generate().swatches
        }
    if (swatches.isEmpty()) return null

    // Filter out washed-out near-white (>0.88L) and crushed near-black (<0.08L)
    val chromaticSwatches = swatches.filter { swatch ->
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(swatch.rgb, it) }
        val s = hsl[1]
        val l = hsl[2]
        l in 0.08f..0.88f && s >= 0.08f
    }

    val usablePool = chromaticSwatches.ifEmpty { swatches }

    // Dominant swatch by population from usable pool
    val dominantSwatch = usablePool.maxByOrNull { it.population } ?: swatches.first()

    // Vibrant swatch: balances high saturation, mid-range lightness and population weight
    val vibrantSwatch = usablePool.maxByOrNull { swatch ->
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(swatch.rgb, it) }
        val s = hsl[1]
        val l = hsl[2]
        val lightnessScore = 1f - abs(l - 0.55f) * 1.5f
        s * s * lightnessScore.coerceAtLeast(0.2f) * sqrt(swatch.population.toFloat())
    } ?: dominantSwatch

    // Distinct muted/harmonic swatch
    val mutedSwatch = usablePool.filter { it != vibrantSwatch && it != dominantSwatch }
        .maxByOrNull { it.population } ?: dominantSwatch

    val edgeColor = sampleBottomEdgeColor(bitmap)

    // Mesh gradient 4-color palette
    val sortedColors = swatches.sortedByDescending { it.population }.map { Color(it.rgb) }
    val distinct = sortedColors.distinctEnough()
    val meshColors = when {
        distinct.isEmpty() -> FallbackMeshColors
        distinct.size >= 4 -> distinct.take(4)
        else -> distinct.expandedToFour()
    }

    return ArtPaletteSeed(
        dominant = Color(dominantSwatch.rgb),
        vibrant = Color(vibrantSwatch.rgb),
        muted = Color(mutedSwatch.rgb),
        bottomEdge = edgeColor,
        meshColors = meshColors,
    )
}

/**
 * Samples the mean color of the artwork's bottom band.
 */
fun sampleBottomEdgeColor(bitmap: Bitmap): Color {
    val band = (bitmap.height * 0.18f).toInt().coerceIn(1, bitmap.height)
    val pixels = IntArray(bitmap.width * band)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, bitmap.height - band, bitmap.width, band)

    var r = 0L
    var g = 0L
    var b = 0L
    for (pixel in pixels) {
        r += (pixel shr 16) and 0xFF
        g += (pixel shr 8) and 0xFF
        b += pixel and 0xFF
    }
    val count = pixels.size.coerceAtLeast(1)
    return Color(
        red = (r / count).toInt(),
        green = (g / count).toInt(),
        blue = (b / count).toInt()
    )
}

/** Drop near-duplicates, so the four mesh blobs don't collapse into one wash. */
fun List<Color>.distinctEnough(): List<Color> {
    val kept = mutableListOf<Color>()
    forEach { color -> if (kept.none { it.isCloseTo(color) }) kept += color }
    return kept
}

private fun Color.isCloseTo(other: Color): Boolean {
    val a = hsl()
    val b = other.hsl()
    val hueGap = abs(a[0] - b[0]).let { min(it, 360f - it) }
    return hueGap < 15f && abs(a[2] - b[2]) < 0.12f
}

/** Fill empty mesh slots off the artwork itself, fanning hue and lightness out. */
fun List<Color>.expandedToFour(): List<Color> {
    val out = toMutableList()
    var step = 1
    while (out.size < 4) {
        out += this[(out.size - size) % size].shifted(24f * step, 0.12f * step)
        step++
    }
    return out
}

private fun Color.shifted(hue: Float, lightness: Float): Color {
    val hsl = hsl()
    hsl[0] = (hsl[0] + hue) % 360f
    hsl[2] = (hsl[2] + lightness).coerceIn(0.2f, 0.7f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.hsl(): FloatArray =
    FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
