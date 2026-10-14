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
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (store.txns.isEmpty()) item { EmptyHint("No transactions yet", "Tap + to add one. They are saved per year in <year>.csv.") }
        items(store.txns.take(500), key = { it.id }) { t -> TxnRow(t, store.accounts) { onEdit(t) } }
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
