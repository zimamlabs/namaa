package dev.namaa.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.namaa.core.I18n
import dev.namaa.core.Lang
import dev.namaa.core.Str
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

const val APP_VERSION = "0.6.0"

enum class Tab(val id: String, val label: Str, val icon: ImageVector) {
    Home("home", Str.TabHome, Icons.Rounded.Home), Money("money", Str.TabMoney, Icons.Rounded.ReceiptLong), Accounts("accounts", Str.TabAccounts, Icons.Rounded.AccountBalanceWallet), Plan("plan", Str.TabPlan, Icons.Rounded.Savings), Report("report", Str.TabReport, Icons.Rounded.BarChart),
}

/** Which dialog is open: a new or edited transaction, a transfer, or a new or renamed account. */
private sealed interface Editing {
    data class Transaction(val txn: Txn?, val income: Boolean = false) : Editing
    data class Transfer(val legs: List<Txn>? = null) : Editing
    data class AccountEdit(val account: Account?) : Editing
}

/** A transfer leg opens the transfer editor (both legs together); any other row opens the ordinary editor. */
private fun editorFor(store: NamaaStore, txn: Txn): Editing =
    if (txn.transferId.isEmpty()) Editing.Transaction(txn) else Editing.Transfer(store.txns.filter { it.transferId == txn.transferId })

private enum class Page { Settings, About }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamaaApp(store: NamaaStore) {
    val language = I18n.resolve(store.settings.lang, systemLanguage())
    CompositionLocalProvider(
        LocalAppLocale provides AppLocale(language, store.settings.arabicDigits),
        LocalLayoutDirection provides if (language == Lang.Ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { NamaaTheme(store.settings.themeMode, store.settings.palette, store.settings.textSize, store.settings.density == "compact") {
        var splash by remember { mutableStateOf(true) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var page by remember { mutableStateOf<Page?>(null) }
        var settingsPage by remember { mutableStateOf<SettingsPage?>(null) }
        var editing by remember { mutableStateOf<Editing?>(null) }
        var planSection by remember { mutableStateOf(PlanSection.Budgets) }
        var planAdd by remember { mutableStateOf(0) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            PlatformBackHandler(page != null) { page = if (page == Page.About) Page.Settings else null }
            PlatformBackHandler(page == null && tab != Tab.Home) { tab = Tab.Home }
            if (store.locked) LockScreen(store) else if (store.blocked.isNotEmpty()) RecoveryScreen(store) else when (page) {
                Page.About -> AboutScreen(onBack = { page = Page.Settings }, onTour = { page = null; store.tourRequested = true })
                Page.Settings -> SettingsScreen(store, settingsPage, { settingsPage = it }, onBack = { page = null }, onAbout = { page = Page.About }, onTour = { page = null; store.tourRequested = true })
                null -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(title = { Text(if (tab == Tab.Home) Str.AppName.t() else tab.label.t()) }, actions = {
                            IconButton(onClick = { page = Page.Settings }) { Icon(Icons.Rounded.Settings, Str.SettingsTitle.t()) }
                        })
                    },
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            Tab.entries.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, t.label.t()) }, label = { Text(t.label.t()) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary)) }
                        }
                    },
                    floatingActionButton = {
                        when (tab) {
                            Tab.Money -> FloatingActionButton(onClick = { editing = Editing.Transaction(null) }) { Icon(Icons.Rounded.Add, Str.NewTransactionFab.t()) }
                            Tab.Accounts -> FloatingActionButton(onClick = { editing = Editing.AccountEdit(null) }) { Icon(Icons.Rounded.Add, Str.NewAccountFab.t()) }
                            Tab.Plan -> FloatingActionButton(onClick = { planAdd++ }) { Icon(Icons.Rounded.Add, Str.AddFab.t()) }
                            else -> {}
                        }
                    },
                ) { padding ->
                    AnimatedContent(tab, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "tab") { shownTab ->
                    when (shownTab) {
                        Tab.Home -> HomeScreen(store, padding, onExpense = { editing = Editing.Transaction(null, false) }, onIncome = { editing = Editing.Transaction(null, true) },
                            onTransfer = { editing = Editing.Transfer() }, onOpen = { tab = it }, onEdit = { editing = editorFor(store, it) }, onTour = { store.tourRequested = true })
                        Tab.Money -> TransactionsScreen(store, padding) { editing = editorFor(store, it) }
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
                is Editing.Transfer -> TransferDialog(store.accounts, store.today, e.legs, e.legs?.let { l -> { store.deleteTxn(l.first().id); editing = null } }, onDismiss = { editing = null }) { from, to, minor, date, note ->
                    if (e.legs == null) store.saveTransfer(date, from, to, minor, note) else store.updateTransfer(e.legs.first().transferId, date, from, to, minor, note)
                    editing = null
                }
                is Editing.AccountEdit -> AccountDialog(e.account, store.settings.currency, onDismiss = { editing = null }) { saved ->
                    store.saveAccounts(if (e.account == null) store.accounts + saved else store.accounts.map { if (it.id == saved.id) saved else it })
                    editing = null
                }
                null -> {}
            }
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
            if (!splash) WelcomeHost(store)
        }
    } }
}
