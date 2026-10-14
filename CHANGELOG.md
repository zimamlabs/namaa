# Changelog

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
