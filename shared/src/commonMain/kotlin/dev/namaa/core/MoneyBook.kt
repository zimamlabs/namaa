package dev.namaa.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
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

    /** A sortable time stamp like 20261015-120000, for file names. */
    fun stamp(): String = clock.now().toLocalDateTime(zone).let { t ->
        listOf(t.year, t.monthNumber, t.dayOfMonth).joinToString("") { it.toString().padStart(2, '0') } + "-" + listOf(t.hour, t.minute, t.second).joinToString("") { it.toString().padStart(2, '0') }
    }

    private fun read(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    // Write to a temp file and move it over, so a crash never leaves a half-written file.
    private fun write(path: Path, text: String) = write(path, text.encodeUtf8())

    private fun write(path: Path, bytes: okio.ByteString) {
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { write(bytes) }
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
        requireSameCurrency(fromAccount, toAccount)
        val tid = newId()
        val legs = transferLegs(tid, newId(), newId(), date, fromAccount, toAccount, amountMinor, note)
        writeYear(date.year, transactions(date.year) + legs)
        return tid
    }

    /** Namaa has no exchange rates, so money only moves between accounts of one currency. */
    private fun requireSameCurrency(a: String, b: String) {
        val byId = accounts().associateBy { it.id }
        val x = byId[a]; val y = byId[b]
        require(x == null || y == null || x.currency.currencyCode() == y.currency.currencyCode()) { "a transfer needs two accounts in the same currency" }
    }

    private fun transferLegs(tid: String, outId: String, inId: String, date: LocalDate, from: String, to: String, amountMinor: Long, note: String): List<Txn> {
        require(amountMinor > 0 && from != to) { "a transfer needs a positive amount and two different accounts" }
        return listOf(Txn(outId, date, -amountMinor, from, "Transfer", note, tid), Txn(inId, date, amountMinor, to, "Transfer", note, tid))
    }

    /** Changes both legs of transfer [tid] together (ids kept); nothing changes if the values are refused. */
    fun updateTransfer(tid: String, date: LocalDate, fromAccount: String, toAccount: String, amountMinor: Long, note: String) {
        requireSameCurrency(fromAccount, toAccount)
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

    /** How many things a restore adds (or would add). */
    data class RestoreReport(val transactions: Int, val accounts: Int, val budgets: Int, val recurring: Int, val debts: Int)

    fun previewRestore(entries: List<Backup.Entry>): RestoreReport = restore(entries, false, "")

    /**
     * Folds a backup into the current data by id; what is already here wins and nothing is deleted. The whole backup is
     * parsed first (an unreadable file refuses the restore with nothing changed), then the current data is saved to
     * `safety-backups/` and checked, then the additions are written.
     */
    fun restoreFrom(entries: List<Backup.Entry>, stamp: String): RestoreReport = restore(entries, true, stamp)

    private fun <T> incoming(entries: List<Backup.Entry>, name: String, ser: kotlinx.serialization.KSerializer<T>): List<T> =
        entries.firstOrNull { it.name == name }?.let { e -> runCatching { json.decodeFromString(ListSerializer(ser), e.bytes.utf8()) }.getOrNull() ?: throw BackupException("$name in the backup cannot be read") }.orEmpty()

    private fun restore(entries: List<Backup.Entry>, apply: Boolean, stamp: String): RestoreReport {
        if (unreadableFiles().isNotEmpty()) throw BackupException("your current data has a file that cannot be read; fix that first")
        val accs = incoming(entries, "accounts.json", Account.serializer())
        val buds = incoming(entries, "budgets.json", Budget.serializer())
        val recs = incoming(entries, "recurring.json", Recurring.serializer())
        val debs = incoming(entries, "debts.json", Debt.serializer())
        val years = entries.filter { it.name.endsWith(".csv") }.associate { e ->
            val rows = Csv.parse(e.bytes.utf8()).drop(1)
            val txns = rows.map { r -> runCatching { Txn(r[0], LocalDate.parse(r[1]), r[2].toLong(), r[3], r.getOrElse(4) { "" }, r.getOrElse(5) { "" }, r.getOrElse(6) { "" }) }.getOrNull() ?: throw BackupException("${e.name} in the backup has a row that cannot be read") }
            e.name.removeSuffix(".csv").toInt() to txns
        }
        val curAccs = accounts(); val curBuds = budgets(); val curRecs = recurring(); val curDebs = debts()
        val newAccs = accs.filter { a -> curAccs.none { it.id == a.id } }
        fun budKey(b: Budget) = b.category.trim().lowercase() + "|" + b.currency.currencyCode()
        val newBuds = buds.filter { b -> curBuds.none { budKey(it) == budKey(b) } }.distinctBy { budKey(it) }
        val newRecs = recs.filter { r -> curRecs.none { it.id == r.id } }
        val newDebs = debs.filter { d -> curDebs.none { it.id == d.id } }
        val newTxns = years.mapValues { (y, list) -> val known = transactions(y).map { it.id }.toSet(); list.filter { it.id !in known } }.filterValues { it.isNotEmpty() }
        val report = RestoreReport(newTxns.values.sumOf { it.size }, newAccs.size, newBuds.size, newRecs.size, newDebs.size)
        if (!apply) return report

        if (dataFilesExist()) {
            val safety = Backup.create(fs, root, stamp)
            val path = root / "safety-backups" / "Namaa-before-restore-$stamp.namaa-backup"
            fs.createDirectories(path.parent!!)
            write(path, safety)
            if (fs.read(path) { readByteString() } != safety) throw BackupException("could not save the safety backup, nothing was changed")
        }
        if (newAccs.isNotEmpty()) saveAccounts(curAccs + newAccs)
        if (newBuds.isNotEmpty()) saveBudgets(curBuds + newBuds)
        if (newRecs.isNotEmpty()) saveRecurring(curRecs + newRecs)
        if (newDebs.isNotEmpty()) saveDebts(curDebs + newDebs)
        newTxns.forEach { (y, list) -> writeYear(y, transactions(y) + list) }
        return report
    }

    /** Moves every data file into `cleared-<stamp>/` inside the folder, so "clear all data" never destroys anything. Returns how many files were moved. */
    fun clearAll(stamp: String): Int {
        val files = fs.listOrNull(root).orEmpty().filter { Backup.isDataFile(it.name) && fs.metadata(it).isRegularFile }
        if (files.isEmpty()) return 0
        val dir = root / "cleared-$stamp"
        fs.createDirectories(dir)
        files.forEach { fs.atomicMove(it, dir / it.name) }
        return files.size
    }

    private fun dataFilesExist() = fs.listOrNull(root).orEmpty().any { Backup.isDataFile(it.name) }

    data class MergeReport(val mergedTransactions: Int, val mergedAccounts: Int)

    private val sideName = Regex("^(\\d{4}) \\(from .+\\)\\.csv$")
    private val sideAccountsName = Regex("^accounts \\(from .+\\)\\.json$")
    private val mergedMarker get() = root / ".side-merged"

    private fun mergedSet(): Set<String> = read(mergedMarker)?.lines().orEmpty().filter { it.isNotBlank() }.toSet()

    /**
     * Year files and accounts that were kept beside the real ones as `name (from X).ext` (for example data written in the
     * private folder while all-files access was off) are folded in: transactions are united by id, accounts too, the real
     * file wins on a clash. Each distinct side file is merged once (so a row you delete later does not come back), checked
     * afterwards, and never deleted or changed.
     */
    fun mergeSideCopies(): MergeReport {
        val done = mergedSet().toMutableSet()
        var txnCount = 0
        var accCount = 0
        val files = fs.listOrNull(root).orEmpty().filter { fs.metadata(it).isRegularFile }
        for (f in files) {
            val sum = fs.read(f) { readByteString() }.sha256().hex()
            val key = "${f.name}\t$sum"
            if (key in done) continue
            val year = sideName.matchEntire(f.name)?.groupValues?.get(1)?.toInt()
            if (year != null) {
                val rows = Csv.parse(fs.read(f) { readUtf8() }).drop(1)
                val parsed = rows.map { r -> runCatching { Txn(r[0], LocalDate.parse(r[1]), r[2].toLong(), r[3], r.getOrElse(4) { "" }, r.getOrElse(5) { "" }, r.getOrElse(6) { "" }) }.getOrNull() }
                if (parsed.any { it == null }) continue // unreadable rows: leave it for the user, reported by unmergedSideCopies
                val side = parsed.filterNotNull()
                val mine = transactions(year)
                val known = mine.map { it.id }.toSet()
                val extra = side.filter { it.id !in known }
                if (extra.isNotEmpty()) writeYear(year, mine + extra)
                if (side.any { s -> transactions(year).none { it.id == s.id } }) continue
                txnCount += extra.size
            } else if (sideAccountsName.matches(f.name)) {
                val side = runCatching { json.decodeFromString(ListSerializer(Account.serializer()), fs.read(f) { readUtf8() }) }.getOrNull() ?: continue
                if (unreadable("accounts.json") != null) continue
                val mine = accounts()
                val extra = side.filter { s -> mine.none { it.id == s.id } }
                if (extra.isNotEmpty()) saveAccounts(mine + extra)
                if (side.any { s -> accounts().none { it.id == s.id } }) continue
                accCount += extra.size
            } else continue
            done += key
        }
        if (done != mergedSet()) write(mergedMarker, done.sorted().joinToString("\n") + "\n")
        return MergeReport(txnCount, accCount)
    }

    /** Files kept beside the real ones as `name (from X).ext` that are not merged (other kinds, or unreadable), so a screen can say where they are. */
    fun unmergedSideCopies(): List<String> {
        val done = mergedSet()
        return fs.listOrNull(root).orEmpty().filter { fs.metadata(it).isRegularFile && it.name.contains(" (from ") && !it.name.endsWith(".part") }
            .filter { f -> "${f.name}\t${fs.read(f) { readByteString() }.sha256().hex()}" !in done }.map { it.name }.sorted()
    }

    /** Writes the CSV for the given transactions into `exports/` and returns its path. */
    fun export(fileName: String, txns: List<Txn>): Path {
        val path = root / "exports" / fileName
        fs.createDirectories(path.parent!!)
        write(path, exportCsv(txns, accounts()))
        return path
    }
}
