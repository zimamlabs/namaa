package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurringEditTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate(y, m, day)
    private class CountingMoves(delegate: FakeFileSystem) : ForwardingFileSystem(delegate) {
        val moved = mutableListOf<String>()
        override fun atomicMove(source: Path, target: Path) { moved += target.name; super.atomicMove(source, target) }
    }
    private fun rule(start: LocalDate, freq: Frequency = Frequency.Monthly, last: LocalDate? = null) = Recurring("r1", -1_000, "c", "Bills", "", freq, start, last)

    @Test fun movingTheStartOfAMonthlyRuleDoesNotAddAnotherTransactionForAMonthAlreadyCovered() {
        val b = MoneyBook(FakeFileSystem(), "/z".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        b.saveRecurring(listOf(rule(d(2026, 7, 5))))
        assertEquals(3, b.applyRecurring(d(2026, 9, 20)))
        // the user moves the start to the 6th, the way the dialog now saves it (history kept)
        val edited = b.recurring().single().copy(start = d(2026, 7, 6))
        b.saveRecurring(listOf(edited))
        assertEquals(0, b.applyRecurring(d(2026, 9, 20)))
        assertEquals(3, b.allTransactions().size)
        assertEquals(1, b.applyRecurring(d(2026, 10, 20)))
        assertEquals(d(2026, 10, 6), b.allTransactions().maxOf { it.date })
    }

    @Test fun aWeeklyRuleShiftedByADayDoesNotAddTwoInOneWeek() {
        val r = rule(d(2026, 10, 1), Frequency.Weekly, last = d(2026, 10, 15)).copy(start = d(2026, 10, 2))
        assertEquals(listOf(d(2026, 10, 23)), dueDates(r, d(2026, 10, 25)))
    }

    @Test fun anUnchangedRuleStillGeneratesEachPeriodOnce() {
        val r = rule(d(2026, 1, 31), last = d(2026, 1, 31))
        assertEquals(listOf(d(2026, 2, 28), d(2026, 3, 31)), dueDates(r, d(2026, 3, 31)))
        assertEquals(listOf(d(2026, 1, 31), d(2026, 2, 28)), dueDates(rule(d(2026, 1, 31)), d(2026, 2, 28)))
    }

    @Test fun aLongCatchUpIsOneWritePerYearFile() {
        val fs = CountingMoves(FakeFileSystem()); val b = MoneyBook(fs, "/z".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        b.saveRecurring(listOf(rule(d(2023, 1, 1), Frequency.Weekly)))
        fs.moved.clear()
        val added = b.applyRecurring(d(2026, 12, 31))
        assertTrue(added > 200, "added $added")
        assertEquals(added, b.allTransactions().size)
        assertEquals(4, fs.moved.count { it.endsWith(".csv") }, fs.moved.toString())
        assertEquals(0, b.applyRecurring(d(2026, 12, 31)))
    }

    @Test fun twoRulesAndDeletedGeneratedItemsStayDeleted() {
        val b = MoneyBook(FakeFileSystem(), "/z".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        b.saveRecurring(listOf(rule(d(2026, 7, 5)), rule(d(2026, 7, 9)).copy(id = "r2")))
        assertEquals(6, b.applyRecurring(d(2026, 9, 20)))
        b.deleteTransaction(b.allTransactions().first().id)
        assertEquals(0, b.applyRecurring(d(2026, 9, 20)))
        assertEquals(5, b.allTransactions().size)
    }
}
