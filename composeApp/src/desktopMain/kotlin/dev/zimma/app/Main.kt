package dev.zimma.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.zimma.core.MoneyBook
import okio.FileSystem
import okio.Path.Companion.toPath

fun main() = application {
    val store = ZimmaStore(MoneyBook(FileSystem.SYSTEM, (System.getProperty("user.home") + "/Zimma").toPath()))
    Window(onCloseRequest = ::exitApplication, title = "Zimma", state = rememberWindowState(width = 460.dp, height = 820.dp)) { ZimmaApp(store) }
}
