package dev.namaa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LangTest {
    @Test fun systemFollowsTheDeviceLanguage() {
        assertEquals(Lang.Ar, I18n.resolve("system", "ar"))
        assertEquals(Lang.Ar, I18n.resolve(null, "ar-EG"))
        assertEquals(Lang.En, I18n.resolve("system", "fr"))
    }

    @Test fun anExplicitChoiceWinsOverTheDevice() {
        assertEquals(Lang.En, I18n.resolve("en", "ar"))
        assertEquals(Lang.Ar, I18n.resolve("ar", "en"))
    }

    @Test fun digitsBecomeArabicIndicOnlyWhenAsked() {
        assertEquals("١٢٫٥٠", I18n.digits("12٫50", true))
        assertEquals("٠٥:١٢", I18n.digits("05:12", true))
        assertEquals("1,234.50", I18n.digits("1,234.50", false))
    }

    @Test fun placeholdersAreFilledInOrder() {
        assertEquals("3 of 5", I18n.fill("{0} of {1}", arrayOf(3, 5)))
    }

    @Test fun everyStringHasTheSamePlaceholdersInBothLanguages() {
        val slot = Regex("\\{\\d+\\}")
        for (s in Str.entries) {
            assertTrue(s.en.isNotBlank() && s.ar.isNotBlank(), "${s.name} is empty")
            assertEquals(slot.findAll(s.en).map { it.value }.toSortedSet(), slot.findAll(s.ar).map { it.value }.toSortedSet(), "${s.name} placeholders differ")
        }
    }

    @Test fun arabicTextHasArabicLetters() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        val keepsLatin = setOf(Str.LangEnglish, Str.DigitsWestern)
        for (s in Str.entries) if (s !in keepsLatin) assertTrue(arabic.containsMatchIn(s.ar), "${s.name} has no Arabic")
    }

    @Test fun amountsTypedInArabicDigitsAreUnderstood() {
        assertEquals(1250L, parseMoney("١٢٫٥"))
        assertEquals(1250L, parseMoney("12,5"))
        assertEquals(-300L, parseMoney("-٣"))
        assertNull(parseMoney("١٢أ"))
    }

    @Test fun builtInCategoriesAreTranslatedAndOthersLeftAlone() {
        assertEquals("الطعام", categoryTitle("Food", Lang.Ar))
        assertEquals("Food", categoryTitle("food", Lang.En))
        assertEquals("أخرى", categoryTitle("", Lang.Ar))
        assertEquals("Cat food", categoryTitle("Cat food", Lang.Ar))
        assertEquals("تحويل", categoryTitle("Transfer", Lang.Ar))
    }

    @Test fun datesAndMonthsHaveBothLanguages() {
        assertEquals("Jan", monthName(1, Lang.En)); assertEquals("يناير", monthName(1, Lang.Ar))
        assertEquals("Mon", dayName(0, Lang.En)); assertEquals("الأحد", dayName(6, Lang.Ar))
        assertEquals(12, monthsAr.size); assertEquals(12, monthsEn.size); assertEquals(7, weekdaysAr.size)
    }

    @Test fun releaseNotesComeInBothLanguages() {
        assertTrue(ReleaseNotes.forVersion("0.5.1", Lang.Ar).first().contains("أيقونة"))
        assertTrue(ReleaseNotes.forVersion("0.5.1", Lang.En).first().contains("icon"))
        assertTrue(ReleaseNotes.forVersion("0.5.0", Lang.Ar).first().contains("عربية"))
        assertTrue(ReleaseNotes.forVersion("0.5.0", Lang.En).first().startsWith("An Arabic"))
        assertEquals(ReleaseNotes.forVersion("0.3.0", Lang.En).size, ReleaseNotes.forVersion("0.3.0", Lang.Ar).size)
    }
}
