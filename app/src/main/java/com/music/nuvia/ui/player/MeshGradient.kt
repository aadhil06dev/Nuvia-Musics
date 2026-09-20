package com.music.nuvia.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.nuvia.data.model.CARD_ART_PX
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.theme.ArtPaletteCache
import com.music.nuvia.ui.theme.extractArtPaletteSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val FallbackColors = listOf(
    Color(0xFF2A1205),
    Color(0xFF140802),
    Color(0xFF381604),
    Color(0xFF0D0502),
)

private val MESH_ANCHORS = arrayOf(
    Offset(0.20f, 0.25f),
    Offset(0.80f, 0.20f),
    Offset(0.75f, 0.80f),
    Offset(0.25f, 0.75f),
)

private val MESH_SPEEDS = floatArrayOf(1f, -0.7f, 0.85f, -1.15f)

private val MESH_VIGNETTE_BRUSH = Brush.verticalGradient(
    colors = listOf(
        Color(0x59000000), // Black 0.35f
        Color(0x2E000000), // Black 0.18f
        Color(0xA6000000), // Black 0.65f
        Color(0xF0000000), // Black 0.94f
    ),
)

/** The four mesh colours, wrapped so the backdrop can skip recomposition. */
@Immutable
data class MeshPalette(val colors: List<Color>)

/**
 * The Apple Music "Now Playing" backdrop: four luminous colour blobs sampled
 * from the album art, drawn as soft radial gradients and blurred into a mesh.
 * Colour changes on track skip crossfade over ~1.4s instead of snapping.
 *
 * The blobs drift when there is a reason to — the player opening, or
 * [trackKey] changing — and then come to rest. They used to orbit forever,
 * which meant re-blurring a full-screen layer at display refresh rate for as
 * long as the player was up: the most expensive thing in the app, for motion
 * that reads as ambient at best and is invisible while the phone is in a
 * pocket. The settled frame looks the same; only the battery drain is gone.
 */
@Composable
fun MeshGradientBackground(
    palette: MeshPalette,
    modifier: Modifier = Modifier,
    trackKey: Any? = null,
    driftMillis: Int = 8_000,
) {
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val ambientLighting by AppSettings.ambientLighting.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    val effectiveAmbient = !minimalUi && ambientLighting && palette.colors.isNotEmpty()

    if (!effectiveAmbient) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val tuned = (palette.colors.ifEmpty { FallbackColors } + FallbackColors)
        .take(4)
        .map { it.tuned() }

    // Each colour slot crossfades independently when the track (palette) changes,
    // unless "reduce animation" is on, in which case colours snap straight to target.
    val colorSpec: AnimationSpec<Color> = if (reduceAnimation) snap() else tween(1400)
    val animatedColors = tuned.mapIndexed { index, color ->
        animateColorAsState(color, colorSpec, label = "meshColor$index").value
    }
    val baseColor by animateColorAsState(tuned.first().dimmed(), colorSpec, label = "meshBase")

    // Read in the draw lambda, not here: an Animatable read during draw
    // invalidates only the drawing, leaving composition out of the loop.
    val phase = remember { Animatable(0f) }
    LaunchedEffect(trackKey, reduceAnimation) {
        if (reduceAnimation) {
            phase.snapTo(0f)
        } else {
            // Allow fullscreen presentation opening transition to settle cleanly
            // before beginning continuous ambient drift to protect first-frame framerate.
            delay(400)
            phase.animateTo(
                targetValue = phase.value + DRIFT_RADIANS,
                animationSpec = tween(driftMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    // Scale up slightly so the blur's clamped edges never show, then blur the
    // whole layer (RenderEffect, API 31+; a no-op below — the radial falloff
    // already reads soft there).
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.3f
                scaleY = 1.3f
            }
            .background(baseColor)
            .blur(64.dp),
    ) {
        val drift = phase.value
        val width = size.width
        val height = size.height
        val radius = size.maxDimension * 0.62f
        val colorCount = min(animatedColors.size, MESH_ANCHORS.size)

        for (index in 0 until colorCount) {
            val color = animatedColors[index]
            val anchor = MESH_ANCHORS[index]
            val speed = MESH_SPEEDS[index]
            val centerX = (anchor.x + 0.16f * cos(drift * speed + index * 1.7f)) * width
            val centerY = (anchor.y + 0.16f * sin(drift * speed * 0.9f + index * 2.3f)) * height
            val center = Offset(centerX, centerY)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        // Scrim & Vignette so AMOLED black dominates and white/orange controls pop with high contrast.
        drawRect(brush = MESH_VIGNETTE_BRUSH)
    }
}

/**
 * Loads the artwork with Coil (software bitmap, thumbnail-sized) and pulls a
 * 4-colour palette out of it. Recomputes when [imageUrl] changes.
 *
 * Reuses the shared [ArtPaletteCache] to eliminate duplicate network fetches,
 * bitmap decodes, and palette quantization passes across the player, mesh, and dynamic UI.
 *
 * A track's motion artwork is frequently lit nothing like its still sleeve —
 * a different shot, a different grade. [canvasFrame], a frame captured off
 * the playing clip once one is available, is quantised the same way and
 * takes over from there, crossfading in exactly like a track skip.
 */
@Composable
fun rememberArtworkColors(
    imageUrl: String?,
    canvasFrame: Bitmap? = null,
): MeshPalette {
    val context = LocalContext.current
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val dynamicArtworkColors by AppSettings.dynamicArtworkColors.collectAsStateWithLifecycle()
    val ambientLighting by AppSettings.ambientLighting.collectAsStateWithLifecycle()

    val effectiveLighting = !minimalUi && dynamicArtworkColors && ambientLighting

    if (!effectiveLighting) {
        return remember { MeshPalette(FallbackColors) }
    }

    var palette by remember(imageUrl) {
        val initial = ArtPaletteCache.get(imageUrl)?.meshColors ?: FallbackColors
        mutableStateOf(MeshPalette(initial))
    }

    LaunchedEffect(imageUrl, effectiveLighting) {
        if (!effectiveLighting || imageUrl == null) return@LaunchedEffect
        val cached = ArtPaletteCache.get(imageUrl)
        if (cached != null) {
            palette = MeshPalette(cached.meshColors)
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl.artworkAt(CARD_ART_PX))
            .size(128) // palette quality is fine at thumbnail size, and it's fast
            .allowHardware(false) // Palette needs pixel access
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return@LaunchedEffect
        val extracted = withContext(Dispatchers.Default) { extractArtPaletteSeed(bitmap) } ?: return@LaunchedEffect
        ArtPaletteCache.put(imageUrl, extracted)
        palette = MeshPalette(extracted.meshColors)
    }

    LaunchedEffect(canvasFrame, effectiveLighting) {
        if (!effectiveLighting) return@LaunchedEffect
        val frame = canvasFrame ?: return@LaunchedEffect
        val colors = withContext(Dispatchers.Default) { paletteOf(frame) }
        palette = MeshPalette(colors)
    }
    return palette
}

/**
 * How far the blobs travel in one settle. A shade under half a turn: enough
 * that the backdrop visibly reacts to a track change, short of a full orbit
 * that would land the blobs back where they started.
 */
private const val DRIFT_RADIANS = (PI * 0.45f).toFloat()

/**
 * Four mesh colours drawn from the artwork.
 *
 * The named swatches — vibrant, muted and friends — are a convenience over the
 * full set, and on dark or desaturated sleeves every vibrant slot comes back
 * null: Karan Aujla's marble interior fills two of the five. Topping the rest
 * up from [FallbackColors] is what left those covers sitting under the stock
 * purple. So the whole swatch list is read instead, and any shortfall is
 * derived from the art's own colours rather than borrowed.
 */
private fun paletteOf(bitmap: Bitmap): List<Color> {
    fun swatchesOf(builder: Palette.Builder): List<Color> =
        builder.maximumColorCount(24).generate().swatches
            .sortedByDescending { it.population }
            .map { Color(it.rgb) }

    val found = swatchesOf(Palette.from(bitmap)).ifEmpty {
        // The default filter discards near-black and near-white, which on a
        // monochrome sleeve can be everything there is.
        swatchesOf(Palette.from(bitmap).clearFilters())
    }

    val distinct = found.distinctEnough()
    return when {
        distinct.isEmpty() -> FallbackColors
        distinct.size >= 4 -> distinct.take(4)
        else -> distinct.expandedToFour()
    }
}

/** Drop near-duplicates, so the four blobs don't collapse into one wash. */
private fun List<Color>.distinctEnough(): List<Color> {
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

/** Fill the empty slots off the art itself, fanning hue and lightness out. */
private fun List<Color>.expandedToFour(): List<Color> {
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

/** Boost saturation and clamp lightness so any artwork yields a rich, non-muddy mesh. */
private fun Color.tuned(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.35f).coerceAtMost(1f)
    hsl[2] = hsl[2].coerceIn(0.28f, 0.58f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.dimmed(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = 0.04f
    return Color(ColorUtils.HSLToColor(hsl))
}
