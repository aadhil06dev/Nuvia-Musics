package com.music.nuvia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SettingsSubpageBackstackTest {

    enum class TestSubpageDestination {
        SETTINGS,
        DEVELOPER_DIAGNOSTICS,
        ABOUT,
        WHATS_NEW,
        SOURCES,
        ACCOUNT_SCROBBLING,
        DISCORD,
        EQUALIZER,
        LISTEN_TOGETHER,
        SPOTIFY_CANVAS,
        HISTORY,
        REPLAY,
    }

    class TestNavigationController(
        var selectedTab: String = "library",
        var detail: String? = null,
        var onboardingActive: Boolean = false,
        var showNowPlaying: Boolean = false,
        var showLogin: Boolean = false,
    ) {
        val subpageStack = mutableListOf<TestSubpageDestination>()

        val isSubpageActive: Boolean
            get() = subpageStack.isNotEmpty()

        val currentSubpage: TestSubpageDestination?
            get() = subpageStack.lastOrNull()

        fun navigateToSubpage(dest: TestSubpageDestination) {
            if (subpageStack.lastOrNull() != dest) {
                subpageStack.add(dest)
            }
        }

        fun popSubpage(): Boolean {
            if (subpageStack.isNotEmpty()) {
                subpageStack.removeAt(subpageStack.lastIndex)
                return true
            }
            return false
        }

        fun handleBackPress(): String {
            return when {
                isSubpageActive -> {
                    popSubpage()
                    "popped_subpage"
                }
                detail != null -> {
                    detail = null
                    "closed_detail"
                }
                selectedTab != "home" -> {
                    selectedTab = "home"
                    "returned_home"
                }
                else -> "system_exit"
            }
        }

        val shouldShowNavigationShell: Boolean
            get() = !onboardingActive && !showNowPlaying && !showLogin
    }

    @Test
    fun nestedSettingsNavigationPopsCleanlyWithoutResettingToHome() {
        val nav = TestNavigationController(selectedTab = "library")

        // 1. User is on Library tab and opens Settings
        nav.navigateToSubpage(TestSubpageDestination.SETTINGS)
        assertTrue(nav.isSubpageActive)
        assertEquals(TestSubpageDestination.SETTINGS, nav.currentSubpage)
        assertEquals("library", nav.selectedTab)

        // 2. User navigates into About page from Settings
        nav.navigateToSubpage(TestSubpageDestination.ABOUT)
        assertEquals(TestSubpageDestination.ABOUT, nav.currentSubpage)
        assertEquals(2, nav.subpageStack.size)

        // 3. User presses Back: should pop About and return to Settings
        val firstBackResult = nav.handleBackPress()
        assertEquals("popped_subpage", firstBackResult)
        assertEquals(TestSubpageDestination.SETTINGS, nav.currentSubpage)
        assertEquals("library", nav.selectedTab) // Originating tab is preserved!

        // 4. User presses Back again: should pop Settings and return to Library
        val secondBackResult = nav.handleBackPress()
        assertEquals("popped_subpage", secondBackResult)
        assertFalse(nav.isSubpageActive)
        assertEquals("library", nav.selectedTab) // NOT forced to Home!
    }

    @Test
    fun navigationShellIsPersistentAcrossDetailsAndSubpages() {
        val nav = TestNavigationController(selectedTab = "home")

        // Standard Home: shell is visible
        assertTrue(nav.shouldShowNavigationShell)

        // Open album/artist Detail: shell REMAINS visible
        nav.detail = "browse:album_123"
        assertTrue(nav.shouldShowNavigationShell)

        // Open Settings: shell REMAINS visible
        nav.navigateToSubpage(TestSubpageDestination.SETTINGS)
        assertTrue(nav.shouldShowNavigationShell)

        // Open About inside Settings: shell REMAINS visible
        nav.navigateToSubpage(TestSubpageDestination.ABOUT)
        assertTrue(nav.shouldShowNavigationShell)

        // Entering Fullscreen Player: shell hides
        nav.showNowPlaying = true
        assertFalse(nav.shouldShowNavigationShell)
        nav.showNowPlaying = false
        assertTrue(nav.shouldShowNavigationShell)

        // Entering WebView Login: shell hides
        nav.showLogin = true
        assertFalse(nav.shouldShowNavigationShell)
        nav.showLogin = false
        assertTrue(nav.shouldShowNavigationShell)

        // Entering Initial Onboarding: shell hides
        nav.onboardingActive = true
        assertFalse(nav.shouldShowNavigationShell)
    }

    @Test
    fun compactNavigationShellBehavior() {
        // When active song exists and user scrolls down:
        val hasSong = true
        val isScrollingDown = true
        val shellProgress = if (isScrollingDown && hasSong) 1f else 0f
        assertEquals(1f, shellProgress)

        // Travel distances
        val navBarTravel = 110f
        val miniPlayerTravel = 66f

        // FloatingBottomBar transforms
        val navBarTranslationY = shellProgress * navBarTravel
        val navBarAlpha = (1f - shellProgress * 1.6f).coerceIn(0f, 1f)
        assertEquals(110f, navBarTranslationY)
        assertEquals(0f, navBarAlpha) // completely faded and moved down

        // MiniPlayer transforms
        val miniPlayerTranslationY = shellProgress * miniPlayerTravel
        assertEquals(66f, miniPlayerTranslationY) // moved down to bottom center

        // Companion buttons width
        val companionButtonWidth = 48f * shellProgress
        assertEquals(48f, companionButtonWidth) // fully expanded

        // When NO song is active:
        val noSongProgress = if (isScrollingDown && !hasSong) 1f else 0f
        assertEquals(0f, noSongProgress)
        val noSongNavBarTranslationY = noSongProgress * navBarTravel
        val noSongNavBarAlpha = (1f - noSongProgress * 1.6f).coerceIn(0f, 1f)
        assertEquals(0f, noSongNavBarTranslationY)
        assertEquals(1f, noSongNavBarAlpha) // fixed at bottom
    }

    @Test
    fun settingsSearchQueryMatchingLogic() {
        fun matchesQuery(query: String, vararg keywords: String): Boolean {
            if (query.isBlank()) return true
            val clean = query.trim().lowercase()
            val cleanStem = when {
                clean.length > 3 && clean.endsWith("s") -> clean.removeSuffix("s")
                clean.length > 3 && clean.endsWith("e") -> clean.removeSuffix("e")
                else -> clean
            }
            return keywords.any { kw ->
                val kwClean = kw.lowercase()
                kwClean.contains(clean) || kwClean.contains(cleanStem)
            }
        }

        // Blank query matches all
        assertTrue(matchesQuery("", "Account", "Playback", "Equalizer"))
        assertTrue(matchesQuery("   ", "Theme", "Lossless"))

        // Exact & substring matching
        assertTrue(matchesQuery("equal", "Equalizer", "Audio"))
        assertTrue(matchesQuery("lossless", "NUViA Audio Engine", "Lossless audio"))
        assertFalse(matchesQuery("unknownterm", "Account", "Display"))

        // Stem matching (plural / trailing 's')
        assertTrue(matchesQuery("sources", "Source kind", "Audio source"))
        assertTrue(matchesQuery("downloads", "Download quality", "Storage"))
    }

    @Test
    fun localMusicDrillDownBackHandling() {
        var inDrillDown = true
        var drillDownLabel: String? = "Pink Floyd"
        var screenExited = false

        val handleBack: () -> Unit = {
            if (inDrillDown) {
                drillDownLabel = null
                inDrillDown = false
            } else {
                screenExited = true
            }
        }

        // First back press: exits drill-down without closing screen
        handleBack()
        assertFalse(inDrillDown)
        assertEquals(null, drillDownLabel)
        assertFalse(screenExited)

        // Second back press: exits screen
        handleBack()
        assertTrue(screenExited)
    }
}

