package dev.namaa.app

import dev.namaa.core.Str
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.namaa.core.Account
import dev.namaa.core.Txn
import dev.namaa.core.formatMoney
import dev.namaa.core.newId
import dev.namaa.core.parseMoney
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

val defaultCategories = listOf("Food", "Transport", "Home", "Bills", "Health", "Fun", "Shopping", "Salary", "Other")

/** A button that shows the date and opens a calendar to pick another; nobody types a date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, label: String? = null, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CalendarMonth, null)
        Text("  ${label ?: Str.DateLabel.t()}: ${prettyDate(date)} ${num(date.year)}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) }
                    open = false
                }) { Text(Str.OK.t()) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(Str.Cancel.t()) } },
        ) { DatePicker(state) }
    }
}

@Composable
fun TxnDialog(existing: Txn?, startIncome: Boolean, accounts: List<Account>, today: LocalDate, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Txn) -> Unit) {
    var amount by remember { mutableStateOf(existing?.let { formatMoney(kotlin.math.abs(it.amountMinor)) } ?: "") }
    var income by remember { mutableStateOf(if (existing != null) existing.amountMinor > 0 else startIncome) }
    var accountId by remember { mutableStateOf(existing?.accountId ?: accounts.first().id) }
    var category by remember { mutableStateOf(existing?.category ?: if (startIncome) "Salary" else "Food") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: today) }
    val minor = parseMoney(amount)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) Str.NewTxn.t() else Str.EditTxn.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!income, { income = false }, { Text(Str.ChipSpent.t()) })
                    FilterChip(income, { income = true }, { Text(Str.ChipReceived.t()) })
                }
                OutlinedTextField(amount, { amount = it }, singleLine = true, placeholder = { Text(Str.AmountHint1250.t()) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                if (accounts.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts, key = { it.id }) { a -> FilterChip(accountId == a.id, { accountId = a.id }, { Text(a.name) }) }
                }
                OutlinedTextField(category, { category = it }, singleLine = true, placeholder = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultCategories) { c -> FilterChip(category == c, { category = c }, { Text(categoryShown(c)) }) }
                }
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text(Str.NoteOptional.t()) }, modifier = Modifier.fillMaxWidth())
                DateField(date) { date = it }
            }
        },
        confirmButton = {
            TextButton(enabled = minor != null, onClick = {
                onSave(Txn(existing?.id ?: "", date, if (income) minor!! else -minor!!, accountId, category.trim(), note.trim(), existing?.transferId ?: ""))
            }) { Text(Str.Save.t()) }
        },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(onClick = it) { Text(Str.Delete.t(), color = MaterialTheme.colorScheme.error) } }
                TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) }
            }
        },
    )
}

@Composable
fun TransferDialog(accounts: List<Account>, today: LocalDate, onDismiss: () -> Unit, onSave: (from: String, to: String, minor: Long, date: LocalDate, note: String) -> Unit) {
    var from by remember { mutableStateOf(accounts.first().id) }
    var to by remember { mutableStateOf(accounts.getOrElse(1) { accounts.first() }.id) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    val minor = parseMoney(amount)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.TransferTitle.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (accounts.size < 2) Text(Str.AddSecondAccount.t(), color = MaterialTheme.colorScheme.error)
                Text(Str.From.t(), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(accounts, key = { it.id }) { a -> FilterChip(from == a.id, { from = a.id }, { Text(a.name) }) } }
                Text(Str.To.t(), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(accounts, key = { it.id }) { a -> FilterChip(to == a.id, { to = a.id }, { Text(a.name) }) } }
                OutlinedTextField(amount, { amount = it }, singleLine = true, placeholder = { Text(Str.AmountHint50.t()) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text(Str.NoteOptional.t()) }, modifier = Modifier.fillMaxWidth())
                DateField(date) { date = it }
            }
        },
        confirmButton = { TextButton(enabled = minor != null && from != to, onClick = { onSave(from, to, minor!!, date, note.trim()) }) { Text(Str.Move.t()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}

@Composable
fun AccountDialog(existing: Account?, defaultCurrency: String, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var opening by remember { mutableStateOf(formatMoney(existing?.openingMinor ?: 0)) }
    val openingMinor = parseMoney(opening)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) Str.NewAccountTitle.t() else Str.EditAccount.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(Str.NameLabel.t()) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(currency, { currency = it }, singleLine = true, label = { Text(Str.CurrencyLabel.t()) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(opening, { opening = it }, singleLine = true, isError = openingMinor == null, label = { Text(Str.OpeningBalance.t()) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && currency.isNotBlank() && openingMinor != null, onClick = {
                onSave(Account(existing?.id ?: newId(), name.trim(), currency.trim().uppercase(), openingMinor!!))
            }) { Text(Str.Save.t()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}
