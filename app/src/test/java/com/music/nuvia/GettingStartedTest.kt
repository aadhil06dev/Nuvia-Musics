package com.music.nuvia

import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.screens.getGreetingForCurrentTime
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GettingStartedTest {

    @Before
    fun setUp() {
        AppSettings.onboardingCompleted.value = false
        AppSettings.preferredGenres.value = emptySet()
        AppSettings.preferredMoods.value = emptySet()
        AppSettings.uiGlow.value = false
    }

    @Test
    fun uiGlow_isFalseByDefault() {
        // Invariant: UI Glow must remain OFF by default for pure minimalism.
        assertFalse(
            "uiGlow must be false by default",
            AppSettings.uiGlow.value,
        )
    }

    @Test
    fun onboardingCompleted_isFalseByDefault() {
        assertFalse(
            "onboardingCompleted must be false on fresh launch",
            AppSettings.onboardingCompleted.value,
        )
    }

    @Test
    fun savingPreferences_preservesGenresAndMoods() {
        val testGenres = setOf("Pop", "Electronic", "Lo-fi")
        val testMoods = setOf("Chill", "Focus", "Late Night")

        AppSettings.preferredGenres.value = testGenres
        AppSettings.preferredMoods.value = testMoods

        assertEquals(testGenres, AppSettings.preferredGenres.value)
        assertEquals(testMoods, AppSettings.preferredMoods.value)
        assertFalse(
            "Saving preferences should not prematurely mark onboarding completed",
            AppSettings.onboardingCompleted.value,
        )
    }

    @Test
    fun completeOnboarding_setsOnboardingCompletedTrue() {
        assertFalse(AppSettings.onboardingCompleted.value)
        AppSettings.onboardingCompleted.value = true
        assertTrue(AppSettings.onboardingCompleted.value)
    }

    @Test
    fun developerSupportUrl_isOfficialBuyMeATea() {
        val expectedUrl = "https://buymeatea.online/adhil"
        assertTrue(
            "Support link must start with https://buymeatea.online/adhil",
            expectedUrl.startsWith("https://buymeatea.online/adhil"),
        )
        assertFalse(
            "Must never contain UPI or banking handles",
            expectedUrl.contains("@") || expectedUrl.contains("upi"),
        )
    }

    @Test
    fun realTimeGreeting_morningDaypart_returnsGoodMorning() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8) }
        val greeting = getGreetingForCurrentTime("Tonight's mix", cal)
        assertEquals("Good morning", greeting)
    }

    @Test
    fun realTimeGreeting_afternoonDaypart_returnsGoodAfternoon() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 14) }
        val greeting = getGreetingForCurrentTime("Tonight's mix", cal)
        assertEquals("Good afternoon", greeting)
    }

    @Test
    fun realTimeGreeting_eveningDaypart_returnsGoodEvening() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 19) }
        val greeting = getGreetingForCurrentTime("Tonight's mix", cal)
        assertEquals("Good evening", greeting)
    }

    @Test
    fun realTimeGreeting_nightDaypart_returnsGoodNight() {
        val lateNightCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23) }
        val greetingLate = getGreetingForCurrentTime("Tonight's mix", lateNightCal)
        assertEquals("Good night", greetingLate)

        val earlyMorningCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 2) }
        val greetingEarly = getGreetingForCurrentTime("Tonight's mix", earlyMorningCal)
        assertEquals("Good night", greetingEarly)
    }

    @Test
    fun realTimeGreeting_neverReturnsLateNightAtmosphere() {
        for (hour in 0..23) {
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour) }
            val greeting = getGreetingForCurrentTime("Tonight's mix", cal)
            assertNotEquals(
                "Must never return 'Late Night Atmosphere' at hour $hour",
                "Late Night Atmosphere",
                greeting,
            )
            assertTrue(
                "Greeting for hour $hour must be a recognized daypart",
                greeting in listOf("Good morning", "Good afternoon", "Good evening", "Good night"),
            )
        }
    }

    @Test
    fun realTimeGreeting_customTitlePreserved() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10) }
        val customGreeting = getGreetingForCurrentTime("Weekend Vibes", cal)
        assertEquals("Weekend Vibes", customGreeting)
    }
}
