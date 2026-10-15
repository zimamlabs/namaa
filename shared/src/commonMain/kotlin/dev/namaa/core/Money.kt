package dev.namaa.core

/** Income and spending for one month, and spending by category (largest first). */
data class MonthSummary(val incomeMinor: Long, val expenseMinor: Long, val byCategory: List<Pair<String, Long>>) {
    val netMinor: Long get() = incomeMinor + expenseMinor
}

fun monthSummary(txns: List<Txn>, year: Int, month: Int): MonthSummary {
    // A transfer moves money between your own accounts: it is neither income nor spending.
    val inMonth = txns.filter { it.date.year == year && it.date.monthNumber == month && it.transferId.isEmpty() }
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

/** Why an amount typed by the user was not accepted. */
enum class MoneyProblem { Empty, NotANumber, Ambiguous, TooManyDecimals, TooLarge }

sealed interface MoneyParse {
    data class Ok(val minor: Long) : MoneyParse
    data class Bad(val problem: MoneyProblem) : MoneyParse
}

/**
 * Reads a typed amount as minor units, or says why it cannot. Digits may be Latin, Arabic-Indic or Persian; the decimal mark
 * is "." or "," (or the Arabic ٫). Thousands grouping is accepted only when it cannot be a decimal: "1,234.50", "1.234,50"
 * and "1,234,567" are fine, "1,234" is [MoneyProblem.Ambiguous] (1.234 or 1234?) and so is "12.999". More than two decimals
 * are never cut off.
 */
fun parseMoneyResult(text: String): MoneyParse {
    val t = text.trim().map {
        when (it) {
            in '٠'..'٩' -> '0' + (it - '٠')
            in '۰'..'۹' -> '0' + (it - '۰')
            '٫' -> '.'
            '٬', '،' -> ','
            '\u2212' -> '-'
            else -> it
        }
    }.joinToString("")
    if (t.isEmpty()) return bad(MoneyProblem.Empty)
    val negative = t.startsWith("-")
    val body = if (t[0] == '-' || t[0] == '+') t.substring(1) else t
    if (body.none { it.isDigit() } || body.any { it !in '0'..'9' && it != '.' && it != ',' }) return bad(MoneyProblem.NotANumber)

    val last = body.indexOfLast { it == '.' || it == ',' }
    var whole = body
    var frac = ""
    if (last >= 0) {
        val before = body.substring(0, last)
        val after = body.substring(last + 1)
        val earlier = before.filter { it == '.' || it == ',' }.toSet()
        val groupChar = earlier.singleOrNull()
        when {
            earlier.isEmpty() -> {
                if (after.length == 3) return bad(MoneyProblem.Ambiguous)
                if (after.length > 3) return bad(MoneyProblem.TooManyDecimals)
                whole = before; frac = after
            }
            earlier.size > 1 -> return bad(MoneyProblem.Ambiguous)
            groupChar != body[last] -> {
                if (!isGrouped(before, groupChar!!)) return bad(MoneyProblem.Ambiguous)
                if (after.length > 2) return bad(MoneyProblem.TooManyDecimals)
                whole = before.replace(groupChar.toString(), ""); frac = after
            }
            else -> { // the same mark everywhere: it can only be grouping
                if (!isGrouped(body, groupChar!!)) return bad(MoneyProblem.Ambiguous)
                whole = body.replace(groupChar.toString(), "")
            }
        }
    }
    val cents = frac.padEnd(2, '0').toLong()
    val units = (whole.ifEmpty { "0" }).toLongOrNull() ?: return bad(MoneyProblem.TooLarge)
    if (units > (Long.MAX_VALUE - cents) / 100) return bad(MoneyProblem.TooLarge)
    val minor = units * 100 + cents
    return MoneyParse.Ok(if (negative) -minor else minor)
}

private fun bad(p: MoneyProblem) = MoneyParse.Bad(p)

/** "1,234,567": a first group of one to three digits, then groups of exactly three. */
private fun isGrouped(text: String, mark: Char): Boolean {
    val groups = text.split(mark)
    return groups.first().length in 1..3 && groups.drop(1).all { it.length == 3 }
}

/** The amount in minor units, or null when [parseMoneyResult] rejects it. */
fun parseMoney(text: String): Long? = (parseMoneyResult(text) as? MoneyParse.Ok)?.minor

/** The month [delta] months from [year]/[month]: `(2026, 1)` and -1 is `(2025, 12)`. */
fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val index = year * 12 + (month - 1) + delta
    return Pair(index.floorDiv(12), index.mod(12) + 1)
}

/** Income and spending for one month, for the bar chart. */
data class MonthTotal(val year: Int, val month: Int, val incomeMinor: Long, val spentMinor: Long)

/** The [count] months ending at [year]/[month], oldest first; months with nothing are zero. */
fun monthlyTotals(txns: List<Txn>, year: Int, month: Int, count: Int = 6): List<MonthTotal> =
    (count - 1 downTo 0).map { back ->
        val (y, m) = shiftMonth(year, month, -back)
        val s = monthSummary(txns, y, m)
        MonthTotal(y, m, s.incomeMinor, -s.expenseMinor)
    }
