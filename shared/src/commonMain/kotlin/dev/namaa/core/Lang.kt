package dev.namaa.core

/** The language the screens are shown in. */
enum class Lang { En, Ar }

object I18n {
    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

    /** [choice] is the saved setting ("en", "ar", anything else means follow the system); [systemLanguage] is the device's language code. */
    fun resolve(choice: String?, systemLanguage: String): Lang = when (choice) {
        "en" -> Lang.En
        "ar" -> Lang.Ar
        else -> if (systemLanguage.lowercase().startsWith("ar")) Lang.Ar else Lang.En
    }

    /** Western digits become Arabic-Indic digits when [arabic] is true; everything else is left alone. */
    fun digits(text: String, arabic: Boolean): String =
        if (!arabic) text else buildString(text.length) { for (c in text) append(if (c in '0'..'9') ARABIC_INDIC[c - '0'] else c) }

    /** Replaces {0}, {1}, ... with [args]. */
    fun fill(template: String, args: Array<out Any?>): String {
        var out = template
        args.forEachIndexed { i, a -> out = out.replace("{$i}", a.toString()) }
        return out
    }
}

val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
val monthsShortEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
val monthsAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
val weekdaysShortEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val weekdaysAr = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

fun monthName(month: Int, lang: Lang): String = (if (lang == Lang.Ar) monthsAr else monthsShortEn)[(month - 1).coerceIn(0, 11)]
fun dayName(dayOfWeekOrdinal: Int, lang: Lang): String = (if (lang == Lang.Ar) weekdaysAr else weekdaysShortEn)[dayOfWeekOrdinal]
