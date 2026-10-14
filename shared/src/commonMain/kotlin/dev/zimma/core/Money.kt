package dev.zimma.core

/** Income and spending for one month, and spending by category (largest first). */
data class MonthSummary(val incomeMinor: Long, val expenseMinor: Long, val byCategory: List<Pair<String, Long>>) {
    val netMinor: Long get() = incomeMinor + expenseMinor
}

fun monthSummary(txns: List<Txn>, year: Int, month: Int): MonthSummary {
    val inMonth = txns.filter { it.date.year == year && it.date.monthNumber == month }
    val spending = inMonth.filter { it.amountMinor < 0 }
    return MonthSummary(
        incomeMinor = inMonth.filter { it.amountMinor > 0 }.sumOf { it.amountMinor },
        expenseMinor = spending.sumOf { it.amountMinor },
        byCategory = spending.groupBy { it.category.ifEmpty { "Other" } }.map { it.key to it.value.sumOf { t -> t.amountMinor } }.sortedBy { it.second },
    )
}

fun balance(account: Account, txns: List<Txn>): Long = account.openingMinor + txns.filter { it.accountId == account.id }.sumOf { it.amountMinor }

/** "12.50" for 1250, with a minus sign for negatives. */
fun formatMoney(minor: Long): String {
    val abs = if (minor < 0) -minor else minor
    return (if (minor < 0) "-" else "") + (abs / 100).toString() + "." + (abs % 100).toString().padStart(2, '0')
}

/** Reads "12.5", "12,50" or "-3" as minor units; null when it is not a number. */
fun parseMoney(text: String): Long? {
    val t = text.trim().replace(',', '.')
    val neg = t.startsWith("-")
    val body = t.removePrefix("-").removePrefix("+")
    if (body.isEmpty() || body.count { it == '.' } > 1 || body.any { !it.isDigit() && it != '.' }) return null
    val (whole, frac) = body.split('.').let { it[0] to it.getOrElse(1) { "" } }
    val minor = (whole.ifEmpty { "0" }.toLongOrNull() ?: return null) * 100 + (frac.padEnd(2, '0').take(2).toLongOrNull() ?: return null)
    return if (neg) -minor else minor
}

/** The month [delta] months from [year]/[month]: `(2026, 1)` and -1 is `(2025, 12)`. */
fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val index = year * 12 + (month - 1) + delta
    return Pair(index.floorDiv(12), index.mod(12) + 1)
}
