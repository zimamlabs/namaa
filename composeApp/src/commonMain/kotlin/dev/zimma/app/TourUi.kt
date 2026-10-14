package dev.zimma.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zimma.core.Onboarding
import dev.zimma.core.ReleaseNotes
import dev.zimma.core.Welcome
import kotlinx.coroutines.launch

/** Opens the first-run tour on a fresh install (or when asked from Settings) and the one-time notes after an update. */
@Composable
fun WelcomeHost(store: ZimmaStore) {
    // Decided once, when Zimma opens: finishing the tour changes what is stored.
    val decision = remember { Onboarding.decide(store.prefs.lastSeenVersion, store.prefs.tourDone, store.hasData, APP_VERSION) }
    var notesClosed by remember { mutableStateOf(false) }
    var tourClosed by remember { mutableStateOf(false) }
    fun seen() = store.updatePrefs { it.copy(lastSeenVersion = APP_VERSION, tourDone = true) }
    if (store.tourRequested || (decision == Welcome.Tour && !tourClosed)) {
        Tour(store, onClose = { seen(); tourClosed = true; store.tourRequested = false })
    } else if (decision == Welcome.WhatsNew && !notesClosed) {
        val notes = ReleaseNotes.forVersion(APP_VERSION)
        if (notes.isEmpty()) LaunchedEffect(Unit) { seen(); notesClosed = true } else AlertDialog(
            onDismissRequest = { seen(); notesClosed = true },
            title = { Text("What's new in Zimma $APP_VERSION") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { notes.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = { TextButton(onClick = { seen(); notesClosed = true }) { Text("Close") } },
        )
    }
}

private data class TourPage(val title: String, val body: String)

private val pages = listOf(
    TourPage("Welcome to Zimma", "Private money tracking: your accounts, what comes in and what goes out. It works offline, has no account and sends nothing anywhere."),
    TourPage("Plain files you own", "Everything is saved as accounts.json and one CSV per year, readable in any editor or spreadsheet. Nothing is locked inside the app."),
    TourPage("Where your data lives", "By default inside the app. Allow All files access to keep it in a visible Zimam/Zimma folder that survives reinstalling the app. Older folders are copied in, checked, and left as a backup."),
    TourPage("Your currency and look", "Pick the currency for new accounts and how Zimma looks. You can change both later in Settings."),
    TourPage("You are ready", "Add an expense or income from Home, move money between accounts, and see where it went. You can see this tour again from Settings."),
)

@Composable
private fun Tour(store: ZimmaStore, onClose: () -> Unit) {
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text("Skip") } }
            HorizontalPager(state, Modifier.weight(1f).fillMaxWidth()) { i ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(pages[i].title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Text(pages[i].body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (i == 2) store.askAccess?.takeIf { !store.location.shared }?.let { ask ->
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = ask) { Text("Use the Zimam/Zimma folder") }
                    }
                    if (i == 3) {
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.take(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.drop(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) -> FilterChip(store.prefs.theme == key, { store.updatePrefs { it.copy(theme = key) } }, { Text(label) }) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i -> Box(Modifier.size(8.dp).background(if (i == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
                }
                val last = state.currentPage == pages.lastIndex
                Button(onClick = { if (last) onClose() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } }) { Text(if (last) "Done" else "Next") }
            }
        }
    }
}
