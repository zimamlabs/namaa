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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import dev.zimma.core.TxnFilter
import dev.zimma.core.filterTxns
import dev.zimma.core.monthFileName
import dev.zimma.core.yearFileName
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zimma.core.Account
import dev.zimma.core.Txn
import dev.zimma.core.balance
import dev.zimma.core.formatMoney
import dev.zimma.core.monthSummary
import dev.zimma.core.shiftMonth

@Composable
fun TransactionsScreen(store: ZimmaStore, padding: PaddingValues, onEdit: (Txn) -> Unit) {
    var filter by remember { mutableStateOf(TxnFilter()) }
    var open by remember { mutableStateOf(false) }
    val shown = remember(store.txns, filter) { filterTxns(store.txns, filter) }
    val categories = remember(store.txns) { store.txns.map { it.category.ifEmpty { "Other" } }.distinct().sorted() }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(filter.text, { filter = filter.copy(text = it) }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Search category, note or amount") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
                    FilterChip(open || filter.category.isNotBlank() || filter.accountId.isNotBlank() || filter.from != null || filter.to != null, { open = !open }, { Text("Filters") }, leadingIcon = { Icon(Icons.Rounded.FilterList, null, Modifier.size(18.dp)) })
                }
                if (open) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categories) { c -> FilterChip(filter.category == c, { filter = filter.copy(category = if (filter.category == c) "" else c) }, { Text(c) }) } }
                    if (store.accounts.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(store.accounts, key = { it.id }) { a -> FilterChip(filter.accountId == a.id, { filter = filter.copy(accountId = if (filter.accountId == a.id) "" else a.id) }, { Text(a.name) }) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { DateField(filter.from ?: store.today, "From") { filter = filter.copy(from = it) } }
                        Box(Modifier.weight(1f)) { DateField(filter.to ?: store.today, "To") { filter = filter.copy(to = it) } }
                    }
                }
                if (filter.active) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${shown.size} of ${store.txns.size} shown", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { filter = TxnFilter() }) { Text("Clear filters") }
                }
            }
        }
        if (store.txns.isEmpty()) item { EmptyHint("No transactions yet", "Tap + to add one. They are saved per year in <year>.csv.") }
        else if (shown.isEmpty()) item { EmptyHint("Nothing matches", "Try fewer filters.") }
        items(shown.take(500), key = { it.id }) { t -> TxnRow(t, store.accounts) { onEdit(t) } }
    }
}

@Composable
fun AccountsScreen(store: ZimmaStore, padding: PaddingValues, onEdit: (Account) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(store.accounts, key = { it.id }) { a ->
            Panel(Modifier.clickable { onEdit(a) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(a.name, style = MaterialTheme.typography.titleMedium)
                        Text(a.currency + if (a.openingMinor != 0L) "  ·  opened with ${formatMoney(a.openingMinor)}" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatMoney(balance(a, store.txns)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** One month at a time: totals, and spending by category with bars. */
@Composable
fun ReportScreen(store: ZimmaStore, padding: PaddingValues) {
    var shown by remember { mutableStateOf(store.today.year to store.today.monthNumber) }
    val (year, month) = shown
    val s = monthSummary(store.txns, year, month)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { shown = shiftMonth(year, month, -1) }) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }
                Text("${monthName(month)} $year", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { shown = shiftMonth(year, month, 1) }) { Icon(Icons.Rounded.ChevronRight, "Next month") }
            }
        }
        item {
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Income", style = MaterialTheme.typography.labelMedium); Text(formatMoney(s.incomeMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Spent", style = MaterialTheme.typography.labelMedium); Text(formatMoney(-s.expenseMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                    Column(horizontalAlignment = Alignment.End) { Text("Left", style = MaterialTheme.typography.labelMedium); Text(formatMoney(s.netMinor), style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        item {
            var saved by remember { mutableStateOf<String?>(null) }
            Panel {
                Text("Export", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saved = store.export(monthFileName(year, month), store.txns.filter { it.date.year == year && it.date.monthNumber == month }) }) { Text("${monthName(month)} $year") }
                    OutlinedButton(onClick = { saved = store.export(yearFileName(year), store.txns.filter { it.date.year == year }) }) { Text("All of $year") }
                }
                saved?.let { Text("Saved to $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (s.byCategory.isEmpty()) item { EmptyHint("Nothing spent this month", "Spending by category shows up here.") } else item {
            Panel {
                Text("Spending by category", style = MaterialTheme.typography.titleSmall)
                val top = -s.byCategory.first().second
                s.byCategory.forEach { (cat, amount) ->
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
    }
}
