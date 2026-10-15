package dev.namaa.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/**
 * A Namaa folder of plain files, readable in any editor.
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
/** A data file Namaa could not read, and the copy of it that was kept. */
data class DataProblem(val file: String, val copy: String)

/** Saving was refused because the file on disk is unreadable and would be overwritten; see [MoneyBook.unreadableFiles]. */
class UnreadableFileException(val file: String) : Exception("$file could not be read, so Namaa will not overwrite it")

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

    private val listFiles: List<Pair<String, (String) -> Unit>> = listOf(
        "accounts.json" to { t -> json.decodeFromString(ListSerializer(Account.serializer()), t) },
        "budgets.json" to { t -> json.decodeFromString(ListSerializer(Budget.serializer()), t) },
        "recurring.json" to { t -> json.decodeFromString(ListSerializer(Recurring.serializer()), t) },
        "debts.json" to { t -> json.decodeFromString(ListSerializer(Debt.serializer()), t) },
    )

    fun hasAccountsFile(): Boolean = fs.exists(accountsPath)

    /** The JSON files that exist but cannot be decoded. Each is copied to `<name>.corrupt-<stamp>` (once per distinct content). Empty when everything reads. */
    fun unreadableFiles(): List<DataProblem> = listFiles.mapNotNull { unreadable(it.first) }

    private fun unreadable(name: String): DataProblem? {
        val text = read(root / name) ?: return null
        return if (runCatching { listFiles.first { it.first == name }.second(text) }.isSuccess) null else DataProblem(name, keepCopy(name))
    }

    /** The user chose to continue without the unreadable files: each is moved away, only after its copy is confirmed. */
    fun startFresh() {
        unreadableFiles().forEach { fs.delete(root / it.file) }
    }

    private fun guardWritable(name: String) {
        if (unreadable(name) != null) throw UnreadableFileException(name)
    }

    /** Copies [name] to `<name>.corrupt-<stamp>` unless an identical copy already exists; returns the copy's name. */
    private fun keepCopy(name: String): String {
        val bytes = fs.read(root / name) { readByteString() }
        fs.list(root).firstOrNull { it.name.startsWith("$name.corrupt-") && fs.read(it) { readByteString() } == bytes }?.let { return it.name }
        val stamp = clock.now().toLocalDateTime(zone).let { listOf(it.year, it.monthNumber, it.dayOfMonth).joinToString("") { n -> n.toString().padStart(2, '0') } + "-" + listOf(it.hour, it.minute, it.second).joinToString("") { n -> n.toString().padStart(2, '0') } }
        var copy = "$name.corrupt-$stamp"
        var n = 2
        while (fs.exists(root / copy)) copy = "$name.corrupt-$stamp-${n++}"
        fs.write(root / copy) { write(bytes) }
        check(fs.read(root / copy) { readByteString() } == bytes) { "could not keep a copy of $name" }
        return copy
    }

    private val rowIssues = mutableMapOf<String, DataProblem>()

    /** Year files with rows that could not be read; the whole file is kept as a copy so no row is lost on rewrite. */
    fun rowProblems(): List<DataProblem> = rowIssues.values.toList()

    fun accounts(): List<Account> = readList("accounts.json", Account.serializer())

    fun saveAccounts(accounts: List<Account>) = writeList("accounts.json", Account.serializer(), accounts)

    private fun txnPath(year: Int) = root / "$year.csv"

    private fun txnYears(): List<Int> = fs.listOrNull(root).orEmpty().filter { it.name.endsWith(".csv") }.mapNotNull { it.name.removeSuffix(".csv").toIntOrNull() }

    fun transactions(year: Int): List<Txn> {
        val name = "$year.csv"
        val rows = Csv.parse(read(txnPath(year)) ?: "").drop(1)
        val parsed = rows.map { r -> runCatching { Txn(r[0], LocalDate.parse(r[1]), r[2].toLong(), r[3], r.getOrElse(4) { "" }, r.getOrElse(5) { "" }, r.getOrElse(6) { "" }) }.getOrNull() }
        if (parsed.any { it == null }) rowIssues[name] = DataProblem(name, keepCopy(name)) else rowIssues.remove(name)
        return parsed.filterNotNull()
    }

    fun allTransactions(): List<Txn> = txnYears().sorted().flatMap { transactions(it) }.sortedByDescending { it.date }

    /** Adds the transaction, or replaces the one with the same id (moving it if its year changed). */
    fun saveTransaction(txn: Txn): Txn {
        require(txn.transferId.isEmpty()) { "a transfer leg is never saved alone; use updateTransfer" }
        val saved = if (txn.id.isEmpty()) txn.copy(id = newId()) else txn
        allTransactions().firstOrNull { it.id == saved.id && it.date.year != saved.date.year }?.let { deleteTransaction(it.id) }
        val year = saved.date.year
        writeYear(year, transactions(year).filter { it.id != saved.id } + saved)
        return saved
    }

    /** Moves [amountMinor] (positive) from one account to another as two legs that always sum to zero, written together; returns the transfer id. */
    fun saveTransfer(date: LocalDate, fromAccount: String, toAccount: String, amountMinor: Long, note: String = ""): String {
        val tid = newId()
        val legs = transferLegs(tid, newId(), newId(), date, fromAccount, toAccount, amountMinor, note)
        writeYear(date.year, transactions(date.year) + legs)
        return tid
    }

    private fun transferLegs(tid: String, outId: String, inId: String, date: LocalDate, from: String, to: String, amountMinor: Long, note: String): List<Txn> {
        require(amountMinor > 0 && from != to) { "a transfer needs a positive amount and two different accounts" }
        return listOf(Txn(outId, date, -amountMinor, from, "Transfer", note, tid), Txn(inId, date, amountMinor, to, "Transfer", note, tid))
    }

    /** Changes both legs of transfer [tid] together (ids kept); nothing changes if the values are refused. */
    fun updateTransfer(tid: String, date: LocalDate, fromAccount: String, toAccount: String, amountMinor: Long, note: String) {
        val old = allTransactions().filter { it.transferId == tid }
        require(old.size == 2) { "no such transfer" }
        val legs = transferLegs(tid, old.first { it.amountMinor < 0 }.id, old.first { it.amountMinor > 0 }.id, date, fromAccount, toAccount, amountMinor, note)
        val oldYear = old.first().date.year
        // Add to the new year first: a crash then leaves a visible duplicate, never a lost transfer.
        writeYear(date.year, transactions(date.year).filter { it.transferId != tid } + legs)
        if (oldYear != date.year) writeYear(oldYear, transactions(oldYear).filter { it.transferId != tid })
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

    // An unreadable file reads as empty here, but [unreadableFiles] reports it and [writeList] refuses to replace it.
    private fun <T> readList(name: String, ser: kotlinx.serialization.KSerializer<T>): List<T> =
        read(root / name)?.let { runCatching { json.decodeFromString(ListSerializer(ser), it) }.getOrNull() }.orEmpty()

    private fun <T> writeList(name: String, ser: kotlinx.serialization.KSerializer<T>, list: List<T>) {
        guardWritable(name)
        write(root / name, json.encodeToString(ListSerializer(ser), list))
    }

    fun budgets(): List<Budget> = readList("budgets.json", Budget.serializer())
    fun saveBudgets(list: List<Budget>) = writeList("budgets.json", Budget.serializer(), list)

    fun recurring(): List<Recurring> = readList("recurring.json", Recurring.serializer())
    fun saveRecurring(list: List<Recurring>) = writeList("recurring.json", Recurring.serializer(), list)

    fun debts(): List<Debt> = readList("debts.json", Debt.serializer())
    fun saveDebts(list: List<Debt>) = writeList("debts.json", Debt.serializer(), list)

    /** Turns every repeating transaction that has come due (up to [today]) into a real one; safe to call again, it never doubles. Returns how many were added. */
    fun applyRecurring(today: LocalDate = today()): Int {
        val rules = recurring()
        val known = allTransactions().mapTo(HashSet()) { it.id }
        val fresh = sortedMapOf<Int, MutableList<Txn>>()
        val updated = rules.map { rule ->
            val due = dueDates(rule, today)
            due.forEach { d ->
                val id = recurringTxnId(rule, d)
                if (known.add(id)) fresh.getOrPut(d.year) { mutableListOf() } += Txn(id, d, rule.amountMinor, rule.accountId, rule.category, rule.note)
            }
            if (due.isEmpty()) rule else rule.copy(lastGenerated = due.last())
        }
        // One write per year file; the rules are saved after the transactions, and ids make a repeat after a crash harmless.
        fresh.forEach { (year, list) -> writeYear(year, transactions(year) + list) }
        if (updated != rules) saveRecurring(updated)
        return fresh.values.sumOf { it.size }
    }

    /** Writes the CSV for the given transactions into `exports/` and returns its path. */
    fun export(fileName: String, txns: List<Txn>): Path {
        val path = root / "exports" / fileName
        fs.createDirectories(path.parent!!)
        write(path, exportCsv(txns, accounts()))
        return path
    }
}
