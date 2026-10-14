package dev.zimma.core

import kotlinx.serialization.Serializable

/** A monthly spending limit for one category, in minor units. */
@Serializable
data class Budget(val category: String, val limitMinor: Long)

enum class BudgetState { Ok, Near, Over }

/** How one budget stands for a month: [spentMinor] is positive; [fraction] is spent over the limit (above 1 when over). */
data class BudgetStatus(val budget: Budget, val spentMinor: Long, val remainingMinor: Long, val fraction: Float, val state: BudgetState)

/** 80 percent of the limit or more is a warning, over the limit is over. Transfers and income never count. */
fun budgetStatuses(budgets: List<Budget>, txns: List<Txn>, year: Int, month: Int): List<BudgetStatus> {
    val spentBy = txns.filter { it.date.year == year && it.date.monthNumber == month && it.transferId.isEmpty() && it.amountMinor < 0 }
        .groupBy { it.category.ifEmpty { "Other" }.lowercase() }.mapValues { e -> -e.value.sumOf { it.amountMinor } }
    return budgets.map { b ->
        val spent = spentBy[b.category.ifEmpty { "Other" }.lowercase()] ?: 0L
        val fraction = if (b.limitMinor > 0) spent.toFloat() / b.limitMinor else if (spent > 0) 2f else 0f
        BudgetStatus(b, spent, b.limitMinor - spent, fraction, when { spent > b.limitMinor -> BudgetState.Over; fraction >= 0.8f -> BudgetState.Near; else -> BudgetState.Ok })
    }.sortedByDescending { it.fraction }
}
