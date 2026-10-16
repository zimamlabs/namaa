package dev.namaa.app

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.namaa.core.Account
import dev.namaa.core.Budget
import dev.namaa.core.DataLocation
import dev.namaa.core.MoneyBook
import dev.namaa.core.Settings
import dev.namaa.core.SettingsRepository
import dev.namaa.core.Txn
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders the real Home offscreen (a fresh install and a filled one, English and Arabic, normal and 130 percent text) and checks it draws something and does not throw. */
class HomeRenderTest {
    private fun store(lang: String, filled: Boolean, tweak: (Settings) -> Settings = { it }): NamaaStore {
        val fs = FakeFileSystem()
        val repo = SettingsRepository(fs, "/s/prefs.json".toPath()).also { r -> r.update { tweak(it.copy(lang = lang, tourDone = true, lastSeenVersion = APP_VERSION)) } }
        val book = MoneyBook(fs, "/data".toPath())
        if (filled) {
            book.saveAccounts(listOf(Account("c", "Cash", "USD", 50_000), Account("b", "Bank", "USD", 120_000)))
            val now = book.today()
            book.saveTransaction(Txn("1", now, -4_550, "c", "Food", "lunch")); book.saveTransaction(Txn("2", now, -12_000, "b", "Bills"))
            book.saveTransaction(Txn("3", now, 250_000, "b", "Salary")); book.saveBudgets(listOf(Budget("Food", 20_000), Budget("Bills", 10_000)))
        }
        return NamaaStore(book, repo, DataLocation(fs, "/data".toPath(), "/shared".toPath(), emptyList()) { false })
    }

    private fun render(name: String, st: NamaaStore, fontScale: Float): ByteArray {
        val scene = ImageComposeScene(824, 1720, Density(2f, fontScale)) { NamaaApp(st, DrawerState(DrawerValue.Closed), {}, showSplash = false) }
        try {
            var t = 0L; repeat(30) { t += 16_000_000; scene.render(t) }
            val png = scene.render(t + 1).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File("build/screenshots").apply { mkdirs() }.resolve("$name.png").writeBytes(png)
            return png
        } finally { scene.close() }
    }

    @Test fun freshHomeShowsTheDesignedEmptyState() = assertTrue(render("home-empty-en", store("en", false), 1f).size > 10_000)
    @Test fun filledHomeInEnglish() = assertTrue(render("home-filled-en", store("en", true), 1f).size > 10_000)
    @Test fun filledHomeInArabic() = assertTrue(render("home-filled-ar", store("ar", true), 1f).size > 10_000)
    @Test fun filledHomeAt130PercentText() = assertTrue(render("home-filled-en-130", store("en", true), 1.3f).size > 10_000)
    @Test fun arabicAt130PercentText() = assertTrue(render("home-filled-ar-130", store("ar", true), 1.3f).size > 10_000)
    @Test fun allFiveCardsCanBeChosenButOnlyFourShow() = assertTrue(render("home-cards", store("en", true) { it.copy(hiddenHomeCards = emptySet(), homeCardOrder = listOf("trend", "recent")) }, 1f).size > 10_000)
}
