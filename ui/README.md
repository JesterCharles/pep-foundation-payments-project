# ui/ — React operational UI

The React client for the Payment Approval and Reconciliation Platform (training project, synthetic data,
USD only). It talks to **your** Spring API through the Vite dev proxy; it never decides a payment rule on
its own: the server stays the authority, and the UI explains what the server said.

| Track | Week | What the supplied checks cover |
|---|---|---|
| Standard | W12 | first components: `PaymentRow` and `PaymentQueue` from props, exact amounts, empty message |
| Standard | W13 | routed submit, correct-with-reason, approve and reject flows; server refusals; exception resolution; loading/error; no delete; your own Testing Library tests |
| Standard | W14 | sign-in, 401 and role redirects, sign-out, approver queue without own requests, approve hidden above the limit |
| Fast | W10 | routed submit, correct-with-reason, approve and reject flows; refusals; loading/error; no delete (your Vitest helper test is checked in `web/`) |
| Fast | W11 | the W14 sign-in and role checks, exception resolution with the signed-in user shared through context, your own Testing Library tests |

## Commands (run in `ui/`)

```sh
npm ci                     # once, installs the pinned versions from package-lock.json
npm run dev                # http://localhost:5173, API calls proxied to http://localhost:8080
npm test                   # your own tests only (passes when you have none yet)
npm test -- standard-w13   # your tests + supplied Standard checks for W12 and W13
npm test -- fast-w11       # your tests + supplied Fast checks for W10 and W11
```

Selections are cumulative within your track, and a selection never runs the other track's checks.
Every selector from `standard-w07` through `standard-w15` and `fast-w07` through `fast-w11` is valid.
The table above lists the weeks that add UI-specific supplied checks; an earlier valid week runs only
your ordinary learner test discovery.
The supplied checks fail on the starter on purpose (fail-first); they pass once the week's work is done.

## Paths

- **Yours:** everything under `ui/src/` (the starter files are only a beginning), and your own tests: any
  `*.test.js` / `*.test.jsx` under `src/` or `tests/` outside `tests/project/`.
- **Supplied (leave as released):** `package.json`, `package-lock.json`, `index.html`, `vite.config.js`,
  `vitest.config.js`, `tests/setup.js`, `tests/run.mjs`, `tests/project/**`.

## Module and screen contract the supplied checks use

Modules (keep these paths and exports):

- `src/App.jsx` — default export `App`. It renders the routes and any providers (for example your
  `AuthProvider`) but **no router**: `src/main.jsx` and the supplied checks wrap it in one.
- `src/components/PaymentRow.jsx` — default export; props `id`, `supplierId`, `invoiceNumber`, `amount`,
  `status`; renders one `<tr>`. Show the amount text exactly as the API sent it (`"9999999.99"`, never a
  rounded or comma-formatted number); a `USD` prefix is fine.
- `src/components/PaymentQueue.jsx` — default export; prop `requests`; a `<table>` with `<thead>` column
  headers (supplier, invoice, amount, status) and one row per request in a `<tbody>`, or the text
  "No payment requests" and no table when the list is empty.
- `src/api/client.js` — named export `approveRequest(id, token)`: resolves to the updated request, or
  rejects with an `Error` carrying `status` (for example 403) and `problem` (the ProblemDetail body).
- Fast W11 only: `src/auth/AuthContext.jsx` — named exports `AuthProvider` and `useAuth`. `useAuth()`
  returns an object whose `user` is `null` before sign-in and, after
  `await signIn(userId, password)` succeeds against `POST /auth/login`, is the response's
  `{ userId, roles, approvalLimit }`.

Screens and labels (accessible names; wording may vary in case):

| Route | Who | Must show |
|---|---|---|
| `/sign-in` | everyone (W14 / Fast W11) | inputs labelled **User ID** and **Password** (`type="password"`), button **Sign in**; a refusal in `role="alert"` |
| `/requests` | requester | their requests, each with a link whose name contains the invoice number, to `/requests/:id` |
| `/requests/new` | requester | inputs **Supplier ID**, **Invoice number**, **Amount** (currency is USD); button **Submit request**; after a 201, go to `/requests/<new id>` |
| `/requests/:id` | any signed-in user | an element labelled **Status** (for example `<strong aria-label="Status">`); a list named **History** (one item per event with action and actor); requester on own SUBMITTED request: **New amount**, **Correction reason**, button **Correct amount**; approver on someone else's SUBMITTED request: button **Approve** (only within the approval limit from W14 / Fast W11), **Rejection reason** and button **Reject** |
| `/queue` | approver (W14 / Fast W11) | the approval queue from `GET /approval-queue`, without the approver's own requests, with links like `/requests` |
| `/exceptions` | payables clerk | one `<article aria-label="Exception case <id>">` per case with a link to its request, the reason, a **Resolution note** field and a **Resolve** button; once resolved the case stays on screen showing "Resolved by <user id>" |

Messages: errors and refusals in `role="alert"` (show the ProblemDetail `detail` and any field `errors`);
success messages in `role="status"`. While data loads, show text containing "Loading". Never offer a
delete or remove control, and never send a `DELETE` request.

Role screens (W14 / Fast W11): after sign-in a requester lands on `/requests`, an approver on `/queue`, a
payables clerk on `/exceptions`. A 401 from any call means "sign in again" (go to `/sign-in`); opening a
screen for another role sends the user to their own screen. Sign-out returns to `/sign-in` and leaves no
token in `localStorage` or `sessionStorage`.

Before sign-in exists (Standard W13, Fast W10) the app asks `GET /me` who the user is; the supplied stub
answers with a fixed synthetic user. The same call answers 401 once sign-in is required.

## How the supplied checks run

`tests/project/support/stubApi.js` replaces `fetch` with an in-memory stand-in for the Spring API
(routes and JSON shapes from `docs/contracts/`); nothing reaches the network. On purpose, its approval
queue also returns the approver's own requests, so the UI must leave them out. `tests/setup.js` blocks real
network access in every test: in your own tests, assign `globalThis.fetch` inside the test (or a
`beforeEach`), not at module level.

Synthetic users: REQ-0001, REQ-0002 (requesters), APR-0001 (limit 5000.00), APR-0002 (25000.00)
(approvers), PAY-0001 (payables clerk); local password `change-me-local-only` (stub and local dev only).
