package dev.namaa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.namaa.core.ReleaseNotes

private const val REPO = "https://github.com/zimamlabs/namaa"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onTour: () -> Unit) {
    val links = LocalUriHandler.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().heroGlow().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(Modifier.size(112.dp))
                Text("Namaa", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("ذمّة", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Private money, in plain files", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("Version $APP_VERSION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Panel { Text("Namaa keeps your accounts, spending, budgets and debts in a folder of plain files you own. It works offline and opens in any editor or spreadsheet.", style = MaterialTheme.typography.bodyMedium) }
            Panel {
                Text("What's new", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ReleaseNotes.forVersion(APP_VERSION).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
                OutlinedButton(onClick = onTour) { Text("Show the tour again") }
            }
            Panel {
                Text("Private by design", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("No account, no tracking, nothing sent anywhere. Your data stays in Zimam/Namaa on your device.", style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text("Links", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { links.openUri(REPO) }) { Text("Repository") }
                TextButton(onClick = { links.openUri("$REPO/releases") }) { Text("Releases") }
                TextButton(onClick = { links.openUri("$REPO/issues") }) { Text("Report an issue") }
                TextButton(onClick = { links.openUri("$REPO/blob/main/LICENSE") }) { Text("Licence (MIT)") }
            }
            Text("by Zimam Labs", Modifier.fillMaxWidth().padding(8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}
