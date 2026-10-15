package dev.namaa.core

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FolderMigrationTest {
    private val fs = FakeFileSystem()
    private val old = "/private/${LegacyNames.FOLDER}".toPath()
    private val shared = "/sdcard/${LegacyNames.SHARED_FOLDER}".toPath()

    private fun put(p: Path, text: String, system: FileSystem = fs) { system.createDirectories(p.parent!!); system.write(p) { writeUtf8(text) } }
    private fun text(p: Path) = fs.read(p) { readUtf8() }

    @Test fun copiesEverythingAndLeavesTheOldFolderUntouched() {
        put(old / "accounts.json", "[a]"); put(old / "2026.csv", "id,date\n1,2026-01-01\n")
        val r = FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        assertTrue(r.ok); assertEquals(2, r.copied)
        assertEquals("[a]", text(shared / "accounts.json")); assertEquals("[a]", text(old / "accounts.json"))
        assertTrue(fs.exists(FolderMigration.marker(shared, LegacyNames.FOLDER)))
        assertTrue(FolderMigration.safeToRemove(fs, old, shared, LegacyNames.FOLDER))
    }

    @Test fun runningTwiceChangesNothing() {
        put(old / "accounts.json", "[a]")
        FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        val again = FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        assertTrue(again.ok); assertEquals(0, again.copied)
    }

    @Test fun neverOverwritesAFileInTheDestinationAndKeepsBoth() {
        put(old / "accounts.json", "OLD"); put(shared / "accounts.json", "NEW")
        val r = FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("NEW", text(shared / "accounts.json"))
        assertEquals("OLD", text(shared / "accounts (from ${LegacyNames.FOLDER}).json"))
        // A second clash gets its own name instead of replacing the first kept copy.
        put(old / "accounts.json", "OLDER")
        FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        assertEquals("OLDER", text(shared / "accounts (from ${LegacyNames.FOLDER} 2).json"))
        assertEquals("OLD", text(shared / "accounts (from ${LegacyNames.FOLDER}).json"))
    }

    @Test fun aLeftoverPartFileDoesNotCountAndTheCopyIsRedone() {
        put(old / "2026.csv", "rows"); put(shared / "2026.csv.part", "half")
        val r = FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        assertTrue(r.ok); assertEquals("rows", text(shared / "2026.csv"))
    }

    @Test fun aFailedCopyIsReportedAndNothingIsMarked() {
        put(old / "accounts.json", "[a]"); put(old / "2026.csv", "rows")
        val failing = object : ForwardingFileSystem(fs) {
            override fun sink(file: Path, mustCreate: Boolean): Sink = if (file.name.startsWith("2026.csv")) throw java.io.IOException("disk full") else super.sink(file, mustCreate)
        }
        val r = FolderMigration.copyAndVerify(failing, old, shared, LegacyNames.FOLDER)
        assertFalse(r.ok); assertFalse(fs.exists(FolderMigration.marker(shared, LegacyNames.FOLDER)))
        assertFalse(FolderMigration.safeToRemove(fs, old, shared, LegacyNames.FOLDER))
        assertTrue(fs.exists(old / "2026.csv"))
    }

    @Test fun removalIsRefusedWhenAFileIsNotVerified() {
        put(old / "accounts.json", "[a]")
        FolderMigration.copyAndVerify(fs, old, shared, LegacyNames.FOLDER)
        put(old / "2027.csv", "new rows after the copy")
        assertFalse(FolderMigration.safeToRemove(fs, old, shared, LegacyNames.FOLDER))
    }
}
