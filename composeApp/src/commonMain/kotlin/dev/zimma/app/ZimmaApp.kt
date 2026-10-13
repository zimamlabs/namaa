package dev.zimma.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

const val APP_VERSION = "0.1.0"

@Composable
fun ZimmaApp(store: ZimmaStore) {
    ZimmaTheme {
        var splash by remember { mutableStateOf(true) }
        var about by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (about) AboutScreen { about = false } else MoneyScreen(store) { about = true }
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
        }
    }
}
