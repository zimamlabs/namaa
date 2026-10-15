package dev.namaa.core


/** A cell a spreadsheet would run as a formula gets a leading quote so it stays text. Only for exports, never the year files. */
private fun safeCell(v: String) = if (v.isNotEmpty() && v[0] in "=+-@\t\r") "'$v" else v

/** The transactions as one CSV: date, account, category, amount, currency, note and whether it is a transfer leg. Amounts are written as 12.50. */
fun exportCsv(txns: List<Txn>, accounts: List<Account>): String {
    val byId = accounts.associateBy { it.id }
    val rows = listOf(listOf("date", "account", "category", "amount", "currency", "note", "transfer")) +
        txns.sortedBy { it.date }.map {
            listOf(it.date.toString(), safeCell(byId[it.accountId]?.name ?: it.accountId), safeCell(it.category), formatMoney(it.amountMinor), byId[it.accountId]?.currency.orEmpty(), safeCell(it.note), if (it.transferId.isEmpty()) "" else "yes")
        }
    return Csv.format(rows)
}

fun monthFileName(year: Int, month: Int) = "Namaa-$year-${month.toString().padStart(2, '0')}.csv"
fun yearFileName(year: Int) = "Namaa-$year.csv"

