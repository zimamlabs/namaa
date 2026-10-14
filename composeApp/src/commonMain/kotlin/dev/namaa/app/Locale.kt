package dev.namaa.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.namaa.core.I18n
import dev.namaa.core.Lang
import dev.namaa.core.Str
import dev.namaa.core.categoryTitle
import dev.namaa.core.formatMoney

/** The language and digits the screens use right now. */
class AppLocale(val lang: Lang, val arabicDigits: Boolean)

val LocalAppLocale = staticCompositionLocalOf { AppLocale(Lang.En, false) }

/** The device's language code, for the "System" language choice. */
expect fun systemLanguage(): String

@Composable fun lang(): Lang = LocalAppLocale.current.lang

/** The text in the current language, with the digits in the chosen style. */
@Composable fun Str.t(vararg args: Any?): String {
    val l = LocalAppLocale.current
    return I18n.digits(text(l.lang, *args), l.arabicDigits)
}

/** Same, but digits are left as they are: for text that holds a file path. */
@Composable fun Str.raw(vararg args: Any?): String = text(LocalAppLocale.current.lang, *args)

/** A number or time shown with the chosen digits. */
@Composable fun num(value: Any?): String = I18n.digits(value.toString(), LocalAppLocale.current.arabicDigits)

/** A category as shown (built-in names translated). */
@Composable fun categoryShown(category: String): String = categoryTitle(category, lang())
/** An amount with the chosen digits. */
@Composable fun money(minor: Long): String = num(formatMoney(minor))
