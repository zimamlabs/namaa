package dev.namaa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParseMoneyTest {
    private fun ok(text: String, minor: Long) = assertEquals(minor, parseMoney(text), "'$text'")
    private fun bad(text: String, why: MoneyProblem) {
        assertEquals(null, parseMoney(text), "'$text' must be rejected")
        assertEquals(why, assertIs<MoneyParse.Bad>(parseMoneyResult(text), "'$text'").problem, "'$text'")
    }

    @Test fun plainAndDecimalAmounts() {
        ok("0", 0); ok("5", 500); ok("1234", 123400); ok("12.5", 1250); ok("12.50", 1250); ok("12,5", 1250); ok("12,50", 1250)
        ok("0.07", 7); ok(".5", 50); ok("5.", 500); ok("  42  ", 4200); ok("+3", 300)
    }

    @Test fun negativeAmounts() { ok("-3", -300); ok("-0.5", -50); ok("−3", -300); ok("-0", 0) }

    @Test fun groupingIsAcceptedOnlyWhenItCannotBeADecimal() {
        ok("1,234,567", 123456700); ok("1.234.567", 123456700); ok("1,234.50", 123450); ok("1.234,50", 123450); ok("12,345.6", 1234560); ok("1,234,567.89", 123456789)
    }

    @Test fun oneSeparatorThenThreeDigitsIsAmbiguousAndRejected() {
        bad("1,234", MoneyProblem.Ambiguous); bad("1.234", MoneyProblem.Ambiguous); bad("1,500", MoneyProblem.Ambiguous); bad("0,500", MoneyProblem.Ambiguous)
    }

    @Test fun moreThanTwoDecimalsIsRejectedNotTruncated() {
        bad("12.999", MoneyProblem.Ambiguous); bad("1.2345", MoneyProblem.TooManyDecimals); bad("1,234.567", MoneyProblem.TooManyDecimals); bad("0.001", MoneyProblem.Ambiguous)
    }

    @Test fun brokenGroupingIsRejected() {
        bad("1,23,456", MoneyProblem.Ambiguous); bad("1.2.3", MoneyProblem.Ambiguous); bad("1,234.567,89", MoneyProblem.Ambiguous); bad("1.234,567.8", MoneyProblem.Ambiguous); bad("1,,2", MoneyProblem.Ambiguous)
        bad("1 234", MoneyProblem.NotANumber)
    }

    @Test fun notNumbers() {
        bad("", MoneyProblem.Empty); bad("   ", MoneyProblem.Empty); bad("abc", MoneyProblem.NotANumber); bad("-", MoneyProblem.NotANumber); bad(".", MoneyProblem.NotANumber)
        bad("12a", MoneyProblem.NotANumber); bad("--3", MoneyProblem.NotANumber); bad("1e5", MoneyProblem.NotANumber)
    }

    @Test fun arabicIndicDigitsAndSeparators() {
        ok("١٢٫٥", 1250); ok("١٢٫٥٠", 1250); ok("-٣", -300); ok("١٬٢٣٤٫٥٠", 123450); ok("١٢،٥", 1250)
        bad("١٬٢٣٤", MoneyProblem.Ambiguous); bad("١٢٫٩٩٩", MoneyProblem.Ambiguous); bad("١٢أ", MoneyProblem.NotANumber)
        ok("۱۲٫۵", 1250)
    }

    @Test fun hugeAmountsAreRejectedNotWrapped() {
        bad("92233720368547759", MoneyProblem.TooLarge); bad("99999999999999999999", MoneyProblem.TooLarge)
        ok("92233720368547758.07", 9223372036854775807L)
    }

    @Test fun whatTheEditFieldShowsParsesBack() {
        for (m in listOf(0L, 7L, 100L, 123450L, -5L, 99999999L)) assertEquals(m, parseMoney(formatMoney(m)), "$m")
    }
}
