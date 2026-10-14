package dev.zimma.core

import okio.FileSystem
import okio.Path

/**
 * Copies an old data folder into the new one without ever losing anything: every file is copied through a `.part`
 * file and renamed, a file already in the destination is never overwritten (when it differs, the old copy is kept
 * beside it as `name (from Old).ext`), and the result is checked by file count, size and SHA-256. The old folder is
 * never touched here; it stays as a backup until the user asks to remove it, and removal re-checks first.
 */
object FolderMigration {
    /** One marker per old folder, written only after its copy verified; its date says when. */
    fun marker(target: Path, label: String): Path = target / ".migrated-$label"

    data class Result(val ok: Boolean, val copied: Int, val kept: Int, val problems: List<String>)

    // shortcut: reads a whole file into memory to hash it; Zimma stores small text files, switch to a HashingSource if big files ever land here.
    private fun hash(fs: FileSystem, file: Path): String = fs.read(file) { readByteString() }.sha256().hex()

    private fun files(fs: FileSystem, root: Path): List<Path> =
        fs.listRecursively(root).filter { !it.name.endsWith(".part") && fs.metadata(it).isRegularFile }.toList()

    /** Where a clashing old file is kept: `accounts.json` becomes `accounts (from Zimma).json`, then `(from Zimma 2)` if that is taken too. */
    internal fun clashName(fs: FileSystem, dest: Path, label: String): Path {
        val dot = dest.name.lastIndexOf('.')
        val stem = if (dot > 0) dest.name.substring(0, dot) else dest.name
        val ext = if (dot > 0) dest.name.substring(dot) else ""
        var n = 1
        while (true) {
            val candidate = dest.parent!! / "$stem (from $label${if (n > 1) " $n" else ""})$ext"
            if (!fs.exists(candidate)) return candidate
            n++
        }
    }

    /** Finds where [file] of [source] ended up under [target]: the same path, or a kept clash copy next to it. */
    private fun landing(fs: FileSystem, source: Path, file: Path, target: Path, label: String): Path? {
        val dest = target / file.relativeTo(source)
        val size = fs.metadata(file).size
        val sum = hash(fs, file)
        if (fs.exists(dest) && fs.metadata(dest).size == size && hash(fs, dest) == sum) return dest
        val dot = dest.name.lastIndexOf('.')
        val stem = if (dot > 0) dest.name.substring(0, dot) else dest.name
        val ext = if (dot > 0) dest.name.substring(dot) else ""
        return fs.listOrNull(dest.parent!!).orEmpty().firstOrNull {
            it.name.startsWith("$stem (from $label") && it.name.endsWith(ext) && fs.metadata(it).isRegularFile && fs.metadata(it).size == size && hash(fs, it) == sum
        }
    }

    /** What is wrong, if anything: a file with no verified counterpart in [target]. Empty means the copy is complete. */
    fun verify(fs: FileSystem, source: Path, target: Path, label: String): List<String> =
        files(fs, source).mapNotNull { f -> if (landing(fs, source, f, target, label) == null) "missing or different: ${f.relativeTo(source)}" else null }

    fun copyAndVerify(fs: FileSystem, source: Path, target: Path, label: String): Result {
        if (!fs.exists(source) || !fs.metadata(source).isDirectory || source == target) return Result(true, 0, 0, emptyList())
        var copied = 0
        var kept = 0
        val problems = mutableListOf<String>()
        try {
            fs.createDirectories(target)
            for (p in fs.listRecursively(source)) if (fs.metadata(p).isDirectory) fs.createDirectories(target / p.relativeTo(source))
            for (f in files(fs, source)) {
                if (landing(fs, source, f, target, label) != null) continue
                var dest = target / f.relativeTo(source)
                if (fs.exists(dest)) { dest = clashName(fs, dest, label); kept++ }
                val part = dest.parent!! / (dest.name + ".part")
                fs.createDirectories(dest.parent!!)
                fs.copy(f, part)
                fs.atomicMove(part, dest)
                copied++
            }
        } catch (e: Exception) {
            problems += "copy stopped: ${e.message ?: e::class.simpleName}"
        }
        problems += verify(fs, source, target, label)
        if (problems.isEmpty()) runCatching {
            fs.appendingSink(marker(target, label)).use { sink -> okio.Buffer().writeUtf8("copied from $label: $copied files, $kept kept beside existing ones\n").let { sink.write(it, it.size) } }
        }
        return Result(problems.isEmpty(), copied, kept, problems)
    }

    /** True when [old] was verified before and nothing in it has changed since, so the copy can be skipped. */
    fun upToDate(fs: FileSystem, old: Path, target: Path, label: String): Boolean {
        val m = marker(target, label)
        if (!fs.exists(m)) return false
        val stamp = fs.metadata(m).lastModifiedAtMillis ?: return false
        return files(fs, old).none { (fs.metadata(it).lastModifiedAtMillis ?: Long.MAX_VALUE) > stamp }
    }

    /** True only when every file of [old] is verified in [target] right now. */
    fun safeToRemove(fs: FileSystem, old: Path, target: Path, label: String): Boolean =
        fs.exists(old) && fs.metadata(old).isDirectory && fs.exists(target) && verify(fs, old, target, label).isEmpty()
}
