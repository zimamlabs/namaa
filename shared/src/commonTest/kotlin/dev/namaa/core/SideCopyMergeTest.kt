package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SideCopyMergeTest {
    private val fs = FakeFileSystem()
    private val priv = "/data/${LegacyNames.FOLDER}".toPath()
    private val shared = "/sdcard/Zimam/Namaa".toPath()
    private var allowed = false
    private val location = DataLocation(fs, priv, shared, listOf(priv)) { allowed }
    private fun d(day: Int) = LocalDate(2026, 10, day)

    /** Work in the shared folder, lose access and keep working privately, then get access back (the folder copy keeps the private file beside). */
    private fun scenario(): MoneyBook {
        allowed = true; location.refresh()
        val main = MoneyBook(fs, shared); main.saveAccounts(listOf(Account("c", "Cash")))
        main.saveTransaction(Txn("s1", d(1), -100, "c", "Food"))
        allowed = false; location.refresh()
        val privateBook = MoneyBook(fs, priv)
        privateBook.saveAccounts(listOf(Account("c", "Cash"), Account("p", "Phone only")))
        privateBook.saveTransaction(Txn("p1", d(2), -200, "p", "Fun")); privateBook.saveTransaction(Txn("s1", d(1), -100, "c", "Food"))
        allowed = true; location.refresh()
        return MoneyBook(fs, shared)
    }

    @Test fun transactionsFromThePrivateFallbackStayVisibleAfterAccessReturns() {
        val b = scenario()
        assertTrue(fs.list(shared).any { it.name.contains("(from ") }, "the clash copy is beside the file")
        assertEquals(setOf("s1"), b.allTransactions().map { it.id }.toSet(), "before merging they are hidden")
        val report = b.mergeSideCopies()
        assertEquals(setOf("s1", "p1"), b.allTransactions().map { it.id }.toSet())
        assertEquals(setOf("c", "p"), b.accounts().map { it.id }.toSet())
        assertEquals(1, report.mergedTransactions)
        assertTrue(b.unmergedSideCopies().isEmpty())
    }

    @Test fun mergingTwiceChangesNothingAndADeletedRowStaysDeleted() {
        val b = scenario(); b.mergeSideCopies()
        b.deleteTransaction("p1")
        val again = b.mergeSideCopies()
        assertEquals(0, again.mergedTransactions)
        assertEquals(setOf("s1"), b.allTransactions().map { it.id }.toSet())
    }

    @Test fun theSideFilesAreNeverDeleted() {
        val b = scenario(); val before = fs.list(shared).filter { it.name.contains("(from ") }.map { it.name }.sorted()
        b.mergeSideCopies()
        assertEquals(before, fs.list(shared).filter { it.name.contains("(from ") }.map { it.name }.sorted())
    }

    @Test fun otherSideCopiesAreReportedNotMerged() {
        val b = scenario()
        fs.write(shared / "budgets (from X).json") { writeUtf8("[]") }
        b.mergeSideCopies()
        assertEquals(listOf("budgets (from X).json"), b.unmergedSideCopies())
    }

    @Test fun anUnreadableSideFileIsLeftAloneAndReported() {
        val b = scenario()
        fs.write(shared / "2026 (from X).csv") { writeUtf8("id,date,amount_minor,account,category,note,transfer\nbad,nope,1,c,,,\n") }
        b.mergeSideCopies()
        assertTrue("2026 (from X).csv" in b.unmergedSideCopies())
    }
}
