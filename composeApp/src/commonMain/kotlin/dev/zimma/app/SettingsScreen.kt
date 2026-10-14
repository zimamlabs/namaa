package dev.zimma.app

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
fun SettingsScreen(store: ZimmaStore, onBack: () -> Unit, onAbout: () -> Unit, onTour: () -> Unit) {
    val loc = store.location
    var confirm by remember { mutableStateOf<Path?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Text("Look", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                        FilterChip(store.prefs.theme == key, { store.updatePrefs { it.copy(theme = key) } }, { Text(label) })
                    }
                }
                Text("Default currency", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.take(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.drop(3).forEach { c -> FilterChip(store.prefs.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
            }
            Panel {
                Text("Zimam folder", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (loc.shared) "Zimma keeps your accounts and transactions in ${loc.root}. They survive reinstalling the app."
                    else "Zimma keeps its data inside the app. " + if (store.askAccess != null) "Allow access to all files to use a visible Zimam/Zimma folder instead, so your data survives reinstalling the app." else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!loc.shared) store.askAccess?.let { ask -> Button(onClick = ask) { Text("Use the Zimam/Zimma folder") } }
                loc.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                // The old folders stay as a backup after the copy; removing one asks first and checks every file again.
                val removable = remember(store.folderTick, loc.root) { loc.removableOldFolders() }
                removable.forEach { old ->
                    Text("Your earlier folder $old was copied into the Zimam folder and checked. It is still there as a backup.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { confirm = old }) { Text("Remove the old folder") }
                }
            }
            Panel {
                OutlinedButton(onClick = onTour) { Text("Show the tour again") }
                OutlinedButton(onClick = onAbout) { Text("About Zimma") }
            }
        }
    }
    confirm?.let { old ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Remove the old folder?") },
            text = { Text("Zimma checks every file of $old once more and removes it only if each one is in the Zimam folder. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { loc.removeOldFolder(old); store.useBook(store.book); confirm = null }) { Text("Check and remove") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep it") } },
        )
    }
}
