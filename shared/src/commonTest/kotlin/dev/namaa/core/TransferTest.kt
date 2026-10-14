package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransferTest {
    private val cash = Account("c", "Cash"); private val bank = Account("b", "Bank", "USD", 5_000)
    private fun book() = MoneyBook(FakeFileSystem(), "/z".toPath()).also { it.saveAccounts(listOf(cash, bank)) }

    @Test fun aTransferMovesMoneyWithoutChangingTheTotal() {
        val b = book()
        b.saveTransaction(Txn("", LocalDate(2026, 10, 1), 10_000, "c", "Salary"))
        val before = balance(cash, b.allTransactions()) + balance(bank, b.allTransactions())
        b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000, "to savings")
        val all = b.allTransactions()
        assertEquals(6_000, balance(cash, all)); assertEquals(9_000, balance(bank, all))
        assertEquals(before, balance(cash, all) + balance(bank, all))
        assertEquals(0, all.filter { it.transferId.isNotEmpty() }.sumOf { it.amountMinor })
    }

    @Test fun transfersAreNotIncomeOrSpending() {
        val b = book()
        b.saveTransaction(Txn("", LocalDate(2026, 10, 1), 10_000, "c", "Salary"))
        b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000)
        val m = monthSummary(b.allTransactions(), 2026, 10)
        assertEquals(10_000, m.incomeMinor); assertEquals(0, m.expenseMinor)
    }

    @Test fun deletingOneLegDeletesBoth() {
        val b = book(); b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000)
        b.deleteTransaction(b.allTransactions().first().id)
        assertTrue(b.allTransactions().isEmpty())
    }

    @Test fun bothLegsSurviveTheFileRoundTripAndOldRowsStillRead() {
        val b = book(); b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000, "note, with comma")
        assertEquals(2, b.allTransactions().count { it.transferId.isNotEmpty() })
        b.fs.write("/z/2025.csv".toPath()) { writeUtf8("id,date,amount_minor,account,category,note\nx1,2025-01-02,-500,c,Food,old row\n") }
        assertEquals("", b.transactions(2025).single().transferId)
    }

    @Test fun badTransfersAreRefused() {
        val b = book()
        assertFailsWith<IllegalArgumentException> { b.saveTransfer(LocalDate(2026, 10, 2), "c", "c", 100) }
        assertFailsWith<IllegalArgumentException> { b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 0) }
    }

    @Test fun sixMonthTotalsWrapTheYear() {
        val t = listOf(Txn("1", LocalDate(2026, 1, 5), 1_000, "c"), Txn("2", LocalDate(2025, 11, 5), -300, "c"))
        val m = monthlyTotals(t, 2026, 1, 6)
        assertEquals(6, m.size); assertEquals(2025 to 8, m.first().year to m.first().month); assertEquals(2026 to 1, m.last().year to m.last().month)
        assertEquals(300, m.first { it.month == 11 }.spentMinor); assertEquals(1_000, m.last().incomeMinor)
    }
}
