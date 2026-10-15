package dev.namaa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.namaa.core.Account
import dev.namaa.core.Budget
import dev.namaa.core.Debt
import dev.namaa.core.Recurring
import dev.namaa.core.Str
import dev.namaa.core.Txn
import dev.namaa.core.parseMoney

/** The long-press menu of a transaction. A transfer leg gets Edit and Delete only (it is edited and deleted as a pair). */
@Composable
internal fun txnActions(store: NamaaStore, t: Txn, onEdit: () -> Unit): List<MenuAction> {
    val edit = MenuAction(Str.MenuEdit.t(), Icons.Rounded.Edit, onClick = onEdit)
    val delete = MenuAction(Str.Delete.t(), Icons.Rounded.Delete, destructive = true,
        confirm = Str.ConfirmDeleteTitle.t() to (if (t.transferId.isEmpty()) Str.DeleteTxnDetail else Str.DeleteTransferDetail).t()) { store.deleteTxn(t.id) }
    if (t.transferId.isNotEmpty()) return listOf(edit, delete)
    return listOf(
        edit,
        MenuAction(Str.MenuDuplicate.t(), Icons.Rounded.ContentCopy) { store.duplicateTxn(t.id) },
        MenuAction(Str.MenuSplit.t(), Icons.Rounded.CallSplit, onClick = { store.splitting = t }).takeIf { kotlin.math.abs(t.amountMinor) > 1 },
        delete,
    ).filterNotNull()
}

@Composable
internal fun accountActions(store: NamaaStore, a: Account, onEdit: () -> Unit, onHistory: () -> Unit): List<MenuAction> = listOf(
    MenuAction(Str.MenuEdit.t(), Icons.Rounded.Edit, onClick = onEdit),
    MenuAction(Str.MenuHistory.t(), Icons.Rounded.History, onClick = onHistory),
    MenuAction((if (a.archived) Str.MenuUnarchive else Str.MenuArchive).t(), Icons.Rounded.Inventory2) { store.setArchived(a.id, !a.archived) },
)

@Composable
internal fun budgetActions(store: NamaaStore, b: Budget, onEdit: () -> Unit): List<MenuAction> = listOf(
    MenuAction(Str.MenuEdit.t(), Icons.Rounded.Edit, onClick = onEdit),
    MenuAction(Str.Delete.t(), Icons.Rounded.Delete, destructive = true, confirm = Str.ConfirmDeleteTitle.t() to Str.DeleteBudgetDetail.t()) { store.saveBudgets(store.budgets.filter { it !== b && !(it.category == b.category && it.currency == b.currency) }) },
)

@Composable
internal fun recurringActions(store: NamaaStore, r: Recurring, onEdit: () -> Unit): List<MenuAction> = listOf(
    MenuAction(Str.MenuEdit.t(), Icons.Rounded.Edit, onClick = onEdit),
    MenuAction(Str.Delete.t(), Icons.Rounded.Delete, destructive = true, confirm = Str.ConfirmDeleteTitle.t() to Str.DeleteRepeatingDetail.t()) { store.saveRecurring(store.recurring.filter { it.id != r.id }) },
)

@Composable
internal fun debtActions(store: NamaaStore, d: Debt, onEdit: () -> Unit, onPay: () -> Unit): List<MenuAction> = listOfNotNull(
    MenuAction(Str.MenuEdit.t(), Icons.Rounded.Edit, onClick = onEdit),
    MenuAction(Str.MenuPay.t(), Icons.Rounded.Payments, onClick = onPay).takeIf { !d.settled },
    MenuAction(Str.Delete.t(), Icons.Rounded.Delete, destructive = true, confirm = Str.ConfirmDeleteTitle.t() to Str.DeleteDebtDetail.t(d.payments.size)) { store.saveDebts(store.debts.filter { it.id != d.id }) },
)

/** Asks for the size of the first part and the category of the rest. */
@Composable
internal fun SplitDialog(t: Txn, onDismiss: () -> Unit, onSplit: (firstMinor: Long, category: String) -> Unit) {
    val whole = kotlin.math.abs(t.amountMinor)
    var first by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val minor = parseMoney(first)?.takeIf { it in 1 until whole }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.SplitTitle.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Str.SplitBody.t(money(whole)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountField(first, { first = it }, label = Str.FirstPart.t())
                OutlinedTextField(category, { category = it }, singleLine = true, placeholder = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultCategories) { c -> FilterChip(category == c, { category = c }, { Text(categoryShown(c)) }) }
                }
            }
        },
        confirmButton = { TextButton(enabled = minor != null && category.isNotBlank(), onClick = { onSplit(minor!!, category.trim()) }) { Text(Str.SplitGo.t()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}
