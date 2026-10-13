package dev.zimma.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.zimma.core.Account
import dev.zimma.core.MoneyBook
import dev.zimma.core.Txn
import dev.zimma.core.newId

class ZimmaStore(val book: MoneyBook) {
    var accounts by mutableStateOf(emptyList<Account>())
        private set
    var txns by mutableStateOf(emptyList<Txn>())
        private set
    var today by mutableStateOf(book.today())
        private set

    init {
        // First launch: a plain Cash account so nothing needs setting up.
        if (book.accounts().isEmpty()) book.saveAccounts(listOf(Account(newId(), "Cash")))
        reload()
    }

    fun reload() {
        accounts = book.accounts()
        txns = book.allTransactions()
        today = book.today()
    }

    fun saveAccounts(list: List<Account>) { book.saveAccounts(list); accounts = list }

    fun saveTxn(txn: Txn) { book.saveTransaction(txn); txns = book.allTransactions() }

    fun deleteTxn(id: String) { book.deleteTransaction(id); txns = book.allTransactions() }
}
