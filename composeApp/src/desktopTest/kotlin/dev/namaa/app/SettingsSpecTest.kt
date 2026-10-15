package dev.namaa.app

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Settings list must keep the order fixed in the shared UI spec (ui-spec-shared.md); Namaa adds Currency and categories as its own row and has no reminders. */
class SettingsSpecTest {
    @Test fun rowsFollowTheSharedSpecOrder() {
        assertEquals(listOf("Appearance", "Sections", "Language", "Currency", "Data", "Privacy", "About"), SettingsPage.entries.map { it.name })
    }
}
