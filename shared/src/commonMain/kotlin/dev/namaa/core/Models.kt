package dev.namaa.core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Account(val id: String, val name: String, val currency: String = "USD", val openingMinor: Long = 0, val archived: Boolean = false)

/** Money amounts are whole minor units (cents): positive is income, negative is spending. */
data class Txn(
    val id: String,
    val date: LocalDate,
    val amountMinor: Long,
    val accountId: String,
    val category: String = "",
    val note: String = "",
    /** Both legs of a transfer between accounts share one id; empty for ordinary income and spending. */
    val transferId: String = "",
)
