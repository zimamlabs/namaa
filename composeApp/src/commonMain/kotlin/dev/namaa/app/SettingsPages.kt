package dev.namaa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.namaa.core.FolderNote
import dev.namaa.core.Palettes
import dev.namaa.core.Sections
import dev.namaa.core.Settings
import dev.namaa.core.Str
import okio.Path

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onChange, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearancePage(store: NamaaStore) {
    val st = store.settings
    Label(Str.SetMode.t())
    val modes = listOf("system", "light", "dark")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { i, m -> SegmentedButton(st.themeMode == m, { store.updateSettings { it.copy(themeMode = m) } }, SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(modeName(m)) } }
    }
    Label(Str.SetColourTheme.t())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Palettes.ids.forEach { id ->
            val color = accents.getValue(id).let { if (MaterialTheme.colorScheme.background.luminance() < 0.5f) it.dark else it.light }
            val name = paletteName(id)
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.RadioButton) { store.updateSettings { it.copy(palette = id) } }
                    .border(if (st.palette == id) 3.dp else 1.dp, if (st.palette == id) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .padding(6.dp).clip(CircleShape).background(color).semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) { if (st.palette == id) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.background) }
        }
    }
    Text(paletteName(st.palette), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Label(Str.SetTextSize.t())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("A", style = MaterialTheme.typography.bodySmall)
        Slider(st.textSize.toFloat(), { store.updateSettings { s -> s.copy(textSize = it.toInt()) } }, Modifier.weight(1f), valueRange = 0f..3f, steps = 2)
        Text("A", style = MaterialTheme.typography.headlineSmall)
    }
    Panel { Text(textSizeName(st.textSize), style = MaterialTheme.typography.titleSmall); Text(Str.TextPreview.t(), style = MaterialTheme.typography.bodyLarge) }
    Label(Str.DensityLabel.t())
    val densities = listOf("comfortable" to Str.DensityComfortable, "compact" to Str.DensityCompact)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        densities.forEachIndexed { i, (k, label) -> SegmentedButton(st.density == k, { store.updateSettings { it.copy(density = k) } }, SegmentedButtonDefaults.itemShape(i, densities.size)) { Text(label.t()) } }
    }
    SwitchRow(Str.ReduceAnimations.t(), st.reduceMotion) { v -> store.updateSettings { it.copy(reduceMotion = v) } }
}

@Composable
internal fun SectionsPage(store: NamaaStore) {
    val st = store.settings
    val order = st.sectionOrder.filter { it in Sections.all }.let { it + (Sections.all - it.toSet()) }
    Text(Str.ShownInMenuAndBar.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    order.forEachIndexed { i, id ->
        val tab = sectionTab(id)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(tab.icon, null, Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(tab.label.t(), style = MaterialTheme.typography.bodyLarge)
                if (id == Sections.HOME) Text(Str.HomeAlwaysShown.t(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(enabled = i > 0, onClick = { store.updateSettings { s -> s.copy(sectionOrder = order.toMutableList().also { it.add(i - 1, it.removeAt(i)) }) } }) { Icon(Icons.Rounded.ArrowUpward, Str.MoveUp.t()) }
            IconButton(enabled = i < order.lastIndex, onClick = { store.updateSettings { s -> s.copy(sectionOrder = order.toMutableList().also { it.add(i + 1, it.removeAt(i)) }) } }) { Icon(Icons.Rounded.ArrowDownward, Str.MoveDown.t()) }
            Switch(id == Sections.HOME || id !in st.hiddenSections, { on -> store.updateSettings { s -> s.copy(hiddenSections = if (on) s.hiddenSections - id else s.hiddenSections + id) } }, enabled = id != Sections.HOME)
        }
    }
    Label(Str.StartOn.t())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        visibleTabs(st).forEach { t -> FilterChip(st.startSection == t.id, { store.updateSettings { it.copy(startSection = t.id) } }, { Text(t.label.t()) }) }
    }
}

@Composable
internal fun LanguagePage(store: NamaaStore) {
    LanguageChips(store)
}

@Composable
internal fun CurrencyPage(store: NamaaStore) {
    Label(Str.DefaultCurrency.t())
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.take(3).forEach { c -> FilterChip(store.settings.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { currencies.drop(3).forEach { c -> FilterChip(store.settings.currency == c, { store.setCurrency(c) }, { Text(c) }) } }
    Text(Str.CategoriesNote.t(defaultCategories.map { categoryShown(it) }.joinToString(", ")), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun DataPage(store: NamaaStore, onTour: () -> Unit) {
    val loc = store.location
    var confirm by remember { mutableStateOf<Path?>(null) }
    var clearing by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val language = lang()
    Panel {
        Text(Str.ZimamFolderTitle.t(), style = MaterialTheme.typography.titleSmall)
        Text(
            if (loc.shared) Str.DataShared.raw(loc.root) else Str.DataPrivate.t() + if (store.askAccess != null) " " + Str.DataPrivateAsk.t() else "",
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
        OutlinedButton(onClick = { typed = ""; clearing = true }) { Text(Str.ClearAllBtn.t(), color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
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
    if (clearing) {
        val word = Str.ClearAllWord.t()
        AlertDialog(
            onDismissRequest = { clearing = false },
            title = { Text(Str.ClearAllTitle.t()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Str.ClearAllBody.raw(store.location.root, "cleared-…", word))
                    OutlinedTextField(typed, { typed = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = typed.trim().equals(word, ignoreCase = true), onClick = { val dir = store.clearAll(); clearing = false; message = Str.ClearAllDone.text(language, dir) }) { Text(Str.Delete.t(), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text(Str.KeepIt.t()) } },
        )
    }
}

@Composable
internal fun PrivacyPage(store: NamaaStore) {
    val lock = store.lock
    Panel { Text(Str.PrivacyBody.raw(store.location.root), style = MaterialTheme.typography.bodyLarge) }
    Panel {
        SwitchRow(Str.AppLockTitle.t(), store.settings.appLock, enabled = lock?.available == true) { on ->
            if (on) lock?.unlock { ok -> if (ok) store.updateSettings { it.copy(appLock = true) } } else store.updateSettings { it.copy(appLock = false) }
        }
        Text(if (lock?.available == true) Str.AppLockBody.t() else Str.AppLockUnavailable.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
