package dev.namaa.core

import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataLocationTest {
    private val fs = FakeFileSystem()
    private val priv = "/data/${LegacyNames.FOLDER}".toPath()
    private val shared = "/sdcard/${LegacyNames.SHARED_FOLDER}".toPath()
    private var allowed = false

    private fun put(p: Path, text: String) { fs.createDirectories(p.parent!!); fs.write(p) { writeUtf8(text) } }
    private fun location() = DataLocation(fs, priv, shared, listOf(priv)) { allowed }

    @Test fun staysPrivateWithoutAccess() {
        put(priv / "accounts.json", "[a]")
        val l = location()
        assertFalse(l.refresh()); assertEquals(priv, l.root); assertFalse(l.shared); assertNull(l.message)
    }

    @Test fun movesToTheSharedFolderWhenAllowedAndKeepsTheOldOne() {
        put(priv / "accounts.json", "[a]")
        allowed = true
        val l = location()
        assertTrue(l.refresh()); assertEquals(shared, l.root); assertTrue(l.shared)
        assertEquals("[a]", fs.read(shared / "accounts.json") { readUtf8() })
        assertTrue(fs.exists(priv / "accounts.json"))
        assertEquals(listOf(priv), l.removableOldFolders())
    }

    @Test fun removingTheOldFolderRechecksFirst() {
        put(priv / "accounts.json", "[a]")
        allowed = true
        val l = location(); l.refresh()
        put(priv / "2027.csv", "unverified")
        assertFalse(l.removeOldFolder(priv)); assertTrue(fs.exists(priv / "accounts.json"))
        fs.delete(priv / "2027.csv")
        assertTrue(l.removeOldFolder(priv)); assertFalse(fs.exists(priv))
    }

    @Test fun aFailedVerificationKeepsTheOldFolderInUseAndSaysSo() {
        put(priv / "accounts.json", "[a]")
        allowed = true
        val l = DataLocation(object : okio.ForwardingFileSystem(fs) {
            override fun sink(file: Path, mustCreate: Boolean): okio.Sink = if (file.name.startsWith("accounts")) throw java.io.IOException("boom") else super.sink(file, mustCreate)
        }, priv, shared, listOf(priv)) { allowed }
        l.refresh()
        assertEquals(priv, l.root); assertNotNull(l.message)
        assertTrue(fs.exists(priv / "accounts.json"))
    }

    @Test fun losingAccessFallsBackWithAMessageAndDeletesNothing() {
        put(priv / "accounts.json", "[old]")
        allowed = true
        val l = location(); l.refresh()
        put(shared / "accounts.json", "[newer]")
        allowed = false
        assertTrue(l.refresh()); assertEquals(priv, l.root); assertNotNull(l.message)
        assertTrue(fs.exists(shared / "accounts.json"))
        allowed = true
        assertTrue(l.refresh()); assertEquals(shared, l.root)
    }

    @Test fun aFreshInstallGoesStraightToTheSharedFolder() {
        allowed = true
        val l = location()
        assertTrue(l.refresh()); assertEquals(shared, l.root); assertTrue(l.oldFolders().isEmpty())
    }

    @Test fun theRenameCopiesBothOldFoldersNewestFirstAndKeepsEveryFile() {
        val namaa = "/sdcard/Zimam/Namaa".toPath(); val oldShared = "/sdcard/${LegacyNames.SHARED_FOLDER}".toPath()
        put(oldShared / "accounts.json", "NEWER"); put(oldShared / "2026.csv", "rows")
        put(priv / "accounts.json", "OLDER"); put(priv / "2025.csv", "old rows")
        allowed = true
        val l = DataLocation(fs, priv, namaa, listOf(oldShared, priv)) { allowed }
        assertTrue(l.refresh()); assertEquals(namaa, l.root); assertNull(l.message)
        assertEquals("NEWER", fs.read(namaa / "accounts.json") { readUtf8() })
        assertEquals("OLDER", fs.read(namaa / "accounts (from ${LegacyNames.FOLDER}).json") { readUtf8() })
        assertEquals("rows", fs.read(namaa / "2026.csv") { readUtf8() }); assertEquals("old rows", fs.read(namaa / "2025.csv") { readUtf8() })
        assertTrue(fs.exists(oldShared / "accounts.json") && fs.exists(priv / "accounts.json"))
        assertEquals(setOf(oldShared, priv), l.removableOldFolders().toSet())
    }
}
