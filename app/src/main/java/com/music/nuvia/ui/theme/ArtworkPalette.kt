package com.music.nuvia.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * The colours an album, playlist or artist page paints itself in.
 *
 * Apple Music's release pages are not one design tinted five ways — the whole
 * page is derived from the sleeve, down to which grey the metadata line is. So
 * rather than hand callers a raw swatch and let each of them guess, this is the
 * finished set: a page tint, an accent that is legible *on that tint*, and the
 * two text colours and hairline that go with them.
 *
 * Every value is theme-aware. The same sleeve yields a near-black tint in dark
 * mode and a pale wash of the same hue in light mode, which is the only way the
 * pages stay readable when the app's theme disagrees with the artwork's.
 */
@Immutable
data class ArtworkPalette(
    /** The page's background wash. */
    val background: Color,
    /**
     * The colour the artwork's own bottom edge blurs down to.
     *
     * A blur wide enough to lose the picture leaves the mean of what it
     * sampled, so a page that starts from this colour where the artwork stops
     * reads as that blur carrying on rather than as a second surface beginning.
     * Lighter than [background], which the page still settles into further
     * down — the artwork's colour is strongest right under the artwork.
     */
    val wash: Color,
    /** Fill for the glass buttons and chips that sit on [background]. */
    val elevated: Color,
    /** The artwork's own colour, contrast-corrected — titles, icons, Play. */
    val accent: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,
    val divider: Color,
)

/**
 * Pulls [ArtworkPalette] out of the artwork at [imageUrl].
 *
 * Artwork that has already been read once is tinted on the very first frame,
 * off [seedCache] — a sheet opened from a page it shares a cover with, or a
 * page opened twice, has nothing to wait for and nothing to fade. Only a sleeve
 * genuinely being seen for the first time starts from the theme's own colours
 * and warms into the artwork's, so it never flashes a placeholder tint.
 * "Reduce animation" turns that crossfade into a cut.
 */
@Composable
fun rememberArtworkPalette(
    imageUrl: String?,
    dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    /**
     * The artwork size to read, which should be whichever one the surface
     * already has on screen, matching the backdrop drawn from it.
     *
     * A quantiser cares about a thumbnail's resolution no more than a blur
     * does, so the only thing this choice decides is whether the read comes
     * out of the cache or off the network.
     */
    artPx: Int = CARD_ART_PX,
): ArtworkPalette {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    // The shared seed everything else is derived from, or null until read.
    var seed by remember(imageUrl) { mutableStateOf(ArtPaletteCache.get(imageUrl)) }
    // Whether the colours were there from the first frame. If they were, there
    // is nothing to crossfade *from* and animating would only put a delay in
    // front of a surface that could already be right.
    val knownUpFront = remember(imageUrl) { seed != null }

    LaunchedEffect(imageUrl, artPx) {
        if (imageUrl == null) return@LaunchedEffect
        val cached = ArtPaletteCache.get(imageUrl)
        if (cached != null) {
            seed = cached
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            // The size the artwork is *displayed* at, deliberately: the fetch
            // then shares a disk-cache entry with the row, card or backdrop
            // drawing the same artwork, instead of pulling its own copy over
            // the wire — which is the difference between a surface that is
            // tinted as it opens and one that turns colour a second later.
            .data(imageUrl.artworkAt(artPx))
            .size(PALETTE_PX) // palette quality holds up here, and it's far faster
            .allowHardware(false) // Palette needs pixel access
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return@LaunchedEffect
        // Quantising 128² pixels is not free, and this coroutine is on the main
        // dispatcher — left there it stutters whatever is animating the surface in.
        val found = withContext(Dispatchers.Default) { extractArtPaletteSeed(bitmap) } ?: return@LaunchedEffect
        ArtPaletteCache.put(imageUrl, found)
        seed = found
    }

    val target = seed?.toPalette(dark) ?: ArtworkPalette(
        background = scheme.background,
        wash = scheme.background,
        elevated = scheme.surfaceVariant,
        accent = scheme.primary,
        onBackground = scheme.onBackground,
        onBackgroundVariant = scheme.onSurfaceVariant,
        divider = scheme.outline,
    )

    val spec: AnimationSpec<Color> = if (reduceAnimation || knownUpFront) {
        snap()
    } else {
        tween(TINT_FADE_MS)
    }
    return ArtworkPalette(
        background = animateColorAsState(target.background, spec, label = "tintBackground").value,
        wash = animateColorAsState(target.wash, spec, label = "tintWash").value,
        elevated = animateColorAsState(target.elevated, spec, label = "tintElevated").value,
        accent = animateColorAsState(target.accent, spec, label = "tintAccent").value,
        onBackground = target.onBackground,
        onBackgroundVariant = target.onBackgroundVariant,
        divider = target.divider,
    )
}

private const val PALETTE_PX = 128

/** Short: this is a surface settling into its colour, not an effect in itself. */
private const val TINT_FADE_MS = 260

private fun ArtPaletteSeed.toPalette(dark: Boolean): ArtworkPalette = if (dark) {
    ArtworkPalette(
        // Deep enough that white body text clears contrast on any sleeve, but
        // not so deep the hue is gone — the whole point is that the page is
        // recognisably *this* record's colour.
        background = dominant.withHsl(saturation = { it.coerceIn(0.20f, 0.62f) }, lightness = { 0.13f }),
        // Follows the edge's own brightness within a band that stays clear of
        // white body text at the top and of [background] at the bottom: a
        // sleeve that ends dark hands over almost invisibly, one that ends
        // bright leaves a page that is visibly lit from under the artwork.
        wash = bottomEdge.withHsl(
            saturation = { it.coerceIn(0.18f, 0.58f) },
            lightness = { it.coerceIn(0.14f, 0.24f) },
        ),
        elevated = dominant.withHsl(saturation = { it.coerceIn(0.20f, 0.62f) }, lightness = { 0.22f }),
        accent = vibrant.withHsl(
            saturation = { it.coerceAtLeast(0.55f) },
            lightness = { it.coerceIn(0.62f, 0.78f) },
        ),
        onBackground = Color.White,
        // Well above the grey the untinted screens use for secondary text. A
        // tint is a *coloured* background, not a black one, so the contrast a
        // dim grey has against black is not the contrast it has here — artist
        // names were sinking into the wash on mid-toned sleeves.
        onBackgroundVariant = Color.White.copy(alpha = 0.80f),
        divider = Color.White.copy(alpha = 0.12f),
    )
} else {
    ArtworkPalette(
        background = dominant.withHsl(saturation = { it.coerceIn(0.14f, 0.50f) }, lightness = { 0.91f }),
        wash = bottomEdge.withHsl(
            saturation = { it.coerceIn(0.12f, 0.46f) },
            lightness = { it.coerceIn(0.78f, 0.90f) },
        ),
        elevated = dominant.withHsl(saturation = { it.coerceIn(0.14f, 0.50f) }, lightness = { 0.83f }),
        accent = vibrant.withHsl(
            saturation = { it.coerceAtLeast(0.55f) },
            lightness = { it.coerceIn(0.30f, 0.44f) },
        ),
        onBackground = Color.Black,
        onBackgroundVariant = Color.Black.copy(alpha = 0.70f),
        divider = Color.Black.copy(alpha = 0.10f),
    )
}

private fun Color.withHsl(
    saturation: (Float) -> Float = { it },
    lightness: (Float) -> Float = { it },
): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[1] = saturation(hsl[1]).coerceIn(0f, 1f)
    hsl[2] = lightness(hsl[2]).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Perceived brightness, used only to tell a dark theme from a light one. */
private fun Color.luminance(): Float = ColorUtils.calculateLuminance(toArgb()).toFloat()
