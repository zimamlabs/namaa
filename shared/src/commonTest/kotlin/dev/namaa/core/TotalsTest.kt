package dev.namaa.core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TotalsTest {
    private val usd = Account("u", "Bank", "USD", 10_000)
    private val egp = Account("e", "Cash", "egp ", 50_000)
    private val accounts = listOf(usd, egp)
    private fun d(day: Int) = LocalDate(2026, 10, day)
    private val txns = listOf(
        Txn("1", d(1), 20_000, "u", "Salary"), Txn("2", d(2), -3_000, "u", "Food"),
        Txn("3", d(2), -9_000, "e", "Food"), Txn("4", d(3), 1_000, "e", "Gift"),
    )

    @Test fun balancesAreNeverAddedAcrossCurrencies() {
        assertEquals(listOf("USD" to 27_000L, "EGP" to 42_000L), balancesByCurrency(accounts, txns))
    }

    @Test fun oneCurrencyLooksLikeBefore() {
        assertEquals(listOf("USD" to 27_000L), balancesByCurrency(listOf(usd), txns))
    }

    @Test fun aMonthIsSummedPerCurrency() {
        val u = monthSummary(txnsInCurrency(txns, accounts, "USD"), 2026, 10); val e = monthSummary(txnsInCurrency(txns, accounts, "EGP"), 2026, 10)
        assertEquals(20_000, u.incomeMinor); assertEquals(-3_000, u.expenseMinor)
        assertEquals(1_000, e.incomeMinor); assertEquals(-9_000, e.expenseMinor)
        assertEquals(listOf("USD", "EGP"), currenciesOf(accounts))
    }

    @Test fun aBudgetOnlyCountsItsOwnCurrency() {
        val s = budgetStatusesIn(listOf(Budget("Food", 10_000, "USD"), Budget("Food", 10_000, "EGP"), Budget("Food", 10_000)), accounts, txns, 2026, 10, "USD")
        assertEquals(listOf(9_000L, 3_000L, 3_000L), s.map { it.spentMinor })
        assertEquals(listOf("EGP", "USD", "USD"), s.map { it.budget.currency.ifEmpty { "USD" } })
    }

    @Test fun debtsAreTotalledPerCurrencyAndPeopleSeparately() {
        val debts = listOf(
            Debt("a", "Sam", Direction.TheyOweMe, 5_000, d(1), currency = "USD"), Debt("b", "Sam", Direction.TheyOweMe, 7_000, d(1), currency = "EGP"),
            Debt("c", "Lee", Direction.IOweThem, 1_000, d(1)),
        )
        assertEquals(listOf("USD" to DebtTotals(5_000, 1_000), "EGP" to DebtTotals(7_000, 0)), debtTotalsByCurrency(debts, "USD"))
        assertEquals(listOf(Triple("Sam", "EGP", 7_000L), Triple("Sam", "USD", 5_000L), Triple("Lee", "USD", -1_000L)), balanceByPersonAndCurrency(debts, "USD"))
    }

    @Test fun aTransferBetweenCurrenciesIsRefusedAndWritesNothing() {
        val b = MoneyBook(FakeFileSystem(), "/z".toPath()); b.saveAccounts(accounts)
        assertFailsWith<IllegalArgumentException> { b.saveTransfer(d(1), "u", "e", 100) }
        assertEquals(0, b.allTransactions().size)
        b.saveAccounts(accounts + Account("u2", "Savings", "usd"))
        b.saveTransfer(d(1), "u", "u2", 100)
        assertFailsWith<IllegalArgumentException> { b.updateTransfer(b.allTransactions().first().transferId, d(1), "u", "e", 100, "") }
    }

    @Test fun oldBudgetAndDebtFilesStillDecode() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(Budget("Food", 500, ""), json.decodeFromString(Budget.serializer(), """{"category":"Food","limitMinor":500}"""))
        assertEquals("", json.decodeFromString(Debt.serializer(), """{"id":"a","person":"S","direction":"TheyOweMe","principalMinor":5,"date":"2026-01-01"}""").currency)
    }
}
