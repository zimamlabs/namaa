package dev.namaa.core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RowActionsTest {
    private fun book() = MoneyBook(FakeFileSystem(), "/z".toPath()).also { it.saveAccounts(listOf(Account("c", "Cash"), Account("b", "Bank"))) }
    private val day = LocalDate(2026, 10, 2)

    @Test fun splittingKeepsTheTotalSignAccountAndDate() {
        val b = book(); b.saveTransaction(Txn("t1", day, -1_000, "c", "Food", "dinner"))
        val newId = b.splitTransaction("t1", 600, "Fun")
        val all = b.allTransactions().associateBy { it.id }
        assertEquals(-600, all.getValue("t1").amountMinor); assertEquals("Food", all.getValue("t1").category)
        assertEquals(-400, all.getValue(newId).amountMinor); assertEquals("Fun", all.getValue(newId).category)
        assertEquals(-1_000, all.values.sumOf { it.amountMinor })
        assertTrue(all.values.all { it.accountId == "c" && it.date == day && it.note == "dinner" })
    }

    @Test fun splittingAnIncomeKeepsItPositive() {
        val b = book(); b.saveTransaction(Txn("t1", day, 900, "c", "Salary"))
        val n = b.splitTransaction("t1", 300, "Gift")
        assertEquals(listOf(300L, 600L), b.allTransactions().filter { it.id == n || it.id == "t1" }.map { it.amountMinor }.sorted())
    }

    @Test fun badSplitsAreRefusedAndChangeNothing() {
        val b = book(); b.saveTransaction(Txn("t1", day, -1_000, "c", "Food")); b.saveTransfer(day, "c", "b", 500)
        for (bad in listOf(0L, 1_000L, 2_000L, -5L)) assertFailsWith<IllegalArgumentException> { b.splitTransaction("t1", bad, "x") }
        assertFailsWith<IllegalArgumentException> { b.splitTransaction(b.allTransactions().first { it.transferId.isNotEmpty() }.id, 100, "x") }
        assertFailsWith<IllegalArgumentException> { b.splitTransaction("nope", 100, "x") }
        assertEquals(3, b.allTransactions().size); assertEquals(-1_000, b.allTransactions().first { it.id == "t1" }.amountMinor)
    }

    @Test fun duplicatingMakesAnIndependentCopy() {
        val b = book(); b.saveTransaction(Txn("t1", day, -1_000, "c", "Food", "x"))
        val copy = b.duplicateTransaction("t1")
        assertTrue(copy != "t1"); assertEquals(2, b.allTransactions().size)
        assertEquals(b.allTransactions().first { it.id == "t1" }.copy(id = ""), b.allTransactions().first { it.id == copy }.copy(id = ""))
        assertFailsWith<IllegalArgumentException> { b.duplicateTransaction(b.also { it.saveTransfer(day, "c", "b", 5) }.allTransactions().first { it.transferId.isNotEmpty() }.id) }
    }

    @Test fun archivedAccountsKeepTheirMoneyAndOldFilesDecode() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(false, json.decodeFromString(Account.serializer(), """{"id":"a","name":"X"}""").archived)
        val b = book(); b.saveAccounts(b.accounts().map { if (it.id == "b") it.copy(archived = true) else it })
        assertEquals(listOf("c"), b.accounts().filter { !it.archived }.map { it.id })
        b.saveTransaction(Txn("t", day, 50, "b"))
        assertEquals(50, balancesByCurrency(b.accounts(), b.allTransactions()).single().second)
    }
}
