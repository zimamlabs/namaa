package dev.namaa.core

import kotlinx.datetime.LocalDate

/** Every field is optional; an empty one matches everything. [text] looks in the category, note and amount. */
data class TxnFilter(val text: String = "", val category: String = "", val accountId: String = "", val from: LocalDate? = null, val to: LocalDate? = null) {
    val active: Boolean get() = text.isNotBlank() || category.isNotBlank() || accountId.isNotBlank() || from != null || to != null
}

fun filterTxns(txns: List<Txn>, f: TxnFilter): List<Txn> {
    val q = f.text.trim().lowercase()
    return txns.filter { t ->
        (q.isEmpty() || t.category.lowercase().contains(q) || t.note.lowercase().contains(q) || formatMoney(kotlin.math.abs(t.amountMinor)).contains(q)) &&
            (f.category.isBlank() || t.category.equals(f.category, ignoreCase = true)) &&
            (f.accountId.isBlank() || t.accountId == f.accountId) &&
            (f.from == null || t.date >= f.from) && (f.to == null || t.date <= f.to)
    }
}
