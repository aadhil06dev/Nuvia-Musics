package com.music.nuvia.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlin.math.roundToInt

/**
 * Ordered primary navigation destinations and transition helpers for NUViA.
 *
 * Ordered destinations:
 * - Home = 0
 * - Explore = 1
 * - Library = 2
 * - Search = 3
 *
 * Direction logic:
 * - destinationIndex > currentIndex: FORWARD (animate toward left)
 * - destinationIndex < currentIndex: REVERSE (animate toward right)
 * - destinationIndex == currentIndex: SAME
 */
object TabNavigation {
    const val TAB_HOME = 0
    const val TAB_EXPLORE = 1
    const val TAB_LIBRARY = 2
    const val TAB_SEARCH = 3

    enum class Direction {
        FORWARD,
        REVERSE,
        SAME,
    }

    fun direction(initialIndex: Int, targetIndex: Int): Direction = when {
        targetIndex > initialIndex -> Direction.FORWARD
        targetIndex < initialIndex -> Direction.REVERSE
        else -> Direction.SAME
    }

    fun parseTabIndex(stateKey: String): Int? {
        if (!stateKey.startsWith("tab:")) return null
        return stateKey.removePrefix("tab:").toIntOrNull()
    }

    fun tabTransition(
        initialIndex: Int,
        targetIndex: Int,
        reduceMotion: Boolean = false,
    ): ContentTransform {
        if (reduceMotion) {
            return ContentTransform(
                targetContentEnter = fadeIn(animationSpec = tween(120)),
                initialContentExit = fadeOut(animationSpec = tween(100)),
                sizeTransform = null,
            )
        }

        val dir = direction(initialIndex, targetIndex)
        if (dir == Direction.SAME) {
            return ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        }

        val isForward = dir == Direction.FORWARD
        val enterOffset = if (isForward) 0.12f else -0.12f
        val exitOffset = if (isForward) -0.10f else 0.10f
        val enterDuration = 220
        val exitDuration = 200

        val enter = fadeIn(animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)) { (it * enterOffset).roundToInt() } +
            scaleIn(initialScale = 0.98f, animationSpec = tween(enterDuration, easing = FastOutSlowInEasing))

        val exit = fadeOut(animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)) { (it * exitOffset).roundToInt() } +
            scaleOut(targetScale = 0.985f, animationSpec = tween(exitDuration, easing = FastOutSlowInEasing))

        return ContentTransform(
            targetContentEnter = enter,
            initialContentExit = exit,
            sizeTransform = null,
        )
    }
}
