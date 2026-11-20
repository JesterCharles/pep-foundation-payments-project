# Payment Approval and Reconciliation Platform: Project 2 PRD (Standard and Fast)

This document describes Project 2 for the Standard and Fast tracks: the product you build, its rules, the contracts
and supplied checks that define each week's work, and how the milestones are assessed. For routes, JSON shapes,
status codes, tables, paths and supplied checks, the repository is authoritative where it differs from this document:
the contracts in `docs/contracts/` and `pipeline/CONTRACT.md`, `RELEASES.md`, `LEARNER-OWNED.md` and the supplied
checks.

Companion documents in this repository: `docs/tracks/standard.md` and `docs/tracks/fast.md` (the weekly table with
taught cutoffs, due days and marks per criterion), and `docs/milestone-maps/standard.md` and
`docs/milestone-maps/fast.md` (each week's deliverables, what is supplied, the taught topics each criterion builds on,
what is not required yet, and where to look). The full milestone criteria are in your course materials.

## 1. Product

A fictional company pays fictional supplier invoices in US dollars. Participants start from a working payment
approval service (Javalin and plain JDBC, in `api/`) and turn it, week by week, into a platform:

- a Spring Boot API with Spring Data JPA, Flyway migrations, DTOs, atomic approval transactions, ProblemDetail
  errors, JWT authentication with separation of duties, and an idempotent client for a stubbed payment provider;
- a Python import pipeline for invoice exports and settlement statements, with validated AI categorisation of a
  non-sensitive description, and a FastAPI reconciliation service that calls the Spring API;
- a plain HTML/CSS/JS browser client and a React operational client;
- a Compose description, Dockerfiles and (Fast required, Standard optional) a CI workflow, all checked statically.

Everything is synthetic. No real money, bank, payment provider, banking credential or live AI service is involved,
and the project makes no regulatory or compliance claim. The baseline's source code calls the company "Northwind
Components (fictional)".

## 2. Users and roles

| Role | Synthetic users | May do |
|---|---|---|
| Requester | REQ-0001, REQ-0002, REQ-0003 | Submit payment requests; correct the amount of their own SUBMITTED request with a reason; cancel |
| Approver | APR-0001 (limit 5,000.00), APR-0002 (25,000.00), APR-0003 (9,999,999.99) | Approve or reject within their limit, never their own request; also submit and cancel |
| Payables clerk | PAY-0001 (from the security week) | Send approved payments to the provider; open and resolve exception cases with a note |

Roles and limits come from the server's user directory, never from the client. Once sign-in exists every user's
local development password is `change-me-local-only` (a published synthetic value, not a secret). The learner is the
developer of the platform; the course assessor reads the repository.

## 3. Domain rules (every week)

- **USD only.** Every request and statement line carries `USD`; anything else is refused (400) or reported as a
  currency discrepancy, never converted.
- **Exact money.** Amounts travel as two-decimal text (`"1234.50"`) and are held as whole cents in `long` / `BIGINT`
  (Python `int`). Valid amounts are 0.01 to 9,999,999.99. Floating point never decides an amount; BigDecimal or
  Decimal are acceptable while parsing but never required.
- **Supplier id** is `SUP-` plus four digits (trimmed, upper-cased). The **invoice key** is `SUPPLIER|INVOICE`,
  upper-cased with spaces removed. A second request with the same key is a duplicate (409). An invoice is paid at
  most once.
- **Approval controls.** Approval up to and including the approver's limit succeeds; one cent over is refused (403).
  Nobody approves their own request (403). A refused approval leaves the status unchanged and is recorded as an
  `approval-refused` event.
- **Lifecycle.** `SUBMITTED -> APPROVED | REJECTED | CANCELLED`; `APPROVED -> CANCELLED | SENT | EXCEPTION`. SENT and
  EXCEPTION arrive with the provider client (Standard W09, Fast W08): an accepted send moves APPROVED to SENT, a
  provider REJECTED answer moves it to EXCEPTION. A payables clerk may open an exception case while a request is
  APPROVED, SENT, PAID or EXCEPTION, which moves it to EXCEPTION (`W13-exception-cases.yaml`). The baseline also moves
  `APPROVED -> PAID` when a settlement for exactly the amount is recorded. Any other move answers 409 and changes
  nothing.
- **Attributable history.** Every change and every refusal is an event `(seq, actorId, action, detail)`. Corrections
  and exception resolutions need a reason or note. Nothing is ever deleted; no route accepts DELETE.
- **Idempotent payment.** One idempotency key per request (`payment-request-<id>`) on every attempt, so a retry never
  pays twice. A provider REJECTED answer moves the request to EXCEPTION without a retry. When the configured attempts
  are used up without an answer, the API answers 502 and the request stays APPROVED, so it can be sent later.
- **Secrets** come from the environment and are never committed.

## 4. Scope

In scope: the routes, tables, pipeline functions and UI flows in the contracts below; the participant's own tests
where a week requires them; a milestone note and AI-use log each week; README run instructions and a walkthrough
outline at the end.

Out of scope: real providers, banks, currency conversion, fees, partial payments, accounting treatment, concurrency
control (tests are sequential), retry or provider libraries, running containers (no container engine, registry,
cloud account or paid CI runner is needed or used), and Foundation Project 1, which is not repeated, reused or ported.

## 5. Starting point, learner-owned and supplied paths

Both tracks start Project 2 on their first Spring Boot day (Standard S1, Fast FT1) from the **supplied payments
baseline**: a complete Javalin + JDBC payment approval service with a passing test suite (202 tests on embedded H2).
It is participant-editable starting source, not a protected supplied path. The repository ships as **one snapshot**:
every supplied file for every week is present from day one; a "release" only names what becomes due that week and
which supplied checks start to apply. LEARNER-OWNED.md is the authority; in summary:

| Kind | Paths |
|---|---|
| Starting source (editable from day one) | `api/src/main/java/payments/**` (baseline packages, `App.java`, `PaymentsApplication.java`), `api/src/main/resources/db/schema.sql`, baseline tests under `api/src/test/**/payments/baseline/**` |
| Learner-owned (no release overwrites them) | `api/src/main/java/**`, `api/pom.xml` (participant pastes the FLYWAY and SECURITY additions from RELEASES.md), `api/src/main/resources/application*.yml`, `api/src/main/resources/db/migration/**`, own tests outside the supplied `project/` folders, `pipeline/src/**`, `pipeline/pyproject.toml`, `web/index.html`, `web/src/**`, `ui/src/**`, `deploy/compose.yaml`, `api/Dockerfile`, `pipeline/Dockerfile`, `.github/workflows/ci.yml`, `docs/milestones/WNN.md`, `docs/ai-use-log.md`, `README.md`, `docs/walkthrough.md`, optional `docs/decisions.md`, `.gitignore`, `.env.example` |
| Supplied (do not edit; hashes in `SUPPLIED.sha256`) | `RELEASES.md`, `LEARNER-OWNED.md`, `scripts/**`, `docs/contracts/**`, `docs/tracks/**`, `docs/onboarding.md`, `docs/templates/**`, `docs/prd.md`, `docs/milestone-maps/**`, every `tests/project/**` and `api/src/test/java/payments/project/**`, `pipeline/CONTRACT.md`, `pipeline/support/**`, `pipeline/data/samples/**`, the web/ui/deploy build files |

Fast learners use the supplied `pipeline/support/db_connection.py`; Standard learners write their own psycopg
connection code. Reference solutions, reviewer material and grading data are not in the repository.

## 6. Architecture and repository layout

| Path | Component | Toolchain and test command (run in the folder) |
|---|---|---|
| `api/` | Baseline (`payments.App`, Javalin, port 7070) and the Spring Boot API (`payments.PaymentsApplication`, port 8080) | JDK 25, Maven 3.9; `mvn -q test -Dmilestone=<track>-wNN` |
| `pipeline/` | `payments_pipeline` package: import, staging, categorisation, reconciliation, FastAPI service | Python 3.12+; `python -m pytest --milestone <track>-wNN` |
| `web/` | Plain HTML/CSS/JS client (Vite) | Node 24.15+, npm 11; `npm test -- <track>-wNN` |
| `ui/` | React client (Vite) | same; `npm test -- <track>-wNN` |
| `deploy/` | Compose description and static checks | same; `npm test -- <track>-wNN` (structure only, no engine) |
| `docs/` | This PRD, contracts, track tables, milestone maps, onboarding, templates, milestone notes | - |
| `scripts/` | `prefetch.sh` (once, needs network; offline afterwards), `check-milestone.sh <track> <week>`, `reset.sh`, `verify-starter.sh` | - |

## 7. APIs and data

The contracts in `docs/contracts/` are authoritative for routes, JSON shapes, status codes and tables.

### 7.1 Spring API routes

| Route | Contract | Required from (Standard / Fast) |
|---|---|---|
| `POST /payment-requests`, `GET /payment-requests[?status=&supplierId=]`, `GET /payment-requests/{id}`, `POST .../{id}/approve`, `POST .../{id}/reject`, `POST .../{id}/cancel` | `W07-openapi.yaml` | W07 / W07 |
| `POST /payment-requests/{id}/correct` (requester, SUBMITTED only, with reason); `GET /approval-queue` (SUBMITTED, paginated, sortable, each with history, no N+1) | `W08-openapi.yaml` | W08 / W08 |
| `POST /auth/login`, `GET /me`, `POST /payment-requests/{id}/send` (payables clerk), ProblemDetail errors, JWT security; approver queue excluding own requests (`/approval-queue` `x-later`, checked by the shared `SecurityChecks` in `StandardW09SecurityTest` and `FastW08SecurityTest`) | `W08-openapi.yaml` items `x-required-from {fast: W08, standard: W09}`, restated in `W09-openapi.yaml` | W09 / W08 |
| `GET /actuator/health` with a `provider` component | `W09-openapi.yaml` | W09 / not on Fast |
| `GET /exception-cases`, `POST /exception-cases` (payables clerk), `POST /exception-cases/{id}/resolve` (note required) | `W13-exception-cases.yaml` | UI against a stub in W13; the learner's API is checked by the W15 scenario / **UI against a stub only in W11; no Fast check runs these routes against the learner's API** (see 13) |

Identity: until the security week the `X-Actor-Id` header names a synthetic actor; it is a stub, not
authentication. From the security week: `Authorization: Bearer <JWT>`, HS256, verified with `security.jwt.secret`
(env `SECURITY_JWT_SECRET`); claims `sub`, `roles` (REQUESTER, APPROVER, PAYABLES_CLERK), `exp`. Without a valid token
every route except `/auth/login` and `/actuator/health` answers 401. The acting user is the token subject. The Spring
profile `dev` keeps every endpoint open and reads `X-Actor-Id`; no other profile may.

Errors (from the security week): `application/problem+json` (RFC 9457) with `type`, `title`, `status`, `detail`, and
`errors: [{field, message}]` for validation. Status codes: 400 invalid input, 401 not signed in, 403 refused (detail
names the rule), 404 unknown id, 409 duplicate invoice or invalid move, 502 provider unavailable.

### 7.2 Persistence

| Table | Columns and constraints |
|---|---|
| `payment_requests` | `id` BIGINT key, `supplier_id`, `invoice_number`, `amount_cents` BIGINT, `currency`, `requested_by`, `status` (text), `submitted_at` (from W08). Constraints from W08: currency exactly USD; `amount_cents` 1..999999999; status in the contract set; unique (`supplier_id`, `invoice_number`) |
| `payment_events` | `request_id` -> `payment_requests(id)`, `seq` (1, 2, ... per request), `actor_id`, `action`, `detail` |
| `approvals` (W08) | `request_id`, `approver_id`, `approved_at` |

JPA persistence is required from Fast W07 and Standard W08; Flyway migrations from W08 on both tracks. Approving
writes the approval row, the APPROVED status and the `approved` event in one transaction; if any write fails none is
kept and the API answers 5xx. Extra columns must be nullable or have a DEFAULT. Supplied checks start the application
with their own in-memory H2 (PostgreSQL mode).

### 7.3 Stub payment provider (`W08-provider-stub.md` Fast, `W09-provider-stub.md` Standard)

`POST {provider.base-url}/payments` with header `Idempotency-Key: payment-request-<id>` and body
`{reference, supplierId, amount, currency}`; answer `{providerPaymentId, status: ACCEPTED|REJECTED, reason}`.
ACCEPTED -> SENT with a `sent` event; REJECTED -> EXCEPTION with `provider-rejected`, no retry; slow answer or 500 ->
retry with the same key (the stub replays the original answer); attempts used up -> 502, request stays APPROVED.
`provider.timeout` (Duration) and `provider.max-attempts` come from configuration. Sending an already SENT request answers 409. The provider runs inside
the supplied checks (`ProviderStub.java`); `PROVIDER_BASE_URL` always points at a stub.

### 7.4 Pipeline (`pipeline/CONTRACT.md`)

`payments_pipeline.amounts` (`parse_amount_cents`, `format_cents`, `MAX_CENTS`), `importer` (invoice export and
settlement statement readers with ordered rejection reasons), `staging` (PostgreSQL `staging_invoices`,
`staging_settlements`; idempotent loads; totals), `db` (Standard only: `connect`, `conninfo_from_env`),
`categorise` (`ALLOWED_CATEGORIES`, a prompt payload of exactly `instruction`, `categories`, `text`; strict output
validation; `needs_review` otherwise), `spring_client` (`SPRING_API_BASE_URL`, `SPRING_API_TOKEN`, timeout; failure
kinds), `reconcile` (`malformed_line`, `duplicate_line`, `unknown_reference`, `currency_mismatch`, `amount_mismatch`,
`missing_line`), `service` (FastAPI `GET /health`, `GET /discrepancies?statement=NAME`,
`GET /discrepancies/{reference}`; Spring failures map to 502/504 without leaking the token). AI output can never change
an amount, currency, supplier, invoice or status. Supplied sample data is under `pipeline/data/samples/`.

### 7.5 Clients and deployment

`web/`: payment-request form, queue page, money and queue helpers, then API calls through the Vite dev proxy with the
API address from environment settings. `ui/`: React components, routed workflows (submit, correct, approve, reject,
resolve exception), sign-in and role screens. `deploy/compose.yaml`: api, database and pipeline service with
health-based `depends_on`, service-name URLs and env indirection (including `PROVIDER_BASE_URL`); multi-stage
`api/Dockerfile` and `pipeline/Dockerfile`. `.github/workflows/ci.yml` ships inert (`workflow_dispatch` only).

The supplied Javalin baseline also serves `GET /reports/reconciliation` (port 7070). It is not part of any Spring
contract; reconciliation moves to the pipeline's FastAPI service.

### 7.6 Optional API checks

Your course materials may include optional, unmarked request/response checks for a locally running Spring API
(`http://localhost:8080`). They are derived from the contracts, add no requirement and are not the milestone checks.
Run them against a fresh database (invoice numbers are fixed, so a second run answers 409): the W07 and W08 sets
before SECURITY is pasted or with the `dev` profile afterwards, and the security set with any non-`dev` profile and
`SECURITY_JWT_SECRET` set. They do not cover `/send` (the stub provider runs only inside the supplied checks),
`/actuator/health`, the exception-case routes or the pipeline's FastAPI routes.

There are three sets: W07 (21 checks), W08 (11) and security (19). A combined file holds all 51 in that order; it
cannot pass in one run, because the first two sets need the `X-Actor-Id` stub and the security set needs real tokens.
The stub means no security yet, or the `dev` profile once security exists; the tokens need a non-`dev` profile. Run
the combined file as two parts. Each check is a JSON object with `name`, `method`, `path`, an optional `body`,
`expect_status`, `expect_body` and an optional `capture`, the same shape as the Foundation Project 1 checks. It also
has one key that Project 1 did not have: `headers`.

**`headers`.** An object that maps a request header name to a string value. The headers are sent with that one
request only. A check without `headers` sends no identity header, and some checks leave it out on purpose to test
exactly that. Only two header names occur:

- `X-Actor-Id`: every W07 and W08 check except one, plus two security checks that show it is ignored;
- `Authorization`: security checks only, always `Bearer ` followed by a token.

No check sets `Content-Type`. A check with a `body` relies on the tool sending it as JSON, as in Project 1.

**Placeholders.** A `{{name}}` in `path`, or inside a header value, is replaced before the request is sent. The value
comes from an earlier check in the same file: `"capture": {"name": "field"}` stores that field of the earlier check's
JSON response. Every placeholder is captured earlier in its own file, and no value passes between files. No placeholder
appears in a `body` or an `expect_body`. Project 1 used placeholders in `path` only. The sign-in user ids and
passwords are literal values in the login bodies (the local password from section 2), not placeholders.

| Placeholder | Set | Used in | Captured from |
|---|---|---|---|
| `{{r1}}` | W07 | `path` | `id` of "W07 submit a payment request" (REQ-0001, 1234.50) |
| `{{r2}}` | W07 | `path` | `id` of "W07 submit one cent over APR-0001's limit" (REQ-0001, 5000.01) |
| `{{r3}}` | W07 | `path` | `id` of "W07 approver submits a request" (APR-0002, 10.00) |
| `{{q1}}` | W08 | `path` | `id` of "W08 submit a payment request" (REQ-0001, 1234.50) |
| `{{req_token}}` | security | `Authorization` | `token` of "Security sign in REQ-0001" (`POST /auth/login`) |
| `{{apr1_token}}` | security | `Authorization` | `token` of "Security sign in APR-0001" |
| `{{apr2_token}}` | security | `Authorization` | `token` of "Security sign in APR-0002" |
| `{{pay_token}}` | security | `Authorization` | `token` of "Security sign in PAY-0001" |
| `{{s1}}` | security | `path` | `id` of "Security the actor comes from the token, not the header" (REQ-0001, 100.00) |
| `{{s2}}` | security | `path` | `id` of "Security an approver submits a request" (APR-0002, 50.00) |
| `{{s3}}` | security | `path` | `id` of "Security submit one cent over APR-0001's limit" (REQ-0001, 5000.01) |

**Which checks depend on headers.**

- **W07.** 20 of the 21 checks send `X-Actor-Id`; "W07 missing X-Actor-Id is 400" sends none. On every POST
  (submit, approve, reject, cancel) the header decides the result: it sets who submitted, and who approves under the
  limit and own-request rules. The six GET checks also send it, but the W07 contract does not take an actor on reads,
  so their result does not depend on it.
- **W08.** All 11 checks send `X-Actor-Id`. It decides the result on submit, `/correct` (requester only) and approve.
  The four `/approval-queue` checks send APR-0001, but the W08 queue takes no actor; leaving out the approver's own
  requests starts in the security week.
- **Security.** The five sign-in checks and "/me without a token" send no headers. Eleven checks send
  `Authorization: Bearer {{..._token}}`, and two send only literal values:
  - "a malformed token is 401" sends `Bearer not.a.token`;
  - "X-Actor-Id alone is ignored outside the dev profile" sends only `X-Actor-Id: REQ-0001`.

  One of the eleven, "the actor comes from the token, not the header", also sends `X-Actor-Id: APR-0003` with the
  REQ-0001 token and expects `requestedBy` to be REQ-0001.

**Status codes for missing or invalid identity.** With the `X-Actor-Id` stub (W07 and W08 sets, before
security or with the `dev` profile):

| Request | Status |
|---|---|
| `X-Actor-Id` missing | 400 |
| unknown actor (`REQ-9999`) | 400 |
| known actor without the right (another requester correcting, over the limit, own request) | 403 |

Outside `dev` (security set):

| Request | Status |
|---|---|
| no token | 401 |
| malformed token | 401 |
| `X-Actor-Id` without a token | 401 |
| wrong password at `/auth/login` | 401 |
| valid token, but the role or a rule refuses | 403, with a ProblemDetail `status` of 403 |

The contract also answers 401 for tampered, expired and wrongly signed tokens. The supplied `SecurityChecks` cover
those cases, and these optional checks do not.

**If your tool lacks support.** The two kinds of gap need different handling:

- **It ignores `headers`.** The W07 and W08 sets are then not usable as a whole. Every POST answers 400 for a
  missing actor, so the submits capture no id and every later `{{r1}}`, `{{r2}}`, `{{r3}}` or `{{q1}}` check breaks,
  GETs included. The checks that expect 400 anyway pass for the wrong reason. Every security check that sends a token
  gets 401. Run these checks by hand with the header, or skip them with the reason "tool sends no headers".
- **It sends headers but does not replace `{{...}}` inside them.** The eleven token checks would send the placeholder
  text and get 401, which is a false failure. Run them by hand: call `POST /auth/login`, copy `token` from the
  response, and send `Authorization: Bearer <token>`. Otherwise skip them with the reason "needs a captured token".
  Skipping "the actor comes from the token, not the header" also leaves `{{s1}}` unset, and two later checks use it.

**Worked example.** Three security checks, copied from the security set:

```json
[
  {
    "name": "Security sign in REQ-0001",
    "method": "POST",
    "path": "/auth/login",
    "body": {"userId": "REQ-0001", "password": "change-me-local-only"},
    "expect_status": 200,
    "expect_body": {"userId": "REQ-0001"},
    "capture": {"req_token": "token"}
  },
  {
    "name": "Security the actor comes from the token, not the header",
    "method": "POST",
    "path": "/payment-requests",
    "headers": {"Authorization": "Bearer {{req_token}}", "X-Actor-Id": "APR-0003"},
    "body": {"supplierId": "SUP-0201", "invoiceNumber": "INV-APIS-1", "amount": "100.00", "currency": "USD"},
    "expect_status": 201,
    "expect_body": {"requestedBy": "REQ-0001", "status": "SUBMITTED"},
    "capture": {"s1": "id"}
  },
  {
    "name": "Security a requester cannot approve",
    "method": "POST",
    "path": "/payment-requests/{{s1}}/approve",
    "headers": {"Authorization": "Bearer {{req_token}}"},
    "expect_status": 403,
    "expect_body": {"status": 403}
  }
]
```

1. The first check stores the `token` field of the login response as `req_token`.
2. The second sends `Authorization: Bearer <that token>` with a spoofed `X-Actor-Id`, expects the request to be
   recorded for REQ-0001, and stores the new `id` as `s1`.
3. The third sends `POST /payment-requests/<s1>/approve` with the same token, and a requester may not approve, so it
   expects 403.

## 8. Non-functional requirements

| Area | Requirement | Where checked |
|---|---|---|
| Money exactness | Integer cents end to end; no floating point decides an amount | Every area's supplied checks; criteria award 0 for float money |
| Integrity | Refusals change nothing; approval is atomic; one provider payment per request; idempotent imports | W08/W09 (Fast W08) API checks; pipeline re-run checks |
| Query efficiency | Queue-with-history statement count bounded independent of row count (N+1 fixed) | W08 queue checks |
| Security | Hashed passwords; verified JWT; actor from token; dev profile off by default; no secret in tracked files (keys ending in password/secret/token/api-key/private-key must be `${NAME}` placeholders) | Security and migration checks; static Compose/CI checks |
| Data minimisation | Only the invoice description reaches the AI provider | Pipeline checks |
| Web safety | Untrusted text rendered through safe APIs; no `innerHTML`/`dangerouslySetInnerHTML` with untrusted data | web/ui checks |
| Accessibility | Landmarks, headings, labelled controls, headed queue table, role/label-queryable UI | web/ui checks; Standard W11 responsive check uses pinned headless Chromium and is skipped when Chromium is not installed |
| Resilience | Configured timeouts and bounded retries; downstream 401/404/timeout handled without crashing | Provider and Spring-client checks |
| Offline and cost | Offline after one `scripts/prefetch.sh` run; no container engine, registry, cloud account or paid CI | README and RELEASES.md ('What is not here'); not checked by a script |
| Synthetic only | No real names, account numbers, passwords or keys in any file | Repository hygiene criterion every week |

## 9. Test and evidence model

- Supplied checks are public and are the whole executable part of each milestone. There are no hidden tests.
- Checks are selected by `<track>-wNN` and accumulate: a week's selector also runs that track's earlier weeks. A track
  never runs the other track's checks. Without a selector only the baseline suite and the participant's own tests run.
- Seeded generated inputs, exact-cents oracles, boundary cases (limit and one cent over; page 0, last page), refusal
  cases that must leave state unchanged, and idempotency by behaviour (timeout then success, duplicate-key replay,
  repeated import).
- A green check is necessary, not sufficient: assessors also read code, notes and pull requests, label each finding
  EXECUTED or STATIC, and do not credit claimed output or behaviour keyed to fixture values. A human assessor owns the
  final judgment.

## 10. Assessment

### 10.1 Weekly milestones

Standard has nine cumulative milestones (W07-W15), Fast five (W07-W11). Each carries recommended marks totalling 100.
Every milestone except the last (Standard W07-W14, Fast W07-W10) is **formative**: its marks do not count toward the
programme Final Assessment. The last milestone (Standard W15, Fast W11) is part of the Final Assessment (10.2).
A milestone requires only topics taught by its **taught cutoff** (that week's Wednesday, inclusive) and is due at the
end of that week's Friday unless stated below. Each week you also commit `docs/milestones/WNN.md` (user story served,
payment rule kept and its evidence, security check, AI use, commands run) and a `docs/ai-use-log.md` entry, and merge
through a pull request. The calendar is self-paced and identifies days by id (S, FT, A), not by date.

### 10.2 The last milestone and the Final Assessment, per track

| | Standard | Fast |
|---|---|---|
| Last milestone | W15, cutoff S38, due **end of S42** (Tuesday) | W11, cutoff FT23, due **end of FT24** (Thursday) |
| Completion time | S40 back-end completion, S41 front-end completion, S42 walkthrough rehearsal and recording | FT24 closing gaps, polish checklist, walkthrough rehearsal and recording |
| Final sitting | **S43** (programme day 73): proctored coding without AI; Project 2 graded | **FT25** (programme day 55): the same |
| After the Final | - | AWS preparation A1-A7 (programme days 56-62), a separate certification-preparation block with no Project 2 milestone, requirement or lab |

The last Project 2 milestone is part of the programme Final Assessment. How the parts of the Final Assessment are
weighted is set by the programme, not by this document.

## 11. Standard track (S1-S43, weeks 7-15)

| Week | Days | Cutoff | Due | Milestone | Marks |
|---|---|---|---|---|---|
| W07 | S1-S5 | S3 | S5 | Port the payment approval service to Spring Boot | 40/25/20/15 |
| W08 | S6-S10 | S8 | S10 | JPA persistence, migrations, DTOs, approval history and the atomic approval transaction | 25/20/30/15/10 |
| W09 | S11-S15 | S13 | S15 | Authenticated approvals, separation of duties and the idempotent payment-provider client | 15/35/24/16/10 |
| W10 | S16-S20 | S18 | S20 | Invoice and settlement import pipeline with FastAPI reconciliation service | 30/20/20/20/10 |
| W11 | S21-S25 | S23 | S25 | Accessible payment-request form, queue page and money helpers | 30/20/10/30/10 |
| W12 | S26-S30 | S28 | S30 | Operational browser client for the approval queue, and first React components | 20/25/15/10/20/10 |
| W13 | S31-S35 | S33 | S35 | React approval and exception workflows with UI tests | 40/20/10/20/10 |
| W14 | S36-S40 | S38 | S40 | Signed-in role screens and the Compose description | 35/30/15/20 |
| W15 | S41-S43 | S38 | end of S42 | Payment platform integration and walkthrough readiness | 40/30/20/10 |

Scheduled touchpoints (programme calendar): Project days S6, S27, S33, S40, S41, S42; required labs S9, S12, S34; module
checks S13, S19, S35, S39; Final S43. Other lab days are separate Standard labs.

Standard-only requirements: constructor injection across layers (S3), Spring health checks (S11, provider health in
W09), the participant's own psycopg connection code (S16). CI/CD (S39) is optional evidence for W15 and earns no
marks. Lab days are separate Standard labs, not milestone work.

### Standard W07
Port the baseline's routes, service and repository to Spring Boot REST controllers with ResponseEntity status codes,
constructor injection and an in-memory repository; no datasource settings. Check `mvn -q test -Dmilestone=standard-w07`.
### Standard W08
Paste FLYWAY; JPA entities and repositories; Flyway migrations whose constraints mirror the rules; profiles and
env-based secrets; DTOs; `/correct`; paginated `/approval-queue` with history and no N+1; atomic approval with
approval record and history. Check `standard-w08` (api).
### Standard W09
Paste SECURITY; Bean Validation and ProblemDetail; JWT roles and separation of duties (dev profile keeps the stub);
idempotent provider client with bounded retries; provider health component; at least four own control tests.
Check `standard-w09` (api).
### Standard W10
Pipeline per `pipeline/CONTRACT.md`: exact cents, own psycopg connection code, idempotent staging loads, validated
minimised AI categorisation, FastAPI discrepancy service calling the Spring API. Check `standard-w10` (pipeline).
### Standard W11
Accessible payment-request form and static queue page in `web/index.html`, responsive CSS, tested money and queue
helpers in `web/src`. Check `standard-w11` (web).
### Standard W12
Queue from the participant's API through the Vite proxy, safe rendering, specific 400/401/403/409/500/network
messages, a Vitest test of a helper, first React components in `ui/src`. Check `standard-w12` (web, ui).
### Standard W13
Routed React submit, correct, approve, reject and resolve-exception workflows with history, pending and failure
feedback, no delete path, and own Testing Library tests. Check `standard-w13` (ui).
### Standard W14
Sign-in and role screens (401 redirect, 403 screen, sign-out, approver queue without own requests, limit-aware
approve control); Compose with api, db and pipeline, healthchecks and env indirection; multi-stage Dockerfiles.
Check `standard-w14` (ui, deploy).
### Standard W15
Whole platform end to end (one payment per request, statement mismatch to an exception case, attributable
resolution), README run instructions and `docs/walkthrough.md`. Check `scripts/check-milestone.sh standard 15`.

## 12. Fast track (FT1-FT25, weeks 7-11; AWS A1-A7 separate)

| Week | Days | Cutoff | Due | Milestone | Marks |
|---|---|---|---|---|---|
| W07 | FT1-FT5 | FT3 | FT5 | Port the payment service to Spring Boot with JPA persistence | 30/20/30/10/10 |
| W08 | FT6-FT10 | FT8 | FT10 | Atomic approvals, authenticated separation of duties and the idempotent provider client | 13/22/15/20/20/10 |
| W09 | FT11-FT15 | FT13 | FT15 | Invoice and settlement import pipeline, FastAPI reconciliation service and accessible payment form | 30/30/30/10 |
| W10 | FT16-FT20 | FT18 | FT20 | Operational browser client and React approval workflows | 25/10/35/20/10 |
| W11 | FT21-FT25 | FT23 | end of FT24 | Signed-in role screens, exception workflow, Compose, CI and Project 2 completion | 25/15/25/15/10/10 |

Scheduled touchpoints: the daily Project block on every Fast-type day (FT1-FT4, FT6, FT8-FT18, FT20-FT24); FT5
error-handling brief block; FT7 API-security brief block; FT19 guided block (login and protected pages); FT24 Project 2
completion and walkthrough; FT25 Final.

Fast has no required standalone module labs; its lab days and project blocks are Project 2 implementation. No Fast
criterion requires a Standard-only topic: constructor injection, Spring health checks and psycopg connection code are
not required. One-to-many mapping (FT6) is optional in W07 and graded in W08. FT9-FT10 Python work builds ahead and is
graded in W09. W08 and W11 are the densest weeks; criteria require core steps only. Module checks fall on FT8, FT11,
FT20 and FT23. CI/CD (FT23) is required in W11.

### Fast W07
Port the baseline to Spring Boot REST controllers with ResponseEntity status codes and persist requests and history
through Spring Data JPA with derived-query filters (status, supplier). No Flyway or DTO requirement. Check `fast-w07`.
### Fast W08
Paste FLYWAY and SECURITY; Flyway schema, `/correct`, paginated `/approval-queue` with history and no N+1, atomic
approval, ProblemDetail, JWT roles and separation of duties (dev profile keeps the stub), idempotent provider client,
at least three own MockMvc tests. No Spring health check. Check `fast-w08` (api).
### Fast W09
Pipeline (exact cents, idempotent staging through the supplied connection helper, validated AI categorisation,
FastAPI discrepancy service) and the accessible payment-request form and queue page with money helpers in `web/`.
Check `fast-w09` (pipeline, web).
### Fast W10
Queue from the participant's API in `web/` with safe rendering, clear refusal messages and a Vitest helper test;
routed React submit, correct, approve and reject workflows with history, pending and failure feedback and no delete
path. Check `fast-w10` (web, ui).
### Fast W11
Sign-in and role screens, the exception workflow with Context-shared state and own Testing Library tests; Compose
with multi-stage Dockerfiles; a CI workflow that keeps secrets out; README and walkthrough; full cumulative suite on
the final commit. Check `scripts/check-milestone.sh fast 11`.

## 13. Invariants and where each is enforced

| Invariant | Standard | Fast |
|---|---|---|
| No duplicate processing | W07 duplicate 409; W08 unique constraint; W09 one provider payment per key; W10 idempotent import | W07 duplicate 409; W08 constraint and idempotent provider; W09 idempotent import |
| No approval-limit bypass | W07 refusal; W08 checks before writes; W09 limit from authenticated approver; W14 limit-aware UI with server 403 | W07; W08; W11 |
| Separation of duties | W07 own-request refusal (stub identity); W09 actor from JWT and role matrix; W14 queue excludes own requests | W07; W08; W11 |
| No contradictory states | W08 atomic approval with rollback; W09 provider REJECTED to EXCEPTION, exhausted retries leave APPROVED (502) | W08 |
| No amount or currency discrepancy | W08 BIGINT cents and USD check; W10 exact import and reconciliation; W11 exact browser helpers | W07-W08; W09 |
| Attributable history | W08 history relationship; W09 actor from token on every event; W13 reasons and notes, no delete; W15 resolution through the API | W08; W10-W11 (UI) |

Known gap: on Fast, exception-case resolution is exercised only by UI tests against a stubbed API; no Fast supplied
check runs `/exception-cases` against the participant's API (the cross-service scenario check is Standard W15 only).

## 14. Limits of the supplied checks

- Supplied checks start the API with their own in-memory H2 database and stub provider; no container engine,
  registry, cloud account or paid CI runner is used, and Docker, Compose and CI files are checked statically only.
- On Fast, server-side exception-case routes are not run by any supplied check (section 13).
- Reference solutions, reviewer material and grading data are not in this repository (RELEASES.md).
