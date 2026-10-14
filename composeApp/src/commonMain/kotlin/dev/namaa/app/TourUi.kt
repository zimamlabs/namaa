package dev.namaa.app

import dev.namaa.core.Str
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
import dev.namaa.core.Onboarding
import dev.namaa.core.ReleaseNotes
import dev.namaa.core.Welcome
import kotlinx.coroutines.launch

/** Opens the first-run tour on a fresh install (or when asked from Settings) and the one-time notes after an update. */
@Composable
fun WelcomeHost(store: NamaaStore) {
    // Decided once, when Namaa opens: finishing the tour changes what is stored.
    val decision = remember { Onboarding.decide(store.prefs.lastSeenVersion, store.prefs.tourDone, store.hasData, APP_VERSION) }
    var notesClosed by remember { mutableStateOf(false) }
    var tourClosed by remember { mutableStateOf(false) }
    fun seen() = store.updatePrefs { it.copy(lastSeenVersion = APP_VERSION, tourDone = true) }
    if (store.tourRequested || (decision == Welcome.Tour && !tourClosed)) {
        Tour(store, onClose = { seen(); tourClosed = true; store.tourRequested = false })
    } else if (decision == Welcome.WhatsNew && !notesClosed) {
        val notes = ReleaseNotes.forVersion(APP_VERSION, lang())
        if (notes.isEmpty()) LaunchedEffect(Unit) { seen(); notesClosed = true } else AlertDialog(
            onDismissRequest = { seen(); notesClosed = true },
            title = { Text(Str.WhatsNewIn.t(APP_VERSION)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { notes.forEach { Text("•  ${num(it)}", style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = { TextButton(onClick = { seen(); notesClosed = true }) { Text(Str.Close.t()) } },
        )
    }
}

private data class TourPage(val title: Str, val body: Str)

private val pages = listOf(
    TourPage(Str.TourWelcome, Str.TourWelcomeBody),
    TourPage(Str.TourFilesTitle, Str.TourFilesBody),
    TourPage(Str.TourDataTitle, Str.TourDataBody),
    TourPage(Str.TourLookTitle, Str.TourLookBody),
    TourPage(Str.TourReadyTitle, Str.TourReadyBody),
)

@Composable
private fun Tour(store: NamaaStore, onClose: () -> Unit) {
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(Str.Skip.t()) } }
            HorizontalPager(state, Modifier.weight(1f).fillMaxWidth()) { i ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(pages[i].title.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Text(pages[i].body.t(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (i == 2) store.askAccess?.takeIf { !store.location.shared }?.let { ask ->
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = ask) { Text(Str.UseFolderBtn.t()) }
                    }
                    if (i == 3) {
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.take(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.drop(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to Str.ThemeSystem, "light" to Str.ThemeLight, "dark" to Str.ThemeDark).forEach { (key, label) -> FilterChip(store.prefs.theme == key, { store.updatePrefs { it.copy(theme = key) } }, { Text(label.t()) }) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column { LanguageChips(store) }
                        Row {
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i -> Box(Modifier.size(8.dp).background(if (i == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
                }
                val last = state.currentPage == pages.lastIndex
                Button(onClick = { if (last) onClose() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } }) { Text(if (last) Str.Done.t() else Str.Next.t()) }
            }
        }
    }
}
