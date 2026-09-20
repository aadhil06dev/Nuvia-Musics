package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.lyrics.LyricsSource
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import dev.chrisbanes.haze.HazeState

/**
 * Compact, scrollable Liquid Glass bottom sheet for configuring lyrics source priority and participation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSourcesDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val savedOrder by AppSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val prioritizeSyllableSync by AppSettings.prioritizeSyllableSync.collectAsStateWithLifecycle()
    val nuviaColors = LocalNUViAColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF4101115),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
            }

            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Lyrics Sources",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Tried in priority order — highest provider to respond wins. Drag handle to reorder, tap row to toggle.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Scrollable source list with maximum height bound
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                ReorderableSourceList(
                    order = savedOrder,
                    selected = selected,
                    onReorder = AppSettings::setLyricsSourceOrder,
                    onToggle = { source ->
                        val checked = source in selected
                        if (checked && selected.size <= 1) return@ReorderableSourceList
                        AppSettings.setLyricsSources(
                            if (checked) selected - source else selected + source,
                        )
                    },
                )
            }

            HorizontalDivider(
                color = nuviaColors.divider,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            SyllableSyncToggle(
                checked = prioritizeSyllableSync,
                onToggle = { AppSettings.setPrioritizeSyllableSync(!prioritizeSyllableSync) },
            )

            HorizontalDivider(
                color = nuviaColors.divider,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0x1AFFFFFF))
                        .clickable { AppSettings.resetLyricsSourceSettings() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = nuviaColors.textPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(nuviaColors.primary)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = nuviaColors.onPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Whether a merely line-synced answer is good enough on its own, or worth
 * holding out on for a word-synced one further down the priority order —
 * see the note on [AppSettings.prioritizeSyllableSync]. A single row rather
 * than one more entry in the checkable list above: this isn't a source to
 * ask or not, it's a rule about what to do once one has answered.
 */
@Composable
private fun SyllableSyncToggle(checked: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_HEIGHT)
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onToggle,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Prioritize Syllable Lyrics",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Keep searching past a whole-line match for a word-by-word one, " +
                    "wherever it falls in the order above",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.width(10.dp))
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Enabled",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * The checkable, drag-reorderable list of sources.
 *
 * Reordering is entirely local until a drag ends — [liveOrder] tracks the
 * list as rows are dragged past each other, and only the finished order is
 * written back through [onReorder]. Writing on every intermediate swap would
 * mean [AppSettings] round-tripping the list back down through
 * [savedOrder][AppSettings.lyricsSourceOrder] on every frame of a drag, fighting
 * the gesture that produced it.
 *
 * The drag keeps exactly two numbers: how far the finger has come since it
 * went down ([totalDrag]), and which slot it went down on ([startIndex]).
 * Where to draw the row and which slot it belongs in are both *derived* from
 * those, so neither can drift from the other however many swaps happen on the
 * way. See [SWAP_THRESHOLD] for why the crossing point is past the halfway
 * mark rather than on it.
 */
@Composable
private fun ReorderableSourceList(
    order: List<LyricsSource>,
    selected: Set<LyricsSource>,
    onReorder: (List<LyricsSource>) -> Unit,
    onToggle: (LyricsSource) -> Unit,
) {
    var liveOrder by remember(order) { mutableStateOf(order) }
    var draggedSource by remember { mutableStateOf<LyricsSource?>(null) }
    /** Distance the finger has covered since this gesture began, in pixels. */
    var totalDrag by remember { mutableStateOf(0f) }
    /** Which slot of [liveOrder] it began on. */
    var startIndex by remember { mutableStateOf(0) }

    // The distance from one row's top to the next one's — which is the row
    // *plus* the hairline above it, not the row alone. Measured off a wrapper
    // holding both, because measuring the row by itself left every swap
    // short by the width of a rule and the error compounded down the list.
    //
    // All the rows are the same height by construction (one line of label,
    // one of detail, both capped), so whichever reports last is as good as
    // any other; [lockedPitchPx] then freezes it for the duration of a
    // gesture, so a relayout mid-drag can't move the boundaries the drag is
    // being measured against underneath it.
    var pitchPx by remember { mutableStateOf(0f) }
    var lockedPitchPx by remember { mutableStateOf(0f) }

    Column {
        liveOrder.forEach { source ->
            // Without this, Compose matches each row to its slot by position
            // rather than by which source it is — so the instant a swap moved
            // a different [LyricsSource] into the slot the finger was on,
            // that slot's `pointerInput` saw its key change and restarted the
            // coroutine mid-gesture, which is indistinguishable from letting
            // go: the touch kept moving but nothing was listening anymore,
            // and the drag stalled one swap after it started. Keying the
            // whole row on the value it represents is what keeps *this
            // composable*, gesture and all, following that value from slot to
            // slot instead of being torn down and rebuilt in place.
            key(source) {
                val checked = source in selected
                // The last one enabled can't be unticked — see the guard in
                // [onToggle] — so it reads the same disabled way the toggle
                // itself already treats it, rather than looking clickable and
                // silently doing nothing.
                val toggleable = !checked || selected.size > 1
                val dragging = source == draggedSource
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .onSizeChanged { pitchPx = it.height.toFloat() }
                        .graphicsLayer {
                            // Read here rather than in composition: this runs
                            // once a frame in the draw phase, so a drag moves
                            // the row without recomposing the list at all.
                            //
                            // The row sits wherever the finger has carried it
                            // from where it was picked up, less whatever the
                            // swaps have already moved its slot — so a swap
                            // relocates the slot and shortens this offset by
                            // exactly as much, and the row does not budge.
                            translationY = if (dragging) {
                                totalDrag - (liveOrder.indexOf(source) - startIndex) * lockedPitchPx
                            } else {
                                0f
                            }
                        },
                ) {
                    AlertRule()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ACTION_HEIGHT)
                            .clickable(
                                enabled = toggleable,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onToggle(source) },
                            )
                            .padding(start = 4.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(18.dp)
                                // A constant key on purpose — see the note above.
                                // The row this coroutine belongs to is now pinned
                                // by [key], so nothing about a reorder should ever
                                // restart it; only the handle's own identity
                                // (there is exactly one, for its whole lifetime)
                                // needs to.
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedSource = source
                                            totalDrag = 0f
                                            startIndex = liveOrder.indexOf(source)
                                            lockedPitchPx = pitchPx
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            val pitch = lockedPitchPx
                                            if (pitch <= 0f) return@detectDragGestures
                                            var index = liveOrder.indexOf(source)
                                            if (index < 0) return@detectDragGestures
                                            // Held past either end the row stops
                                            // there under the finger, rather than
                                            // running off the list and having to
                                            // be dragged all the way back before
                                            // it answers again.
                                            totalDrag = (totalDrag + delta.y).coerceIn(
                                                -startIndex * pitch,
                                                (liveOrder.lastIndex - startIndex) * pitch,
                                            )
                                            // A loop, not an `if`: one pointer
                                            // event can cover several rows when
                                            // the finger is quick, and settling
                                            // one row per event would leave the
                                            // list trailing the drag.
                                            while (true) {
                                                val travelled = totalDrag / pitch
                                                val moved = (index - startIndex).toFloat()
                                                if (travelled > moved + SWAP_THRESHOLD && index < liveOrder.lastIndex) {
                                                    liveOrder = liveOrder.toMutableList().apply {
                                                        add(index + 1, removeAt(index))
                                                    }
                                                    index++
                                                } else if (travelled < moved - SWAP_THRESHOLD && index > 0) {
                                                    liveOrder = liveOrder.toMutableList().apply {
                                                        add(index - 1, removeAt(index))
                                                    }
                                                    index--
                                                } else {
                                                    break
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedSource = null
                                            totalDrag = 0f
                                            onReorder(liveOrder)
                                        },
                                        onDragCancel = {
                                            draggedSource = null
                                            totalDrag = 0f
                                            liveOrder = order
                                        },
                                    )
                                },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = source.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = if (toggleable) 1f else 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = source.detail,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        if (checked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Enabled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * How far past a neighbour the finger has to carry a row before the two trade
 * places, as a share of one row's pitch.
 *
 * Deliberately more than half. At exactly half, a row that has just swapped
 * lands with its offset sitting precisely on the boundary of swapping *back* —
 * so a single pixel of the shake any real finger has flipped it, and the
 * compensating shift put it straight back on the forward boundary again. The
 * row juddered between two slots for as long as it was held near a crossing,
 * which is the "loops up and down in the same position" this fixes. Anything
 * over half opens a gap between the two boundaries; a tenth of a row is enough
 * to swallow the shake without the swap feeling reluctant.
 */
private const val SWAP_THRESHOLD = 0.6f
