package com.music.nuvia.ui.components

import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

/**
 * Keeps Haze's visual style while reducing its sampling resolution to 0.33x.
 * Haze 1.x otherwise processes every effect at full resolution, even when a
 * large blur makes those extra source pixels invisible.
 *
 * A third rather than [HazeInputScale.Auto]. Auto only steps the resolution down
 * once the blur radius is large enough for it to be confident, which leaves
 * floating surfaces sampling at full resolution on every frame of every scroll.
 * 0.33x (nine times fewer pixels) cuts GPU load drastically, while blur hides
 * the upscaling. [block] can still override it per call site.
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.optimizedHazeEffect(
    state: HazeState,
    style: HazeStyle = HazeStyle.Unspecified,
    block: (HazeEffectScope.() -> Unit)? = null,
): Modifier = hazeEffect(state, style) {
    inputScale = HazeInputScale.Fixed(0.33f)
    block?.invoke(this)
}

