package dev.zimma.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zimma.core.Account
import dev.zimma.core.Txn
import dev.zimma.core.balance
import dev.zimma.core.formatMoney
import dev.zimma.core.monthSummary
import dev.zimma.core.newId
import dev.zimma.core.parseMoney
import kotlinx.datetime.LocalDate

private val defaultCategories = listOf("Food", "Transport", "Home", "Bills", "Health", "Fun", "Shopping", "Salary", "Other")

/** Which dialog is open: a new or edited transaction, or a new or renamed account. */
private sealed interface Editing {
    data class Transaction(val txn: Txn?) : Editing
    data class AccountEdit(val account: Account?) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(store: ZimmaStore, onAbout: () -> Unit) {
    var editing by remember { mutableStateOf<Editing?>(null) }
    var menu by remember { mutableStateOf(false) }
    val today = store.today
    val month = monthSummary(store.txns, today.year, today.monthNumber)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("Zimma") }, actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Menu") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Add account") }, onClick = { menu = false; editing = Editing.AccountEdit(null) })
                    DropdownMenuItem(text = { Text("About") }, onClick = { menu = false; onAbout() })
                }
            })
        },
        floatingActionButton = { FloatingActionButton(onClick = { editing = Editing.Transaction(null) }) { Icon(Icons.Rounded.Add, "New transaction") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Panel {
                    Text("${monthName(today.monthNumber)} ${today.year}", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Income", style = MaterialTheme.typography.labelMedium); Text(formatMoney(month.incomeMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Spent", style = MaterialTheme.typography.labelMedium); Text(formatMoney(-month.expenseMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                        Column(horizontalAlignment = Alignment.End) { Text("Left", style = MaterialTheme.typography.labelMedium); Text(formatMoney(month.netMinor), style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
            item {
                Panel {
                    Text("Accounts", style = MaterialTheme.typography.titleSmall)
                    store.accounts.forEach { a ->
                        Row(Modifier.fillMaxWidth().clickable { editing = Editing.AccountEdit(a) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge)
                            Text(formatMoney(balance(a, store.txns)) + " " + a.currency, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (month.byCategory.isNotEmpty()) item {
                Panel {
                    Text("Spending by category", style = MaterialTheme.typography.titleSmall)
                    val top = -month.byCategory.first().second
                    month.byCategory.forEach { (cat, amount) ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium)
                                Text(formatMoney(-amount), style = MaterialTheme.typography.bodyMedium)
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                Box(Modifier.fillMaxWidth((-amount).toFloat() / top).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
            if (store.txns.isEmpty()) item { EmptyHint("No transactions yet", "Tap + to add one. They are saved per year in <year>.csv.") }
            items(store.txns.take(200), key = { it.id }) { t ->
                Row(Modifier.fillMaxWidth().clickable { editing = Editing.Transaction(t) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.category.ifEmpty { "Other" }, style = MaterialTheme.typography.bodyLarge)
                        Text(prettyDate(t.date) + if (t.note.isNotBlank()) "  ·  ${t.note}" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(formatMoney(t.amountMinor), style = MaterialTheme.typography.titleSmall, color = if (t.amountMinor < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
    when (val e = editing) {
        is Editing.Transaction -> TxnDialog(e.txn, store.accounts, today, onDismiss = { editing = null },
            onDelete = e.txn?.let { t -> { store.deleteTxn(t.id); editing = null } }) { saved -> store.saveTxn(saved); editing = null }
        is Editing.AccountEdit -> AccountDialog(e.account, onDismiss = { editing = null }) { saved ->
            store.saveAccounts(if (e.account == null) store.accounts + saved else store.accounts.map { if (it.id == saved.id) saved else it })
            editing = null
        }
        null -> {}
    }
}

@Composable
private fun TxnDialog(existing: Txn?, accounts: List<Account>, today: LocalDate, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Txn) -> Unit) {
    var amount by remember { mutableStateOf(existing?.let { formatMoney(kotlin.math.abs(it.amountMinor)) } ?: "") }
    var income by remember { mutableStateOf((existing?.amountMinor ?: -1) > 0) }
    var accountId by remember { mutableStateOf(existing?.accountId ?: accounts.first().id) }
    var category by remember { mutableStateOf(existing?.category ?: "Food") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf((existing?.date ?: today).toString()) }
    val minor = parseMoney(amount)?.takeIf { it > 0 }
    val day = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New transaction" else "Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!income, { income = false }, { Text("Spent") })
                    FilterChip(income, { income = true }, { Text("Received") })
                }
                OutlinedTextField(amount, { amount = it }, singleLine = true, placeholder = { Text("Amount, e.g. 12.50") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                if (accounts.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts, key = { it.id }) { a -> FilterChip(accountId == a.id, { accountId = a.id }, { Text(a.name) }) }
                }
                OutlinedTextField(category, { category = it }, singleLine = true, placeholder = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultCategories) { c -> FilterChip(category == c, { category = c }, { Text(c) }) }
                }
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, singleLine = true, isError = day == null, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = minor != null && day != null, onClick = {
                onSave(Txn(existing?.id ?: "", day!!, if (income) minor!! else -minor!!, accountId, category.trim(), note.trim()))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(onClick = it) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun AccountDialog(existing: Account?, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: "USD") }
    var opening by remember { mutableStateOf(formatMoney(existing?.openingMinor ?: 0)) }
    val openingMinor = parseMoney(opening)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New account" else "Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(currency, { currency = it }, singleLine = true, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(opening, { opening = it }, singleLine = true, isError = openingMinor == null, label = { Text("Opening balance") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && currency.isNotBlank() && openingMinor != null, onClick = {
                onSave(Account(existing?.id ?: newId(), name.trim(), currency.trim().uppercase(), openingMinor!!))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
