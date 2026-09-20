package com.music.nuvia

import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.AppLanguage
import com.music.nuvia.ui.components.SUPPORTED_LANGUAGES
import com.music.nuvia.ui.components.languageDisplayNameRes
import com.music.nuvia.ui.components.languageNativeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Locale

class LocalizationTest {

    @Before
    fun setUp() {
        // Reset app language to system default before each test
        AppSettings.appLanguage.value = ""
    }

    @Test
    fun `supported languages contains all 7 required options`() {
        val tags = SUPPORTED_LANGUAGES.map { it.tag }
        
        // Must contain System Default (""), English ("en"), Spanish ("es"), French ("fr"),
        // German ("de"), Hindi ("hi"), Japanese ("ja")
        assertEquals(7, SUPPORTED_LANGUAGES.size)
        assertTrue("Must include System Default", tags.contains(""))
        assertTrue("Must include English", tags.contains("en"))
        assertTrue("Must include Spanish", tags.contains("es"))
        assertTrue("Must include French", tags.contains("fr"))
        assertTrue("Must include German", tags.contains("de"))
        assertTrue("Must include Hindi", tags.contains("hi"))
        assertTrue("Must include Japanese", tags.contains("ja"))
    }

    @Test
    fun `supported languages have correct native names`() {
        val map = SUPPORTED_LANGUAGES.associate { it.tag to it.nativeName }
        assertEquals("System Default", map[""])
        assertEquals("English", map["en"])
        assertEquals("Español", map["es"])
        assertEquals("Français", map["fr"])
        assertEquals("Deutsch", map["de"])
        assertEquals("हिन्दी", map["hi"])
        assertEquals("日本語", map["ja"])
    }

    @Test
    fun `supported languages have valid resource IDs`() {
        SUPPORTED_LANGUAGES.forEach { lang ->
            assertTrue("Resource ID for ${lang.nativeName} must be positive", lang.nameRes > 0)
        }
    }

    @Test
    fun `language resolution maps known tags to resource IDs`() {
        assertEquals(R.string.system_default, languageDisplayNameRes(""))
        assertEquals(R.string.english, languageDisplayNameRes("en"))
        assertEquals(R.string.spanish, languageDisplayNameRes("es"))
        assertEquals(R.string.french, languageDisplayNameRes("fr"))
        assertEquals(R.string.german, languageDisplayNameRes("de"))
        assertEquals(R.string.hindi, languageDisplayNameRes("hi"))
        assertEquals(R.string.japanese, languageDisplayNameRes("ja"))
    }

    @Test
    fun `language resolution handles case-insensitivity`() {
        assertEquals(R.string.spanish, languageDisplayNameRes("ES"))
        assertEquals(R.string.french, languageDisplayNameRes("Fr"))
        assertEquals(R.string.japanese, languageDisplayNameRes("JA"))
        assertEquals("Español", languageNativeName("ES"))
        assertEquals("日本語", languageNativeName("Ja"))
    }

    @Test
    fun `unknown language tag safely falls back to default`() {
        val fallbackRes = languageDisplayNameRes("unknown_locale")
        assertEquals(R.string.system_default, fallbackRes)

        val fallbackNative = languageNativeName("unknown_locale")
        assertEquals("System Default", fallbackNative)
    }

    @Test
    fun `appSettings language selection and state flow update`() {
        assertEquals("", AppSettings.appLanguage.value)

        AppSettings.setAppLanguage("es")
        assertEquals("es", AppSettings.appLanguage.value)

        AppSettings.setAppLanguage("ja")
        assertEquals("ja", AppSettings.appLanguage.value)

        AppSettings.setAppLanguage("")
        assertEquals("", AppSettings.appLanguage.value)
    }

    @Test
    fun `applyAppLanguage executes gracefully for all supported tags`() {
        SUPPORTED_LANGUAGES.forEach { lang ->
            // Should not throw exceptions even in pure JVM environment without device
            AppSettings.applyAppLanguage(lang.tag)
        }
        // System / empty tag
        AppSettings.applyAppLanguage("")
        AppSettings.applyAppLanguage("system")
    }

    @Test
    fun `system language detection and fallback logic`() {
        val currentLocale = Locale.getDefault()
        assertNotNull("System locale should not be null", currentLocale)

        // When user hasn't selected a specific language (empty tag),
        // the app relies on system default locale
        val savedLang = AppSettings.appLanguage.value
        assertTrue("Initial saved language should be empty (system default)", savedLang.isEmpty())
    }

    @Test
    fun `resource directories exist for all supported target languages`() {
        val resDir = File("src/main/res")
        if (resDir.exists()) {
            val expectedDirs = listOf(
                "values",
                "values-es",
                "values-fr",
                "values-de",
                "values-hi",
                "values-ja",
            )
            expectedDirs.forEach { dirName ->
                val dir = File(resDir, dirName)
                assertTrue("Directory $dirName must exist", dir.exists() && dir.isDirectory)
                val stringsXml = File(dir, "strings.xml")
                assertTrue("strings.xml in $dirName must exist and not be empty", stringsXml.exists() && stringsXml.length() > 0)
            }
        }
    }
}
