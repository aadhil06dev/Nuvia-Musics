/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package com.music.nuvia.ui.components.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.music.nuvia.ui.components.backdrop.BackdropEffectScope
import com.music.nuvia.ui.components.backdrop.internal.RoundedRectRefractionShaderString
import com.music.nuvia.ui.components.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import com.music.nuvia.ui.components.backdrop.internal.RuntimeShaderEffect
import com.music.nuvia.ui.components.backdrop.isRuntimeShaderSupported

fun BackdropEffectScope.lens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    val currentSize = size
    if (!currentSize.isSpecified || currentSize.width <= 0f || currentSize.height <= 0f) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader =
                if (!chromaticAberration) {
                    obtainRuntimeShader(
                        "Refraction",
                        RoundedRectRefractionShaderString
                    )
                } else {
                    obtainRuntimeShader(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    )
                }
            shader.apply {
                setFloatUniform("size", currentSize.width, currentSize.height)
                setFloatUniform("offset", -padding, -padding)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("refractionAmount", -refractionAmount)
                setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
                if (chromaticAberration) {
                    setFloatUniform("chromaticAberration", 1f)
                }
            }
            RuntimeShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

// Vendored change: support for io.github.kyant0:shapes' RoundedRectangularShape was
// dropped so the vendored sources have no external dependency; this app only passes
// CornerBasedShape here.
private val BackdropEffectScope.cornerRadii: FloatArray?
    get() {
        val currentSize = size
        if (!currentSize.isSpecified || currentSize.width <= 0f || currentSize.height <= 0f) return null
        return when (val shape = shape) {
            is AbsoluteRoundedCornerShape -> {
                val maxRadius = currentSize.minDimension / 2f
                val topLeft = shape.topStart.toPx(currentSize, this)
                val topRight = shape.topEnd.toPx(currentSize, this)
                val bottomRight = shape.bottomEnd.toPx(currentSize, this)
                val bottomLeft = shape.bottomStart.toPx(currentSize, this)
                floatArrayOf(
                    topLeft.fastCoerceAtMost(maxRadius),
                    topRight.fastCoerceAtMost(maxRadius),
                    bottomRight.fastCoerceAtMost(maxRadius),
                    bottomLeft.fastCoerceAtMost(maxRadius)
                )
            }

            is CornerBasedShape -> {
                val maxRadius = currentSize.minDimension / 2f
                val isLtr = layoutDirection == LayoutDirection.Ltr
                val topLeft =
                    if (isLtr) shape.topStart.toPx(currentSize, this)
                    else shape.topEnd.toPx(currentSize, this)
                val topRight =
                    if (isLtr) shape.topEnd.toPx(currentSize, this)
                    else shape.topStart.toPx(currentSize, this)
                val bottomRight =
                    if (isLtr) shape.bottomEnd.toPx(currentSize, this)
                    else shape.bottomStart.toPx(currentSize, this)
                val bottomLeft =
                    if (isLtr) shape.bottomStart.toPx(currentSize, this)
                    else shape.bottomEnd.toPx(currentSize, this)
                floatArrayOf(
                    topLeft.fastCoerceAtMost(maxRadius),
                    topRight.fastCoerceAtMost(maxRadius),
                    bottomRight.fastCoerceAtMost(maxRadius),
                    bottomLeft.fastCoerceAtMost(maxRadius)
                )
            }

            else -> null
        }
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException(
        "Only RoundedRectangularShape or CornerBasedShape is supported in lens effects."
    )
}
