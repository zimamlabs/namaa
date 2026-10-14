package dev.namaa.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.namaa.core.DataLocation
import dev.namaa.core.MoneyBook
import dev.namaa.core.PrefsFile
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    private lateinit var store: NamaaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The private folder keeps its old name (Zimma) so installs from before the rename still find their data.
        val privateRoot = filesDir.resolve("Zimma").also { it.mkdirs() }.toOkioPath()
        val sdcard = Environment.getExternalStorageDirectory()
        // Newest first: the shared Zimam/Zimma folder of 0.2 and 0.3, then the private copy. A clashing older file is kept beside, never overwritten.
        val location = DataLocation(
            FileSystem.SYSTEM, privateRoot, sdcard.resolve("Zimam/Namaa").toOkioPath(),
            listOf(sdcard.resolve("Zimam/Zimma").toOkioPath(), privateRoot),
        ) { Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager() }
        location.refresh()
        val ask: (() -> Unit)? = if (Build.VERSION.SDK_INT >= 30) ({
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }) else null
        store = NamaaStore(MoneyBook(FileSystem.SYSTEM, location.root), PrefsFile(FileSystem.SYSTEM, filesDir.resolve("prefs.json").toOkioPath()), location, ask)
        setContent { NamaaApp(store) }
    }

    override fun onResume() {
        super.onResume()
        if (!::store.isInitialized) return
        // The permission may have been granted while we were away; the day may have rolled over, or the files changed under us.
        if (store.location.refresh()) store.useBook(MoneyBook(FileSystem.SYSTEM, store.location.root)) else store.reload()
    }
}
