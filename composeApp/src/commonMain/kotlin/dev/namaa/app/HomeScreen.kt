package dev.namaa.app

import dev.namaa.core.Str
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.namaa.core.Account
import dev.namaa.core.ReleaseNotes
import dev.namaa.core.Txn
import dev.namaa.core.BudgetState
import dev.namaa.core.balance
import dev.namaa.core.balancesByCurrency
import dev.namaa.core.currencyCode
import dev.namaa.core.budgetStatusesIn
import dev.namaa.core.formatMoney
import dev.namaa.core.monthSummary
import dev.namaa.core.monthlyTotals

@Composable
fun SectionTitle(icon: ImageVector, text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        trailing?.invoke()
    }
}

@Composable
internal fun TxnRow(t: Txn, accounts: List<Account>, actions: List<MenuAction>, onClick: () -> Unit) {
    val colour = if (t.amountMinor < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
    ContextMenuBox(actions, onClick, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(categoryShown(t.category).take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(categoryShown(t.category), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            val account = accounts.firstOrNull { it.id == t.accountId }?.name.orEmpty()
            Text(prettyDate(t.date) + (if (account.isNotEmpty() && accounts.size > 1) "  ·  $account" else "") + (if (t.note.isNotBlank()) "  ·  ${t.note}" else ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(money(t.amountMinor), style = MaterialTheme.typography.titleSmall, color = colour)
    } }
}

/** The first page: where the month stands, where the money went, and the quickest ways to add to it. */
@Composable
fun HomeScreen(
    store: NamaaStore, padding: PaddingValues,
    onExpense: () -> Unit, onIncome: () -> Unit, onTransfer: () -> Unit,
    onOpen: (Tab) -> Unit, onEdit: (Txn) -> Unit, onTour: () -> Unit,
) {
    val today = store.today
    val language = lang()
    val month = monthSummary(store.activeTxns, today.year, today.monthNumber)
    val balances = balancesByCurrency(store.accounts, store.txns)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (store.rowProblems.isNotEmpty() || store.sideCopies.isNotEmpty()) item { RowProblemsCard(store) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.heroGlow().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(num("${monthName(today.monthNumber)} ${today.year}") + "  ·  " + prettyDate(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Str.Balance.t(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    balances.forEachIndexed { i, (code, minor) ->
                        Text((money(minor) + " " + code).trim(), style = if (i == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    CurrencyChips(store)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(Str.Income.t(), money(month.incomeMinor), MaterialTheme.colorScheme.secondary)
                        Stat(Str.Spent.t(), money(-month.expenseMinor), MaterialTheme.colorScheme.primary)
                        Stat(Str.Left.t(), money(month.netMinor), MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(Icons.Rounded.Remove, Str.QuickExpense.t(), Modifier.weight(1f), onExpense)
                QuickAction(Icons.Rounded.Add, Str.Income.t(), Modifier.weight(1f), onIncome)
                QuickAction(Icons.Rounded.SwapHoriz, Str.QuickTransfer.t(), Modifier.weight(1f), onTransfer)
            }
        }
        val budgetStatus = budgetStatusesIn(store.budgets, store.accounts, store.txns, today.year, today.monthNumber, store.settings.currency)
        if (budgetStatus.isNotEmpty()) item {
            Panel {
                val warn = budgetStatus.count { it.state != BudgetState.Ok }
                SectionTitle(Icons.Rounded.Savings, if (warn > 0) Str.BudgetsAttention.t(warn) else Str.Budgets.t()) { TextButton(onClick = { onOpen(Tab.Plan) }) { Text(Str.TabPlan.t()) } }
                budgetStatus.take(3).forEach { BudgetBar(it, if (store.currencies.size > 1) it.budget.currency.ifBlank { store.settings.currency }.currencyCode() else "") }
            }
        }
        if (month.byCategory.isNotEmpty()) item {
            Panel {
                SectionTitle(Icons.Rounded.PieChart, Str.WhereItWent.t())
                DonutChart(month.byCategory.map { it.first to -it.second }, Str.Spent.t(), money(-month.expenseMinor))
            }
        }
        item {
            Panel {
                SectionTitle(Icons.Rounded.BarChart, Str.LastSix.t())
                MonthBars(monthlyTotals(store.activeTxns, today.year, today.monthNumber, 6))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(Icons.Rounded.AccountBalanceWallet, Str.TabAccounts.t()) { TextButton(onClick = { onOpen(Tab.Accounts) }) { Text(Str.All.t()) } }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(store.activeAccounts, key = { it.id }) { a ->
                        Card(Modifier.width(168.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                Text(money(balance(a, store.txns)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(a.currency, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            Panel {
                SectionTitle(Icons.Rounded.History, Str.Recent.t()) { TextButton(onClick = { onOpen(Tab.Money) }) { Text(Str.SeeAll.t()) } }
                if (store.txns.isEmpty()) Text(Str.NothingYet.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                store.txns.take(5).forEach { t -> TxnRow(t, store.accounts, txnActions(store, t) { onEdit(t) }) { onEdit(t) } }
            }
        }
        val notes = ReleaseNotes.forVersion(APP_VERSION, language)
        if (notes.isNotEmpty()) item {
            Panel {
                SectionTitle(Icons.Rounded.AutoAwesome, "What's new in Namaa $APP_VERSION")
                notes.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
                TextButton(onClick = onTour) { Text(Str.ShowTour.t()) }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, colour: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colour)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colour)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, text: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick, modifier, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)) {
        Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text, maxLines = 1)
    }
}
