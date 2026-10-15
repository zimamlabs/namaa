package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportTest {
    private val accounts = listOf(Account("a", "Cash", "EGP"), Account("b", "=Bank", "USD"))
    private fun rows(txns: List<Txn>) = Csv.parse(exportCsv(txns, accounts))

    @Test fun headerHasACurrencyColumnAndEachRowCarriesItsAccountsCurrency() {
        val r = rows(listOf(Txn("1", LocalDate(2026, 1, 2), -1250, "a", "Food", "x"), Txn("2", LocalDate(2026, 1, 3), 500, "b", "Salary")))
        assertEquals(listOf("date", "account", "category", "amount", "currency", "note", "transfer"), r[0])
        assertEquals("EGP", r[1][4]); assertEquals("USD", r[2][4])
    }

    @Test fun textCellsThatLookLikeFormulasAreMadeText() {
        val r = rows(listOf(Txn("1", LocalDate(2026, 1, 2), -100, "b", "+cmd", "=1+1"), Txn("2", LocalDate(2026, 1, 3), -100, "a", "@x", "-2+3"), Txn("3", LocalDate(2026, 1, 4), -100, "a", "ok", "\t=x")))
        assertEquals("'=Bank", r[1][1]); assertEquals("'+cmd", r[1][2]); assertEquals("'=1+1", r[1][5])
        assertEquals("'@x", r[2][2]); assertEquals("'-2+3", r[2][5]); assertEquals("'\t=x", r[3][5])
    }

    @Test fun amountsAndPlainTextAreNotTouched() {
        val r = rows(listOf(Txn("1", LocalDate(2026, 1, 2), -1250, "a", "Food", "lunch")))
        assertEquals("-12.50", r[1][3]); assertEquals("Food", r[1][2]); assertEquals("lunch", r[1][5])
    }

    @Test fun theInternalYearFilesAreNotAltered() {
        val b = MoneyBook(okio.fakefilesystem.FakeFileSystem(), "/z".toPath())
        b.saveAccounts(accounts); b.saveTransaction(Txn("", LocalDate(2026, 1, 2), -100, "a", "=x", "=y"))
        assertEquals("=y", b.transactions(2026).single().note)
    }
}
