package dev.namaa.app

import dev.namaa.core.FolderNote
import dev.namaa.core.Str
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okio.Path

val currencies = listOf("USD", "EUR", "GBP", "EGP", "SAR", "AED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: NamaaStore, onBack: () -> Unit, onAbout: () -> Unit, onTour: () -> Unit) {
    val loc = store.location
    var confirm by remember { mutableStateOf<Path?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(Str.SettingsTitle.t()) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, Str.Back.t()) } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Text(Str.Look.t(), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to Str.ThemeSystem, "light" to Str.ThemeLight, "dark" to Str.ThemeDark).forEach { (key, label) ->
                        FilterChip(store.prefs.theme == key, { store.updatePrefs { it.copy(theme = key) } }, { Text(label.t()) })
                    }
                }
                Text(Str.LanguageTitle.t(), style = MaterialTheme.typography.titleSmall)
                LanguageChips(store)
                Text(Str.DefaultCurrency.t(), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.take(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.drop(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
            }
            Panel {
                Text(Str.ZimamFolderTitle.t(), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (loc.shared) Str.DataShared.raw(loc.root)
                    else Str.DataPrivate.t() + if (store.askAccess != null) " " + Str.DataPrivateAsk.t() else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!loc.shared) store.askAccess?.let { ask -> Button(onClick = ask) { Text(Str.UseFolderBtn.t()) } }
                loc.message?.let { note -> Text(if (note.kind == FolderNote.Kind.Unreachable) Str.FolderUnreachable.raw(note.a) else Str.FolderVerifyFailed.raw(note.a, note.b), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                // The old folders stay as a backup after the copy; removing one asks first and checks every file again.
                val removable = remember(store.folderTick, loc.root) { loc.removableOldFolders() }
                removable.forEach { old ->
                    Text(Str.EarlierFolder.raw(old), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { confirm = old }) { Text(Str.RemoveOldFolder.t()) }
                }
            }
            BackupPanel(store)
            Panel {
                OutlinedButton(onClick = onTour) { Text(Str.ShowTour.t()) }
                OutlinedButton(onClick = onAbout) { Text(Str.AboutNamaa.t()) }
            }
        }
    }
    confirm?.let { old ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(Str.RemoveOldQ.t()) },
            text = { Text(Str.RemoveOldQBody.raw(old)) },
            confirmButton = { TextButton(onClick = { loc.removeOldFolder(old); store.useBook(store.book); confirm = null }) { Text(Str.CheckAndRemove.t()) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(Str.KeepIt.t()) } },
        )
    }
}

/** Language (System, English, العربية) and the digit style; both apply at once. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LanguageChips(store: NamaaStore) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("system" to Str.ThemeSystem.t(), "en" to Str.LangEnglish.t(), "ar" to Str.LangArabic.t()).forEach { (k, v) ->
            FilterChip(store.prefs.lang == k, { store.updatePrefs { it.copy(lang = k) } }, { Text(v) })
        }
    }
    Text(Str.DigitsTitle.t(), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!store.prefs.arabicDigits, { store.updatePrefs { it.copy(arabicDigits = false) } }, { Text(Str.DigitsWestern.text(dev.namaa.core.Lang.En)) })
        FilterChip(store.prefs.arabicDigits, { store.updatePrefs { it.copy(arabicDigits = true) } }, { Text(Str.DigitsArabic.text(dev.namaa.core.Lang.En)) })
    }
}
