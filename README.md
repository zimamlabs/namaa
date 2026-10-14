<p align="center"><img src="docs/icon.png" width="120" alt="Namaa"></p>

<h1 align="center">Namaa</h1>

<p align="center">Private money tracking in plain files, by <a href="https://github.com/zimamlabs">Zimam Labs</a>.</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/zimamlabs/namaa?color=FF5A1F&labelColor=1A1613"></a>
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/zimamlabs/namaa/total?color=FF5A1F&labelColor=1A1613"></a>
  <a href="LICENSE"><img alt="Licence: MIT" src="https://img.shields.io/badge/licence-MIT-FF5A1F?labelColor=1A1613"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-FF5A1F?labelColor=1A1613">
</p>

## What it is

Namaa (نماء, growth) keeps your accounts and your income and spending in a folder of plain files you own. It works offline, has no account and sends nothing anywhere.

## Features

- **Home**: this month's balance, income, spent and left in one card; a donut chart of where the money went; income against spending for the last six months; a card for each account; recent transactions; quick actions to add an expense, an income or a transfer.
- **Accounts** with their balances. A plain Cash account is there from the first launch; add more or rename them.
- **Transfers** between your own accounts: two legs that always sum to zero and never count as income or spending.
- **Transactions**, newest first: amount, income or spending, account, category (chips or your own words), note and a date picked from a calendar. Tap one to edit or delete it.
- **Report**: any month, with totals and spending by category.
- **Plan**: budgets per category with progress bars and a warning at 80% and when you go over; repeating transactions (weekly, monthly); and who owes whom, with partial payments.
- **Search and export**: filter transactions by text, category, account and dates; export a month or a year to CSV in the `exports` folder.
- **A first-run tour** (reopen it from Settings) and a one-time *What's new* after each update.
- **Plain files** in one **Zimam folder**: `accounts.json` and one `<year>.csv` per year, amounts in minor units (cents). Open them in any editor or spreadsheet.

## Languages

English today. Arabic and right-to-left screens are being added; this line will say when they are in.

## Screenshots

Screenshots: coming with the next release.

## Download

Get the file for your phone from the [latest release](../../releases/latest). Next to every file is a checksum file with the same name plus `.sha256`.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Namaa_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Namaa_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Namaa_<version>_Android_x86_64.apk` |

```
sha256sum -c Namaa_<version>_Android_arm64-v8a.apk.sha256
```

## Verify

Every APK is signed with Namaa's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: 57:1E:18:EF:3D:87:CA:CB:C0:C7:38:07:11:E0:B4:BC:12:2C:9A:0A:FD:09:2B:15:18:6D:1D:81:28:AB:57:16
```

```
apksigner verify --print-certs Namaa_<version>_Android_arm64-v8a.apk
```

## Where your data lives

On the phone Namaa keeps its files in a visible `Zimam/Namaa` folder when you allow *All files access* (Settings has the button), so they survive reinstalling the app. Without it they stay inside the app. Earlier folders (the one inside the app, or `~/Namaa` on the desktop) are copied into the new folder and every file is checked by size and SHA-256. **Nothing is lost:** the old folder is never deleted automatically, a file already in the new folder is never overwritten (both are kept), and if anything does not verify Namaa keeps using the old folder and tries again later. Settings can remove the old folder, but only after checking every file again and asking you first.

## Formerly Zimma

Namaa was called Zimma until 0.3.0. The Android application id (`dev.zimma.money`) and the signing key are unchanged, so Namaa installs as an update over Zimma and keeps your data: on first open it copies the old folders (`Zimam/Zimma` and the private one) into `Zimam/Namaa`, checks every file, never overwrites, and leaves the old folders as a backup. Settings has a button to remove an old folder, which checks everything again first.

## Privacy

No account, no ads, no tracking. Namaa keeps your data on your phone in plain files you can open anywhere, and sends nothing over the network.

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the money tests
./gradlew :composeApp:run                # run it on the desktop (data in ~/Zimam/Namaa)
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

`shared` holds the files and the sums (plain Kotlin, tested without a screen) and `composeApp` holds the screens for both platforms.

## Roadmap

Savings goals, charts by category over time, import from a bank CSV, and a Linux edition once the phone app is stable.

## Licence

MIT. See [LICENSE](LICENSE).
