package com.music.nuvia

import com.music.nuvia.ui.navigation.TabNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TabNavigationTest {

    @Test
    fun testDestinationIndices() {
        assertEquals(0, TabNavigation.TAB_HOME)
        assertEquals(1, TabNavigation.TAB_EXPLORE)
        assertEquals(2, TabNavigation.TAB_LIBRARY)
        assertEquals(3, TabNavigation.TAB_SEARCH)
    }

    @Test
    fun testParseTabIndex() {
        assertEquals(0, TabNavigation.parseTabIndex("tab:0"))
        assertEquals(1, TabNavigation.parseTabIndex("tab:1"))
        assertEquals(2, TabNavigation.parseTabIndex("tab:2"))
        assertEquals(3, TabNavigation.parseTabIndex("tab:3"))
        assertNull(TabNavigation.parseTabIndex("discord"))
        assertNull(TabNavigation.parseTabIndex("settings"))
        assertNull(TabNavigation.parseTabIndex("sources"))
        assertNull(TabNavigation.parseTabIndex("tab:abc"))
        assertNull(TabNavigation.parseTabIndex(""))
    }

    @Test
    fun testForwardTransitions() {
        // Direct forward
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_HOME, TabNavigation.TAB_EXPLORE),
        )
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_EXPLORE, TabNavigation.TAB_LIBRARY),
        )
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_LIBRARY, TabNavigation.TAB_SEARCH),
        )

        // Skipped forward
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_HOME, TabNavigation.TAB_LIBRARY),
        )
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_HOME, TabNavigation.TAB_SEARCH),
        )
        assertEquals(
            TabNavigation.Direction.FORWARD,
            TabNavigation.direction(TabNavigation.TAB_EXPLORE, TabNavigation.TAB_SEARCH),
        )
    }

    @Test
    fun testReverseTransitions() {
        // Direct reverse
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_SEARCH, TabNavigation.TAB_LIBRARY),
        )
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_LIBRARY, TabNavigation.TAB_EXPLORE),
        )
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_EXPLORE, TabNavigation.TAB_HOME),
        )

        // Skipped reverse (crucial test: Library -> Home must be reverse!)
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_LIBRARY, TabNavigation.TAB_HOME),
        )
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_SEARCH, TabNavigation.TAB_HOME),
        )
        assertEquals(
            TabNavigation.Direction.REVERSE,
            TabNavigation.direction(TabNavigation.TAB_SEARCH, TabNavigation.TAB_EXPLORE),
        )
    }

    @Test
    fun testSameDestination() {
        assertEquals(
            TabNavigation.Direction.SAME,
            TabNavigation.direction(TabNavigation.TAB_HOME, TabNavigation.TAB_HOME),
        )
        assertEquals(
            TabNavigation.Direction.SAME,
            TabNavigation.direction(TabNavigation.TAB_SEARCH, TabNavigation.TAB_SEARCH),
        )
    }

    @Test
    fun testTabTransitionCreation() {
        // Ensure transition creation succeeds without throwing
        val forwardTransform = TabNavigation.tabTransition(
            initialIndex = TabNavigation.TAB_HOME,
            targetIndex = TabNavigation.TAB_EXPLORE,
            reduceMotion = false,
        )
        assertNotNull(forwardTransform)

        val reverseTransform = TabNavigation.tabTransition(
            initialIndex = TabNavigation.TAB_LIBRARY,
            targetIndex = TabNavigation.TAB_HOME,
            reduceMotion = false,
        )
        assertNotNull(reverseTransform)

        val reducedMotionTransform = TabNavigation.tabTransition(
            initialIndex = TabNavigation.TAB_HOME,
            targetIndex = TabNavigation.TAB_SEARCH,
            reduceMotion = true,
        )
        assertNotNull(reducedMotionTransform)

        val sameTransform = TabNavigation.tabTransition(
            initialIndex = TabNavigation.TAB_HOME,
            targetIndex = TabNavigation.TAB_HOME,
            reduceMotion = false,
        )
        assertNotNull(sameTransform)
    }
}
