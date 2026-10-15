package dev.namaa.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import dev.namaa.core.Str

/** One entry of a long-press menu. [confirm] makes the action ask first (title and text of the question), for anything that cannot be undone. */
internal class MenuAction(val label: String, val icon: ImageVector? = null, val destructive: Boolean = false, val confirm: Pair<String, String>? = null, val onClick: () -> Unit)

/** A question before something that cannot be undone, with the text saying what is lost. */
@Composable
internal fun ConfirmDelete(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(Str.Delete.t(), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.Cancel.t()) } },
    )
}

/**
 * The one long-press menu of the app (Edit, Duplicate, Split, Delete...). A tap does [onClick]; a long press gives the system
 * long-press haptic tick (combinedClickable performs it) and opens the menu on the row. Actions that [MenuAction.confirm] ask before they run.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContextMenuBox(actions: List<MenuAction>, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf<MenuAction?>(null) }
    Box(modifier.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = { open = true })) {
        content()
        DropdownMenu(open, { open = false }) {
            actions.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.label, color = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = a.icon?.let { icon -> { Icon(icon, null, tint = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { open = false; if (a.confirm != null) asking = a else a.onClick() },
                )
            }
        }
    }
    asking?.let { a -> ConfirmDelete(a.confirm!!.first, a.confirm.second, { asking = null; a.onClick() }, { asking = null }) }
}
