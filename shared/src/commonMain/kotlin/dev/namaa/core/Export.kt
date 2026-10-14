package dev.namaa.core


/** The transactions as one CSV: date, account, category, amount, note and whether it is a transfer leg. Amounts are written as 12.50. */
fun exportCsv(txns: List<Txn>, accounts: List<Account>): String {
    val names = accounts.associate { it.id to it.name }
    val rows = listOf(listOf("date", "account", "category", "amount", "note", "transfer")) +
        txns.sortedBy { it.date }.map { listOf(it.date.toString(), names[it.accountId] ?: it.accountId, it.category, formatMoney(it.amountMinor), it.note, if (it.transferId.isEmpty()) "" else "yes") }
    return Csv.format(rows)
}

fun monthFileName(year: Int, month: Int) = "Namaa-$year-${month.toString().padStart(2, '0')}.csv"
fun yearFileName(year: Int) = "Namaa-$year.csv"

