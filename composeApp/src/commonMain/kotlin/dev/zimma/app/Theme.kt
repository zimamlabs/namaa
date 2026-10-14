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

private val Light = lightColorScheme(
    primary = Color(0xFFE5481A), onPrimary = Color.White, primaryContainer = Color(0xFFFFDDD0), onPrimaryContainer = Color(0xFF3A0E00),
    secondary = Color(0xFF6B5B4B), background = Color(0xFFF5F1E8), surface = Color(0xFFF5F1E8), surfaceContainer = Color(0xFFFFFBF3),
    surfaceContainerHigh = Color(0xFFEDE6D8), onSurface = Color(0xFF121214), onSurfaceVariant = Color(0xFF5E5A52), outlineVariant = Color(0xFFD8D0C0),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFF5A1F), onPrimary = Color(0xFF1A0A02), primaryContainer = Color(0xFF4A1D0B), onPrimaryContainer = Color(0xFFFFDDD0),
    secondary = Color(0xFFD9CDB8), background = Color(0xFF121214), surface = Color(0xFF121214), surfaceContainer = Color(0xFF1B1B1E),
    surfaceContainerHigh = Color(0xFF26262A), onSurface = Color(0xFFF5F1E8), onSurfaceVariant = Color(0xFFA9A59B), outlineVariant = Color(0xFF3A3A40),
)

@Composable
fun ZimmaTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
