package dev.namaa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.namaa.core.Str

/**
 * The side menu, in the order fixed by ui-spec-shared.md: header (mark tile, name, tagline, version), the sections,
 * a divider, then Search, Quick add, Settings, What's new, Tour, About, and a small footer. Items are 56 dp high with
 * 16 dp side padding. Search opens the Money section, where the search and filters live.
 */
@Composable
internal fun AppDrawer(
    tabs: List<Tab>, current: Tab,
    onSection: (Tab) -> Unit, onSearch: () -> Unit, onQuickAdd: () -> Unit,
    onSettings: () -> Unit, onAbout: () -> Unit, onWhatsNew: () -> Unit, onTour: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { BrandMark(Modifier.size(40.dp)) }
                Column {
                    Text(Str.AppName.t(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(Str.ByZimamLabs.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(num("v$APP_VERSION"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                tabs.forEach { t -> Item(t.label.t(), { Icon(t.icon, null) }, current == t) { onSection(t) } }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Item(Str.SearchItem.t(), { Icon(Icons.Rounded.Search, null) }, false, onSearch)
                Item(Str.QuickAddTitle.t(), { Icon(Icons.Rounded.Add, null) }, false, onQuickAdd)
                Item(Str.SettingsTitle.t(), { Icon(Icons.Rounded.Settings, null) }, false, onSettings)
                Item(Str.WhatsNew.t(), { Icon(Icons.Rounded.AutoAwesome, null) }, false, onWhatsNew)
                Item(Str.ShowTour.t(), { Icon(Icons.Rounded.Tour, null) }, false, onTour)
                Item(Str.AboutNamaa.t(), { Icon(Icons.Rounded.Info, null) }, false, onAbout)
            }
            Text(Str.ZimamLabs.t(), Modifier.fillMaxWidth().padding(vertical = 16.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** The small form behind Quick add: the three things people add most often. */
@Composable
internal fun QuickAddDialog(onDismiss: () -> Unit, onExpense: () -> Unit, onIncome: () -> Unit, onTransfer: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.QuickAddTitle.t()) },
        text = {
            Column {
                Item(Str.QuickExpense.t(), { Icon(Icons.Rounded.Add, null) }, false, onExpense)
                Item(Str.Income.t(), { Icon(Icons.Rounded.Add, null) }, false, onIncome)
                Item(Str.QuickTransfer.t(), { Icon(Icons.Rounded.Add, null) }, false, onTransfer)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}

@Composable
private fun Item(label: String, icon: @Composable () -> Unit, selected: Boolean, onClick: () -> Unit) =
    NavigationDrawerItem(label = { Text(label) }, icon = icon, selected = selected, onClick = onClick, modifier = Modifier.heightIn(min = 56.dp))

/**
 * The pager takes every horizontal swipe, so on the first section a thin strip at the start edge opens the side menu
 * (the menu button always works). The strip sits on the side the reading direction starts from.
 */
@Composable
internal fun BoxScope.DrawerEdge(onOpen: () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    Box(
        Modifier.align(Alignment.CenterStart).width(24.dp).fillMaxHeight().pointerInput(rtl) {
            var dragged = 0f
            detectHorizontalDragGestures(onDragStart = { dragged = 0f }, onDragEnd = { dragged = 0f }) { change, amount ->
                change.consume()
                dragged += if (rtl) -amount else amount
                if (dragged > threshold) { dragged = Float.NEGATIVE_INFINITY; onOpen() }
            }
        },
    )
}
