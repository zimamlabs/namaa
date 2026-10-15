package dev.namaa.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SortTest {
    private val t = listOf(
        Txn("a", LocalDate(2026, 10, 2), -500, "c"), Txn("b", LocalDate(2026, 10, 5), 9_000, "c"),
        Txn("c", LocalDate(2026, 10, 3), -9_500, "c"), Txn("d", LocalDate(2026, 10, 5), 100, "c"),
    )
    private fun ids(s: TxnSort) = filterTxns(t, TxnFilter(sort = s)).map { it.id }

    @Test fun newestKeepsTheOrderTheBookGives() = assertEquals(listOf("a", "b", "c", "d"), ids(TxnSort.Newest))
    @Test fun oldestFirstReversesDatesAndKeepsTiesStable() = assertEquals(listOf("a", "c", "b", "d"), ids(TxnSort.Oldest))
    @Test fun largestBySizeRegardlessOfSign() = assertEquals(listOf("c", "b", "a", "d"), ids(TxnSort.Largest))
    @Test fun smallestBySize() = assertEquals(listOf("d", "a", "b", "c"), ids(TxnSort.Smallest))
    @Test fun sortingIsNotAFilter() = assertEquals(false, TxnFilter(sort = TxnSort.Largest).active)
}
