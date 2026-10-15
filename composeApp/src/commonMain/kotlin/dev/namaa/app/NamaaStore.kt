package dev.namaa.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.namaa.core.Account
import dev.namaa.core.Budget
import dev.namaa.core.Debt
import dev.namaa.core.Recurring
import dev.namaa.core.DataLocation
import dev.namaa.core.DataProblem
import dev.namaa.core.MoneyBook
import dev.namaa.core.Prefs
import dev.namaa.core.PrefsFile
import dev.namaa.core.Txn
import dev.namaa.core.newId
import kotlinx.datetime.LocalDate

/** [askAccess] opens the system screen that grants access to the shared Zimam folder; null where there is no such step. */
class NamaaStore(book: MoneyBook, private val prefsFile: PrefsFile, val location: DataLocation, val askAccess: (() -> Unit)? = null) {
    var book by mutableStateOf(book)
        private set
    var accounts by mutableStateOf(emptyList<Account>())
        private set
    var txns by mutableStateOf(emptyList<Txn>())
        private set
    var today by mutableStateOf(book.today())
        private set
    var budgets by mutableStateOf(emptyList<Budget>())
        private set
    var recurring by mutableStateOf(emptyList<Recurring>())
        private set
    var debts by mutableStateOf(emptyList<Debt>())
        private set
    var prefs by mutableStateOf(prefsFile.load())
        private set
    var tourRequested by mutableStateOf(false)
    /** Bumps when the data folder changes, so the folder section can look again. */
    var folderTick by mutableIntStateOf(0)
        private set

    /** Files that exist but could not be read. While any are listed the app shows a recovery screen and writes nothing. */
    var blocked by mutableStateOf(emptyList<DataProblem>())
        private set
    /** Year files with rows that could not be read; the file was copied before anything can be rewritten. */
    var rowProblems by mutableStateOf(emptyList<DataProblem>())
        private set

    init {
        ensureCash()
        reload()
    }

    // First launch: a plain Cash account so nothing needs setting up.
    // Only when there is no accounts file at all: an existing file that fails to read is never replaced.
    private fun ensureCash() { if (!book.hasAccountsFile()) book.saveAccounts(listOf(Account(newId(), "Cash", prefs.currency))) }

    /** Anything beyond the automatic Cash account counts as the user's own data. */
    val hasData: Boolean get() = txns.isNotEmpty() || accounts.size > 1

    fun reload() {
        today = book.today()
        blocked = book.unreadableFiles()
        if (blocked.isNotEmpty()) {
            accounts = emptyList(); txns = emptyList(); budgets = emptyList(); recurring = emptyList(); debts = emptyList(); rowProblems = emptyList()
            return
        }
        // Repeating transactions that came due since the last open are added here, once each.
        book.applyRecurring(today)
        accounts = book.accounts()
        txns = book.allTransactions()
        budgets = book.budgets()
        recurring = book.recurring()
        debts = book.debts()
        rowProblems = book.rowProblems()
    }

    /** The user chose to go on without the unreadable files (copies are kept next to them). */
    fun startFresh() { book.startFresh(); ensureCash(); reload() }

    fun saveBudgets(list: List<Budget>) { book.saveBudgets(list); budgets = list }

    fun saveRecurring(list: List<Recurring>) { book.saveRecurring(list); reload() }

    fun saveDebts(list: List<Debt>) { book.saveDebts(list); debts = list }

    /** Writes the CSV into the Zimam folder's `exports/` and returns where it went. */
    fun export(fileName: String, list: List<Txn>): String = book.export(fileName, list).toString()

    fun useBook(newBook: MoneyBook) { book = newBook; ensureCash(); reload(); folderTick++ }

    fun updatePrefs(change: (Prefs) -> Prefs) { prefs = change(prefs); prefsFile.save(prefs) }

    fun setCurrency(code: String) {
        updatePrefs { it.copy(currency = code) }
        if (accounts.size == 1 && txns.isEmpty()) saveAccounts(listOf(accounts[0].copy(currency = code)))
    }

    fun saveAccounts(list: List<Account>) { book.saveAccounts(list); accounts = list }

    fun saveTxn(txn: Txn) { book.saveTransaction(txn); txns = book.allTransactions() }

    fun deleteTxn(id: String) { book.deleteTransaction(id); txns = book.allTransactions() }

    fun saveTransfer(date: LocalDate, from: String, to: String, amountMinor: Long, note: String) {
        book.saveTransfer(date, from, to, amountMinor, note); txns = book.allTransactions()
    }
}
