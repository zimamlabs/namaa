# N-04 Multi-currency totals

Problem: accounts carry a currency, but every total adds raw minor units, so 100 USD plus 100 EGP shows 200.

Decision: never add different currencies together, and do not add exchange rates.

- A `Money` total is always a map `currency -> minor units` (`Totals`), built in `shared`. Pure functions, table tests.
- Home hero shows the balance per currency, one line each, largest first. With one currency it looks exactly as today.
- Month income, spent and left are per currency, in the same order. The donut and the six-month bars show the currency chosen by a small chip row (default: the preferences currency, else the first). Budgets belong to a currency (new optional `currency` field, empty means the preferences currency) and only count transactions of accounts in that currency.
- Transfers between accounts of different currencies are refused with a clear message (a conversion needs a rate, which Namaa does not have yet).
- Debts get an optional `currency` (empty means the preferences currency), totals per currency.
- Data format: new fields are optional with defaults, old files read unchanged; no migration needed. Test: an old budgets.json and debts.json still decode.

Not now: rates, converted grand total, per-currency decimals (N-15).
