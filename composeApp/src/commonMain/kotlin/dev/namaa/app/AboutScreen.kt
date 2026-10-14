package dev.namaa.app

import dev.namaa.core.Str
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
        topBar = { TopAppBar(title = { Text(Str.AboutTitle.t()) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, Str.Back.t()) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().heroGlow().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(Modifier.size(112.dp))
                Text(Str.AppName.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                if (lang() == dev.namaa.core.Lang.En) Text("نماء", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(Str.AboutTagline.t(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(Str.VersionBig.t(APP_VERSION), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Panel { Text(Str.AboutBody.t(), style = MaterialTheme.typography.bodyMedium) }
            Panel {
                Text(Str.WhatsNew.t(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ReleaseNotes.forVersion(APP_VERSION, lang()).forEach { Text("•  ${num(it)}", style = MaterialTheme.typography.bodyMedium) }
                OutlinedButton(onClick = onTour) { Text(Str.ShowTour.t()) }
            }
            Panel {
                Text(Str.PrivateTitle.t(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(Str.PrivateBody.t(), style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text(Str.LinksTitle.t(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { links.openUri(REPO) }) { Text(Str.LinkRepo.t()) }
                TextButton(onClick = { links.openUri("$REPO/releases") }) { Text(Str.LinkReleases.t()) }
                TextButton(onClick = { links.openUri("$REPO/issues") }) { Text(Str.LinkIssue.t()) }
                TextButton(onClick = { links.openUri("$REPO/blob/main/LICENSE") }) { Text(Str.LinkLicence.t()) }
            }
            Text(Str.ByLabs.t(), Modifier.fillMaxWidth().padding(8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}
