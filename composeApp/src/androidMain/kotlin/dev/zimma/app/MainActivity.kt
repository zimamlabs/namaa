package dev.zimma.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.zimma.core.MoneyBook
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    private lateinit var store: ZimmaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = ZimmaStore(MoneyBook(FileSystem.SYSTEM, filesDir.resolve("Zimma").also { it.mkdirs() }.toOkioPath()))
        setContent { ZimmaApp(store) }
    }

    override fun onResume() {
        super.onResume()
        // The day may have rolled over, or the files changed under us.
        if (::store.isInitialized) store.reload()
    }
}
