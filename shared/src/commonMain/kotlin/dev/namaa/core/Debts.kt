package dev.namaa.core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class Direction { TheyOweMe, IOweThem }

@Serializable
data class Payment(val date: LocalDate, val amountMinor: Long, val note: String = "")

/** Money lent or borrowed, settled in parts. */
@Serializable
data class Debt(
    val id: String, val person: String, val direction: Direction, val principalMinor: Long, val date: LocalDate,
    val note: String = "", val payments: List<Payment> = emptyList(), val currency: String = "",
) {
    val paidMinor: Long get() = payments.sumOf { it.amountMinor }
    val remainingMinor: Long get() = principalMinor - paidMinor
    val settled: Boolean get() = remainingMinor <= 0
}

/** Adds a partial or final payment; refuses a payment of nothing or one that is more than what is left. */
fun Debt.withPayment(payment: Payment): Debt {
    require(payment.amountMinor > 0) { "a payment must be more than zero" }
    require(payment.amountMinor <= remainingMinor) { "a payment cannot be more than what is left" }
    return copy(payments = payments + payment)
}

data class DebtTotals(val owedToMeMinor: Long, val iOweMinor: Long) {
    val netMinor: Long get() = owedToMeMinor - iOweMinor
}

fun debtTotals(debts: List<Debt>): DebtTotals = DebtTotals(
    debts.filter { it.direction == Direction.TheyOweMe }.sumOf { it.remainingMinor.coerceAtLeast(0) },
    debts.filter { it.direction == Direction.IOweThem }.sumOf { it.remainingMinor.coerceAtLeast(0) },
)

/** One line per person: what they owe you minus what you owe them (positive: they owe you). Settled debts add nothing. */
fun balanceByPerson(debts: List<Debt>): List<Pair<String, Long>> =
    debts.groupBy { it.person.trim().lowercase() }.map { (_, list) ->
        list.first().person.trim() to list.sumOf { if (it.direction == Direction.TheyOweMe) it.remainingMinor.coerceAtLeast(0) else -it.remainingMinor.coerceAtLeast(0) }
    }.filter { it.second != 0L }.sortedByDescending { kotlin.math.abs(it.second) }

/** Totals per currency (empty means [defaultCurrency]), in order of first appearance. */
fun debtTotalsByCurrency(debts: List<Debt>, defaultCurrency: String): List<Pair<String, DebtTotals>> =
    debts.groupBy { it.currency.ifBlank { defaultCurrency }.currencyCode() }.map { (c, list) -> c to debtTotals(list) }

/** One line per person and currency: positive means they owe you. Settled lines are left out. */
fun balanceByPersonAndCurrency(debts: List<Debt>, defaultCurrency: String): List<Triple<String, String, Long>> =
    debts.groupBy { it.currency.ifBlank { defaultCurrency }.currencyCode() }.flatMap { (c, list) -> balanceByPerson(list).map { Triple(it.first, c, it.second) } }
        .sortedWith(compareByDescending<Triple<String, String, Long>> { kotlin.math.abs(it.third) })
