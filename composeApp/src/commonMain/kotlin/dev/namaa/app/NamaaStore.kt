package dev.namaa.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.namaa.core.Account
import dev.namaa.core.Backup
import okio.Path.Companion.toPath
import dev.namaa.core.Budget
import dev.namaa.core.Debt
import dev.namaa.core.Recurring
import dev.namaa.core.DataLocation
import dev.namaa.core.DataProblem
import dev.namaa.core.MoneyBook
import dev.namaa.core.Settings
import dev.namaa.core.SettingsRepository
import dev.namaa.core.Txn
import dev.namaa.core.currenciesOf
import dev.namaa.core.currencyCode
import dev.namaa.core.txnsInCurrency
import dev.namaa.core.newId
import kotlinx.datetime.LocalDate

/** [askAccess] opens the system screen that grants access to the shared Zimam folder; null where there is no such step. */
class NamaaStore(book: MoneyBook, private val settingsRepository: SettingsRepository, val location: DataLocation, val askAccess: (() -> Unit)? = null, val files: FileActions? = null, val lock: AppLock? = null) {
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
    var settings by mutableStateOf(settingsRepository.current)
        private set
    var tourRequested by mutableStateOf(false)
    /** Bumps when the data folder changes, so the folder section can look again. */
    var folderTick by mutableIntStateOf(0)
        private set

    /** Files that exist but could not be read. While any are listed the app shows a recovery screen and writes nothing. */
    var blocked by mutableStateOf(emptyList<DataProblem>())
        private set
    /** Year files with rows that could not be read; the file was copied before anything can be rewritten. */
    /** Files kept beside the data (from an older folder) that Namaa did not fold in. */
    var sideCopies by mutableStateOf(emptyList<String>())
        private set
    var rowProblems by mutableStateOf(emptyList<DataProblem>())
        private set

    init {
        ensureCash()
        reload()
    }

    // First launch: a plain Cash account so nothing needs setting up.
    // Only when there is no accounts file at all: an existing file that fails to read is never replaced.
    private fun ensureCash() { if (!book.hasAccountsFile()) book.saveAccounts(listOf(Account(newId(), "Cash", settings.currency))) }

    /** The currency the month figures and charts show when the accounts use more than one; never added together. */
    var shownCurrency by mutableStateOf("")
    val currencies: List<String> get() = currenciesOf(accounts)
    val activeCurrency: String get() = currencies.let { c -> shownCurrency.takeIf { it in c } ?: settings.currency.currencyCode().takeIf { it in c } ?: c.firstOrNull() ?: settings.currency.currencyCode() }
    /** The transactions of [activeCurrency] (all of them when there is only one currency). */
    val activeTxns: List<Txn> get() = if (currencies.size <= 1) txns else txnsInCurrency(txns, accounts, activeCurrency)

    /** Anything beyond the automatic Cash account counts as the user's own data. */
    val hasData: Boolean get() = txns.isNotEmpty() || accounts.size > 1

    fun reload() {
        today = book.today()
        blocked = book.unreadableFiles()
        if (blocked.isNotEmpty()) {
            accounts = emptyList(); txns = emptyList(); budgets = emptyList(); recurring = emptyList(); debts = emptyList(); rowProblems = emptyList(); sideCopies = emptyList()
            return
        }
        // Rows saved in another folder while the shared one was out of reach are folded back in.
        book.mergeSideCopies()
        sideCopies = book.unmergedSideCopies()
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

    /** True while the app lock is on and the user has not got through the phone's lock screen yet. */
    var locked by mutableStateOf(settingsRepository.current.appLock && lock != null)

    fun relock() { if (settings.appLock && lock != null) locked = true }

    /** Moves all data files into a `cleared-<stamp>` folder (nothing is destroyed) and starts fresh; returns that folder. */
    fun clearAll(): String {
        val stamp = book.stamp()
        book.clearAll(stamp)
        ensureCash(); reload()
        return (book.root / "cleared-$stamp").toString()
    }

    /** The transaction being split, and the account whose history was asked for; the screens act on these. */
    var splitting by mutableStateOf<Txn?>(null)
    var historyAccount by mutableStateOf<String?>(null)

    fun duplicateTxn(id: String) { book.duplicateTransaction(id); txns = book.allTransactions() }

    fun splitTxn(id: String, firstMinor: Long, category: String) { book.splitTransaction(id, firstMinor, category); txns = book.allTransactions() }

    fun setArchived(accountId: String, archived: Boolean) = saveAccounts(accounts.map { if (it.id == accountId) it.copy(archived = archived) else it })

    /** The accounts new entries can go to. */
    val activeAccounts: List<Account> get() = accounts.filter { !it.archived }

    /** A whole-folder backup, checked by reading it back, handed to the system "save as" screen. */
    fun backupEverything(onDone: (Boolean) -> Unit) {
        val actions = files ?: return onDone(false)
        val stamp = book.stamp()
        val bytes = Backup.create(book.fs, book.root, stamp)
        if (!Backup.matchesFolder(bytes, book.fs, book.root)) return onDone(false)
        actions.saveAs(Backup.fileName(stamp), bytes.toByteArray(), onDone)
    }

    /** Folds a backup in (current data wins, nothing deleted, a safety backup is kept first), then reloads. */
    fun restore(entries: List<Backup.Entry>) { book.restoreFrom(entries, book.stamp()); reload() }

    /** Saves a copy of an exported file wherever the user chooses. */
    fun saveCopyOf(path: String, onDone: (Boolean) -> Unit) {
        val actions = files ?: return onDone(false)
        val p = path.toPath()
        actions.saveAs(p.name, book.fs.read(p) { readByteArray() }, onDone)
    }

    /** Writes the CSV into the Zimam folder's `exports/` and returns where it went. */
    fun export(fileName: String, list: List<Txn>): String = book.export(fileName, list).toString()

    fun useBook(newBook: MoneyBook) { book = newBook; ensureCash(); reload(); folderTick++ }

    fun updateSettings(change: (Settings) -> Settings) { settings = settingsRepository.update(change) }

    fun setCurrency(code: String) {
        updateSettings { it.copy(currency = code) }
        if (accounts.size == 1 && txns.isEmpty()) saveAccounts(listOf(accounts[0].copy(currency = code)))
    }

    fun saveAccounts(list: List<Account>) { book.saveAccounts(list); accounts = list }

    fun saveTxn(txn: Txn) { book.saveTransaction(txn); txns = book.allTransactions() }

    fun deleteTxn(id: String) { book.deleteTransaction(id); txns = book.allTransactions() }

    fun updateTransfer(tid: String, date: LocalDate, from: String, to: String, amountMinor: Long, note: String) {
        book.updateTransfer(tid, date, from, to, amountMinor, note); txns = book.allTransactions()
    }

    fun saveTransfer(date: LocalDate, from: String, to: String, amountMinor: Long, note: String) {
        book.saveTransfer(date, from, to, amountMinor, note); txns = book.allTransactions()
    }
}
