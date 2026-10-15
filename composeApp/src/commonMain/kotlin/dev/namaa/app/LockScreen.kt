package dev.namaa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.namaa.core.Str

/** Shown instead of the app while the app lock is on and the phone's lock screen has not been passed. */
@Composable
fun LockScreen(store: NamaaStore) {
    fun ask() { store.lock?.unlock { ok -> if (ok) store.locked = false } }
    LaunchedEffect(Unit) { ask() }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark(Modifier.size(96.dp))
        Text(Str.AppName.t(), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { ask() }) { Text(Str.AppLockRetry.t()) }
    }
}
