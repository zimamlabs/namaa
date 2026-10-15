# Changelog

## Unreleased

- **Back up and restore.** Settings can back up everything (accounts, transactions, budgets, repeating rules, debts) into one file you save anywhere through the system screen, no permission needed. The backup is read back and checked. Restore shows what it will add, saves your current data first in `safety-backups/`, adds only what is missing by id and never deletes or replaces. Exports can be saved to another place too.
- Android system backup is switched off, so financial files are never uploaded to a Google account behind your back; the in-app backup is the way.

- **Different currencies are never added together.** Home shows a balance per currency; the month figures, charts and Report show one currency at a time (chips to switch); each budget counts only its own currency; debts are totalled per currency. A transfer between accounts of different currencies is refused with a message. Existing files read unchanged.

- **Nothing vanishes after access returns.** Transactions and accounts added while Namaa was using its private folder (all-files access off) used to be hidden once the shared folder came back. They are now merged in by id, once, without touching or deleting the kept files; anything that cannot be merged is listed on Home.

- **Repeating rules.** Editing the start date of a rule no longer adds a second transaction for a month or week already covered. A rule that starts far in the past asks before adding many transactions at once, and the catch-up now writes each year file once, so it is fast.

- The previous product name is gone from the app, the README and the docs. Data from earlier versions is still found and copied.

- **Safer CSV exports.** Text that a spreadsheet would run as a formula (starting with = + - or @) is exported as plain text, and there is a new currency column. Your own year files are not changed.

- **Transfers stay balanced.** Saving a transfer writes both legs in one write. Tapping a transfer opens a transfer editor that changes both legs together (a single leg can no longer be edited alone).
- **Every delete asks first**, and says what is lost: the matching transfer leg, a debt's payments, a rule's kept transactions.

- **A file Namaa cannot read is never treated as empty.** If accounts.json, budgets.json, recurring.json or debts.json is damaged, Namaa keeps a copy (`<name>.corrupt-<time>`), shows a recovery screen and writes nothing over it until you fix the file or choose to start without it. A year file with unreadable rows is copied whole before it is ever rewritten, and Home says so.

- **Amounts are no longer misread.** "1,234" used to be saved as 1.23. Namaa now accepts grouping only when it is unambiguous ("1,234.50", "1.234,50", "1,234,567"), refuses "1,234" and "12.999" with a message instead of guessing or cutting digits, and refuses amounts too large to store.

## 0.5.1

- A new app icon.

## 0.5.0

- **Arabic and right-to-left.** Namaa now has an Arabic interface with a mirrored layout (Settings, Language: System, English or العربية, applied at once). Menus, dialogs, the tour, About, the built-in categories and dates are in Arabic, and the charts run right to left. Digits can be Western (0 1 2 3) or Arabic-Indic (٠ ١ ٢ ٣). You can type amounts in either kind of digit.

## 0.4.0

- **A new name: Namaa** (نماء, growth), with a new icon. The Android application id and the signing key are unchanged, so it installs as an update.
- **One folder, new name.** Your data is copied into `Zimam/Namaa` from the folders earlier versions used (checked file by file; nothing is overwritten, a clashing older file is kept beside as `name (from <old folder>).ext`; the old folders stay as a backup, and Settings can remove one only after checking again).
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
- **One Zimam folder.** With All files access your data lives in one Zimam folder on the phone (and in your home folder on the desktop). Older folders are copied in and verified by file count, size and SHA-256, and left as a backup; nothing is deleted or overwritten. Settings can remove the old folder after checking again and asking.
- **A calendar date picker** instead of typing a date.
- **A first-run tour** and a one-time What's new after each update, both reachable again from Settings.
- **The final icon** on the launcher (adaptive and themed), the splash and About, and a Settings page with theme and default currency.

## 0.1.2

- A better splash screen: the name sits clearly apart from the logo, and the logo and name ease in.

## 0.1.1

- **New icon.** A wallet with the Zimam dot, on the app icon and the splash screen.

## 0.1.0

- First release: accounts (a plain Cash account is made on first launch; add and rename more), income and spending with category, note and date, add, edit and delete, this month's income, spent and left, balances per account, and spending by category. Everything is stored as plain files: `accounts.json` and one `<year>.csv` in the app's data folder. Amounts are kept in minor units.
