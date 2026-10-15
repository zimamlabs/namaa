package dev.namaa.core

import kotlinx.datetime.LocalDate
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupTest {
    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)
    @Test fun aBackupRoundTripsEveryFileByteForByte() {
        val b = MoneyBook(FakeFileSystem(), "/a".toPath()); b.saveAccounts(listOf(Account("c", "Cash"), Account("d", "Bank")))
        b.saveTransaction(Txn("t1", d(1, 2), -500, "c", "Food", "lunch, \"x\"\nline2")); b.saveTransaction(Txn("t2", LocalDate(2025, 12, 30), 900, "d", "Salary"))
        b.saveBudgets(listOf(Budget("Food", 5_000)))
        val bytes = Backup.create(b.fs, b.root, "2026-10-15T12:00:00")
        val entries = Backup.read(bytes).associate { it.name to it.bytes }
        assertEquals(setOf("accounts.json", "2026.csv", "2025.csv", "budgets.json"), entries.keys)
        for ((name, content) in entries) assertEquals(b.fs.read(b.root / name) { readByteString() }, content, name)
        assertTrue(Backup.matchesFolder(bytes, b.fs, b.root))
    }

    @Test fun exportsSideCopiesAndTempFilesAreNotBackedUp() {
        val b = MoneyBook(FakeFileSystem(), "/a".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        b.fs.write(b.root / "2026 (from X).csv") { writeUtf8("x") }; b.fs.write(b.root / "accounts.json.tmp") { writeUtf8("x") }; b.fs.createDirectories(b.root / "exports"); b.fs.write(b.root / "exports/e.csv") { writeUtf8("x") }
        assertEquals(listOf("accounts.json"), Backup.read(Backup.create(b.fs, b.root, "t")).map { it.name })
    }

    @Test fun aDamagedOrTamperedBackupIsRefused() {
        val b = MoneyBook(FakeFileSystem(), "/a".toPath()); b.saveAccounts(listOf(Account("c", "Cash")))
        val good = Backup.create(b.fs, b.root, "t")
        assertFailsWith<BackupException> { Backup.read("not a backup".encodeUtf8()) }
        assertFailsWith<BackupException> { Backup.read(good.substring(0, good.size - 20)) }
        val text = good.utf8(); val i = text.indexOf("Cash")
        assertFailsWith<BackupException> { Backup.read((text.substring(0, i) + "Cosh" + text.substring(i + 4)).encodeUtf8()) }
    }

    @Test fun anUnsafeFileNameInABackupIsRefused() {
        val evil = Buffer().writeUtf8("NAMAA-BACKUP 1\ncreated t\nfile ../x.json 2 ${"hi".encodeUtf8().sha256().hex()}\nhi\nend\n").readByteString()
        assertFailsWith<BackupException> { Backup.read(evil) }
    }

    @Test fun restoringOntoACleanInstallBringsBackIdenticalData() {
        val src = MoneyBook(FakeFileSystem(), "/a".toPath()); src.saveAccounts(listOf(Account("c", "Cash", "USD", 100), Account("d", "Bank", "EGP")))
        src.saveTransaction(Txn("t1", d(1, 2), -500, "c", "Food")); src.saveTransaction(Txn("t2", LocalDate(2025, 12, 30), 900, "d", "Salary")); src.saveBudgets(listOf(Budget("Food", 5_000, "USD")))
        val bytes = Backup.create(src.fs, src.root, "t")
        val dst = MoneyBook(FakeFileSystem(), "/b".toPath())
        val report = dst.restoreFrom(Backup.read(bytes), "20261015-120000")
        assertEquals(2, report.transactions); assertEquals(2, report.accounts); assertEquals(1, report.budgets)
        assertEquals(src.accounts(), dst.accounts()); assertEquals(src.allTransactions(), dst.allTransactions()); assertEquals(src.budgets(), dst.budgets())
        assertTrue(Backup.matchesFolder(bytes, dst.fs, dst.root))
    }

    @Test fun restoringMergesByIdKeepsWhatIsThereAndMakesASafetyBackupFirst() {
        val src = MoneyBook(FakeFileSystem(), "/a".toPath()); src.saveAccounts(listOf(Account("c", "Cash")))
        src.saveTransaction(Txn("t1", d(1, 2), -500, "c", "Food")); src.saveTransaction(Txn("t2", d(1, 3), -700, "c", "Fun"))
        val bytes = Backup.create(src.fs, src.root, "t")
        val dst = MoneyBook(FakeFileSystem(), "/b".toPath()); dst.saveAccounts(listOf(Account("c", "Cash renamed"), Account("n", "New")))
        dst.saveTransaction(Txn("t1", d(1, 2), -999, "c", "Food")); dst.saveTransaction(Txn("t9", d(1, 9), -1, "n", "Other"))
        val before = dst.fs.read(dst.root / "2026.csv") { readUtf8() }
        val preview = dst.previewRestore(Backup.read(bytes))
        assertEquals(1, preview.transactions); assertEquals(0, preview.accounts)
        assertEquals(before, dst.fs.read(dst.root / "2026.csv") { readUtf8() })
        val report = dst.restoreFrom(Backup.read(bytes), "20261015-120000")
        assertEquals(1, report.transactions)
        assertEquals(setOf("t1", "t2", "t9"), dst.allTransactions().map { it.id }.toSet())
        assertEquals(-999, dst.allTransactions().first { it.id == "t1" }.amountMinor)
        assertEquals("Cash renamed", dst.accounts().first { it.id == "c" }.name)
        val safety = dst.fs.list(dst.root / "safety-backups").single()
        assertTrue(safety.name.startsWith("Namaa-before-restore-"))
        assertEquals(setOf("accounts.json", "2026.csv"), Backup.read(dst.fs.read(safety) { readByteString() }).map { it.name }.toSet())
    }

    @Test fun aBackupWithAnUnreadableFileChangesNothing() {
        val dst = MoneyBook(FakeFileSystem(), "/b".toPath()); dst.saveAccounts(listOf(Account("c", "Cash")))
        val srcFs = FakeFileSystem(); srcFs.createDirectories("/a".toPath()); srcFs.write("/a/debts.json".toPath()) { writeUtf8("{broken") }
        val bad = Backup.read(Backup.create(srcFs, "/a".toPath(), "t"))
        assertFailsWith<BackupException> { dst.restoreFrom(bad, "s") }
        assertEquals(listOf(Account("c", "Cash")), dst.accounts()); assertTrue(!dst.fs.exists(dst.root / "safety-backups"))
    }

    @Test fun backupNamesNeverOverwriteAndSortByTime() {
        assertEquals("Namaa-backup-20261015-120000.namaa-backup", Backup.fileName("20261015-120000"))
    }
}
