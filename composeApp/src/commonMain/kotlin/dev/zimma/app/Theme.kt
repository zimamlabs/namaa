package dev.zimma.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Warm near-black and ivory with the single Zimam orange, to match the icon.
private val Light = lightColorScheme(
    primary = Color(0xFFE5481A), onPrimary = Color.White, primaryContainer = Color(0xFFFFE3D6), onPrimaryContainer = Color(0xFF3A0E00),
    secondary = Color(0xFF2F7D6F), tertiary = Color(0xFFB7791F), background = Color(0xFFFFF8F0), surface = Color(0xFFFFF8F0), surfaceContainer = Color(0xFFFFFDF9),
    surfaceContainerHigh = Color(0xFFF3E9DD), onSurface = Color(0xFF1A1613), onSurfaceVariant = Color(0xFF6B6259), outlineVariant = Color(0x1F1A1613),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFF5A1F), onPrimary = Color(0xFF1A0A02), primaryContainer = Color(0xFF4A1D0B), onPrimaryContainer = Color(0xFFFFDDD0),
    secondary = Color(0xFF5CC2B0), tertiary = Color(0xFFE8A317), background = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F), surfaceContainer = Color(0xFF1A1613),
    surfaceContainerHigh = Color(0xFF2A231E), onSurface = Color(0xFFF5EFE6), onSurfaceVariant = Color(0xFFB5ABA0), outlineVariant = Color(0x14FFFFFF),
)

@Composable
fun ZimmaTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
