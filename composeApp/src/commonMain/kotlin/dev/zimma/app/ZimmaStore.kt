package dev.zimma.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.zimma.core.Account
import dev.zimma.core.DataLocation
import dev.zimma.core.MoneyBook
import dev.zimma.core.Prefs
import dev.zimma.core.PrefsFile
import dev.zimma.core.Txn
import dev.zimma.core.newId
import kotlinx.datetime.LocalDate

/** [askAccess] opens the system screen that grants access to the shared Zimam folder; null where there is no such step. */
class ZimmaStore(book: MoneyBook, private val prefsFile: PrefsFile, val location: DataLocation, val askAccess: (() -> Unit)? = null) {
    var book by mutableStateOf(book)
        private set
    var accounts by mutableStateOf(emptyList<Account>())
        private set
    var txns by mutableStateOf(emptyList<Txn>())
        private set
    var today by mutableStateOf(book.today())
        private set
    var prefs by mutableStateOf(prefsFile.load())
        private set
    var tourRequested by mutableStateOf(false)
    /** Bumps when the data folder changes, so the folder section can look again. */
    var folderTick by mutableIntStateOf(0)
        private set

    init {
        ensureCash()
        reload()
    }

    // First launch: a plain Cash account so nothing needs setting up.
    private fun ensureCash() { if (book.accounts().isEmpty()) book.saveAccounts(listOf(Account(newId(), "Cash", prefs.currency))) }

    /** Anything beyond the automatic Cash account counts as the user's own data. */
    val hasData: Boolean get() = txns.isNotEmpty() || accounts.size > 1

    fun reload() {
        accounts = book.accounts()
        txns = book.allTransactions()
        today = book.today()
    }

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
