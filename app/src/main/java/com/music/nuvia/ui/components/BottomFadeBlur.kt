package com.music.nuvia.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState

/**
 * The floor the floating bars sit on: the page's own colour, faded in from
 * nothing at the top to solid at the very bottom via a multi-stop cubic gradient.
 *
 * Powered by [BottomFadeScrim]. This eliminates:
 * 1. Overlapping dual blur layers with the floating bottom bar.
 * 2. The dark rectangular box caused by Haze's background rect.
 * 3. Low-resolution pixelation and muddy bands from 0.33x downsampling.
 * 4. High GPU fill-rate cost during 120Hz scrolling.
 */
@Composable
fun BottomFadeBlur(
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    withMiniPlayer: Boolean = false,
    pageColor: Color = MaterialTheme.colorScheme.background,
) {
    BottomFadeScrim(
        modifier = modifier,
        withMiniPlayer = withMiniPlayer,
        pageColor = pageColor,
    )
}
