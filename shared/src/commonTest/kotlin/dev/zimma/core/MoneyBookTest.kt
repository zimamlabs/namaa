package dev.zimma.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyBookTest {
    private fun book() = MoneyBook(FakeFileSystem(), "/z".toPath())

    @Test fun moneyTotalsBalancesAndYearFiles() {
        val v = book()
        val acc = Account("a", "Cash", "USD", 10_000)
        v.saveAccounts(listOf(acc))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 1), 250_000, "a", "Salary"))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 3), -4_550, "a", "Food", "lunch, with \"friends\""))
        v.saveTransaction(Txn("", LocalDate(2026, 10, 9), -12_000, "a", "Rent"))
        v.saveTransaction(Txn("", LocalDate(2025, 12, 30), -1_000, "a", "Food"))
        val all = v.allTransactions()
        assertEquals(4, all.size)
        assertEquals("lunch, with \"friends\"", all.first { it.category == "Food" && it.date.year == 2026 }.note)
        val m = monthSummary(all, 2026, 10)
        assertEquals(250_000, m.incomeMinor)
        assertEquals(-16_550, m.expenseMinor)
        assertEquals("Rent", m.byCategory.first().first)
        assertEquals(10_000 + 250_000 - 4_550 - 12_000 - 1_000, balance(acc, all))
        assertEquals(2, v.fs.list("/z".toPath()).count { it.name.endsWith(".csv") })
        v.deleteTransaction(all.first().id)
        assertEquals(3, v.allTransactions().size)
    }

    @Test fun editingMovesATransactionBetweenYears() {
        val v = book()
        val t = v.saveTransaction(Txn("", LocalDate(2025, 12, 31), -500, "a", "Food"))
        v.saveTransaction(t.copy(date = LocalDate(2026, 1, 1), amountMinor = -700))
        val all = v.allTransactions()
        assertEquals(1, all.size)
        assertEquals(-700, all.single().amountMinor)
        assertTrue(v.transactions(2025).isEmpty())
    }

    @Test fun accountsRoundTripAndMissingFileIsEmpty() {
        val v = book()
        assertTrue(v.accounts().isEmpty())
        v.saveAccounts(listOf(Account("a", "Bank", "EGP", 5)))
        assertEquals(listOf(Account("a", "Bank", "EGP", 5)), v.accounts())
    }

    @Test fun moneyParsingAndFormatting() {
        assertEquals(1250L, parseMoney("12.5"))
        assertEquals(1250L, parseMoney("12,50"))
        assertEquals(-300L, parseMoney("-3"))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("1.2.3"))
        assertEquals("12.05", formatMoney(1205))
        assertEquals("-0.07", formatMoney(-7))
    }
}
