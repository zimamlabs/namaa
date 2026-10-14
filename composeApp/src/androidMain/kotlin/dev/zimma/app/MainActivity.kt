package dev.zimma.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.zimma.core.DataLocation
import dev.zimma.core.MoneyBook
import dev.zimma.core.PrefsFile
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    private lateinit var store: ZimmaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val privateRoot = filesDir.resolve("Zimma").also { it.mkdirs() }.toOkioPath()
        val location = DataLocation(
            FileSystem.SYSTEM, privateRoot, Environment.getExternalStorageDirectory().resolve("Zimam/Zimma").toOkioPath(), listOf(privateRoot),
        ) { Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager() }
        location.refresh()
        val ask: (() -> Unit)? = if (Build.VERSION.SDK_INT >= 30) ({
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }) else null
        store = ZimmaStore(MoneyBook(FileSystem.SYSTEM, location.root), PrefsFile(FileSystem.SYSTEM, filesDir.resolve("prefs.json").toOkioPath()), location, ask)
        setContent { ZimmaApp(store) }
    }

    override fun onResume() {
        super.onResume()
        if (!::store.isInitialized) return
        // The permission may have been granted while we were away; the day may have rolled over, or the files changed under us.
        if (store.location.refresh()) store.useBook(MoneyBook(FileSystem.SYSTEM, store.location.root)) else store.reload()
    }
}
