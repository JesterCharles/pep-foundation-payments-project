# Synthetic sample exports (supplied; do not edit)

Every supplier, invoice, reference and amount here is fictional training data. No real company,
bank detail or payment appears. The supplied checks generate their own larger sets at test time;
these files are for running and demonstrating your pipeline by hand.

| File | What it contains |
|---|---|
| `invoices-clean.csv` | Eight valid USD invoices, including the smallest (0.01) and largest (9999999.99) amounts |
| `invoices-dirty.csv` | A duplicate with case and space variety, malformed amounts (`1,200.00`, `12.5`, `-5.00`, `0.00`, blank, too large), a non-USD row, a lower-case `usd` row (valid), a bad supplier id, a blank invoice number, a short row, and a description with markup and amount-like text |
| `invoices-dirty.json` | The JSON variant: duplicate, three decimals, non-USD, an amount sent as a JSON number (a malformed row: every value is text), a non-object element |
| `settlements-clean.csv` | Provider settlement lines that match requests 1-3 in `spring-requests.json` |
| `settlements-mixed.csv` | Matched, amount mismatch, currency mismatch, unknown reference, repeated reference, missing reference, malformed amount and a short row |
| `settlements-mixed.json` | The JSON variant (lower-case reference and currency are normalised) |
| `spring-requests.json` | Payment-request views as your Spring API returns them (`GET /payment-requests?status=SENT`), for a stub API |
