# N-12 Bank CSV import

Do after N-01, N-03, N-05 (all done or in this round). Not built yet.

Flow: pick a file (`OpenDocument`) -> detect delimiter (comma, semicolon, tab) and strip a UTF-8 BOM -> choose the account -> map columns (date, amount or debit and credit, note, category optional) with a saved preset per bank -> preview table with a status per row (new, duplicate, error) -> import as one batch.

Rules:
- Amounts go through `parseMoneyResult`; ambiguous or invalid rows are shown as errors, never guessed.
- Date formats are chosen explicitly in the mapping (dd/MM/yyyy, yyyy-MM-dd, MM/dd/yyyy), no auto-guess for ambiguous days.
- Duplicate detection on (date, amount, normalised note, account) against existing transactions and within the file.
- Every imported row gets `batch` id in its transaction id prefix (`i-<batch>-<n>`), so the whole batch can be undone with one action, and a second import of the same file adds nothing.
- Writes one year file at a time, once, after the user confirms the count.
- A `Csv.parse` test file first: BOM, semicolons, quotes, CRLF, embedded newlines.

Out of scope: OFX/QIF, auto-categorisation, bank connections.
