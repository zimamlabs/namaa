package dev.namaa.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/** The colour themes the user can pick; the colours themselves are defined once in the app's theme. */
object Palettes {
    const val DEFAULT = "zimam"
    val ids = listOf(DEFAULT, "ocean", "forest", "violet", "rose", "graphite")
}

/** The cards under the hero on Home. Home stays minimal: at most [MAX] show, in the user's order. */
object HomeCards {
    const val MAX = 4
    val all = listOf("budgets", "spending", "recent", "accounts", "trend")
    val defaultHidden = setOf("trend")

    /** The cards to show: the user's [order] first (unknown names dropped), then any missing ones, minus [hidden], at most [MAX]. */
    fun visible(order: List<String>, hidden: Set<String>): List<String> =
        (order.filter { it in all }.distinct().let { it + (all - it.toSet()) }).filter { it !in hidden }.take(MAX)
}

/** The main sections of the app, in their default order. */
object Sections {
    const val HOME = "home"
    val all = listOf(HOME, "money", "accounts", "plan", "report")

    /** The sections to show: the user's [order] first (unknown names dropped), then any missing ones, minus [hidden]. Home is always kept. */
    fun visible(order: List<String>, hidden: Set<String>): List<String> {
        val ordered = order.filter { it in all }.distinct().let { it + (all - it.toSet()) }
        return ordered.filter { it == HOME || it !in hidden }
    }
}

/**
 * Every setting of the app in one typed object, kept in this install's `settings`-style file (not in the data folder, so it
 * stays put when the folder moves). Older files that hold only some of these keys still load; missing keys take the defaults.
 */
@Serializable
data class Settings(
    val lastSeenVersion: String = "",
    val tourDone: Boolean = false,
    @SerialName("theme") val themeMode: String = "system",
    val currency: String = "USD",
    val lang: String = "system",
    val arabicDigits: Boolean = false,
    val palette: String = Palettes.DEFAULT,
    /** 0 small, 1 normal, 2 large, 3 largest. */
    val textSize: Int = 1,
    /** "comfortable" or "compact". */
    val density: String = "comfortable",
    val reduceMotion: Boolean = false,
    val startSection: String = Sections.HOME,
    val sectionOrder: List<String> = emptyList(),
    val hiddenSections: Set<String> = emptySet(),
    val appLock: Boolean = false,
    val homeCardOrder: List<String> = emptyList(),
    val hiddenHomeCards: Set<String> = HomeCards.defaultHidden,
)

/** The values a settings file is allowed to hold; anything else (a hand edit, an older or newer version) is brought back into range. */
fun Settings.normalized(): Settings = copy(
    themeMode = themeMode.takeIf { it in setOf("system", "light", "dark") } ?: "system",
    currency = currency.currencyCode().ifEmpty { "USD" },
    lang = lang.takeIf { it in setOf("system", "en", "ar") } ?: "system",
    palette = palette.takeIf { it in Palettes.ids } ?: Palettes.DEFAULT,
    textSize = textSize.coerceIn(0, 3),
    density = density.takeIf { it in setOf("comfortable", "compact") } ?: "comfortable",
    sectionOrder = sectionOrder.filter { it in Sections.all }.distinct(),
    hiddenSections = hiddenSections.filter { it in Sections.all && it != Sections.HOME }.toSet(),
    homeCardOrder = homeCardOrder.filter { it in HomeCards.all }.distinct(),
    hiddenHomeCards = hiddenHomeCards.filter { it in HomeCards.all }.toSet(),
    startSection = startSection.takeIf { it in Sections.all && (it == Sections.HOME || it !in hiddenSections) } ?: Sections.HOME,
)

/** The one place settings are read and written: always normalised, always saved through a temp file and a rename. */
class SettingsRepository(private val fs: FileSystem, private val path: Path) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    var current: Settings = read()
        private set

    private fun read(): Settings =
        if (fs.exists(path)) runCatching { json.decodeFromString(Settings.serializer(), fs.read(path) { readUtf8() }).normalized() }.getOrDefault(Settings()) else Settings()

    fun reload(): Settings { current = read(); return current }

    fun update(change: (Settings) -> Settings): Settings {
        val next = change(current).normalized()
        fs.createDirectories(path.parent!!)
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(json.encodeToString(Settings.serializer(), next)) }
        fs.atomicMove(tmp, path)
        current = next
        return next
    }
}
