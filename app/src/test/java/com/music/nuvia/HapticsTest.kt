package com.music.nuvia

import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.Haptics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HapticsTest {

    @Test
    fun hapticEnum_containsAllExpectedGestures() {
        val names = Haptic.values().map { it.name }
        assertTrue(names.contains("SkipNext"))
        assertTrue(names.contains("SkipPrevious"))
        assertTrue(names.contains("Tick"))
        assertTrue(names.contains("Tap"))
        assertTrue(names.contains("Select"))
        assertTrue(names.contains("ToggleOn"))
        assertTrue(names.contains("ToggleOff"))
        assertTrue(names.contains("Resume"))
        assertTrue(names.contains("Pause"))
        assertTrue(names.contains("Expand"))
    }

    @Test
    fun manifest_declaresVibratePermission() {
        // Invariant: VIBRATE permission must be explicitly declared in AndroidManifest.xml
        // so that Vibrator.vibrate() does not throw SecurityException on physical devices.
        val manifestFile = File("src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml should exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "AndroidManifest.xml must declare android.permission.VIBRATE",
            content.contains("android.permission.VIBRATE")
        )
    }
}
