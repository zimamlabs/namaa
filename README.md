<p align="center"><img src="docs/icon.svg" width="120" alt="Zimma"></p>

<h1 align="center">Zimma</h1>

<p align="center">Private money tracking in plain files, by <a href="https://github.com/zimamlabs">Zimam</a>.</p>

Zimma (ذمّة, what you owe and are owed) keeps your accounts and your income and spending in a folder of plain files you own. It works offline, has no account and sends nothing anywhere.

## What is in it

- **This month** at a glance: income, spent and what is left.
- **Accounts** with their balances. A plain Cash account is there from the first launch; add more or rename them.
- **Transactions**, newest first: amount, income or spending, account, category (chips or your own words), note and date. Tap one to edit or delete it.
- **Spending by category** for the month.
- **Plain files**: `accounts.json` and one `<year>.csv` per year, amounts in minor units (cents). Open them in any editor or spreadsheet.

## Download

Get the file for your phone from the [latest release](../../releases/latest). Next to every file is a checksum file with the same name plus `.sha256`.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Zimma_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Zimma_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Zimma_<version>_Android_x86_64.apk` |

```
sha256sum -c Zimma_<version>_Android_arm64-v8a.apk.sha256
```

### Verify the Android download

Every APK is signed with Zimma's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: 57:1E:18:EF:3D:87:CA:CB:C0:C7:38:07:11:E0:B4:BC:12:2C:9A:0A:FD:09:2B:15:18:6D:1D:81:28:AB:57:16
```

```
apksigner verify --print-certs Zimma_<version>_Android_arm64-v8a.apk
```

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the money tests
./gradlew :composeApp:run                # run it on the desktop (data in ~/Zimma)
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

`shared` holds the files and the sums (plain Kotlin, tested without a screen) and `composeApp` holds the screens for both platforms.

## Roadmap

A visible `/Zimma` folder on the phone, budgets, savings and plans, who owes whom, and a Linux edition.

## Licence

MIT. See [LICENSE](LICENSE).
