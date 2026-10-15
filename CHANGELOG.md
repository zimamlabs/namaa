# Changelog

## Unreleased

- **Amounts are no longer misread.** "1,234" used to be saved as 1.23. Namaa now accepts grouping only when it is unambiguous ("1,234.50", "1.234,50", "1,234,567"), refuses "1,234" and "12.999" with a message instead of guessing or cutting digits, and refuses amounts too large to store.

## 0.5.1

- A new app icon.

## 0.5.0

- **Arabic and right-to-left.** Namaa now has an Arabic interface with a mirrored layout (Settings, Language: System, English or العربية, applied at once). Menus, dialogs, the tour, About, the built-in categories and dates are in Arabic, and the charts run right to left. Digits can be Western (0 1 2 3) or Arabic-Indic (٠ ١ ٢ ٣). You can type amounts in either kind of digit.

## 0.4.0

- **Zimma is now Namaa** (نماء, growth). New name, new icon. The Android application id and the signing key are unchanged, so it installs as an update.
- **One folder, new name.** Your data is copied into `Zimam/Namaa` from `Zimam/Zimma` and the private folder (checked file by file; nothing is overwritten, a clashing older file is kept beside as `name (from Zimma).ext`; the old folders stay as a backup, and Settings can remove one only after checking again).
- The README no longer carries the brand boards; they live in `branding/boards`.

## 0.3.0

- **Budgets** per category with a progress bar, a warning at 80 percent and an over-budget alert; the Home card shows the ones that need attention.
- **Repeating transactions**, weekly or monthly (a rule on the 31st falls on the last day of shorter months). They are added once each when they come due, and never doubled.
- **Search and filters** on the Money page: text, category, account and a date range.
- **Export** a month or a whole year as CSV into `exports/` in the Zimam folder.
- **Who owes whom**: money you lent or borrowed, settled with partial payments, with totals and a balance per person.
- **A new look** that follows the icon: warm dark and ivory surfaces, the Zimam orange accent, a soft glow behind the hero card, large rounded cards, and a redesigned About page.

## 0.2.0

- **A professional Home.** The month's balance with income, spent and left; a donut chart of spending by category; income against spending for the last six months; a card per account; recent transactions; quick actions for an expense, an income and a transfer. Bottom navigation for Home, Money, Accounts and Report.
- **Transfers** between your own accounts. Both legs always sum to zero and are not counted as income or spending; deleting one deletes both.
- **One Zimam folder.** With All files access your data lives in `Zimam/Zimma` on the phone (`~/Zimam/Zimma` on the desktop). Older folders are copied in and verified by file count, size and SHA-256, and left as a backup; nothing is deleted or overwritten. Settings can remove the old folder after checking again and asking.
- **A calendar date picker** instead of typing a date.
- **A first-run tour** and a one-time What's new after each update, both reachable again from Settings.
- **The final Zimma icon** on the launcher (adaptive and themed), the splash and About, and a Settings page with theme and default currency.

## 0.1.2

- A better splash screen: the name sits clearly apart from the logo, and the logo and name ease in.

## 0.1.1

- **New icon.** A wallet with the Zimam dot, on the app icon and the splash screen.

## 0.1.0

- First release: accounts (a plain Cash account is made on first launch; add and rename more), income and spending with category, note and date, add, edit and delete, this month's income, spent and left, balances per account, and spending by category. Everything is stored as plain files: `accounts.json` and one `<year>.csv` in the `Zimma` folder (the app's private folder on Android, `~/Zimma` on the desktop). Amounts are kept in minor units.
