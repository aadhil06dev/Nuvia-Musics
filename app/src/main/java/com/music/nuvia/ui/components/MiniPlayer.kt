package com.music.nuvia.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.nuvia.data.model.ROW_ART_PX
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.artworkAt
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs

private val GLYPH_SLOT = 36.dp
private val ROW_PADDING = 6.dp
private val ART_CORNER = 10.dp
private val BAR_SHAPE = RoundedCornerShape(percent = 50)

/**
 * NUViA Liquid-Glass Mini Player.
 * Floats elegantly above the floating tab bar with dynamic artwork reflection,
 * tactile controls, smooth swipe gestures, and an integrated progress track.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onPrevious: (() -> Unit)? = null,
    progress: Float = 0f,
    progressProvider: (() -> Float)? = null,
    ownBackdrop: Boolean = false,
) {
    val nuviaColors = LocalNUViAColors.current
    val density = LocalDensity.current
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = BAR_SHAPE

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "miniPlayerDragOffset",
    )

    val verticalSwipeThreshold = with(density) { 32.dp.toPx() }
    val horizontalSwipeThreshold = with(density) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .padding(horizontal = 0.dp, vertical = 4.dp)
            .nuviaAmbientBacklight(nuviaColors.ambientGlow, radiusDp = 20.dp, alpha = 0.22f)
            .graphicsLayer {
                translationX = animatedOffsetX
            }
            .nuviaGlass(
                tier = NUViAGlassTier.Elevated,
                shape = shape,
                hazeState = hazeState,
            )
            .pointerInput(onExpand, onNext, onPrevious) {
                var totalX = 0f
                var totalY = 0f
                var isVerticalDirection: Boolean? = null

                detectDragGestures(
                    onDragStart = {
                        totalX = 0f
                        totalY = 0f
                        isVerticalDirection = null
                    },
                    onDragEnd = {
                        if (isVerticalDirection == true && totalY < -verticalSwipeThreshold) {
                            onExpand()
                        } else if (isVerticalDirection == false) {
                            if (totalX < -horizontalSwipeThreshold) {
                                onNext()
                            } else if (totalX > horizontalSwipeThreshold && onPrevious != null) {
                                onPrevious()
                            }
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        totalX += dragAmount.x
                        totalY += dragAmount.y

                        if (isVerticalDirection == null) {
                            val absX = abs(totalX)
                            val absY = abs(totalY)
                            if (absY > 10f || absX > 10f) {
                                isVerticalDirection = absY > absX
                            }
                        }

                        if (isVerticalDirection == false) {
                            change.consume()
                            dragOffsetX = totalX * 0.32f
                        } else if (isVerticalDirection == true && totalY < -verticalSwipeThreshold) {
                            change.consume()
                        }
                    },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(ART_CORNER))
                    .nuviaSpecularBorder(
                        shape = RoundedCornerShape(ART_CORNER),
                        width = 0.75.dp,
                        topHighlight = if (nuviaColors.isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.65f),
                        bottomBorder = if (nuviaColors.isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.12f),
                    )
                    .background(nuviaColors.surfaceCard),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                    ),
                    color = nuviaColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isLoading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = nuviaColors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                LiquidGlassIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    onClick = onPlayPause,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    size = GLYPH_SLOT,
                    iconSize = 22.dp,
                    tint = nuviaColors.textPrimary,
                    tier = NUViAGlassTier.Elevated,
                    hazeState = hazeState,
                )
            }
            Spacer(Modifier.width(8.dp))
            LiquidGlassIconButton(
                icon = Icons.Rounded.SkipNext,
                onClick = onNext,
                contentDescription = "Next",
                size = GLYPH_SLOT,
                iconSize = 22.dp,
                tint = nuviaColors.primary,
                tier = NUViAGlassTier.Subtle,
                hazeState = hazeState,
            )
        }

        // Integrated bottom playback progress track - isolated into separate composable
        // so real-time progress recompositions do not invalidate the rest of MiniPlayer.
        MiniPlayerProgressBar(
            progressProvider = progressProvider ?: { progress },
            primaryColor = nuviaColors.primary,
            secondaryColor = nuviaColors.secondary,
        )
    }
}

@Composable
private fun BoxScope.MiniPlayerProgressBar(
    progressProvider: () -> Float,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
) {
    val currentProgress = progressProvider()
    if (currentProgress > 0.001f) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(currentProgress.coerceIn(0f, 1f))
                .height(2.dp)
                .clip(BAR_SHAPE)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            secondaryColor.copy(alpha = 0.85f),
                            primaryColor,
                        ),
                    ),
                ),
        )
    }
}


