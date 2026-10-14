package dev.zimma.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingTest {
    @Test fun freshInstallShowsTheTour() = assertEquals(Welcome.Tour, Onboarding.decide("", false, false, "0.2.0"))
    @Test fun sameVersionShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.2.0", true, true, "0.2.0"))
    @Test fun updateShowsTheNotesOnce() = assertEquals(Welcome.WhatsNew, Onboarding.decide("0.1.2", true, true, "0.2.0"))
    @Test fun downgradeShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.3.0", true, true, "0.2.0"))
    @Test fun anUpdateFromAVersionThatStoredNothingShowsTheNotesNotTheTour() = assertEquals(Welcome.WhatsNew, Onboarding.decide("", false, true, "0.2.0"))
    @Test fun versionsCompareNumerically() { assertEquals(1, Onboarding.compare("0.10.0", "0.9.9")); assertEquals(0, Onboarding.compare("1.0", "1.0.0")) }
    @Test fun notesFallBackToTheNewestOlderVersion() { assertEquals(ReleaseNotes.forVersion("0.2.0"), ReleaseNotes.forVersion("0.2.1")); assertEquals(emptyList(), ReleaseNotes.forVersion("0.0.1")) }

    @Test fun prefsRoundTripAndSurviveGarbage() {
        val fs = FakeFileSystem(); val path = "/p/prefs.json".toPath(); val f = PrefsFile(fs, path)
        assertEquals(Prefs(), f.load())
        f.save(Prefs("0.2.0", true, "dark", "EGP")); assertEquals(Prefs("0.2.0", true, "dark", "EGP"), f.load())
        fs.write(path) { writeUtf8("not json") }; assertEquals(Prefs(), f.load())
    }

    @Test fun monthsShiftAcrossYears() {
        assertEquals(2025 to 12, shiftMonth(2026, 1, -1)); assertEquals(2027 to 1, shiftMonth(2026, 12, 1)); assertEquals(2026 to 6, shiftMonth(2026, 6, 0)); assertEquals(2025 to 7, shiftMonth(2026, 1, -6))
    }
}
