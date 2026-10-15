package dev.namaa.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.namaa.core.Palettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every colour theme must stay readable in light and dark (WCAG contrast ratios). */
class ThemeTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble(); val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test fun everyPaletteIdHasAnAccent() = assertEquals(Palettes.ids.toSet(), accents.keys)

    @Test fun contrastHoldsForEveryPaletteInLightAndDark() {
        for (palette in Palettes.ids) for (dark in listOf(false, true)) {
            val c = colorSchemeFor(palette, dark); val tag = "$palette dark=$dark"
            assertTrue(ratio(c.onSurface, c.background) >= 7.0, "text on background, $tag")
            assertTrue(ratio(c.onSurfaceVariant, c.background) >= 4.5, "secondary text on background, $tag")
            assertTrue(ratio(c.onSurfaceVariant, c.surfaceContainer) >= 4.5, "secondary text on a card, $tag")
            assertTrue(ratio(c.primary, c.background) >= 3.0, "accent on background, $tag")
            assertTrue(ratio(c.primary, c.surfaceContainer) >= 3.0, "accent on a card, $tag")
            assertTrue(ratio(c.onPrimary, c.primary) >= 3.9, "label on a filled accent, $tag")
            assertTrue(ratio(c.onPrimaryContainer, c.primaryContainer) >= 7.0, "text on the accent container, $tag")
        }
    }

    @Test fun textScalesGrowAndNormalIsOne() {
        assertEquals(1f, textScales[1])
        assertTrue(textScales.toList().zipWithNext().all { (a, b) -> a < b })
    }
}
