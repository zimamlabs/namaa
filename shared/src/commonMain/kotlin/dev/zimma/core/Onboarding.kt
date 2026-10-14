package dev.zimma.core

/** What to show when Zimma opens: the first-install tour, the one-time notes after an update, or nothing. */
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
     * finished or skipped, [hasData] whether Zimma already holds anything (an update from a version that did not
     * store these values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version, taken from the changelog; shown once after an update. */
object ReleaseNotes {
    private val notes: Map<String, List<String>> = mapOf(
        "0.3.0" to listOf(
            "Budgets per category with progress bars and a warning when you go over.",
            "Repeating transactions, search and filters, CSV export of a month or a year, and who owes whom.",
        ),
        "0.2.0" to listOf(
            "A Home page with this month at a glance, recent transactions and shortcuts.",
            "Your data now lives in one Zimam folder (Zimam/Zimma). Old folders are copied and checked, never deleted.",
            "Pick a date from a calendar, a first-run tour, and these notes after each update.",
        ),
        "0.1.2" to listOf("A better opening screen."),
        "0.1.1" to listOf("A new wallet icon."),
        "0.1.0" to listOf("Accounts, income and spending, and monthly totals in plain files."),
    )

    /** The notes for [version], or for the newest older version that has some. */
    fun forVersion(version: String): List<String> =
        notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty()
}
