/*
 * NUViA Liquid Glass - Haze Backdrop Bridge
 *
 * Bridges Compose content recorded via [dev.chrisbanes.haze.hazeSource]
 * into the Kyant0 backdrop rendering pipeline ([Backdrop]), allowing AGSL
 * runtime shaders (lens refraction, dispersion, specular highlights, inner shadows)
 * to sample live screen content recorded by HazeState without duplicating passes.
 */
package com.music.nuvia.ui.components.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import com.music.nuvia.ui.components.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState

/**
 * Creates and remembers a [Backdrop] backed by live [HazeState] content areas.
 */
@Composable
fun rememberHazeBackdrop(hazeState: HazeState): Backdrop {
    return remember(hazeState) {
        HazeBackdrop(hazeState)
    }
}

/**
 * [Backdrop] implementation that samples recorded [GraphicsLayer]s from [hazeState.areas].
 */
@Stable
class HazeBackdrop(val hazeState: HazeState) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val coords = coordinates ?: return
        if (!coords.isAttached) return

        val myPos = try {
            coords.positionOnScreen()
        } catch (_: Exception) {
            try {
                coords.positionInWindow()
            } catch (_: Exception) {
                return
            }
        }

        // Lay down window background floor so plain text and lists blur against
        // an opaque base rather than a transparent void.
        drawRect(Color(0xFF0C0D12))

        val areas = hazeState.areas
        for (i in areas.indices) {
            val area = areas.getOrNull(i) ?: continue
            val layer = area.contentLayer ?: continue
            val areaPos = area.positionOnScreen
            if (!areaPos.isSpecified) continue

            val dx = areaPos.x - myPos.x
            val dy = areaPos.y - myPos.y

            withTransform({
                translate(dx, dy)
            }) {
                drawLayer(layer)
            }
        }
    }
}

