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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.namaa.core.Budget
import dev.namaa.core.BudgetState
import dev.namaa.core.BudgetStatus
import dev.namaa.core.Debt
import dev.namaa.core.Direction
import dev.namaa.core.Frequency
import dev.namaa.core.Payment
import dev.namaa.core.Recurring
import dev.namaa.core.balanceByPerson
import dev.namaa.core.budgetStatuses
import dev.namaa.core.debtTotals
import dev.namaa.core.formatMoney
import dev.namaa.core.newId
import dev.namaa.core.dueDates
import dev.namaa.core.parseMoney
import dev.namaa.core.withPayment
import kotlinx.datetime.LocalDate

enum class PlanSection(val label: Str) { Budgets(Str.SecBudgets), Repeating(Str.SecRepeating), Owed(Str.SecOwed) }

@Composable
fun BudgetBar(status: BudgetStatus) {
    val colour = when (status.state) { BudgetState.Ok -> MaterialTheme.colorScheme.primary; BudgetState.Near -> MaterialTheme.colorScheme.tertiary; BudgetState.Over -> MaterialTheme.colorScheme.error }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (status.state != BudgetState.Ok) Icon(Icons.Rounded.Warning, null, tint = colour, modifier = Modifier.height(16.dp))
                Text(categoryShown(status.budget.category), style = MaterialTheme.typography.bodyLarge)
            }
            Text(Str.AmountOf.t(money(status.spentMinor), money(status.budget.limitMinor)), style = MaterialTheme.typography.labelLarge, color = colour)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(Modifier.fillMaxWidth(status.fraction.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colour))
        }
        Text(if (status.state == BudgetState.Over) Str.OverBy.t(money(-status.remainingMinor)) else Str.LeftAmount.t(money(status.remainingMinor)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private sealed interface PlanEdit {
    data class BudgetEdit(val budget: Budget?) : PlanEdit
    data class RecurringEdit(val rule: Recurring?) : PlanEdit
    data class DebtEdit(val debt: Debt?) : PlanEdit
    data class PayDebt(val debt: Debt) : PlanEdit
}

/** Budgets, repeating transactions and who owes whom; the add button follows the section shown. */
@Composable
fun PlanScreen(store: NamaaStore, padding: PaddingValues, section: PlanSection, onSection: (PlanSection) -> Unit, addTick: Int) {
    var edit by remember { mutableStateOf<PlanEdit?>(null) }
    var lastTick by remember { mutableStateOf(addTick) }
    if (addTick != lastTick) {
        lastTick = addTick
        edit = when (section) { PlanSection.Budgets -> PlanEdit.BudgetEdit(null); PlanSection.Repeating -> PlanEdit.RecurringEdit(null); PlanSection.Owed -> PlanEdit.DebtEdit(null) }
    }
    val today = store.today
    Column(Modifier.fillMaxSize().padding(padding)) {
        LazyRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PlanSection.entries) { s -> FilterChip(section == s, { onSection(s) }, { Text(s.label.t()) }) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (section) {
                PlanSection.Budgets -> {
                    val statuses = budgetStatuses(store.budgets, store.txns, today.year, today.monthNumber)
                    if (statuses.isEmpty()) item { EmptyHint(Str.NoBudgetsTitle.t(), Str.NoBudgetsBody.t()) }
                    items(statuses, key = { it.budget.category }) { st -> Panel(Modifier.clickable { edit = PlanEdit.BudgetEdit(st.budget) }) { BudgetBar(st) } }
                }
                PlanSection.Repeating -> {
                    if (store.recurring.isEmpty()) item { EmptyHint(Str.NoRepeatTitle.t(), Str.NoRepeatBody.t()) }
                    items(store.recurring, key = { it.id }) { r ->
                        Panel(Modifier.clickable { edit = PlanEdit.RecurringEdit(r) }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(categoryShown(r.category) + if (r.note.isNotBlank()) "  ·  ${r.note}" else "", style = MaterialTheme.typography.titleSmall)
                                    Text(Str.FromDate.t(if (r.frequency == Frequency.Weekly) Str.EveryWeek.t() else Str.EveryMonth.t(), prettyDate(r.start) + " " + num(r.start.year)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(money(r.amountMinor), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                PlanSection.Owed -> {
                    val totals = debtTotals(store.debts)
                    item {
                        Panel {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(Str.TheyOweYou.t(), style = MaterialTheme.typography.labelMedium); Text(money(totals.owedToMeMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary) }
                                Column(horizontalAlignment = Alignment.End) { Text(Str.YouOwe.t(), style = MaterialTheme.typography.labelMedium); Text(money(totals.iOweMinor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                            }
                            balanceByPerson(store.debts).forEach { (person, net) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(person, style = MaterialTheme.typography.bodyMedium)
                                    Text(if (net > 0) Str.OwesYou.t(money(net)) else Str.YouOweAmt.t(money(-net)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    if (store.debts.isEmpty()) item { EmptyHint(Str.NoDebtsTitle.t(), Str.NoDebtsBody.t()) }
                    items(store.debts, key = { it.id }) { debt ->
                        Panel(Modifier.clickable { edit = PlanEdit.DebtEdit(debt) }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(debt.person, style = MaterialTheme.typography.titleSmall)
                                    Text(Str.DebtLine.t(if (debt.direction == Direction.TheyOweMe) Str.Lent.t() else Str.Borrowed.t(), money(debt.principalMinor), prettyDate(debt.date)) + if (debt.note.isNotBlank()) "  ·  ${debt.note}" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if (debt.settled) Str.Settled.t() else Str.LeftAmount.t(money(debt.remainingMinor)), style = MaterialTheme.typography.titleSmall)
                            }
                            if (!debt.settled) TextButton(onClick = { edit = PlanEdit.PayDebt(debt) }) { Text(if (debt.direction == Direction.TheyOweMe) Str.PayReceived.t() else Str.PayMade.t()) }
                        }
                    }
                }
            }
        }
    }
    when (val e = edit) {
        is PlanEdit.BudgetEdit -> BudgetDialog(e.budget, onDismiss = { edit = null }, onDelete = e.budget?.let { b -> { store.saveBudgets(store.budgets.filter { it.category != b.category }); edit = null } }) { saved ->
            store.saveBudgets(store.budgets.filter { it.category != e.budget?.category && it.category != saved.category } + saved); edit = null
        }
        is PlanEdit.RecurringEdit -> RecurringDialog(e.rule, store, today, onDismiss = { edit = null }, onDelete = e.rule?.let { r -> { store.saveRecurring(store.recurring.filter { it.id != r.id }); edit = null } }) { saved ->
            store.saveRecurring(if (e.rule == null) store.recurring + saved else store.recurring.map { if (it.id == saved.id) saved else it }); edit = null
        }
        is PlanEdit.DebtEdit -> DebtDialog(e.debt, today, onDismiss = { edit = null }, onDelete = e.debt?.let { d -> { store.saveDebts(store.debts.filter { it.id != d.id }); edit = null } }) { saved ->
            store.saveDebts(if (e.debt == null) store.debts + saved else store.debts.map { if (it.id == saved.id) saved else it }); edit = null
        }
        is PlanEdit.PayDebt -> PaymentDialog(e.debt, today, onDismiss = { edit = null }) { paid -> store.saveDebts(store.debts.map { if (it.id == paid.id) paid else it }); edit = null }
        null -> {}
    }
}

@Composable
private fun BudgetDialog(existing: Budget?, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Budget) -> Unit) {
    var category by remember { mutableStateOf(existing?.category ?: "Food") }
    var limit by remember { mutableStateOf(existing?.let { formatMoney(it.limitMinor) } ?: "") }
    val minor = parseMoney(limit)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (existing == null) Str.NewBudget.t() else Str.EditBudget.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(category, { category = it }, singleLine = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(defaultCategories) { c -> FilterChip(category == c, { category = c }, { Text(categoryShown(c)) }) } }
                AmountField(limit, { limit = it }, label = Str.LimitPerMonth.t())
            }
        },
        confirmButton = { TextButton(enabled = category.isNotBlank() && minor != null, onClick = { onSave(Budget(category.trim(), minor!!)) }) { Text(Str.Save.t()) } },
        dismissButton = { Row { onDelete?.let { DeleteButton(Str.DeleteBudgetDetail.t(), it) }; TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } } },
    )
}

private const val CATCH_UP_CONFIRM_ABOVE = 12

@Composable
private fun RecurringDialog(existing: Recurring?, store: NamaaStore, today: LocalDate, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Recurring) -> Unit) {
    var amount by remember { mutableStateOf(existing?.let { formatMoney(kotlin.math.abs(it.amountMinor)) } ?: "") }
    var income by remember { mutableStateOf((existing?.amountMinor ?: -1) > 0) }
    var accountId by remember { mutableStateOf(existing?.accountId ?: store.accounts.first().id) }
    var category by remember { mutableStateOf(existing?.category ?: "Bills") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: Frequency.Monthly) }
    var start by remember { mutableStateOf(existing?.start ?: today) }
    val minor = parseMoney(amount)?.takeIf { it > 0 }
    var pending by remember { mutableStateOf<Recurring?>(null) }
    pending?.let { rule ->
        AlertDialog(
            onDismissRequest = { pending = null }, title = { Text(Str.CatchUpTitle.t()) }, text = { Text(Str.CatchUpBody.t(dueDates(rule, today).size)) },
            confirmButton = { TextButton(onClick = { pending = null; onSave(rule) }) { Text(Str.AddThem.t()) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(Str.Cancel.t()) } },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (existing == null) Str.NewRepeating.t() else Str.EditRepeating.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(!income, { income = false }, { Text(Str.ChipSpent.t()) }); FilterChip(income, { income = true }, { Text(Str.ChipReceived.t()) }) }
                AmountField(amount, { amount = it }, placeholder = Str.AmountHint450.t())
                if (store.accounts.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(store.accounts, key = { it.id }) { a -> FilterChip(accountId == a.id, { accountId = a.id }, { Text(a.name) }) } }
                OutlinedTextField(category, { category = it }, singleLine = true, placeholder = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text(Str.NoteOptional.t()) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(frequency == Frequency.Weekly, { frequency = Frequency.Weekly }, { Text(Str.Weekly.t()) }); FilterChip(frequency == Frequency.Monthly, { frequency = Frequency.Monthly }, { Text(Str.Monthly.t()) }) }
                DateField(start, Str.Starts.t()) { start = it }
            }
        },
        confirmButton = {
            TextButton(enabled = minor != null, onClick = {
                // What was already added stays, and the same month or week is never added twice after an edit.
                val rule = Recurring(existing?.id ?: newId(), if (income) minor!! else -minor!!, accountId, category.trim(), note.trim(), frequency, start, existing?.lastGenerated)
                if (dueDates(rule, today).size > CATCH_UP_CONFIRM_ABOVE) pending = rule else onSave(rule)
            }) { Text(Str.Save.t()) }
        },
        dismissButton = { Row { onDelete?.let { DeleteButton(Str.DeleteRepeatingDetail.t(), it) }; TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } } },
    )
}

@Composable
private fun DebtDialog(existing: Debt?, today: LocalDate, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Debt) -> Unit) {
    var person by remember { mutableStateOf(existing?.person ?: "") }
    var direction by remember { mutableStateOf(existing?.direction ?: Direction.TheyOweMe) }
    var amount by remember { mutableStateOf(existing?.let { formatMoney(it.principalMinor) } ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: today) }
    val minor = parseMoney(amount)?.takeIf { it > 0 && (existing == null || it >= existing.paidMinor) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (existing == null) Str.NewDebt.t() else Str.EditDebt.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(direction == Direction.TheyOweMe, { direction = Direction.TheyOweMe }, { Text(Str.ILent.t()) }); FilterChip(direction == Direction.IOweThem, { direction = Direction.IOweThem }, { Text(Str.IBorrowed.t()) }) }
                OutlinedTextField(person, { person = it }, singleLine = true, label = { Text(Str.Person.t()) }, modifier = Modifier.fillMaxWidth())
                AmountField(amount, { amount = it }, label = Str.Amount.t())
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text(Str.NoteOptional.t()) }, modifier = Modifier.fillMaxWidth())
                DateField(date) { date = it }
                if (existing != null && existing.payments.isNotEmpty()) Text(Str.PaymentsSoFar.t(existing.payments.joinToString { formatMoney(it.amountMinor) }), style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(enabled = person.isNotBlank() && minor != null, onClick = { onSave(Debt(existing?.id ?: newId(), person.trim(), direction, minor!!, date, note.trim(), existing?.payments.orEmpty())) }) { Text(Str.Save.t()) } },
        dismissButton = { Row { onDelete?.let { DeleteButton(Str.DeleteDebtDetail.t(existing?.payments?.size ?: 0), it) }; TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } } },
    )
}

@Composable
private fun PaymentDialog(debt: Debt, today: LocalDate, onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var amount by remember { mutableStateOf(formatMoney(debt.remainingMinor)) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    val minor = parseMoney(amount)?.takeIf { it > 0 && it <= debt.remainingMinor }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(Str.PaymentTitle.t(debt.person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Str.LeftAmount.t(money(debt.remainingMinor)), style = MaterialTheme.typography.bodyMedium)
                AmountField(amount, { amount = it }, label = Str.AmountAtMost.t())
                OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text(Str.NoteOptional.t()) }, modifier = Modifier.fillMaxWidth())
                DateField(date) { date = it }
            }
        },
        confirmButton = { TextButton(enabled = minor != null, onClick = { onSave(debt.withPayment(Payment(date, minor!!, note.trim()))) }) { Text(Str.Save.t()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}
