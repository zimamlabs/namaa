package dev.namaa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmountDisplayTest {
    private val lri = "⁦"; private val pdi = "⁩"

    @Test fun englishIsUnchanged() {
        assertEquals("-12.50", formatAmountDisplay(-1250, arabicDigits = false, rtl = false))
        assertEquals("1234.05", formatAmountDisplay(123405, false, false))
    }

    @Test fun arabicLayoutWithLatinDigitsGetsATrueMinusAndAnIsolate() {
        assertEquals("$lri−12.50$pdi", formatAmountDisplay(-1250, false, true))
        assertEquals("${lri}12.50$pdi", formatAmountDisplay(1250, false, true))
    }

    @Test fun arabicIndicDigitsUseTheArabicDecimalMark() {
        assertEquals("$lri−١٢٫٥٠$pdi", formatAmountDisplay(-1250, true, true))
        assertEquals("١٢٫٥٠", formatAmountDisplay(1250, true, false))
    }

    @Test fun whatIsShownCanBeTypedBackIn() {
        for (m in listOf(0L, 5L, -5L, 1250L, -123456789L)) for (digits in listOf(false, true)) for (rtl in listOf(false, true))
            assertEquals(m, parseMoney(formatAmountDisplay(m, digits, rtl).replace(lri, "").replace(pdi, "")), "$m $digits $rtl")
    }

    @Test fun percentagesAlwaysAddUpToOneHundred() {
        for (values in listOf(listOf(1L, 1L, 1L), listOf(50L, 30L, 20L), listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L), listOf(333L, 333L, 334L), listOf(5L))) {
            val p = percentShares(values); assertEquals(100, p.sum(), "$values -> $p"); assertEquals(values.size, p.size)
        }
        assertEquals(listOf(34, 33, 33), percentShares(listOf(1L, 1L, 1L)))
        assertEquals(emptyList(), percentShares(emptyList()))
        assertEquals(listOf(0, 0), percentShares(listOf(0L, 0L)))
        assertTrue(percentShares(listOf(1L, 1_000_000L)).all { it >= 0 })
    }
}
