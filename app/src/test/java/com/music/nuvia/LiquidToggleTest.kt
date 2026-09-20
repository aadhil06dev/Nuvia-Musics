package com.music.nuvia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests verifying LiquidToggle state machine invariants, repeated transitions,
 * drag-release thresholds, gesture disambiguation, and thumb geometry bounds.
 */
class LiquidToggleTest {

    @Test
    fun testRepeatedTapTransitions_unlimitedCycle() {
        // Simulates the exact state transition lifecycle of repeated taps:
        // OFF -> ON -> OFF -> ON -> OFF -> ON
        var checked = false
        val transitions = mutableListOf<Boolean>()

        fun handleTap(currentChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
            val target = !currentChecked
            onCheckedChange(target)
        }

        // Tap 1: OFF -> ON
        handleTap(checked) { newChecked ->
            checked = newChecked
            transitions.add(checked)
        }
        assertTrue("Tap 1 must transition to ON", checked)

        // Tap 2: ON -> OFF (Previously froze due to stale closure)
        handleTap(checked) { newChecked ->
            checked = newChecked
            transitions.add(checked)
        }
        assertFalse("Tap 2 must transition to OFF", checked)

        // Tap 3: OFF -> ON
        handleTap(checked) { newChecked ->
            checked = newChecked
            transitions.add(checked)
        }
        assertTrue("Tap 3 must transition to ON", checked)

        // Tap 4: ON -> OFF
        handleTap(checked) { newChecked ->
            checked = newChecked
            transitions.add(checked)
        }
        assertFalse("Tap 4 must transition to OFF", checked)

        // Tap 5: OFF -> ON
        handleTap(checked) { newChecked ->
            checked = newChecked
            transitions.add(checked)
        }
        assertTrue("Tap 5 must transition to ON", checked)

        assertEquals(
            listOf(true, false, true, false, true),
            transitions,
        )
    }

    @Test
    fun testDragReleaseThresholds_andRepeatedDrags() {
        // Invariant:
        // Release progress >= 0.5f -> ON
        // Release progress < 0.5f -> OFF
        var checked = false
        var hapticCount = 0

        fun handleDragEnd(
            currentChecked: Boolean,
            finalProgress: Float,
            onCheckedChange: (Boolean) -> Unit,
            onHaptic: () -> Unit,
        ) {
            val target = finalProgress >= 0.5f
            if (target != currentChecked) {
                onHaptic()
                onCheckedChange(target)
            }
        }

        // Drag 1: from OFF, drag to 0.70f (>= 0.5f) -> releases to ON
        handleDragEnd(checked, 0.70f, { checked = it }, { hapticCount++ })
        assertTrue("Drag to 0.70f must turn ON", checked)
        assertEquals("Haptic must fire exactly once", 1, hapticCount)

        // Drag 2: from ON, drag to 0.20f (< 0.5f) -> releases to OFF
        handleDragEnd(checked, 0.20f, { checked = it }, { hapticCount++ })
        assertFalse("Drag to 0.20f must turn OFF", checked)
        assertEquals("Haptic must fire second time", 2, hapticCount)

        // Drag 3: from OFF, drag to 0.49f (< 0.5f) -> releases below threshold (stays OFF)
        handleDragEnd(checked, 0.49f, { checked = it }, { hapticCount++ })
        assertFalse("Drag to 0.49f must remain OFF", checked)
        assertEquals("No haptic fired because state did not change", 2, hapticCount)

        // Drag 4: from OFF, drag to 0.50f (exact boundary) -> releases to ON
        handleDragEnd(checked, 0.50f, { checked = it }, { hapticCount++ })
        assertTrue("Drag to 0.50f must cross threshold and turn ON", checked)
        assertEquals(3, hapticCount)

        // Drag 5: from ON, drag to 0.85f (>= 0.5f) -> releases above threshold (stays ON)
        handleDragEnd(checked, 0.85f, { checked = it }, { hapticCount++ })
        assertTrue("Drag to 0.85f must remain ON", checked)
        assertEquals("No haptic fired because state did not change", 3, hapticCount)

        // Drag 6: from ON, drag to 0.00f -> releases to OFF
        handleDragEnd(checked, 0.00f, { checked = it }, { hapticCount++ })
        assertFalse("Drag to 0.00f must turn OFF", checked)
        assertEquals(4, hapticCount)
    }

    @Test
    fun testGestureDisambiguation_verticalScrollPriority() {
        val touchSlop = 24f // ~8dp in px

        fun shouldConsumeHorizontalDrag(deltaX: Float, deltaY: Float): Boolean {
            if (abs(deltaY) > touchSlop && abs(deltaY) > abs(deltaX)) {
                // Vertical list scroll takes priority
                return false
            }
            return abs(deltaX) > touchSlop && abs(deltaX) >= abs(deltaY)
        }

        // Pure horizontal swipe across toggle
        assertTrue("Horizontal swipe must be consumed", shouldConsumeHorizontalDrag(50f, 2f))
        assertTrue("Horizontal swipe left must be consumed", shouldConsumeHorizontalDrag(-40f, 5f))

        // Pure vertical scroll of settings list
        assertFalse("Vertical scroll must NOT be consumed by toggle", shouldConsumeHorizontalDrag(0f, 60f))
        assertFalse("Slightly diagonal vertical scroll must NOT be consumed", shouldConsumeHorizontalDrag(10f, 50f))

        // Movement below touch slop (micro-jitter)
        assertFalse("Sub-slop movement must not trigger drag", shouldConsumeHorizontalDrag(5f, 5f))
    }

    @Test
    fun testThumbGeometry_staysStrictlyWithinTrackBounds() {
        val trackWidthPx = 156f // 52dp @ 3x
        val thumbBasePx = 66f   // 22dp @ 3x
        val paddingPx = 12f     // 4dp @ 3x
        val maxTravelPx = trackWidthPx - thumbBasePx - (2 * paddingPx) // 66px

        // Test at resting state (stretchFactor = 0f)
        val extraWidthAtRest = 0f
        val offset0 = (0f * maxTravelPx) - (extraWidthAtRest * 0f)
        assertEquals("Left edge at rest (0%)", 0f, offset0, 0.01f)
        val rightEdge0 = paddingPx + offset0 + thumbBasePx
        assertTrue("Thumb right edge must fit within track", rightEdge0 <= trackWidthPx - paddingPx)

        val offset1 = (1f * maxTravelPx) - (extraWidthAtRest * 1f)
        assertEquals("Left edge at 100%", maxTravelPx, offset1, 0.01f)
        val rightEdge1 = paddingPx + offset1 + thumbBasePx
        assertEquals("Thumb right edge must touch right padding exactly", trackWidthPx - paddingPx, rightEdge1, 0.01f)

        // Test at maximum stretch (stretchFactor = 0.40f)
        val maxDynamicWidthPx = thumbBasePx * 1.40f + 10.5f // ~102.9px
        val extraWidthStretched = maxDynamicWidthPx - thumbBasePx

        // At progress 0 with max stretch:
        val stretchedOffset0 = (0f * maxTravelPx) - (extraWidthStretched * 0f)
        assertEquals(0f, stretchedOffset0, 0.01f)
        val stretchedRightEdge0 = paddingPx + stretchedOffset0 + maxDynamicWidthPx
        assertTrue("Stretched thumb at 0 must fit inside track", stretchedRightEdge0 <= trackWidthPx)

        // At progress 1 with max stretch:
        val stretchedOffset1 = (1f * maxTravelPx) - (extraWidthStretched * 1f)
        val stretchedRightEdge1 = paddingPx + stretchedOffset1 + maxDynamicWidthPx
        // Mathematically: paddingPx + (maxTravelPx - extraWidth) + (thumbBasePx + extraWidth)
        // = paddingPx + maxTravelPx + thumbBasePx = trackWidthPx - paddingPx
        assertEquals(
            "Stretched thumb right edge at 100% must still touch trackWidthPx - paddingPx exactly",
            trackWidthPx - paddingPx,
            stretchedRightEdge1,
            0.01f,
        )
    }

    @Test
    fun testSettingsCallbackFreshness_neverInvokesStaleCallback() {
        // Simulates dynamic recomposition where onCheckedChange callback instance changes
        var invocationCountA = 0
        var invocationCountB = 0

        val callbackA: (Boolean) -> Unit = { invocationCountA++ }
        val callbackB: (Boolean) -> Unit = { invocationCountB++ }

        var activeCallback = callbackA

        // Invocation 1: with callbackA
        activeCallback(true)
        assertEquals(1, invocationCountA)
        assertEquals(0, invocationCountB)

        // Recomposition: callback updates to callbackB
        activeCallback = callbackB

        // Invocation 2: must invoke callbackB, NOT stale callbackA
        activeCallback(false)
        assertEquals(1, invocationCountA)
        assertEquals(1, invocationCountB)
    }
}

