package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every file Zimma writes (data, budgets, repeating, debts, exports) lands under the one data folder, nothing beside it. */
class StaysInFolderTest {
    @Test fun allFilesLandUnderTheDataFolder() {
        val fs = FakeFileSystem(); val root = "/sdcard/Zimam/Zimma".toPath()
        val b = MoneyBook(fs, root)
        b.saveAccounts(listOf(Account("c", "Cash"), Account("b", "Bank")))
        b.saveTransaction(Txn("", LocalDate(2026, 10, 1), -100, "c", "Food"))
        b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 50)
        b.saveBudgets(listOf(Budget("Food", 1_000)))
        b.saveRecurring(listOf(Recurring("r", -100, "c", frequency = Frequency.Weekly, start = LocalDate(2026, 10, 1))))
        b.applyRecurring(LocalDate(2026, 10, 10))
        b.saveDebts(listOf(Debt("d", "Sam", Direction.TheyOweMe, 500, LocalDate(2026, 10, 1))))
        b.export(monthFileName(2026, 10), b.allTransactions()); b.export(yearFileName(2026), b.allTransactions())
        val files = fs.listRecursively("/".toPath()).filter { fs.metadata(it).isRegularFile }.toList()
        assertTrue(files.isNotEmpty()); assertTrue(files.all { it.toString().startsWith(root.toString() + "/") }, "stray files: " + files.filter { !it.toString().startsWith(root.toString() + "/") })
        assertEquals(setOf("exports"), fs.list(root).filter { fs.metadata(it).isDirectory }.map { it.name }.toSet())
    }
}
