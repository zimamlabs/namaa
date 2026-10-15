package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClearAllTest {
    @Test fun clearingMovesTheDataAsideAndLosesNothing() {
        val b = MoneyBook(FakeFileSystem(), "/z".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        b.saveTransaction(Txn("t", LocalDate(2026, 1, 2), -5, "c")); b.saveBudgets(listOf(Budget("Food", 9)))
        val before = Backup.create(b.fs, b.root, "x")
        assertEquals(3, b.clearAll("s1"))
        assertTrue(b.accounts().isEmpty() && b.allTransactions().isEmpty() && b.budgets().isEmpty())
        val kept = MoneyBook(b.fs, "/z/cleared-s1".toPath())
        assertTrue(Backup.matchesFolder(before, kept.fs, kept.root))
        assertEquals(0, b.clearAll("s2"))
    }
}
