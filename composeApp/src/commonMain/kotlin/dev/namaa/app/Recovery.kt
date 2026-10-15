package dev.namaa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.namaa.core.Str

/** Shown instead of the app while a data file cannot be read, so nothing is written over it. */
@Composable
fun RecoveryScreen(store: NamaaStore) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Str.DataProblemTitle.t(), style = MaterialTheme.typography.headlineSmall)
        store.blocked.forEach { Panel { Text(Str.DataProblemFile.t(it.file, it.copy)) } }
        Text(Str.DataProblemHelp.t(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { store.reload() }) { Text(Str.CheckAgain.t()) }
            OutlinedButton(onClick = { store.startFresh() }) { Text(Str.StartWithout.t()) }
        }
    }
}

/** A note on Home when some rows of a year file could not be read. */
@Composable
fun RowProblemsCard(store: NamaaStore) {
    Panel {
        store.rowProblems.forEach { Text(Str.RowsProblem.t(it.file, it.copy), color = MaterialTheme.colorScheme.error) }
        if (store.sideCopies.isNotEmpty()) Text(Str.SideCopies.t(store.sideCopies.joinToString()), color = MaterialTheme.colorScheme.error)
    }
}
