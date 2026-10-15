package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.ForwardingFileSystem
import okio.Path
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

    /** A file system whose Nth atomic move fails, like a crash or a full disk in the middle of a save. */
    private class FailingMoves(delegate: FakeFileSystem, var failAt: Int = Int.MAX_VALUE) : ForwardingFileSystem(delegate) {
        var moves = 0
        override fun atomicMove(source: Path, target: Path) { if (++moves >= failAt) throw okio.IOException("disk full"); super.atomicMove(source, target) }
    }

    private fun transferLegs(b: MoneyBook) = b.allTransactions().filter { it.transferId.isNotEmpty() }

    @Test fun aFailingSaveLeavesBothLegsOrNeither() {
        for (failAt in 1..3) {
            val fs = FailingMoves(FakeFileSystem()); val b = MoneyBook(fs, "/z".toPath())
            b.saveAccounts(listOf(cash, bank)); fs.moves = 0; fs.failAt = failAt
            runCatching { b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000) }
            assertTrue(transferLegs(b).size in setOf(0, 2), "failAt=$failAt")
            assertEquals(0, transferLegs(b).sumOf { it.amountMinor })
        }
    }

    @Test fun aTransferIsOneWritePerYearFile() {
        val fs = FailingMoves(FakeFileSystem()); val b = MoneyBook(fs, "/z".toPath()); b.saveAccounts(listOf(cash, bank)); fs.moves = 0
        b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000)
        assertEquals(1, fs.moves)
    }

    @Test fun aSingleLegCannotBeSavedOnItsOwn() {
        val b = book(); b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000)
        val leg = transferLegs(b).first()
        assertFailsWith<IllegalArgumentException> { b.saveTransaction(leg.copy(amountMinor = leg.amountMinor * 2)) }
        assertEquals(0, transferLegs(b).sumOf { it.amountMinor })
    }

    @Test fun editingATransferUpdatesBothLegsAndTheyStillSumToZero() {
        val b = book(); val tid = b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000, "old")
        val ids = transferLegs(b).map { it.id }.toSet()
        b.updateTransfer(tid, LocalDate(2026, 10, 5), "b", "c", 2_500, "new")
        val legs = transferLegs(b)
        assertEquals(ids, legs.map { it.id }.toSet())
        assertEquals(0, legs.sumOf { it.amountMinor })
        assertEquals(-2_500, legs.first { it.accountId == "b" }.amountMinor); assertEquals(2_500, legs.first { it.accountId == "c" }.amountMinor)
        assertTrue(legs.all { it.note == "new" && it.date == LocalDate(2026, 10, 5) })
    }

    @Test fun editingATransferIntoAnotherYearMovesBothLegs() {
        val b = book(); val tid = b.saveTransfer(LocalDate(2026, 1, 2), "c", "b", 4_000)
        b.updateTransfer(tid, LocalDate(2025, 12, 31), "c", "b", 4_000, "")
        assertEquals(2, b.transactions(2025).size); assertTrue(b.transactions(2026).isEmpty())
    }

    @Test fun editingATransferWithBadValuesIsRefusedAndChangesNothing() {
        val b = book(); val tid = b.saveTransfer(LocalDate(2026, 10, 2), "c", "b", 4_000)
        assertFailsWith<IllegalArgumentException> { b.updateTransfer(tid, LocalDate(2026, 10, 2), "c", "c", 100, "") }
        assertFailsWith<IllegalArgumentException> { b.updateTransfer("nope", LocalDate(2026, 10, 2), "c", "b", 100, "") }
        assertEquals(listOf(-4_000L, 4_000L), transferLegs(b).map { it.amountMinor }.sorted())
    }
}
