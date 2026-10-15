package dev.namaa.app

import dev.namaa.core.Str
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
import dev.namaa.core.TxnFilter
import dev.namaa.core.filterTxns
import dev.namaa.core.monthFileName
import dev.namaa.core.yearFileName
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.namaa.core.Account
import dev.namaa.core.Txn
import dev.namaa.core.balance
import dev.namaa.core.formatMoney
import dev.namaa.core.monthSummary
import dev.namaa.core.shiftMonth

@Composable
fun TransactionsScreen(store: NamaaStore, padding: PaddingValues, onEdit: (Txn) -> Unit) {
    var filter by remember { mutableStateOf(TxnFilter()) }
    var open by remember { mutableStateOf(false) }
    val shown = remember(store.txns, filter) { filterTxns(store.txns, filter) }
    LaunchedEffect(store.historyAccount) { store.historyAccount?.let { filter = TxnFilter(accountId = it); open = true; store.historyAccount = null } }
    val categories = remember(store.txns) { store.txns.map { it.category.ifEmpty { "Other" } }.distinct().sorted() }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(filter.text, { filter = filter.copy(text = it) }, Modifier.weight(1f), singleLine = true, placeholder = { Text(Str.SearchHint.t()) }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
                    FilterChip(open || filter.category.isNotBlank() || filter.accountId.isNotBlank() || filter.from != null || filter.to != null, { open = !open }, { Text(Str.Filters.t()) }, leadingIcon = { Icon(Icons.Rounded.FilterList, null, Modifier.size(18.dp)) })
                }
                if (open) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categories) { c -> FilterChip(filter.category == c, { filter = filter.copy(category = if (filter.category == c) "" else c) }, { Text(categoryShown(c)) }) } }
                    if (store.accounts.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(store.accounts, key = { it.id }) { a -> FilterChip(filter.accountId == a.id, { filter = filter.copy(accountId = if (filter.accountId == a.id) "" else a.id) }, { Text(a.name) }) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { DateField(filter.from ?: store.today, Str.From.t()) { filter = filter.copy(from = it) } }
                        Box(Modifier.weight(1f)) { DateField(filter.to ?: store.today, Str.To.t()) { filter = filter.copy(to = it) } }
                    }
                }
                if (filter.active) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Str.ShownOf.t(shown.size, store.txns.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { filter = TxnFilter() }) { Text(Str.ClearFilters.t()) }
                }
            }
        }
        if (store.txns.isEmpty()) item { EmptyHint(Str.NoTxnTitle.t(), "Tap + to add one. They are saved per year in <year>.csv.") }
        else if (shown.isEmpty()) item { EmptyHint(Str.NothingMatches.t(), Str.TryFewer.t()) }
        items(shown.take(500), key = { it.id }) { t -> TxnRow(t, store.accounts, txnActions(store, t) { onEdit(t) }) { onEdit(t) } }
    }
}

@Composable
fun AccountsScreen(store: NamaaStore, padding: PaddingValues, onEdit: (Account) -> Unit, onHistory: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(store.accounts.sortedBy { it.archived }, key = { it.id }) { a ->
            ContextMenuBox(accountActions(store, a, { onEdit(a) }) { onHistory(a.id) }, { onEdit(a) }) { Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(a.name, style = MaterialTheme.typography.titleMedium)
                        Text(a.currency + if (a.openingMinor != 0L) "  ·  " + Str.OpenedWith.t(money(a.openingMinor)) else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(balance(a, store.txns)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            } }
        }
    }
}

/** One month at a time: totals, and spending by category with bars. */
@Composable
fun ReportScreen(store: NamaaStore, padding: PaddingValues) {
    var shown by remember { mutableStateOf(store.today.year to store.today.monthNumber) }
    val (year, month) = shown
    val s = monthSummary(store.activeTxns, year, month)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { shown = shiftMonth(year, month, -1) }) { Icon(Icons.Rounded.ChevronLeft, Str.PrevMonth.t()) }
                Text(num("${monthName(month)} $year"), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { shown = shiftMonth(year, month, 1) }) { Icon(Icons.Rounded.ChevronRight, Str.NextMonth.t()) }
            }
        }
        item { CurrencyChips(store) }
        item {
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(Str.Income.t(), style = MaterialTheme.typography.labelMedium); Text(money(s.incomeMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(Str.Spent.t(), style = MaterialTheme.typography.labelMedium); Text(money(-s.expenseMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                    Column(horizontalAlignment = Alignment.End) { Text(Str.Left.t(), style = MaterialTheme.typography.labelMedium); Text(money(s.netMinor), style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        item {
            var saved by remember { mutableStateOf<String?>(null) }
            Panel {
                Text(Str.Export.t(), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saved = store.export(monthFileName(year, month), store.txns.filter { it.date.year == year && it.date.monthNumber == month }) }) { Text(num("${monthName(month)} $year")) }
                    OutlinedButton(onClick = { saved = store.export(yearFileName(year), store.txns.filter { it.date.year == year }) }) { Text(Str.AllOfYear.t(year)) }
                }
                saved?.let {
                    Text(Str.SavedTo.raw(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (store.files != null) OutlinedButton(onClick = { store.saveCopyOf(it) {} }) { Text(Str.SaveCopy.t()) }
                }
            }
        }
        if (s.byCategory.isEmpty()) item { EmptyHint(Str.NothingSpentTitle.t(), Str.NothingSpentBody.t()) } else item {
            Panel {
                Text(Str.SpendingByCategory.t(), style = MaterialTheme.typography.titleSmall)
                val top = -s.byCategory.first().second
                s.byCategory.forEach { (cat, amount) ->
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(categoryShown(cat), style = MaterialTheme.typography.bodyMedium)
                            Text(money(-amount), style = MaterialTheme.typography.bodyMedium)
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
