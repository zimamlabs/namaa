package dev.zimma.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** The card used everywhere: large rounded corners, a hairline border, and the small orange diamond of the mark in the corner. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            Canvas(Modifier.align(Alignment.TopEnd).padding(12.dp).size(12.dp)) { zimamDot(center, size.minDimension, accent.copy(alpha = 0.9f)) }
        }
    }
}

/** A soft orange glow, drawn behind a hero area. */
fun Modifier.heroGlow(): Modifier = this.drawBehind {
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF5A1F).copy(alpha = 0.28f), Color.Transparent), center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.75f))
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF5A1F).copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * 0.1f, size.height), radius = size.width * 0.6f))
}

@Composable
fun EmptyHint(title: String, hint: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun prettyDate(d: LocalDate): String = "${dayName(d.dayOfWeek)} ${d.dayOfMonth} ${monthName(d.monthNumber)}"

fun dayName(d: DayOfWeek): String = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[d.ordinal]

fun monthName(n: Int): String = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[(n - 1).coerceIn(0, 11)]
