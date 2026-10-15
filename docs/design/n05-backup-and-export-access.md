# N-05 Backup, restore and export access

Problem: without All files access the data lives in the app's private folder, exports land there too, and nothing can be taken out; uninstalling loses everything.

Decision, smallest safe version:

1. **Back up everything**: one file (a plain-text-framed bundle with sizes and SHA-256 per file, not a zip, because zip is not available in shared Kotlin code) of the whole data folder (json, csv, exports excluded) named `Namaa-backup-<yyyyMMdd-HHmmss>.namaa-backup`, never overwriting. After writing, the file is read back and every file's SHA-256 is compared with the source. Saved with the system document picker (`CreateDocument`), so no permission is needed and it can go to any app or cloud the user chooses.
2. **Restore**: the user picks a backup file (`OpenDocument`). Namaa reads it, shows counts (accounts, transactions per year) and what would change, and merges by id into the current data (transactions and accounts united, the current file wins on a clash, like the side-copy merge), after first making an automatic safety backup of the current data. Nothing is deleted. A file that fails to read is refused with a message.
3. **Export access**: after exporting a month or year, offer Share (the system share sheet with the file) and keep the copy in `exports/`.
4. Android `allowBackup` is set to false: the system must not upload financial files to a Google account behind the user's back; the in-app backup is the supported way. README says so.
5. The backup logic (build, verify, read, merge plan) lives in `shared` on okio and is tested with a fake file system; only the pickers live in the Android module.

Not now: encryption (needs an app lock decision), scheduled backups, restore that replaces.
