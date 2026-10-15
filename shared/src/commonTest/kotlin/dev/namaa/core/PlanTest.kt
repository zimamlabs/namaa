package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate(y, m, day)
    private fun book() = MoneyBook(FakeFileSystem(), "/z".toPath()).also { it.saveAccounts(listOf(Account("c", "Cash"))) }

    // Budgets
    @Test fun budgetStatesFollowTheThresholds() {
        val txns = listOf(Txn("1", d(2026, 10, 2), -8_000, "c", "Food"), Txn("2", d(2026, 10, 3), -9_500, "c", "Fun"), Txn("3", d(2026, 10, 3), -2_000, "c", "Home"))
        val s = budgetStatuses(listOf(Budget("Food", 10_000), Budget("Fun", 9_000), Budget("Home", 10_000)), txns, 2026, 10).associateBy { it.budget.category }
        assertEquals(BudgetState.Near, s.getValue("Food").state); assertEquals(2_000, s.getValue("Food").remainingMinor)
        assertEquals(BudgetState.Over, s.getValue("Fun").state); assertEquals(-500, s.getValue("Fun").remainingMinor)
        assertEquals(BudgetState.Ok, s.getValue("Home").state)
    }
    @Test fun budgetsIgnoreIncomeTransfersAndOtherMonths() {
        val txns = listOf(Txn("1", d(2026, 10, 2), 5_000, "c", "Food"), Txn("2", d(2026, 10, 2), -5_000, "c", "Food", transferId = "t"), Txn("3", d(2026, 9, 30), -5_000, "c", "Food"), Txn("4", d(2026, 10, 5), -1_000, "c", "FOOD"))
        assertEquals(1_000, budgetStatuses(listOf(Budget("food", 10_000)), txns, 2026, 10).single().spentMinor)
    }
    @Test fun exactlyTheLimitIsNearNotOver() = assertEquals(BudgetState.Near, budgetStatuses(listOf(Budget("Food", 1_000)), listOf(Txn("1", d(2026, 10, 2), -1_000, "c", "Food")), 2026, 10).single().state)

    // Recurring
    @Test fun weeklyDatesStepSevenDays() {
        val r = Recurring("r", -500, "c", "Gym", frequency = Frequency.Weekly, start = d(2026, 10, 1))
        assertEquals(listOf(d(2026, 10, 1), d(2026, 10, 8), d(2026, 10, 15)), dueDates(r, d(2026, 10, 20)))
    }
    @Test fun monthlyOnThe31stFallsOnTheLastDayOfShortMonths() {
        val r = Recurring("r", -500, "c", frequency = Frequency.Monthly, start = d(2026, 1, 31))
        assertEquals(listOf(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)), dueDates(r, d(2026, 4, 30)))
        assertEquals(d(2028, 2, 29), occurrence(r.copy(start = d(2027, 12, 31)), 2))
    }
    @Test fun nothingIsDueBeforeTheStartOrAfterTheLastGenerated() {
        val r = Recurring("r", -500, "c", frequency = Frequency.Monthly, start = d(2026, 10, 15), lastGenerated = d(2026, 11, 15))
        assertTrue(dueDates(r, d(2026, 10, 1)).isEmpty()); assertEquals(listOf(d(2026, 12, 15)), dueDates(r, d(2026, 12, 20)))
    }
    @Test fun applyingTwiceNeverDoublesAndAdvancesTheRule() {
        val b = book(); b.saveRecurring(listOf(Recurring("r", -500, "c", "Gym", frequency = Frequency.Weekly, start = d(2026, 10, 1))))
        assertEquals(3, b.applyRecurring(d(2026, 10, 20))); assertEquals(0, b.applyRecurring(d(2026, 10, 20)))
        assertEquals(3, b.allTransactions().size); assertEquals(d(2026, 10, 15), b.recurring().single().lastGenerated)
        assertEquals(1, b.applyRecurring(d(2026, 10, 22)))
    }
    @Test fun deletingAGeneratedTransactionDoesNotBringItBack() {
        val b = book(); b.saveRecurring(listOf(Recurring("r", -500, "c", frequency = Frequency.Weekly, start = d(2026, 10, 1))))
        b.applyRecurring(d(2026, 10, 10)); b.deleteTransaction(b.allTransactions().first().id)
        assertEquals(0, b.applyRecurring(d(2026, 10, 10))); assertEquals(1, b.allTransactions().size)
    }

    // Search and export
    private val sample = listOf(
        Txn("1", d(2026, 10, 2), -1_250, "c", "Food", "Lunch with Sam"), Txn("2", d(2026, 10, 9), -9_000, "b", "Home", "rent"),
        Txn("3", d(2026, 9, 30), 50_000, "c", "Salary"),
    )
    @Test fun filtersCombine() {
        assertEquals(listOf("1"), filterTxns(sample, TxnFilter(text = "sam")).map { it.id })
        assertEquals(listOf("1"), filterTxns(sample, TxnFilter(text = "12.50")).map { it.id })
        assertEquals(listOf("2"), filterTxns(sample, TxnFilter(category = "home", accountId = "b")).map { it.id })
        assertEquals(listOf("1", "2"), filterTxns(sample, TxnFilter(from = d(2026, 10, 1), to = d(2026, 10, 31))).map { it.id })
        assertEquals(3, filterTxns(sample, TxnFilter()).size); assertTrue(!TxnFilter().active && TxnFilter(text = "x").active)
    }
    @Test fun exportWritesAReadableCsvIntoTheExportsFolder() {
        val b = book(); b.saveTransaction(Txn("", d(2026, 10, 2), -1_250, "c", "Food", "lunch, with \"friends\""))
        val path = b.export(monthFileName(2026, 10), b.allTransactions())
        assertEquals("/z/exports/Namaa-2026-10.csv".toPath(), path)
        val rows = Csv.parse(b.fs.read(path) { readUtf8() })
        assertEquals(listOf("date", "account", "category", "amount", "currency", "note", "transfer"), rows[0]); assertEquals(listOf("2026-10-02", "Cash", "Food", "-12.50", "USD", "lunch, with \"friends\"", ""), rows[1])
        assertEquals("Namaa-2026.csv", yearFileName(2026))
    }

    // Debts
    private val loan = Debt("d1", "Sam", Direction.TheyOweMe, 10_000, d(2026, 10, 1))
    @Test fun partialPaymentsReduceWhatIsLeft() {
        val d1 = loan.withPayment(Payment(d(2026, 10, 5), 3_000)).withPayment(Payment(d(2026, 10, 9), 2_000))
        assertEquals(5_000, d1.remainingMinor); assertEquals(5_000, d1.paidMinor); assertTrue(!d1.settled)
        assertTrue(d1.withPayment(Payment(d(2026, 10, 12), 5_000)).settled)
    }
    @Test fun badPaymentsAreRefused() {
        assertFailsWith<IllegalArgumentException> { loan.withPayment(Payment(d(2026, 10, 5), 0)) }
        assertFailsWith<IllegalArgumentException> { loan.withPayment(Payment(d(2026, 10, 5), 10_001)) }
    }
    @Test fun totalsAndPersonBalancesNetBothDirections() {
        val debts = listOf(loan.withPayment(Payment(d(2026, 10, 5), 4_000)), Debt("d2", " sam ", Direction.IOweThem, 1_500, d(2026, 10, 2)), Debt("d3", "Lina", Direction.IOweThem, 2_000, d(2026, 10, 2)).withPayment(Payment(d(2026, 10, 3), 2_000)))
        assertEquals(DebtTotals(6_000, 1_500), debtTotals(debts)); assertEquals(4_500, debtTotals(debts).netMinor)
        assertEquals(listOf("Sam" to 4_500L), balanceByPerson(debts))
    }
    @Test fun budgetsRecurringAndDebtsSurviveTheFiles() {
        val b = book(); b.saveBudgets(listOf(Budget("Food", 10_000))); b.saveRecurring(listOf(Recurring("r", -500, "c", frequency = Frequency.Monthly, start = d(2026, 1, 31)))); b.saveDebts(listOf(loan.withPayment(Payment(d(2026, 10, 5), 1_000, "first"))))
        assertEquals(listOf(Budget("Food", 10_000)), b.budgets()); assertEquals(d(2026, 1, 31), b.recurring().single().start); assertEquals(9_000, b.debts().single().remainingMinor)
    }
}
