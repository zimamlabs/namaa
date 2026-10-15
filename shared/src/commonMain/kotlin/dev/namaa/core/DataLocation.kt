package dev.namaa.core

import okio.FileSystem
import okio.Path

/**
 * Where Namaa keeps its files: the shared `Zimam/Namaa` folder when the platform allows it, otherwise the private
 * folder. Older folders are copied into the shared one, verified, and left in place as a backup. If a copy does not
 * verify, the old location stays in use, [message] says why, and the next [refresh] tries again. Nothing is deleted here.
 */
/** Why the last folder switch was held back; the screen words it in the current language. */
data class FolderNote(val kind: Kind, val a: String, val b: String) {
    enum class Kind { Unreachable, VerifyFailed }
}

class DataLocation(
    private val fs: FileSystem,
    val privateRoot: Path,
    val sharedFolder: Path,
    private val legacy: List<Path>,
    private val canUseShared: () -> Boolean,
) {
    var root: Path = privateRoot
        private set

    val shared: Boolean get() = root == sharedFolder

    /** Set when the last switch could not be completed, so the screen can say why. */
    var message: FolderNote? = null
        private set

    // Two old folders can share a name (the private one and the shared one), so the label includes the parent when it is Zimam.
    private fun label(p: Path) = if (p.parent?.name == "Zimam") "Zimam-${p.name}" else p.name

    fun oldFolders(): List<Path> = legacy.filter { it != sharedFolder && fs.exists(it) && fs.metadata(it).isDirectory }

    /** Old folders whose every file is verified in the shared folder right now, so removing them loses nothing. */
    fun removableOldFolders(): List<Path> =
        if (shared) oldFolders().filter { FolderMigration.safeToRemove(fs, it, sharedFolder, label(it)) } else emptyList()

    /** Removes one old folder, only after checking again that everything in it is in the shared folder. */
    fun removeOldFolder(old: Path): Boolean {
        if (old !in removableOldFolders() || !FolderMigration.safeToRemove(fs, old, sharedFolder, label(old))) return false
        fs.deleteRecursively(old)
        return true
    }

    /** Picks the folder to use now (call again after the permission changes). Returns true when it changed, so callers reload. */
    fun refresh(): Boolean {
        message = null
        var target = privateRoot
        if (canUseShared()) {
            target = if (runCatching { fs.createDirectories(sharedFolder) }.isSuccess) sharedFolder else privateRoot
        } else if (fs.exists(sharedFolder) && fs.listOrNull(sharedFolder).orEmpty().any { !it.name.startsWith(".") }) {
            message = FolderNote(FolderNote.Kind.Unreachable, sharedFolder.toString(), "")
        }
        if (target == sharedFolder) {
            for (old in oldFolders()) {
                if (FolderMigration.upToDate(fs, old, sharedFolder, label(old))) continue
                if (!FolderMigration.copyAndVerify(fs, old, sharedFolder, label(old)).ok) {
                    message = FolderNote(FolderNote.Kind.VerifyFailed, old.toString(), sharedFolder.toString())
                    target = old
                    break
                }
            }
        }
        val changed = target != root
        root = target
        return changed
    }
}
