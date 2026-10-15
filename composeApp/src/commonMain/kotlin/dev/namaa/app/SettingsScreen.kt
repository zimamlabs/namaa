package dev.namaa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.namaa.core.Sections
import dev.namaa.core.Str

val currencies = listOf("USD", "EUR", "GBP", "EGP", "SAR", "AED")

/** The Settings pages, in the order of the shared spec (ui-spec-shared.md). Namaa has no reminders, so no Notifications page. About leaves Settings for the About screen. */
internal enum class SettingsPage(val title: Str, val icon: ImageVector) {
    Appearance(Str.SetAppearance, Icons.Rounded.Palette),
    Sections(Str.SetSections, Icons.Rounded.ViewQuilt),
    Language(Str.LanguageTitle, Icons.Rounded.Language),
    Currency(Str.SetCurrencyCats, Icons.Rounded.Payments),
    Data(Str.SetData, Icons.Rounded.Cloud),
    Privacy(Str.SetPrivacy, Icons.Rounded.Lock),
    About(Str.AboutNamaa, Icons.Rounded.Info),
}

@Composable
internal fun paletteName(id: String): String = when (id) {
    "ocean" -> Str.PalOcean; "forest" -> Str.PalForest; "violet" -> Str.PalViolet; "rose" -> Str.PalRose; "graphite" -> Str.PalGraphite; else -> Str.PalZimam
}.t()

@Composable
internal fun textSizeName(step: Int): String = when (step) { 0 -> Str.TextSmall; 2 -> Str.TextLarge; 3 -> Str.TextLargest; else -> Str.TextNormal }.t()

@Composable
internal fun modeName(mode: String): String = when (mode) { "light" -> Str.ThemeLight; "dark" -> Str.ThemeDark; else -> Str.ThemeSystem }.t()

internal fun sectionTab(id: String): Tab = Tab.entries.firstOrNull { it.id == id } ?: Tab.Home

/** The sections the user chose to show, in their order. */
internal fun visibleTabs(settings: dev.namaa.core.Settings): List<Tab> = Sections.visible(settings.sectionOrder, settings.hiddenSections).map(::sectionTab)

/** Settings as a list of labelled rows (icon, title, one line about the current value); a row opens its page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(store: NamaaStore, page: SettingsPage?, onPage: (SettingsPage?) -> Unit, onBack: () -> Unit, onAbout: () -> Unit, onTour: () -> Unit) {
    PlatformBackHandler(true) { if (page != null) onPage(null) else onBack() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text((page?.title ?: Str.SettingsTitle).t(), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { if (page != null) onPage(null) else onBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, Str.Back.t()) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(rowGap)) {
            when (page) {
                null -> SettingsRows(store) { if (it == SettingsPage.About) onAbout() else onPage(it) }
                SettingsPage.Appearance -> AppearancePage(store)
                SettingsPage.Sections -> SectionsPage(store)
                SettingsPage.Language -> LanguagePage(store)
                SettingsPage.Currency -> CurrencyPage(store)
                SettingsPage.Data -> DataPage(store, onTour)
                SettingsPage.Privacy -> PrivacyPage(store)
                SettingsPage.About -> Unit
            }
        }
    }
}

@Composable
private fun SettingsRows(store: NamaaStore, onOpen: (SettingsPage) -> Unit) {
    val st = store.settings
    val summaries = mapOf(
        SettingsPage.Appearance to listOf(modeName(st.themeMode), paletteName(st.palette), textSizeName(st.textSize)).joinToString(", "),
        SettingsPage.Sections to Str.SummarySections.t(sectionTab(st.startSection).label.t(), visibleTabs(st).size),
        SettingsPage.Language to listOf(when (st.lang) { "en" -> Str.LangEnglish.text(dev.namaa.core.Lang.En); "ar" -> Str.LangArabic.text(dev.namaa.core.Lang.En); else -> Str.ThemeSystem.t() }, if (st.arabicDigits) Str.DigitsArabic.text(dev.namaa.core.Lang.En) else Str.DigitsWestern.text(dev.namaa.core.Lang.En)).joinToString(", "),
        SettingsPage.Currency to st.currency,
        SettingsPage.Data to store.location.root.toString(),
        SettingsPage.Privacy to (if (st.appLock) Str.SummaryAppLockOn else Str.SummaryPrivacy).t(),
        SettingsPage.About to "v$APP_VERSION",
    )
    SettingsPage.entries.forEach { p -> SettingsRow(p.icon, p.title.t(), summaries.getValue(p)) { onOpen(p) } }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Language (System, English, العربية) and the digit style; both apply at once. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LanguageChips(store: NamaaStore) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("system" to Str.ThemeSystem.t(), "en" to Str.LangEnglish.t(), "ar" to Str.LangArabic.t()).forEach { (k, v) ->
            FilterChip(store.settings.lang == k, { store.updateSettings { it.copy(lang = k) } }, { Text(v) })
        }
    }
    Text(Str.DigitsTitle.t(), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!store.settings.arabicDigits, { store.updateSettings { it.copy(arabicDigits = false) } }, { Text(Str.DigitsWestern.text(dev.namaa.core.Lang.En)) })
        FilterChip(store.settings.arabicDigits, { store.updateSettings { it.copy(arabicDigits = true) } }, { Text(Str.DigitsArabic.text(dev.namaa.core.Lang.En)) })
    }
}
