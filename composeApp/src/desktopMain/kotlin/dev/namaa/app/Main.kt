package dev.namaa.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.namaa.core.DataLocation
import dev.namaa.core.MoneyBook
import dev.namaa.core.PrefsFile
import okio.FileSystem
import okio.Path.Companion.toPath

fun main() = application {
    val home = System.getProperty("user.home")
    // Earlier versions kept their files in ~/Zimma and then ~/Zimam/Zimma; both are copied into ~/Zimam/Namaa and left in place.
    val old = "$home/Zimma".toPath()
    val location = DataLocation(FileSystem.SYSTEM, old, "$home/Zimam/Namaa".toPath(), listOf("$home/Zimam/Zimma".toPath(), old)) { true }
    location.refresh()
    val store = NamaaStore(MoneyBook(FileSystem.SYSTEM, location.root), PrefsFile(FileSystem.SYSTEM, "$home/.config/zimma/prefs.json".toPath()), location)
    Window(onCloseRequest = ::exitApplication, title = "Namaa", state = rememberWindowState(width = 460.dp, height = 820.dp)) { NamaaApp(store) }
}
