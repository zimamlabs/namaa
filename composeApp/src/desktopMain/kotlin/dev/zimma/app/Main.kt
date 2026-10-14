package dev.zimma.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.zimma.core.DataLocation
import dev.zimma.core.MoneyBook
import dev.zimma.core.PrefsFile
import okio.FileSystem
import okio.Path.Companion.toPath

fun main() = application {
    val home = System.getProperty("user.home")
    // Earlier versions kept their files in ~/Zimma; they are copied into ~/Zimam/Zimma and left in place.
    val old = "$home/Zimma".toPath()
    val location = DataLocation(FileSystem.SYSTEM, old, "$home/Zimam/Zimma".toPath(), listOf(old)) { true }
    location.refresh()
    val store = ZimmaStore(MoneyBook(FileSystem.SYSTEM, location.root), PrefsFile(FileSystem.SYSTEM, "$home/.config/zimma/prefs.json".toPath()), location)
    Window(onCloseRequest = ::exitApplication, title = "Zimma", state = rememberWindowState(width = 460.dp, height = 820.dp)) { ZimmaApp(store) }
}
