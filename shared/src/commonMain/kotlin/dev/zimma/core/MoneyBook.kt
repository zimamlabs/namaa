package dev.zimma.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/**
 * A Zimma folder of plain files, readable in any editor.
 *
 * ```
 * accounts.json    the accounts
 * <year>.csv       that year's transactions, amounts in minor units
 * budgets.json     monthly limits per category
 * recurring.json   repeating transactions
 * debts.json       money lent and borrowed, with payments
 * exports/         CSV exports of a month or a year
 * ```
 */
class MoneyBook(
    val fs: FileSystem,
    val root: Path,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    init { fs.createDirectories(root) }

    fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    private fun read(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    // Write to a temp file and move it over, so a crash never leaves a half-written file.
    private fun write(path: Path, text: String) {
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(text) }
        fs.atomicMove(tmp, path)
    }

    private val accountsPath get() = root / "accounts.json"

    fun accounts(): List<Account> = read(accountsPath)?.let { runCatching { json.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull() }.orEmpty()

    fun saveAccounts(accounts: List<Account>) = write(accountsPath, json.encodeToString(ListSerializer(Account.serializer()), accounts))

    private fun txnPath(year: Int) = root / "$year.csv"

    private fun txnYears(): List<Int> = fs.listOrNull(root).orEmpty().filter { it.name.endsWith(".csv") }.mapNotNull { it.name.removeSuffix(".csv").toIntOrNull() }

    fun transactions(year: Int): List<Txn> = Csv.parse(read(txnPath(year)) ?: "").drop(1).mapNotNull { r ->
        runCatching { Txn(r[0], LocalDate.parse(r[1]), r[2].toLong(), r[3], r.getOrElse(4) { "" }, r.getOrElse(5) { "" }, r.getOrElse(6) { "" }) }.getOrNull()
    }

    fun allTransactions(): List<Txn> = txnYears().sorted().flatMap { transactions(it) }.sortedByDescending { it.date }

    /** Adds the transaction, or replaces the one with the same id (moving it if its year changed). */
    fun saveTransaction(txn: Txn): Txn {
        val saved = if (txn.id.isEmpty()) txn.copy(id = newId()) else txn
        allTransactions().firstOrNull { it.id == saved.id && it.date.year != saved.date.year }?.let { deleteTransaction(it.id) }
        val year = saved.date.year
        writeYear(year, transactions(year).filter { it.id != saved.id } + saved)
        return saved
    }

    /** Moves [amountMinor] (positive) from one account to another as two legs that always sum to zero; returns the transfer id. */
    fun saveTransfer(date: LocalDate, fromAccount: String, toAccount: String, amountMinor: Long, note: String = ""): String {
        require(amountMinor > 0 && fromAccount != toAccount) { "a transfer needs a positive amount and two different accounts" }
        val tid = newId()
        saveTransaction(Txn("", date, -amountMinor, fromAccount, "Transfer", note, tid))
        saveTransaction(Txn("", date, amountMinor, toAccount, "Transfer", note, tid))
        return tid
    }

    /** Deletes the transaction; for a transfer both legs go together so balances never drift. */
    fun deleteTransaction(id: String) {
        val tid = allTransactions().firstOrNull { it.id == id }?.transferId.orEmpty()
        txnYears().forEach { y -> transactions(y).let { list -> if (list.any { it.id == id || (tid.isNotEmpty() && it.transferId == tid) }) writeYear(y, list.filter { it.id != id && (tid.isEmpty() || it.transferId != tid) }) } }
    }

    private fun writeYear(year: Int, list: List<Txn>) {
        val rows = listOf(listOf("id", "date", "amount_minor", "account", "category", "note", "transfer")) +
            list.sortedBy { it.date }.map { listOf(it.id, it.date.toString(), it.amountMinor.toString(), it.accountId, it.category, it.note, it.transferId) }
        write(txnPath(year), Csv.format(rows))
    }

    private fun <T> readList(name: String, ser: kotlinx.serialization.KSerializer<T>): List<T> =
        read(root / name)?.let { runCatching { json.decodeFromString(ListSerializer(ser), it) }.getOrNull() }.orEmpty()

    private fun <T> writeList(name: String, ser: kotlinx.serialization.KSerializer<T>, list: List<T>) = write(root / name, json.encodeToString(ListSerializer(ser), list))

    fun budgets(): List<Budget> = readList("budgets.json", Budget.serializer())
    fun saveBudgets(list: List<Budget>) = writeList("budgets.json", Budget.serializer(), list)

    fun recurring(): List<Recurring> = readList("recurring.json", Recurring.serializer())
    fun saveRecurring(list: List<Recurring>) = writeList("recurring.json", Recurring.serializer(), list)

    fun debts(): List<Debt> = readList("debts.json", Debt.serializer())
    fun saveDebts(list: List<Debt>) = writeList("debts.json", Debt.serializer(), list)

    /** Turns every repeating transaction that has come due (up to [today]) into a real one; safe to call again, it never doubles. Returns how many were added. */
    fun applyRecurring(today: LocalDate = today()): Int {
        var added = 0
        val rules = recurring()
        val updated = rules.map { rule ->
            val due = dueDates(rule, today)
            due.forEach { d ->
                if (allTransactions().none { it.id == recurringTxnId(rule, d) }) {
                    saveTransaction(Txn(recurringTxnId(rule, d), d, rule.amountMinor, rule.accountId, rule.category, rule.note)); added++
                }
            }
            if (due.isEmpty()) rule else rule.copy(lastGenerated = due.last())
        }
        if (updated != rules) saveRecurring(updated)
        return added
    }

    /** Writes the CSV for the given transactions into `exports/` and returns its path. */
    fun export(fileName: String, txns: List<Txn>): Path {
        val path = root / "exports" / fileName
        fs.createDirectories(path.parent!!)
        write(path, exportCsv(txns, accounts()))
        return path
    }
}
