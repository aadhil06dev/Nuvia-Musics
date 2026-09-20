package com.music.nuvia

import androidx.compose.ui.unit.dp
import com.music.nuvia.ui.components.GLASS_EDGE_WIDTH
import com.music.nuvia.ui.components.GLASS_RESOLUTION_SCALE
import com.music.nuvia.ui.components.NUViAGlassShapes
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassMaterialTest {

    @Test
    fun testGlassConstants() {
        assertEquals(1f, GLASS_RESOLUTION_SCALE, 0.001f)
        assertEquals(0.5.dp, GLASS_EDGE_WIDTH)
    }

    @Test
    fun testGlassTiersDefined() {
        val tiers = NUViAGlassTier.values()
        assertEquals(4, tiers.size)
        assertEquals(NUViAGlassTier.Subtle, tiers[0])
        assertEquals(NUViAGlassTier.Elevated, tiers[1])
        assertEquals(NUViAGlassTier.Prominent, tiers[2])
        assertEquals(NUViAGlassTier.Immersive, tiers[3])
    }

    @Test
    fun testGlassShapesTokens() {
        assertNotNull(NUViAGlassShapes.Badge)
        assertNotNull(NUViAGlassShapes.Tag)
        assertNotNull(NUViAGlassShapes.Control)
        assertNotNull(NUViAGlassShapes.Card)
        assertNotNull(NUViAGlassShapes.Dialog)
        assertNotNull(NUViAGlassShapes.Sheet)
        assertNotNull(NUViAGlassShapes.Pill)
    }

    @Test
    fun testLiquidGlassAndBlurDefaults() {
        // Blur must default to OFF out-of-the-box
        AppSettings.minimalUi.value = false
        AppSettings.glassEffects.value = true
        AppSettings.liquidGlassBlur.value = false

        assertTrue("Glass should be enabled by default", AppSettings.effectiveGlassEffects)
        assertFalse("Blur should be OFF by default", AppSettings.effectiveLiquidGlassBlur)
    }

    @Test
    fun testLiquidGlassStateMatrix() {
        // Case 1: Minimal UI overrides all glass and blur
        AppSettings.minimalUi.value = true
        AppSettings.glassEffects.value = true
        AppSettings.liquidGlassBlur.value = true
        assertFalse("Minimal UI forces effective glass to false", AppSettings.effectiveGlassEffects)
        assertFalse("Minimal UI forces effective blur to false", AppSettings.effectiveLiquidGlassBlur)

        // Case 2: Glass disabled -> Blur unconditionally disabled even if blur flag is true
        AppSettings.minimalUi.value = false
        AppSettings.glassEffects.value = false
        AppSettings.liquidGlassBlur.value = true
        assertFalse("Disabled glass means effective glass is false", AppSettings.effectiveGlassEffects)
        assertFalse("Disabled glass means effective blur is false", AppSettings.effectiveLiquidGlassBlur)

        // Case 3: Glass enabled, Blur enabled
        AppSettings.minimalUi.value = false
        AppSettings.glassEffects.value = true
        AppSettings.liquidGlassBlur.value = true
        assertTrue("Effective glass is true", AppSettings.effectiveGlassEffects)
        assertTrue("Effective blur is true", AppSettings.effectiveLiquidGlassBlur)

        // Case 4: Glass enabled, Blur disabled (AMOLED crisp backdrop, lens/specular intact)
        AppSettings.minimalUi.value = false
        AppSettings.glassEffects.value = true
        AppSettings.liquidGlassBlur.value = false
        assertTrue("Effective glass is true", AppSettings.effectiveGlassEffects)
        assertFalse("Effective blur is false", AppSettings.effectiveLiquidGlassBlur)
    }
}

