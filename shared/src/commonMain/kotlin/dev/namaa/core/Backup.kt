package dev.namaa.core

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path

class BackupException(message: String) : Exception(message)

/**
 * A backup is one plain-text-framed file holding the data files of the folder (accounts, budgets, repeating, debts and the
 * year CSVs), each with its size and SHA-256, so it can be checked before anything is restored. Exports, side copies and
 * temporary files are not part of it.
 *
 * ```
 * NAMAA-BACKUP 1
 * created <stamp>
 * file <name> <size> <sha256>
 * <the file's bytes>
 * end
 * ```
 */
object Backup {
    data class Entry(val name: String, val bytes: ByteString)

    private val allowed = Regex("^(accounts|budgets|recurring|debts)\\.json$|^\\d{4}\\.csv$")

    fun isDataFile(name: String) = allowed.matches(name)

    fun fileName(stamp: String) = "Namaa-backup-$stamp.namaa-backup"

    private fun dataFiles(fs: FileSystem, root: Path): List<Entry> =
        fs.listOrNull(root).orEmpty().filter { isDataFile(it.name) && fs.metadata(it).isRegularFile }.sortedBy { it.name }
            .map { Entry(it.name, fs.read(it) { readByteString() }) }

    fun create(fs: FileSystem, root: Path, created: String): ByteString = encode(dataFiles(fs, root), created)

    internal fun encode(entries: List<Entry>, created: String): ByteString {
        val out = Buffer().writeUtf8("NAMAA-BACKUP 1\ncreated $created\n")
        for (e in entries) out.writeUtf8("file ${e.name} ${e.bytes.size} ${e.bytes.sha256().hex()}\n").write(e.bytes).writeUtf8("\n")
        return out.writeUtf8("end\n").readByteString()
    }

    /** The files of a backup, each checked against its size and SHA-256. Throws [BackupException] on anything wrong. */
    fun read(data: ByteString): List<Entry> {
        val src = Buffer().write(data)
        fun line(): String = src.readUtf8Line() ?: throw BackupException("the file ended early")
        if (line() != "NAMAA-BACKUP 1") throw BackupException("this is not a Namaa backup")
        if (!line().startsWith("created ")) throw BackupException("the header is damaged")
        val out = mutableListOf<Entry>()
        while (true) {
            val head = line()
            if (head == "end") break
            val p = head.split(" ")
            if (p.size != 4 || p[0] != "file") throw BackupException("a file entry is damaged")
            val size = p[2].toLongOrNull() ?: throw BackupException("a file size is damaged")
            if (!isDataFile(p[1]) || out.any { it.name == p[1] }) throw BackupException("unexpected file name ${p[1]}")
            if (src.size < size + 1) throw BackupException("the file ended early")
            val bytes = src.readByteString(size)
            if (src.readUtf8CodePoint() != '\n'.code) throw BackupException("a file entry is damaged")
            if (bytes.sha256().hex() != p[3]) throw BackupException("${p[1]} does not match its checksum")
            out += Entry(p[1], bytes)
        }
        return out
    }

    /** True when every data file in [fs]/[root] is in [data] with the same bytes and vice versa. */
    fun matchesFolder(data: ByteString, fs: FileSystem, root: Path): Boolean =
        runCatching { read(data).associate { it.name to it.bytes } == dataFiles(fs, root).associate { it.name to it.bytes } }.getOrDefault(false)
}
