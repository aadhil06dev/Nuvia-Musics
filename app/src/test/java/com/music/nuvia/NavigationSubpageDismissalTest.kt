package com.music.nuvia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSubpageDismissalTest {

    data class NavigationState(
        var selectedTab: String = "home",
        var showReplay: Boolean = false,
        var showHistory: Boolean = false,
        var showEqualizer: Boolean = false,
        var showListenTogether: Boolean = false,
        var showSettings: Boolean = false,
        var showDownloadManager: Boolean = false,
    ) {
        fun dismissAllSubpages() {
            showReplay = false
            showHistory = false
            showEqualizer = false
            showListenTogether = false
            showSettings = false
            showDownloadManager = false
        }

        fun onTabSelected(tab: String) {
            dismissAllSubpages()
            selectedTab = tab
        }

        val activeScreenKey: String
            get() = when {
                showReplay -> "replay"
                showHistory -> "history"
                showEqualizer -> "equalizer"
                showListenTogether -> "listen_together"
                showSettings -> "settings"
                showDownloadManager -> "download_manager"
                else -> selectedTab
            }
    }

    @Test
    fun tappingTabDismissesReplayAndSwitchesContent() {
        val state = NavigationState()
        // User opens Replay
        state.showReplay = true
        assertEquals("replay", state.activeScreenKey)

        // User taps "explore" bottom bar tab
        state.onTabSelected("explore")
        assertFalse(state.showReplay)
        assertEquals("explore", state.selectedTab)
        assertEquals("explore", state.activeScreenKey)
    }

    @Test
    fun tappingTabDismissesAnySubpage() {
        val state = NavigationState()
        state.showEqualizer = true
        assertEquals("equalizer", state.activeScreenKey)

        state.onTabSelected("library")
        assertFalse(state.showEqualizer)
        assertEquals("library", state.activeScreenKey)
    }
}

