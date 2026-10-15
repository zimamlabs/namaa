package dev.namaa.core

/** What to show when Namaa opens: the first-install tour, the one-time notes after an update, or nothing. */
enum class Welcome { Tour, WhatsNew, Nothing }

object Onboarding {
    /** `0.12.0` > `0.11.2`; missing or non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /**
     * [lastSeen] is the version whose notes were closed last (empty when never), [tourDone] whether the tour was
     * finished or skipped, [hasData] whether Namaa already holds anything (an update from a version that did not
     * store these values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version, taken from the changelog; shown once after an update. Each note is English then Arabic. */
object ReleaseNotes {
    private val notes: Map<String, List<Pair<String, String>>> = mapOf(
        "0.5.1" to listOf("A new app icon: a sprout growing from the ground." to "أيقونة جديدة للتطبيق: برعم ينمو من التربة."),
        "0.5.0" to listOf(
            "An Arabic interface with a right-to-left layout. Choose the language and the digits in Settings." to "واجهة عربية بتخطيط من اليمين إلى اليسار. اختر اللغة والأرقام من الإعدادات.",
        ),
        "0.4.0" to listOf(
            "Zimma is now Namaa (نماء, growth), with a new icon. Your data is copied into the Zimam/Namaa folder; the old folders stay as a backup." to "صار «ذمّة» الآن «نماء» بأيقونة جديدة. تُنسخ بياناتك إلى مجلد Zimam/Namaa وتبقى المجلدات القديمة كنسخة احتياطية.",
        ),
        "0.3.0" to listOf(
            "Budgets per category with progress bars and a warning when you go over." to "ميزانيات لكل فئة بأشرطة تقدّم وتحذير عند التجاوز.",
            "Repeating transactions, search and filters, CSV export of a month or a year, and who owes whom." to "معاملات متكررة، وبحث وعوامل تصفية، وتصدير CSV لشهر أو سنة، ومن يدين لمن.",
        ),
        "0.2.0" to listOf(
            "A Home page with this month at a glance, recent transactions and shortcuts." to "صفحة رئيسية تعرض شهرك بنظرة واحدة، وأحدث المعاملات، واختصارات.",
            "Your data now lives in one Zimam folder (Zimam/Zimma). Old folders are copied and checked, never deleted." to "صارت بياناتك في مجلد زمام واحد (Zimam/Zimma). تُنسخ المجلدات القديمة وتُفحص ولا تُحذف أبدًا.",
            "Pick a date from a calendar, a first-run tour, and these notes after each update." to "اختيار التاريخ من تقويم، وجولة تعريفية عند أول تشغيل، وهذه الملاحظات بعد كل تحديث.",
        ),
        "0.1.2" to listOf("A better opening screen." to "شاشة افتتاحية أفضل."),
        "0.1.1" to listOf("A new wallet icon." to "أيقونة محفظة جديدة."),
        "0.1.0" to listOf("Accounts, income and spending, and monthly totals in plain files." to "حسابات ودخل وإنفاق ومجاميع شهرية في ملفات بسيطة."),
    )

    /** The notes for [version], or for the newest older version that has some, in [lang]. */
    fun forVersion(version: String, lang: Lang = Lang.En): List<String> =
        (notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty())
            .map { if (lang == Lang.Ar) it.second else it.first }
}
