package dev.namaa.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

enum class Frequency { Weekly, Monthly }

/**
 * A transaction that repeats. [start] is the first date; a monthly rule starting on the 31st falls on the last day of
 * shorter months. [lastGenerated] is the last date already turned into a transaction.
 */
@Serializable
data class Recurring(
    val id: String, val amountMinor: Long, val accountId: String, val category: String = "", val note: String = "",
    val frequency: Frequency, val start: LocalDate, val lastGenerated: LocalDate? = null,
)

private fun daysIn(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** The n-th occurrence (0 is the start). */
fun occurrence(rule: Recurring, n: Int): LocalDate = when (rule.frequency) {
    Frequency.Weekly -> rule.start.plus(n * 7, DateTimeUnit.DAY)
    Frequency.Monthly -> {
        val (y, m) = shiftMonth(rule.start.year, rule.start.monthNumber, n)
        LocalDate(y, m, minOf(rule.start.dayOfMonth, daysIn(y, m)))
    }
}

/** The first date a rule may generate again: the next month (monthly) or 7 days on (weekly) after [Recurring.lastGenerated], so editing the start never adds a second one for a period already covered. */
private fun earliestNext(rule: Recurring): LocalDate? = rule.lastGenerated?.let { last ->
    when (rule.frequency) {
        Frequency.Weekly -> last.plus(7, DateTimeUnit.DAY)
        Frequency.Monthly -> shiftMonth(last.year, last.monthNumber, 1).let { (y, m) -> LocalDate(y, m, 1) }
    }
}

/** Occurrences after [Recurring.lastGenerated] (or from the start) up to and including [upTo]. */
fun dueDates(rule: Recurring, upTo: LocalDate): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    val earliest = earliestNext(rule)
    var n = 0
    while (true) {
        val d = occurrence(rule, n)
        if (d > upTo || n > 5_000) break
        if (earliest == null || d >= earliest) out += d
        n++
    }
    return out
}

/** The id a generated transaction gets, so generating twice can never double it. */
fun recurringTxnId(rule: Recurring, date: LocalDate) = "r-${rule.id}-$date"

