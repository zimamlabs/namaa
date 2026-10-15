package dev.namaa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.namaa.core.Backup
import dev.namaa.core.BackupException
import dev.namaa.core.MoneyBook
import dev.namaa.core.Str
import okio.ByteString.Companion.toByteString

/** Back up everything / restore: the supported way to move data between phones or survive an uninstall. */
@Composable
fun BackupPanel(store: NamaaStore) {
    val actions = store.files ?: return
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<List<Backup.Entry>?>(null) }
    val okSaved = Str.BackupSaved.t(); val failed = Str.BackupFailed.t(); val restored = Str.RestoreDone.t(); val language = lang()
    Panel {
        Text(Str.BackupTitle.t(), style = MaterialTheme.typography.titleSmall)
        Text(Str.BackupBody.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { store.backupEverything { ok -> message = if (ok) okSaved else failed } }) { Text(Str.BackupNow.t()) }
            OutlinedButton(onClick = {
                actions.pick { bytes ->
                    message = null
                    if (bytes != null) runCatching { Backup.read(bytes.toByteString()) }.onSuccess { pending = it }.onFailure { message = Str.RestoreRefused.text(language, it.message ?: "") }
                }
            }) { Text(Str.RestoreBtn.t()) }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
    pending?.let { entries ->
        val preview = remember(entries) { runCatching { store.book.previewRestore(entries) } }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(Str.RestoreQ.t()) },
            text = {
                preview.fold(
                    { r -> Text(Str.RestoreBody.t(r.transactions, r.accounts, r.budgets + r.recurring + r.debts)) },
                    { Text(Str.RestoreRefused.t(it.message ?: ""), color = MaterialTheme.colorScheme.error) },
                )
            },
            confirmButton = {
                TextButton(enabled = preview.isSuccess, onClick = {
                    pending = null
                    message = runCatching { store.restore(entries) }.fold({ restored }, { Str.RestoreRefused.text(language, it.message ?: "") })
                }) { Text(Str.RestoreGo.t()) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(Str.Cancel.t()) } },
        )
    }
}
