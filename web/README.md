# web/ — browser client (vanilla HTML, CSS and JavaScript)

Training project, synthetic data only. The Spring API stays the authority for every payment rule;
the browser only helps people use it.

## Commands (run in `web/`)

```sh
npm ci                     # once; installs the pinned tools from package-lock.json
npm test                   # your own tests only
npm test -- standard-w11   # Standard: plus the supplied week 11 checks
npm test -- standard-w12   # Standard: plus weeks 11 and 12
npm test -- fast-w09       # Fast: plus the supplied week 9 checks
npm test -- fast-w10       # Fast: plus weeks 9 and 10
npm run prefetch           # once, with network: the pinned headless Chromium (Standard W11 responsive check)
npm run dev                # dev server on http://localhost:5173, API calls forwarded to http://localhost:8080
```

`npm run dev` forwards `/payment-requests`, `/approval-queue`, `/exception-cases`, `/auth`, `/me` and
`/actuator` to your Spring API, so the browser code calls same-origin paths such as `fetch('/payment-requests')`.
Set `API_BASE_URL` to use another API address. If Chromium is not installed, the responsive check is
skipped with a message; it is not skipped when the course runs it.

## Who owns what

| Path | Owner |
|---|---|
| `index.html`, `src/**`, your tests (`tests/*.test.js`, `src/**/*.test.js`) | you |
| `package.json`, `package-lock.json`, `vite.config.js`, `vitest.config.mjs`, `tests/run.mjs`, `tests/project/**` | supplied; do not edit |

## What the supplied checks look for

Week 11 (Standard) / week 9 (Fast): `index.html` and the money helpers.

- `lang`, a `<title>`, a `width=device-width` viewport; one page header, one `<main>`, a footer, one `<h1>`.
- One form with fields named `supplierId`, `invoiceNumber` and `amount`, each labelled and `required`.
  `supplierId` has a `pattern` accepting `SUP-` plus four digits; `amount` is a text field with
  `inputmode="decimal"` and a `pattern` accepting digits, a dot and exactly two digits. USD is the only currency
  (a `currency` field offering only `USD`, or the text USD in the form). A submit button with text.
- A `<table>` in `<main>` with a caption (or accessible name) and `<th scope="col">` headers including Amount and Status.
- `src/money.js`: `parseAmountToCents(text)` (integer cents, or `null` when invalid), `formatCents(cents)`, `sumCents(values)`.
- Standard only: `src/queue.js` `filterByStatus(requests, status)` and `sortByAmount(requests, 'desc' | 'asc')`,
  and the page fits 360px and 1280px screens without sideways scrolling, with at least one `@media` rule
  (checked in headless Chromium).

Week 12 (Standard) / week 10 (Fast): the page talks to the API (stubbed in the checks; no network).

- `src/queue-page.js` `startQueuePage(root, { fetch, token })` loads the queue with `GET /payment-requests`
  (or `/approval-queue`), sends `Authorization: Bearer <token>` when a token is given, and renders one element
  per request with `data-request-id="<id>"` showing supplier, invoice, amount text and status.
  Invoice text is shown as text, never as HTML. An empty queue says "No payment requests".
- Each SUBMITTED request has an Approve button that calls `POST /payment-requests/<id>/approve` and shows the
  status from the response. A refusal (403, 409) or failure shows the server's `detail` in a `role="alert"`
  element, leaves the shown status unchanged and shows no success message.
- A 401 on loading shows an alert asking the user to sign in; 500 and network errors show an alert.
- Standard only: a `<select>` labelled Status filters the visible rows.
- `src/request-form.js` `startRequestForm(form, { fetch, token })` posts
  `{"supplierId","invoiceNumber","amount","currency":"USD"}` (amount as the typed two-decimal text) to
  `POST /payment-requests`; success goes in a `role="status"` message, a 400 or 409 shows the `detail` and any
  field `errors[].message` in a `role="alert"` element.
- Your own Vitest test file (for example `tests/money.test.js`) imports `src/money.js` or `src/queue.js` and
  uses `expect`. It runs with every `npm test`.

The API shapes are in `../docs/contracts/`.
