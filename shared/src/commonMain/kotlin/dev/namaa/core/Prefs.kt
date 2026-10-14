package dev.namaa.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/** Settings that belong to this install, not to the data folder: they stay put when the folder moves. */
@Serializable
data class Prefs(val lastSeenVersion: String = "", val tourDone: Boolean = false, val theme: String = "system", val currency: String = "USD")

class PrefsFile(private val fs: FileSystem, private val path: Path) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): Prefs = if (fs.exists(path)) runCatching { json.decodeFromString(Prefs.serializer(), fs.read(path) { readUtf8() }) }.getOrDefault(Prefs()) else Prefs()

    fun save(prefs: Prefs) {
        fs.createDirectories(path.parent!!)
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(json.encodeToString(Prefs.serializer(), prefs)) }
        fs.atomicMove(tmp, path)
    }
}
