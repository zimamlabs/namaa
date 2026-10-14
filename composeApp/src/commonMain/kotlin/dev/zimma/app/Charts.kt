package dev.zimma.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.zimma.core.MonthTotal
import dev.zimma.core.formatMoney

/** A palette that reads in light and dark, starting from the Zimam orange. */
val chartColors = listOf(Color(0xFFFF5A1F), Color(0xFF2E9E8F), Color(0xFFE8A317), Color(0xFF6C7AE0), Color(0xFFD1507A), Color(0xFF8A9A5B), Color(0xFF9B9B9B))

/** Spending by category as a ring; the biggest slices get their own colour and the rest share the last one. */
@Composable
fun DonutChart(slices: List<Pair<String, Long>>, centreTop: String, centreBottom: String, modifier: Modifier = Modifier) {
    val shown = slices.take(chartColors.size - 1) + if (slices.size > chartColors.size - 1) listOf("Other" to slices.drop(chartColors.size - 1).sumOf { it.second }) else emptyList()
    val total = shown.sumOf { it.second }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(132.dp)) {
                val stroke = size.width * 0.16f
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                var start = -90f
                shown.forEachIndexed { i, (_, v) ->
                    val sweep = 360f * v / total
                    // A small gap between slices; skipped when there is only one.
                    val gap = if (shown.size > 1) 2f else 0f
                    drawArc(chartColors[i % chartColors.size], start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centreTop, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(centreBottom, style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEachIndexed { i, (name, v) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).also { }) { Canvas(Modifier.size(10.dp)) { drawCircle(chartColors[i % chartColors.size]) } }
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text("${(100L * v / total)}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Income and spending side by side for each month, scaled to the biggest value shown. */
@Composable
fun MonthBars(months: List<MonthTotal>, modifier: Modifier = Modifier) {
    val top = months.maxOfOrNull { maxOf(it.incomeMinor, it.spentMinor) }?.coerceAtLeast(1) ?: 1
    val income = MaterialTheme.colorScheme.secondary
    val spent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), 2f)
            val slot = size.width / months.size
            val bar = slot * 0.26f
            months.forEachIndexed { i, m ->
                val x = slot * i + slot / 2
                val hi = size.height * 0.94f * m.incomeMinor / top
                val hs = size.height * 0.94f * m.spentMinor / top
                if (hi > 0) drawRoundRect(income, Offset(x - bar - 2f, size.height - hi), Size(bar, hi), CornerRadius(6f))
                if (hs > 0) drawRoundRect(spent, Offset(x + 2f, size.height - hs), Size(bar, hs), CornerRadius(6f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            months.forEach { Text(monthName(it.month), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(income, "Income"); Legend(spent, "Spent")
            Spacer1()
            Text("Top: " + formatMoney(top), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun Spacer1() { Box(Modifier.width(4.dp)) }

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
