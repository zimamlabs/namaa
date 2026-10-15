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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import dev.namaa.core.ReleaseNotes
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
fun NamaaApp(store: NamaaStore, drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed), onPageChanged: (Tab) -> Unit = {}, showSplash: Boolean = true) {
    val language = I18n.resolve(store.settings.lang, systemLanguage())
    CompositionLocalProvider(
        LocalAppLocale provides AppLocale(language, store.settings.arabicDigits),
        LocalLayoutDirection provides if (language == Lang.Ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { NamaaTheme(store.settings.themeMode, store.settings.palette, store.settings.textSize, store.settings.density == "compact") {
        var splash by remember { mutableStateOf(showSplash) }
        var page by remember { mutableStateOf<Page?>(null) }
        var settingsPage by remember { mutableStateOf<SettingsPage?>(null) }
        var editing by remember { mutableStateOf<Editing?>(null) }
        var planSection by remember { mutableStateOf(PlanSection.Budgets) }
        var planAdd by remember { mutableStateOf(0) }
        var quickAdd by remember { mutableStateOf(false) }
        var showNew by remember { mutableStateOf(false) }
        var extra by remember { mutableStateOf<Tab?>(null) }
        val shown = visibleTabs(store.settings)
        val tabs = shown + listOfNotNull(extra?.takeIf { it !in shown })
        val scope = rememberCoroutineScope()
        val pager = rememberPagerState(initialPage = tabs.indexOf(sectionTab(store.settings.startSection)).coerceAtLeast(0)) { tabs.size }
        val tab = tabs[pager.currentPage.coerceIn(0, tabs.lastIndex)]
        val reduceMotion = store.settings.reduceMotion
        fun goTo(target: Tab) {
            if (target !in tabs) extra = target
            val index = (visibleTabs(store.settings) + listOfNotNull(extra)).indexOf(target).coerceAtLeast(0)
            scope.launch { if (reduceMotion) pager.scrollToPage(index) else pager.animateScrollToPage(index) }
        }
        fun closeDrawerThen(action: () -> Unit) { scope.launch { drawerState.close() }; action() }
        LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { onPageChanged(tabs[it.coerceIn(0, tabs.lastIndex)]) } }
        // An extra page (a hidden section opened from Home) goes away once the user leaves it.
        LaunchedEffect(pager.currentPage, extra) { if (extra != null && tab != extra && !pager.isScrollInProgress) extra = null }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            PlatformBackHandler(page != null) { page = if (page == Page.About) Page.Settings else null }
            PlatformBackHandler(page == null && drawerState.isOpen) { scope.launch { drawerState.close() } }
            PlatformBackHandler(page == null && !drawerState.isOpen && tab != Tab.Home) { goTo(Tab.Home) }
            if (store.locked) LockScreen(store) else if (store.blocked.isNotEmpty()) RecoveryScreen(store) else when (page) {
                Page.About -> AboutScreen(onBack = { page = Page.Settings }, onTour = { page = null; store.tourRequested = true })
                Page.Settings -> SettingsScreen(store, settingsPage, { settingsPage = it }, onBack = { page = null }, onAbout = { page = Page.About }, onTour = { page = null; store.tourRequested = true })
                null -> ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            tabs = shown, current = tab,
                            onSection = { t -> closeDrawerThen { goTo(t) } },
                            onSearch = { closeDrawerThen { goTo(Tab.Money) } },
                            onQuickAdd = { closeDrawerThen { quickAdd = true } },
                            onSettings = { closeDrawerThen { settingsPage = null; page = Page.Settings } },
                            onAbout = { closeDrawerThen { page = Page.About } },
                            onWhatsNew = { closeDrawerThen { showNew = true } },
                            onTour = { closeDrawerThen { store.tourRequested = true } },
                        )
                    },
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = { Text(if (tab == Tab.Home) Str.AppName.t() else tab.label.t(), fontWeight = FontWeight.SemiBold) },
                                navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Rounded.Menu, Str.MenuBtn.t()) } },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                            )
                        },
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                                shown.forEach { t -> NavigationBarItem(tab == t, { goTo(t) }, { Icon(t.icon, t.label.t()) }, label = { Text(t.label.t()) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary)) }
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
                        Box(Modifier.fillMaxSize()) {
                            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { tabs[it.coerceIn(0, tabs.lastIndex)].id }) { index ->
                                when (tabs[index.coerceIn(0, tabs.lastIndex)]) {
                                    Tab.Home -> HomeScreen(store, padding, onExpense = { editing = Editing.Transaction(null, false) }, onIncome = { editing = Editing.Transaction(null, true) },
                                        onTransfer = { editing = Editing.Transfer() }, onOpen = { goTo(it) }, onEdit = { editing = editorFor(store, it) }, onTour = { store.tourRequested = true })
                                    Tab.Money -> TransactionsScreen(store, padding) { editing = editorFor(store, it) }
                                    Tab.Accounts -> AccountsScreen(store, padding) { editing = Editing.AccountEdit(it) }
                                    Tab.Plan -> PlanScreen(store, padding, planSection, { planSection = it }, planAdd)
                                    Tab.Report -> ReportScreen(store, padding)
                                }
                            }
                            if (pager.currentPage == 0) DrawerEdge { scope.launch { drawerState.open() } }
                        }
                    }
                }
            }
            if (quickAdd) QuickAddDialog(
                onDismiss = { quickAdd = false },
                onExpense = { quickAdd = false; editing = Editing.Transaction(null, false) },
                onIncome = { quickAdd = false; editing = Editing.Transaction(null, true) },
                onTransfer = { quickAdd = false; editing = Editing.Transfer() },
            )
            if (showNew) AlertDialog(
                onDismissRequest = { showNew = false },
                title = { Text(Str.WhatsNewIn.t(APP_VERSION)) },
                text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { val notes = ReleaseNotes.forVersion(APP_VERSION, language); notes.forEach { Text("•  ${num(it)}", style = MaterialTheme.typography.bodyMedium) } } },
                confirmButton = { TextButton(onClick = { showNew = false }) { Text(Str.Close.t()) } },
            )
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
