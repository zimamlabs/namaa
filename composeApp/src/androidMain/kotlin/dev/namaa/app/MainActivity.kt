package dev.namaa.app

import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.namaa.core.DataLocation
import dev.namaa.core.I18n
import dev.namaa.core.Str
import dev.namaa.core.LegacyNames
import dev.namaa.core.MoneyBook
import dev.namaa.core.SettingsRepository
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    private lateinit var store: NamaaStore
    private var pendingSave: Pair<ByteArray, (Boolean) -> Unit>? = null
    private var pendingPick: ((ByteArray?) -> Unit)? = null
    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val (bytes, done) = pendingSave ?: return@registerForActivityResult
        pendingSave = null
        done(uri != null && runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false))
    }
    private val pickLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val done = pendingPick ?: return@registerForActivityResult
        pendingPick = null
        done(uri?.let { runCatching { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }.getOrNull() })
    }
    private var pendingLock: ((Boolean) -> Unit)? = null
    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingLock?.invoke(result.resultCode == RESULT_OK); pendingLock = null
    }
    private val appLock = object : AppLock {
        private val keyguard get() = getSystemService(KeyguardManager::class.java)
        override val available: Boolean get() = keyguard.isDeviceSecure
        override fun unlock(onResult: (Boolean) -> Unit) {
            val title = Str.AppLockPrompt.text(I18n.resolve(store.settings.lang, systemLanguage()))
            val intent = keyguard.createConfirmDeviceCredentialIntent(title, null) ?: return onResult(true)
            pendingLock = onResult; lockLauncher.launch(intent)
        }
    }
    private val files = object : FileActions {
        override fun saveAs(suggestedName: String, bytes: ByteArray, onDone: (Boolean) -> Unit) { pendingSave = bytes to onDone; saveLauncher.launch(suggestedName) }
        override fun pick(onPicked: (ByteArray?) -> Unit) { pendingPick = onPicked; pickLauncher.launch(arrayOf("*/*")) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The private folder keeps its original name so installs from before the rename still find their data.
        val privateRoot = filesDir.resolve(LegacyNames.FOLDER).also { it.mkdirs() }.toOkioPath()
        val sdcard = Environment.getExternalStorageDirectory()
        // Newest first: the shared folder of 0.2 and 0.3, then the private copy. A clashing older file is kept beside, never overwritten.
        val location = DataLocation(
            FileSystem.SYSTEM, privateRoot, sdcard.resolve("Zimam/Namaa").toOkioPath(),
            listOf(sdcard.resolve(LegacyNames.SHARED_FOLDER).toOkioPath(), privateRoot),
        ) { Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager() }
        location.refresh()
        val ask: (() -> Unit)? = if (Build.VERSION.SDK_INT >= 30) ({
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }) else null
        store = NamaaStore(MoneyBook(FileSystem.SYSTEM, location.root), SettingsRepository(FileSystem.SYSTEM, filesDir.resolve("prefs.json").toOkioPath()), location, ask, files, appLock)
        setContent { NamaaApp(store) }
    }

    override fun onStop() {
        super.onStop()
        // Back to the lock screen when the app leaves the screen, except while one of our own pickers or the lock prompt is open.
        if (::store.isInitialized && pendingSave == null && pendingPick == null && pendingLock == null) store.relock()
    }

    override fun onResume() {
        super.onResume()
        if (!::store.isInitialized) return
        // The permission may have been granted while we were away; the day may have rolled over, or the files changed under us.
        if (store.location.refresh()) store.useBook(MoneyBook(FileSystem.SYSTEM, store.location.root)) else store.reload()
    }
}
