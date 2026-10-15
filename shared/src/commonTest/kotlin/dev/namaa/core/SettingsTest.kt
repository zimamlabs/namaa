package dev.namaa.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTest {
    private val fs = FakeFileSystem()
    private val path = "/p/prefs.json".toPath()
    private fun repo() = SettingsRepository(fs, path)

    @Test fun defaults() {
        val s = repo().current
        assertEquals(Settings(), s)
        assertEquals("system", s.themeMode); assertEquals("zimam", s.palette); assertEquals(1, s.textSize); assertEquals("comfortable", s.density)
        assertEquals(false, s.reduceMotion); assertEquals("home", s.startSection); assertEquals(false, s.appLock)
    }

    @Test fun roundTrip() {
        val r = repo()
        val saved = r.update { it.copy(themeMode = "dark", palette = "ocean", textSize = 3, density = "compact", reduceMotion = true, lang = "ar", arabicDigits = true, currency = "egp", hiddenSections = setOf("report"), sectionOrder = listOf("money", "home"), startSection = "money", appLock = true) }
        assertEquals("EGP", saved.currency)
        assertEquals(saved, repo().current)
    }

    @Test fun anOldFileWithOnlyTheFirstSixKeysStillLoadsWithDefaultsForTheRest() {
        fs.createDirectories("/p".toPath())
        fs.write(path) { writeUtf8("""{"lastSeenVersion":"0.5.1","tourDone":true,"theme":"light","currency":"EGP","lang":"ar","arabicDigits":true}""") }
        assertEquals(Settings("0.5.1", true, "light", "EGP", "ar", true), repo().current)
    }

    @Test fun theThemeKeyKeepsItsOldFileName() {
        repo().update { it.copy(themeMode = "dark") }
        assertTrue(fs.read(path) { readUtf8() }.contains("\"theme\": \"dark\""))
    }

    @Test fun valuesOutOfRangeAreBroughtBack() {
        fs.createDirectories("/p".toPath())
        fs.write(path) { writeUtf8("""{"theme":"neon","palette":"x","textSize":9,"density":"tiny","lang":"fr","sectionOrder":["plan","nope","plan"],"hiddenSections":["home","report","zzz"],"startSection":"report"}""") }
        val s = repo().current
        assertEquals("system", s.themeMode); assertEquals("zimam", s.palette); assertEquals(3, s.textSize); assertEquals("comfortable", s.density); assertEquals("system", s.lang)
        assertEquals(listOf("plan"), s.sectionOrder); assertEquals(setOf("report"), s.hiddenSections); assertEquals("home", s.startSection)
    }

    @Test fun garbageGivesDefaultsAndIsNotWrittenUntilSomethingChanges() {
        fs.createDirectories("/p".toPath()); fs.write(path) { writeUtf8("not json") }
        val r = repo(); assertEquals(Settings(), r.current)
        assertEquals("not json", fs.read(path) { readUtf8() })
        r.update { it.copy(palette = "rose") }; assertEquals("rose", repo().current.palette)
    }

    @Test fun sectionsFollowTheOrderAndHomeCannotBeHidden() {
        assertEquals(listOf("home", "money", "accounts", "plan", "report"), Sections.visible(emptyList(), emptySet()))
        assertEquals(listOf("plan", "money", "home", "accounts", "report"), Sections.visible(listOf("plan", "money", "home"), emptySet()))
        assertEquals(listOf("home", "money"), Sections.visible(emptyList(), setOf("accounts", "plan", "report", "home")))
    }

    @Test fun startSectionMustBeVisible() {
        val s = Settings(startSection = "plan", hiddenSections = setOf("plan")).normalized()
        assertEquals("home", s.startSection)
    }
}
