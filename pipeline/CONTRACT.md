# Pipeline contract (supplied with the pipeline release; do not edit)

The supplied checks in `tests/project/` call your code through the names below. Everything else
inside `src/payments_pipeline/` is your design. All data is synthetic: fictional suppliers,
invoices and a stubbed payment provider. No real money, bank details or live services.

Money is always a whole number of cents in an `int`. Never use `float` for an amount (`Decimal`
is allowed while parsing, but every function below returns `int` cents). USD only.

## `payments_pipeline.amounts`

| Name | Behaviour |
|---|---|
| `parse_amount_cents(text) -> int \| None` | After trimming spaces: one or more ASCII digits, `.`, exactly two ASCII digits. Returns cents (`"1234.50"` -> `123450`). Returns `None` for anything else: `None`, `""`, `"1.5"`, `"1,000.00"`, `"-5.00"`, `"+5.00"`, non-ASCII digits, zero, or more than `9999999.99`. |
| `format_cents(cents) -> str` | Two decimals: `123450` -> `"1234.50"`, `5` -> `"0.05"`, `0` -> `"0.00"`, `-5` -> `"-0.05"`. |
| `MAX_CENTS` | `999_999_999` |

## `payments_pipeline.importer`

The dataclasses are already declared in the starter; keep their field names.

- `read_invoice_export(path) -> InvoiceImport`: reads a `.csv` (header
  `supplier_id,invoice_number,amount,currency,description`) or a `.json` file (a list of objects with
  those keys). Lines are numbered from 1 = first data row / first array element.
  - `InvoiceImport.valid`: `InvoiceRow`s in file order. `supplier_id` is trimmed and upper-cased and
    must match `SUP-` + 4 digits; `invoice_number` is trimmed and non-blank; `invoice_key` is
    `SUPPLIER|INVOICE` with the invoice upper-cased and all spaces removed (`"SUP-0042|INV7781"`);
    `amount_cents` from `parse_amount_cents`; `currency` is `"USD"` (trimmed, case-insensitive on input);
    `description` is the trimmed text (may be empty).
  - `InvoiceImport.rejected`: `Rejected(line, reason)` in file order. Check in this order and record the
    first reason only: `malformed_row` (wrong number of columns; a JSON element that is not an object, lacks a key or has a non-text value),
    `bad_supplier`, `missing_invoice`, `malformed_amount`, `non_usd`, `duplicate` (an invoice key already
    accepted from an earlier line of the same file).
- `read_settlement_statement(path) -> SettlementImport`: a `.csv` (header
  `reference,provider_payment_id,amount,currency,settled_on`) or `.json` list.
  - `valid`: every well-formed line as a `SettlementRow` in file order, INCLUDING repeated references and
    non-USD currencies (reconciliation reports those). `reference` is trimmed and upper-cased
    (`PR-<request id>`), `currency` trimmed and upper-cased.
  - `rejected`: `Rejected(line, reason)` with `malformed_row`, `missing_reference` or `malformed_amount`.

## `payments_pipeline.staging` (PostgreSQL)

Every function takes an open psycopg connection and commits its own work.

- `create_staging_tables(conn)`: creates, if missing, in the connection's current schema:
  - `staging_invoices(invoice_key TEXT PRIMARY KEY, supplier_id TEXT, invoice_number TEXT, amount_cents BIGINT NOT NULL, currency TEXT NOT NULL, description TEXT, category TEXT, needs_review BOOLEAN, source_file TEXT)`
  - `staging_settlements(reference TEXT PRIMARY KEY, provider_payment_id TEXT, amount_cents BIGINT NOT NULL, currency TEXT NOT NULL, settled_on TEXT)`
  You may add constraints and columns.
- `load_invoices(conn, rows, source_file) -> int`: inserts rows whose `invoice_key` is not staged yet;
  returns how many were inserted. Loading the same rows again inserts 0 and changes nothing.
- `load_settlements(conn, rows) -> int`: same, keyed by `reference` (the first line of a repeated
  reference wins).
- `staging_totals(conn) -> dict`: `{"invoice_count", "invoice_cents", "settlement_count", "settlement_cents"}` (all `int`).

Connection code: Standard writes `payments_pipeline.db.connect(env=None)` and
`payments_pipeline.db.conninfo_from_env(env)`. `conninfo_from_env` returns a nonblank
`PIPELINE_DATABASE_URL` unchanged when present. Otherwise it builds libpq conninfo from `PGHOST`,
`PGPORT`, `PGDATABASE`, `PGUSER` and `PGPASSWORD`, using `localhost`, `5432` and `postgres` when the
first three are blank or absent and omitting blank user/password values. Nothing in `src/` may import
`support`. Fast uses the supplied `support/db_connection.py`.

## `payments_pipeline.categorise`

- `ALLOWED_CATEGORIES`: `("office_supplies", "it_hardware", "software", "facilities",
  "professional_services", "travel", "logistics")`.
- `build_prompt_payload(description) -> dict`: exactly the keys `instruction` (fixed text of your choice
  that names the allowed categories and asks for `{"category": ...}` JSON), `categories`
  (the allowed list) and `text` (the description, unchanged). Nothing else: no supplier, invoice
  number or amount ever reaches the AI provider.
- `AiClient(base_url, timeout_seconds=2.0)`; `AiClient.from_env()` reads `AI_PROVIDER_BASE_URL` and
  `AI_PROVIDER_TIMEOUT_SECONDS`. `complete(payload) -> object` POSTs the payload as JSON to
  `{base_url}/v1/categorise` and returns the decoded JSON response; raises `AiProviderError` on a
  timeout, a non-2xx response or a body that is not JSON.
- `validate_ai_output(raw) -> CategoryResult`: accepted only when `raw` is a dict with exactly one key,
  `category`, whose value is one of `ALLOWED_CATEGORIES`. Anything else (unknown category, extra
  fields, amount-like or empty text, wrong type) gives `CategoryResult(category=None, needs_review=True)`.
- `categorise_invoice(row, ai) -> CategorisedInvoice`: sends only `build_prompt_payload(row.description)`,
  validates the answer, and returns the ORIGINAL row unchanged with the result. A provider error also
  gives `needs_review=True`. AI output can never change an amount, currency, supplier, invoice or status.

## `payments_pipeline.spring_client`

- `SpringApiClient(base_url, token, timeout_seconds=2.0)`; `SpringApiClient.from_env()` reads
  `SPRING_API_BASE_URL`, `SPRING_API_TOKEN`, `SPRING_API_TIMEOUT_SECONDS` (default 2).
- Every call sends `Authorization: Bearer <token>` and uses the timeout.
- `sent_requests() -> list[dict]`: `GET {base}/payment-requests?status=SENT`, the JSON array.
- `get_request(request_id) -> dict | None`: `GET {base}/payment-requests/{id}`; `None` on 404.
- Failures raise `SpringApiError` whose `kind` is `"unauthorized"` (401/403), `"timeout"`,
  `"unavailable"` (connection refused or 5xx) or `"bad_response"` (other status or invalid JSON).

## `payments_pipeline.reconcile`

`reconcile(statement, requests) -> list[Discrepancy]` where `statement` is a `SettlementImport` and
`requests` are Spring API payment-request views (`id`, `amount` text, `currency`, `status`).
Statement lines are checked in line order, first match wins:
`malformed_line` (from `statement.rejected`), `duplicate_line` (reference already seen on an earlier
valid line), `unknown_reference` (no request with id `<n>` for `PR-<n>`), `currency_mismatch`
(not `USD`), `amount_mismatch` (cents differ). Then every request with status `SENT` that has no
matched, amount-mismatch or currency-mismatch line gives `missing_line`, in request id order.
Matched lines produce nothing. `Discrepancy(kind, line, reference, request_id, statement_cents, expected_cents)`
uses `None` where a value does not apply (`request_id` is the matching request's id whenever one exists, so a
`duplicate_line` of `PR-1` carries `1`; `unknown_reference` and `malformed_line` carry `None`). Malformed and line discrepancies come first, in line order.

## `payments_pipeline.service` (FastAPI)

`create_app(spring, statements_dir) -> FastAPI` (`spring` is a `SpringApiClient`-like object;
`statements_dir` a folder of settlement statements). `app = create_app(SpringApiClient.from_env(), ...)`
is how you run it (see README.md).

- `GET /health` -> 200 `{"status": "ok"}` (does not call Spring).
- `GET /discrepancies?statement=NAME` -> 200
  `{"statement": NAME, "discrepancies": [{"kind","line","reference","requestId","statementAmount","expectedAmount"}], "summary": {"<kind>": count}}`
  (amounts as two-decimal text or `null`; `summary` lists only kinds that occur).
- `GET /discrepancies/{reference}?statement=NAME` -> 200 with only that reference's discrepancies, 404 when it has none.
- NAME must be a file directly inside `statements_dir` (no `/`, `\` or `..`); otherwise 400. 404 when the file does not exist.
- Spring failures: `unauthorized` -> 502, `timeout` -> 504, `unavailable` / `bad_response` -> 502, each with
  a JSON `{"detail": "..."}` that never contains the token.
