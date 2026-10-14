package dev.namaa.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.namaa.core.Account
import dev.namaa.core.Txn

const val APP_VERSION = "0.4.0"

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home), Money("Money", Icons.Rounded.ReceiptLong), Accounts("Accounts", Icons.Rounded.AccountBalanceWallet), Plan("Plan", Icons.Rounded.Savings), Report("Report", Icons.Rounded.BarChart),
}

/** Which dialog is open: a new or edited transaction, a transfer, or a new or renamed account. */
private sealed interface Editing {
    data class Transaction(val txn: Txn?, val income: Boolean = false) : Editing
    data object Transfer : Editing
    data class AccountEdit(val account: Account?) : Editing
}

private enum class Page { Settings, About }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamaaApp(store: NamaaStore) {
    NamaaTheme(store.prefs.theme) {
        var splash by remember { mutableStateOf(true) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var page by remember { mutableStateOf<Page?>(null) }
        var editing by remember { mutableStateOf<Editing?>(null) }
        var planSection by remember { mutableStateOf(PlanSection.Budgets) }
        var planAdd by remember { mutableStateOf(0) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            PlatformBackHandler(page != null) { page = if (page == Page.About) Page.Settings else null }
            PlatformBackHandler(page == null && tab != Tab.Home) { tab = Tab.Home }
            when (page) {
                Page.About -> AboutScreen(onBack = { page = Page.Settings }, onTour = { page = null; store.tourRequested = true })
                Page.Settings -> SettingsScreen(store, onBack = { page = null }, onAbout = { page = Page.About }, onTour = { page = null; store.tourRequested = true })
                null -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(title = { Text(if (tab == Tab.Home) "Namaa" else tab.label) }, actions = {
                            IconButton(onClick = { page = Page.Settings }) { Icon(Icons.Rounded.Settings, "Settings") }
                        })
                    },
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            Tab.entries.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, t.label) }, label = { Text(t.label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary)) }
                        }
                    },
                    floatingActionButton = {
                        when (tab) {
                            Tab.Money -> FloatingActionButton(onClick = { editing = Editing.Transaction(null) }) { Icon(Icons.Rounded.Add, "New transaction") }
                            Tab.Accounts -> FloatingActionButton(onClick = { editing = Editing.AccountEdit(null) }) { Icon(Icons.Rounded.Add, "New account") }
                            Tab.Plan -> FloatingActionButton(onClick = { planAdd++ }) { Icon(Icons.Rounded.Add, "Add") }
                            else -> {}
                        }
                    },
                ) { padding ->
                    AnimatedContent(tab, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "tab") { shownTab ->
                    when (shownTab) {
                        Tab.Home -> HomeScreen(store, padding, onExpense = { editing = Editing.Transaction(null, false) }, onIncome = { editing = Editing.Transaction(null, true) },
                            onTransfer = { editing = Editing.Transfer }, onOpen = { tab = it }, onEdit = { editing = Editing.Transaction(it) }, onTour = { store.tourRequested = true })
                        Tab.Money -> TransactionsScreen(store, padding) { editing = Editing.Transaction(it) }
                        Tab.Accounts -> AccountsScreen(store, padding) { editing = Editing.AccountEdit(it) }
                        Tab.Plan -> PlanScreen(store, padding, planSection, { planSection = it }, planAdd)
                        Tab.Report -> ReportScreen(store, padding)
                    }
                    }
                }
            }
            when (val e = editing) {
                is Editing.Transaction -> TxnDialog(e.txn, e.income, store.accounts, store.today, onDismiss = { editing = null },
                    onDelete = e.txn?.let { t -> { store.deleteTxn(t.id); editing = null } }) { saved -> store.saveTxn(saved); editing = null }
                Editing.Transfer -> TransferDialog(store.accounts, store.today, onDismiss = { editing = null }) { from, to, minor, date, note -> store.saveTransfer(date, from, to, minor, note); editing = null }
                is Editing.AccountEdit -> AccountDialog(e.account, store.prefs.currency, onDismiss = { editing = null }) { saved ->
                    store.saveAccounts(if (e.account == null) store.accounts + saved else store.accounts.map { if (it.id == saved.id) saved else it })
                    editing = null
                }
                null -> {}
            }
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
            if (!splash) WelcomeHost(store)
        }
    }
}
