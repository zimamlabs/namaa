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

**Android today, Linux later.** Releases are Android APKs only. The desktop code stays in the repository and keeps compiling and testing, but it is not packaged or promised yet.

Namaa (نماء, growth) keeps your accounts and your income and spending in a folder of plain files you own. It works offline, has no account and sends nothing anywhere.

## Features

- **Home**: this month's balance, income, spent and left; a balance per currency (different currencies are never added together); a donut chart of where the money went; income against spending for the last six months; a card for each account; recent transactions; quick actions to add an expense, an income or a transfer.
- **Side menu and swipe**: swipe between Home, Money, Accounts, Plan and Report (mirrored in Arabic) or open the menu for the sections, Search, Quick add, Settings, What's new, the tour and About.
- **Accounts** with their balances. A plain Cash account is there from the first launch; add more, rename or archive them (archived accounts keep their history and their place in the totals).
- **Transfers** between your own accounts in the same currency: two legs written and edited together, never counted as income or spending.
- **Transactions**, newest first: amount, income or spending, account, category (chips or your own words), note and a date picked from a calendar. Long-press a row for Edit, Duplicate, Split and Delete (which asks first).
- **Report**: any month, with totals and spending by category, per currency.
- **Plan**: budgets per category with progress bars and a warning at 80% and when you go over; repeating transactions (weekly, monthly); and who owes whom, with partial payments. Long-press for the menu on each.
- **Search, sort and export**: filter transactions by text, category, account and dates, sort by date or size, export a month or a year to CSV (spreadsheet formulas are neutralised, with a currency column).
- **Back up and restore** the whole data folder to one checked file with the system save screen; a restore only adds what is missing and keeps a safety copy first.
- **Settings** as a list of sections: Appearance (mode, six colour themes, four text sizes, spacing, reduce animations), Sections and layout, Language, Currency and categories, Data and storage, Privacy and security (optional app lock), About.
- **A first-run tour** (reopen it from the menu) and a one-time *What's new* after each update.
- **Plain files** in one **Zimam folder**: `accounts.json`, one `<year>.csv` per year (amounts in minor units, cents), `budgets.json`, `recurring.json`, `debts.json`, and `exports/` for CSV exports. Open them in any editor or spreadsheet. If a file ever cannot be read, Namaa keeps a `.corrupt-<time>` copy and never overwrites it.

## Languages

English and Arabic (العربية), with a right-to-left layout. Settings has a Language choice (System, English, العربية) and a digit style (0 1 2 3 or ٠ ١ ٢ ٣); both apply at once. Amounts can be typed in either kind of digit.

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

On the phone Namaa keeps its files in a visible `Zimam/Namaa` folder when you allow *All files access* (Settings has the button), so they survive reinstalling the app. Without it they stay inside the app. Earlier folders (the one inside the app, or the old desktop folder) are copied into the new folder and every file is checked by size and SHA-256. **Nothing is lost:** the old folder is never deleted automatically, a file already in the new folder is never overwritten (both are kept), and if anything does not verify Namaa keeps using the old folder and tries again later. Settings can remove the old folder, but only after checking every file again and asking you first.

## Updating from earlier versions

Namaa keeps its original Android application id (`dev.zimma.money`) and its signing key, so it installs as an update and keeps your data: on first open it copies the folders earlier versions used into `Zimam/Namaa`, checks every file, never overwrites, and leaves the old folders as a backup. Settings has a button to remove an old folder, which checks everything again first.

## Privacy

No account, no ads, no tracking. Namaa has no network permission at all: it cannot send anything anywhere. Your data is plain, unencrypted files and there is no app lock yet, so anyone or any app with access to the folder can read them.

Namaa asks for *All files access* (Android 11 and newer) for one reason: to keep the files in the visible `Zimam/Namaa` folder so they survive reinstalling and you can open, copy and back them up with any file manager. Without it the files stay in the app's private storage, which is removed when you uninstall or clear the app's data, so export or copy them first. Android 8 to 10 cannot use the shared folder and always use the private storage.

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the money tests
./gradlew :composeApp:desktopTest        # the screen tests (shell, swipe, menus, themes), rendered offscreen
./gradlew :composeApp:run                # run the desktop build for development (data in ~/Zimam/Namaa)
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

`shared` holds the files and the sums (plain Kotlin, tested without a screen) and `composeApp` holds the screens for both platforms.

## Roadmap

Next, in this order: home-screen widgets (this month's balance and budget bar, quick add an expense, debts), import from a bank CSV with a preview and duplicate detection (design in `docs/design`), savings goals, a currency picker with per-currency decimals, and receipts or notes on transactions. A Linux edition comes after the phone app is stable.

## Licence

MIT. See [LICENSE](LICENSE).
