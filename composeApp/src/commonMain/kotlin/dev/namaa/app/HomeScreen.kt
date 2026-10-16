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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import dev.namaa.core.HomeCards
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

/**
 * The first page, kept minimal: one hero (the month's balance and budget) with the three quick actions, then at most four
 * single-purpose cards chosen in Settings, each opening its section. A designed empty state replaces the cards on a fresh start.
 */
@Composable
fun HomeScreen(
    store: NamaaStore, padding: PaddingValues,
    onExpense: () -> Unit, onIncome: () -> Unit, onTransfer: () -> Unit,
    onOpen: (Tab) -> Unit, onEdit: (Txn) -> Unit, onTour: () -> Unit,
) {
    val today = store.today
    val month = monthSummary(store.activeTxns, today.year, today.monthNumber)
    val balances = balancesByCurrency(store.accounts, store.txns)
    val budgetStatus = budgetStatusesIn(store.budgets, store.accounts, store.txns, today.year, today.monthNumber, store.settings.currency)
    val cards = HomeCards.visible(store.settings.homeCardOrder, store.settings.hiddenHomeCards)
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = gutter + 4.dp, end = gutter + 4.dp, top = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(rowGap + 4.dp)) {
        if (store.rowProblems.isNotEmpty() || store.sideCopies.isNotEmpty()) item { RowProblemsCard(store) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.heroGlow().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(num("${monthName(today.monthNumber)} ${today.year}"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    balances.forEachIndexed { i, (code, minor) ->
                        Text((money(minor) + " " + code).trim(), style = if (i == 0) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    CurrencyChips(store)
                    Spacer(Modifier.height(10.dp))
                    BudgetLine(budgetStatus.filter { store.currencies.size < 2 || it.budget.currency.ifBlank { store.settings.currency }.currencyCode() == store.activeCurrency })
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(Str.Income.t(), money(month.incomeMinor), MaterialTheme.colorScheme.secondary)
                        Stat(Str.Spent.t(), money(-month.expenseMinor), MaterialTheme.colorScheme.primary)
                        Stat(Str.Left.t(), money(month.netMinor), MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Rounded.Remove, Str.QuickExpense.t(), Modifier.weight(1f), onExpense)
                QuickAction(Icons.Rounded.Add, Str.Income.t(), Modifier.weight(1f), onIncome)
                QuickAction(Icons.Rounded.SwapHoriz, Str.QuickTransfer.t(), Modifier.weight(1f), onTransfer)
            }
        }
        if (store.txns.isEmpty() && store.activeAccounts.size <= 1) item { HomeEmpty(onExpense, onTour) } else cards.forEach { id ->
            when (id) {
                "budgets" -> if (budgetStatus.isNotEmpty()) item(key = id) {
                    Panel {
                        val warn = budgetStatus.count { it.state != BudgetState.Ok }
                        CardTitle(Icons.Rounded.Savings, if (warn > 0) Str.BudgetsAttention.t(warn) else Str.Budgets.t()) { onOpen(Tab.Plan) }
                        budgetStatus.take(2).forEach { BudgetBar(it, if (store.currencies.size > 1) it.budget.currency.ifBlank { store.settings.currency }.currencyCode() else "") }
                    }
                }
                "spending" -> if (month.byCategory.isNotEmpty()) item(key = id) {
                    Panel {
                        CardTitle(Icons.Rounded.PieChart, Str.WhereItWent.t()) { onOpen(Tab.Report) }
                        DonutChart(month.byCategory.map { it.first to -it.second }, Str.Spent.t(), money(-month.expenseMinor))
                    }
                }
                "recent" -> if (store.txns.isNotEmpty()) item(key = id) {
                    Panel {
                        CardTitle(Icons.Rounded.History, Str.Recent.t()) { onOpen(Tab.Money) }
                        store.txns.take(3).forEach { t -> TxnRow(t, store.accounts, txnActions(store, t) { onEdit(t) }) { onEdit(t) } }
                    }
                }
                "accounts" -> if (store.activeAccounts.size > 1) item(key = id) {
                    Panel {
                        CardTitle(Icons.Rounded.AccountBalanceWallet, Str.TabAccounts.t()) { onOpen(Tab.Accounts) }
                        store.activeAccounts.take(3).forEach { a ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(a.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
                                Text((money(balance(a, store.txns)) + if (store.currencies.size > 1) " " + a.currency.currencyCode() else ""), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                "trend" -> item(key = id) {
                    Panel {
                        CardTitle(Icons.Rounded.BarChart, Str.LastSix.t()) { onOpen(Tab.Report) }
                        MonthBars(monthlyTotals(store.activeTxns, today.year, today.monthNumber, 6))
                    }
                }
            }
        }
    }
}

/** A card header: icon, title, and a chevron; tapping it opens the section the card summarises. */
@Composable
private fun CardTitle(icon: ImageVector, text: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onOpen).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One thin bar for how much of this month's budgets is used, under the balance; nothing when there are no budgets. */
@Composable
private fun BudgetLine(statuses: List<dev.namaa.core.BudgetStatus>) {
    if (statuses.isEmpty()) return
    val spent = statuses.sumOf { it.spentMinor }; val limit = statuses.sumOf { it.budget.limitMinor }
    val fraction = if (limit > 0) (spent.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val colour = if (spent > limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(Str.AmountOf.t(money(spent), money(limit)) + "  ·  " + Str.Budgets.t(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(Modifier.fillMaxWidth(fraction.coerceAtLeast(0.02f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colour))
        }
    }
}

/** A fresh start: the mark in a soft glow, one sentence, one button. */
@Composable
private fun HomeEmpty(onAdd: () -> Unit, onTour: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { BrandMark(Modifier.size(56.dp)) }
        Text(Str.HomeEmptyTitle.t(), style = MaterialTheme.typography.titleLarge)
        Text(Str.HomeEmptyBody.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        androidx.compose.material3.Button(onClick = onAdd) { Text(Str.HomeEmptyButton.t()) }
        TextButton(onClick = onTour) { Text(Str.ShowTour.t()) }
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
