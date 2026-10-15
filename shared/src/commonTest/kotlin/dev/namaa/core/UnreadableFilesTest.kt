package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UnreadableFilesTest {
    private val fs = FakeFileSystem()
    private val root = "/z".toPath()
    private fun book() = MoneyBook(fs, root)
    private fun text(name: String) = fs.read(root / name) { readUtf8() }
    private fun copies(prefix: String) = fs.list(root).filter { it.name.startsWith("$prefix.corrupt-") }.map { fs.read(it) { readUtf8() } }

    @Test fun aTruncatedAccountsFileIsNotEmptyAndIsNeverOverwritten() {
        val b = book(); b.saveAccounts(listOf(Account("a", "Bank", "USD", 5)))
        val good = text("accounts.json"); fs.write(root / "accounts.json") { writeUtf8(good.take(good.length / 2)) }
        val broken = text("accounts.json")
        assertTrue(b.hasAccountsFile())
        val problems = b.unreadableFiles()
        assertEquals(listOf("accounts.json"), problems.map { it.file })
        assertEquals(listOf(broken), copies("accounts.json"))
        assertFailsWith<UnreadableFileException> { b.saveAccounts(listOf(Account("x", "Cash"))) }
        assertEquals(broken, text("accounts.json"))
    }

    @Test fun aGarbageBudgetsFileBlocksTheNextSaveAndKeepsItsBytes() {
        val b = book(); fs.write(root / "budgets.json") { writeUtf8("{ not json") }
        assertFailsWith<UnreadableFileException> { b.saveBudgets(listOf(Budget("Food", 100))) }
        assertEquals("{ not json", text("budgets.json"))
        assertEquals(listOf("{ not json"), copies("budgets.json"))
    }

    @Test fun theSameBrokenFileIsCopiedOnlyOnce() {
        val b = book(); fs.write(root / "debts.json") { writeUtf8("oops") }
        b.unreadableFiles(); b.unreadableFiles()
        assertEquals(1, copies("debts.json").size)
    }

    @Test fun startingFreshMovesTheBrokenFileAsideAfterTheCopyExists() {
        val b = book(); fs.write(root / "recurring.json") { writeUtf8("oops") }
        b.unreadableFiles()
        b.startFresh()
        assertTrue(!fs.exists(root / "recurring.json"))
        assertEquals(listOf("oops"), copies("recurring.json"))
        assertTrue(b.unreadableFiles().isEmpty())
        b.saveRecurring(emptyList())
    }

    @Test fun aFixedFileWorksAgain() {
        val b = book(); fs.write(root / "budgets.json") { writeUtf8("oops") }
        assertEquals(1, b.unreadableFiles().size)
        fs.write(root / "budgets.json") { writeUtf8("[]") }
        assertTrue(b.unreadableFiles().isEmpty())
        b.saveBudgets(listOf(Budget("Food", 100)))
    }

    @Test fun aBadCsvRowIsReportedAndItsFileIsKeptBeforeTheNextRewrite() {
        val b = book(); b.saveAccounts(listOf(Account("a", "Cash")))
        val original = "id,date,amount_minor,account,category,note,transfer\ng1,2026-01-02,-500,a,Food,,\nbad,not-a-date,xx,a,,,\n"
        fs.write(root / "2026.csv") { writeUtf8(original) }
        assertEquals(1, b.transactions(2026).size)
        assertEquals(listOf("2026.csv"), b.rowProblems().map { it.file })
        b.saveTransaction(Txn("", LocalDate(2026, 3, 1), -100, "a", "Food"))
        assertEquals(listOf(original), copies("2026.csv"))
    }

    @Test fun aFreshFolderHasNoProblems() {
        val b = book(); assertTrue(!b.hasAccountsFile()); assertTrue(b.unreadableFiles().isEmpty()); assertTrue(b.rowProblems().isEmpty())
    }
}
