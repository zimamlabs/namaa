package dev.namaa.app

import androidx.compose.runtime.Composable

/** The system back gesture or button, on the platforms that have one. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
