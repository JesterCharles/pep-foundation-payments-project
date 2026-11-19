# Project 2 Standard milestone map

Companion to the Project 2 PRD ([docs/prd.md](../prd.md)) and the Standard track table ([docs/tracks/standard.md](../tracks/standard.md)). For each milestone it lists what you deliver, what is already supplied, the taught topics each criterion builds on, what is not required yet, and where to look in this repository. Criteria are listed by reference number, short name and marks only; the full criteria are in your course materials for each milestone.

Nine cumulative Standard milestones, one per schedule week W07-W15 (S1-S43). Cutoff = that week's Wednesday, inclusive; due = that Friday. Week 15 is S41-S43 only: cutoff S38, due end of S42, before the S43 Final. Lab days are separate Standard labs and not milestone work. W07-W14 are formative; W15, the last milestone, is the Project 2 part of the programme Final Assessment (PRD section 10).

Legend: each topic is named as in the programme calendar, with the day it is taught; every listed day is on or before the milestone's taught cutoff. `(skim)` = the topic is covered at skim depth on this track.

| Week | Days | Cutoff | Due | Title | Marks (C1..Cn) |
|---|---|---|---|---|---|
| W07 | S1-S5 | S3 | S5 | Port the payment approval service to Spring Boot | 40, 25, 20, 15 |
| W08 | S6-S10 | S8 | S10 | JPA persistence, migrations, DTOs, approval history and the atomic approval transaction | 25, 20, 30, 15, 10 |
| W09 | S11-S15 | S13 | S15 | Authenticated approvals, separation of duties and the idempotent payment-provider client | 15, 35, 24, 16, 10 |
| W10 | S16-S20 | S18 | S20 | Invoice and settlement import pipeline with FastAPI reconciliation service | 30, 20, 20, 20, 10 |
| W11 | S21-S25 | S23 | S25 | Accessible payment-request form, queue page and money helpers | 30, 20, 10, 30, 10 |
| W12 | S26-S30 | S28 | S30 | Operational browser client for the approval queue, and first React components | 20, 25, 15, 10, 20, 10 |
| W13 | S31-S35 | S33 | S35 | React approval and exception workflows with UI tests | 40, 20, 10, 20, 10 |
| W14 | S36-S40 | S38 | S40 | Signed-in role screens and the Compose description | 35, 30, 15, 20 |
| W15 | S41-S43 (partial week) | S38 | end of S42 | Payment platform integration and walkthrough readiness | 40, 30, 20, 10 |

## Standard W07 - Port the payment approval service to Spring Boot

Days S1-S5; taught cutoff S3 (Wed, programme day 33, 'Porting to Spring Boot'); due S5. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- Spring Boot port (payments.PaymentsApplication) of the supplied baseline's payment-request routes, service and repository in api/src/main/java, keeping every W07 contract rule (exact cents, USD, duplicate invoice 409, lifecycle 409, limit and own-request 403 with state unchanged)
- REST controllers returning the contract status codes (201/200/400/403/404/409) through ResponseEntity, JSON error bodies, no stack traces
- Constructor injection across controller, service and repository/policy beans; an in-memory repository; no datasource configuration
- docs/milestones/W07.md and a docs/ai-use-log.md entry; work merged to main through a branch and pull request

**Supplied for this week** (already in the one-snapshot repository)

- supplied payments baseline (participant-editable starting source, LEARNER-OWNED.md section 1)
- api/pom.xml skeleton
- docs/contracts/W07-openapi.yaml
- api/src/test/java/payments/project/w07/StandardW07PortTest.java, StandardW07WiringTest.java

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Ported payment rules keep their behaviour | 40 | S3: Porting a Javalin route, service and repository to Spring Boot; S2: REST controllers; S2: Path, query and body parameters; S2: Reading an OpenAPI spec and exploring it in Swagger UI |
| C2 | Status codes with ResponseEntity | 25 | S2: Response codes with ResponseEntity; S2: REST controllers |
| C3 | Dependency injection and layering | 20 | S1: Dependency injection replacing manual wiring; S1: Beans and bean scopes; S3: Constructor injection across layers |
| C4 | Milestone note, control evidence and repository hygiene | 15 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S4 Configuration files and profiles / Secrets from the environment / Schema migrations with Flyway; S5 Spring Data JPA (all topics).

**Where to look**

- Track table: `docs/tracks/standard.md row W07`
- Milestone note: `docs/milestones/W07.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W07-openapi.yaml`
- Supplied checks: `api/src/test/java/payments/project/w07/StandardW07*.java`
- Release row: RELEASES.md 'Week by week' row W07 (Standard column)
- Run: `scripts/check-milestone.sh standard 7` (selector `standard-w07`, areas: api; cumulative)
- Milestone paths: `api/src/main/java`, `api/src/test/java/payments/project/w07`, `docs/contracts/W07-openapi.yaml`, `docs/milestones/W07.md`, `RELEASES.md`

## Standard W08 - JPA persistence, migrations, DTOs, approval history and the atomic approval transaction

Days S6-S10; taught cutoff S8 (Wed, programme day 38, 'Transactions and N+1'); due S10. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- Paste the FLYWAY dependency addition from RELEASES.md into api/pom.xml
- Spring Data JPA entities and repositories replace the in-memory store (payment_requests, payment_events, approvals tables per W08 x-persistence)
- Flyway migration(s) in api/src/main/resources/db/migration with BIGINT cents, USD-only check, amount range 1..999999999, status check and unique (supplier_id, invoice_number)
- application*.yml with profiles; any password/secret/token/key value only as a ${NAME} placeholder
- DTOs at the controller boundary; POST /payment-requests/{id}/correct; paginated, sortable GET /approval-queue returning history without N+1 (join fetch)
- PaymentRequest 1-* PaymentEvent mapping; approve/reject/correct in one @Transactional service method, checks before writes, full rollback on failure
- docs/milestones/W08.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- docs/contracts/W08-openapi.yaml (Standard uses the items not marked x-required-from standard W09)
- api/src/test/resources/project/w08/queue-seed.csv
- api/src/test/java/payments/project/w08/StandardW08*.java

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Persistence, profiles and invariant-enforcing migrations | 25 | S4: Configuration files and profiles; S4: Secrets from the environment; S4: Schema migrations with Flyway; S5: Spring Data JPA with Hibernate; S5: Entities and IDs; S5: Repositories, derived queries and @Query |
| C2 | DTO layering and the paginated approval queue | 20 | S6: Controller, service and repository layers; S6: DTOs and mapping; S6: Pagination and sorting |
| C3 | Approval history and the atomic approval transaction | 30 | S7: One-to-many and many-to-one relationships; S7: Relationship pitfalls; S8: How transactions work in Spring |
| C4 | Queue query efficiency (N+1) | 15 | S5: Seeing the generated SQL; S7: Lazy and eager loading; S8: The N+1 query problem: spotting it in SQL logs; S8: Fixing N+1 with JOIN FETCH |
| C5 | Milestone note, control evidence and repository hygiene | 10 | S4: Secrets from the environment; daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S9 Bean Validation / Global error handling / ProblemDetail; S10 @MockitoBean / MockMvc / Debugging a failing request.

**Where to look**

- Track table: `docs/tracks/standard.md row W08`
- Milestone note: `docs/milestones/W08.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml`
- Supplied checks: `api/src/test/java/payments/project/w08/StandardW08*.java`, `api/src/test/resources/project/w08/**`
- Release row: RELEASES.md 'Week by week' row W08 (Standard column)
- Run: `scripts/check-milestone.sh standard 8` (selector `standard-w08`, areas: api; cumulative)
- Milestone paths: `api/src/main/java`, `api/src/main/resources`, `api/src/test/java/payments/project/w08`, `docs/milestones/W08.md`, `RELEASES.md`, `api/pom.xml`

## Standard W09 - Authenticated approvals, separation of duties and the idempotent payment-provider client

Days S11-S15; taught cutoff S13 (Wed, programme day 43, 'Spring review and check'); due S15. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- Paste the SECURITY dependency addition from RELEASES.md into api/pom.xml
- Bean Validation on request DTOs and global ProblemDetail errors with field errors (W08 x-errors)
- POST /auth/login and GET /me; HS256 JWT verified with security.jwt.secret from the environment; hashed passwords; role rules and separation of duties; actor taken from the token on every event; approver queue excludes own requests; dev profile keeps the X-Actor-Id stub and open endpoints, no other profile does
- POST /payment-requests/{id}/send: RestClient provider client with provider.base-url, provider.timeout and provider.max-attempts from configuration, one Idempotency-Key per request, bounded retries with the same key, REJECTED moves the request to EXCEPTION with an attributable event, retries exhausted without an answer give 502 and leave the request APPROVED, a second send answers 409
- Provider health component in GET /actuator/health (Standard only)
- At least four learner tests outside payments/project: MockMvc with @MockitoBean covering an own-request or over-limit refusal and an unauthenticated call, plus one integration test
- docs/milestones/W09.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- docs/contracts/W09-openapi.yaml
- docs/contracts/W09-provider-stub.md
- api/src/test/java/payments/project/w09/StandardW09*.java and payments/project/support/** (in-test ProviderStub, JWT helper; test keys generated at test time)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Validation and standard errors | 15 | S9: Bean Validation on requests; S9: Global error handling; S9: Standard error responses with ProblemDetail |
| C2 | Authenticated approvals and separation of duties | 35 | S12: Password hashing; S12: Issuing and checking a JWT; S12: Role-protected endpoints; S12: A dev profile that keeps endpoints open during front-end work; S12: Reading: session cookies vs JWTs, trade-offs; S12: Reading: common API security risks (OWASP) |
| C3 | Idempotent provider client with bounded retries | 24 | S11: Calling another service with RestClient: base URL and timeouts; S4: Configuration files and profiles; S8: How transactions work in Spring; Idempotency key and bounded retry: not a separate topic; built from taught Java (a bounded loop with exception handling around the RestClient call) and taught persistence. No retry library is required. |
| C4 | Learner-written control tests and provider health (Standard-only health check) | 16 | S10: Mockito in Spring: @MockitoBean; S10: Web-layer tests with MockMvc; S11: An integration test; S11: Health checks |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S14-S15 Python.

**Where to look**

- Track table: `docs/tracks/standard.md row W09`
- Milestone note: `docs/milestones/W09.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml (x-required-from standard W09 items)`, `docs/contracts/W09-openapi.yaml`, `docs/contracts/W09-provider-stub.md`
- Supplied checks: `api/src/test/java/payments/project/w09/StandardW09*.java`, `api/src/test/java/payments/project/support/**`
- Release row: RELEASES.md 'Week by week' row W09 (Standard column)
- Run: `scripts/check-milestone.sh standard 9` (selector `standard-w09`, areas: api; cumulative)
- Milestone paths: `api/src/main/java`, `api/src/test/java`, `docs/contracts/W09-openapi.yaml`, `docs/contracts/W09-provider-stub.md`, `docs/milestones/W09.md`, `RELEASES.md`, `api/pom.xml`, `api/src/main/resources`

## Standard W10 - Invoice and settlement import pipeline with FastAPI reconciliation service

Days S16-S20; taught cutoff S18 (Wed, programme day 48, 'FastAPI and service calls'); due S20. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- pipeline/src/payments_pipeline implementing pipeline/CONTRACT.md: amounts (exact int cents, no float), importer (generator-based CSV/JSON readers with validation and per-line rejection reasons)
- db.connect / db.conninfo_from_env: the learner's own psycopg connection code (Standard only); staging tables and idempotent loads with parameterised SQL
- categorise: prompt payload carries only the description; AI output validated against ALLOWED_CATEGORIES, otherwise needs_review; never touches amount, currency, supplier, invoice or status
- spring_client (URL, token, timeout from env; failure kinds), reconcile (discrepancy kinds in contract order), service (FastAPI /health, /discrepancies, /discrepancies/{reference})
- docs/milestones/W10.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- pipeline/pyproject.toml skeleton
- pipeline/CONTRACT.md
- pipeline/data/samples/**
- pipeline/tests/project/w10/test_standard_w10_pipeline.py and tests/project/support/** (stub AI provider, stub Spring API)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Import validation with exact amounts | 30 | S16: Reading CSV and JSON files with generators; S17: Cleaning and validating records with Pydantic and type hints; S14: Python syntax for Java developers; S15: Exceptions and logging |
| C2 | Idempotent load and duplicate detection | 20 | S16: Connecting to Postgres from Python with psycopg; S16: Loading into a staging table with upserts; S17: Deduplicating: in SQL vs in Python |
| C3 | Validated, minimised AI categorisation | 20 | S17: AI enrichment with validated outputs; S17: Reading: keeping sensitive data out of prompts |
| C4 | FastAPI discrepancy routes and Spring call | 20 | S18: FastAPI routes; S18: Serving pipeline results as an API; S18: Calling the Spring Boot API from FastAPI: service URL, token and timeouts |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S19 Python review and module check (no new topic); S20 HTML, forms, accessibility.

**Where to look**

- Track table: `docs/tracks/standard.md row W10`
- Milestone note: `docs/milestones/W10.md (learner note template, not a spec)`
- Contracts: `pipeline/CONTRACT.md`
- Supplied checks: `pipeline/tests/project/w10/**`, `pipeline/data/samples/**`
- Release row: RELEASES.md 'Week by week' row W10 (Standard column)
- Run: `scripts/check-milestone.sh standard 10` (selector `standard-w10`, areas: pipeline; cumulative)
- Milestone paths: `pipeline/src`, `pipeline/tests/project/w10`, `pipeline/data/samples`, `docs/milestones/W10.md`, `RELEASES.md`, `pipeline/CONTRACT.md`

## Standard W11 - Accessible payment-request form, queue page and money helpers

Days S21-S25; taught cutoff S23 (Wed, programme day 53, 'Modern JS'); due S25. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- web/index.html: semantic, accessible payment-request form (labels, required, supplier pattern, two-decimal amount pattern, currency fixed to USD) and a static queue page with a headed table
- Mobile-first responsive CSS with flexbox/grid
- web/src ES-module helpers: parse two-decimal text to integer cents and format cents without floating point; filter and sort requests by status and amount
- docs/milestones/W11.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- web/package.json and lockfile (Vite/Vitest scaffold)
- web/tests/project/w11/form.test.js, money.test.js, queue-helpers.test.js, responsive.test.js

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Semantic and accessible structure | 30 | S20: Semantic HTML page structure; S20: Accessibility basics |
| C2 | Payment form validation | 20 | S20: Forms and HTML5 validation |
| C3 | Responsive layout | 10 | S20: The box model; S21: Flexbox; S21: Grid; S21: Responsive, mobile-first layout |
| C4 | Exact money and queue helpers | 30 | S22: JavaScript for Java developers; S22: let, const and ===; S22: Functions as values: arrow functions and callbacks; S22: Arrays and objects: map, filter, reduce; S23: Destructuring and spread; S23: Modules: import and export |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Note: running the supplied `npm test -- standard-w11` uses npm, which is taught on S24, after the S23 cutoff. No criterion grades npm or Vite setup; you only run the supplied command (the README's prefetch step installs the dependencies).

Not required (taught after the cutoff): S24 DOM, events, XSS; S25 Promises, async/await; S26-S27 fetch.

**Where to look**

- Track table: `docs/tracks/standard.md row W11`
- Milestone note: `docs/milestones/W11.md (learner note template, not a spec)`
- Contracts: none new
- Supplied checks: `web/tests/project/w11/**`
- Release row: RELEASES.md 'Week by week' row W11 (Standard column)
- Run: `scripts/check-milestone.sh standard 11` (selector `standard-w11`, areas: web; cumulative)
- Milestone paths: `web/index.html`, `web/src`, `web/tests/project/w11`, `docs/milestones/W11.md`, `RELEASES.md`

## Standard W12 - Operational browser client for the approval queue, and first React components

Days S26-S30; taught cutoff S28 (Wed, programme day 58, 'Components and props'); due S30. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- web/src: load and filter the approval queue from the learner's API through the Vite dev proxy, API address from environment settings, exact amounts
- Specific UI messages for 400 ProblemDetail, 401, 403 refusal, 409 duplicate, 500 and network error; displayed status unchanged and no success shown after a refusal
- Untrusted invoice text rendered with textContent or an equivalent safe API
- At least one learner Vitest test of a pure money or filter helper (web/tests outside tests/project, or web/src/**/*.test.js)
- ui/src: first React components with props and conditional display rendering queue rows
- docs/milestones/W12.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- ui/ Vite + React scaffold
- web/tests/project/w12/client.test.js, learner-test.test.js
- ui/tests/project/w12/components.test.jsx

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | API integration for the queue | 20 | S24: Project setup with npm and Vite; S25: Promises; S25: async and await; S26: Fetching data; S26: Fixing CORS: Spring Security config vs Vite dev proxy; S27: Calling your Spring Boot API from the browser; S27: Environment settings for the API address |
| C2 | Refusal and error states | 25 | S26: Handling HTTP errors and ProblemDetail responses; S24: Selecting and changing the DOM; S24: Events and event bubbling |
| C3 | XSS-safe rendering | 15 | S24: Preventing cross-site scripting |
| C4 | First React components | 10 | S28: Components and JSX; S28: Props; S28: Project structure; S28: Conditional display |
| C5 | Learner Vitest test | 20 | S27: Testing a plain function with Vitest |
| C6 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S29 useState and lists; S30 effects and custom hooks; S31-S33 routing, forms, Context, Testing Library.

**Where to look**

- Track table: `docs/tracks/standard.md row W12`
- Milestone note: `docs/milestones/W12.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml / W09-openapi.yaml (ProblemDetail shapes the stub reproduces)`
- Supplied checks: `web/tests/project/w12/**`, `ui/tests/project/w12/**`
- Release row: RELEASES.md 'Week by week' row W12 (Standard column)
- Run: `scripts/check-milestone.sh standard 12` (selector `standard-w12`, areas: web, ui; cumulative)
- Milestone paths: `web/src`, `ui/src`, `web/tests/project/w12`, `ui/tests/project/w12`, `docs/milestones/W12.md`, `RELEASES.md`

## Standard W13 - React approval and exception workflows with UI tests

Days S31-S35; taught cutoff S33 (Wed, programme day 63, 'CRUD screens and UI tests'); due S35. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- ui/src routed React workflows against the stubbed API: submit, correct (with reason, while SUBMITTED), approve, reject (with reason), resolve exception case (with note); route parameters for request detail; history shown; no delete control or call
- Controlled, labelled forms that surface server validation and refusal messages; refusals leave displayed status unchanged
- Loading and error states for data effects/custom hooks; action buttons disabled while pending
- At least three learner Testing Library tests (ui/tests outside tests/project) querying by role and label, including one refusal case
- docs/milestones/W13.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- docs/contracts/W13-exception-cases.yaml
- ui/tests/project/w13/workflows.test.jsx, exceptions.test.jsx, learner-tests.test.js (stubbed API, fixed synthetic user)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Approval and exception workflows | 40 | S29: useState; S29: Updating state immutably; S29: Rendering lists with keys; S31: Routing and route parameters; S31: Navigation; S31: Lifting state up; S32: Sharing state with Context; S33: Create, edit and delete against the API |
| C2 | Forms and accessibility | 20 | S32: Controlled forms; S32: Accessible forms; S33: Handling submission errors in forms |
| C3 | Loading and error states | 10 | S30: Loading data with useEffect; S30: Loading and error states; S30: Custom hooks |
| C4 | Learner UI tests | 20 | S33: UI tests with Testing Library: querying by role and label |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S34 login, protected pages, 401/403, dev profile off.

**Where to look**

- Track table: `docs/tracks/standard.md row W13`
- Milestone note: `docs/milestones/W13.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W13-exception-cases.yaml`
- Supplied checks: `ui/tests/project/w13/**`
- Release row: RELEASES.md 'Week by week' row W13 (Standard column)
- Run: `scripts/check-milestone.sh standard 13` (selector `standard-w13`, areas: ui; cumulative)
- Milestone paths: `ui/src`, `ui/tests`, `docs/milestones/W13.md`, `RELEASES.md`

## Standard W14 - Signed-in role screens and the Compose description

Days S36-S40; taught cutoff S38 (Wed, programme day 68, 'Microservices with Compose'); due S40. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- ui/src sign-in, protected and role-based screens; 401 redirects to sign-in, 403 shows a role screen, sign-out clears state; approver queue excludes own requests; approve control hidden above the user's limit (server still answers 403); dev profile off
- deploy/compose.yaml describing api, database and pipeline (FastAPI) services with health-based depends_on, service-name URLs and env indirection (database settings, PROVIDER_BASE_URL); no secrets
- Multi-stage api/Dockerfile and pipeline/Dockerfile (static checks only; no container engine runs)
- docs/milestones/W14.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- deploy/package.json and deploy/tests/project/w14/compose.test.mjs, dockerfiles.test.mjs
- ui/tests/project/w14/auth.test.jsx (stubbed auth API)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Signed-in role screens | 35 | S34: Login with a session cookie or JWT; S34: Protected pages and role-based screens; S34: Handling 401 and 403: redirect and sign out; S34: Turning off the dev profile |
| C2 | Compose correctness (static) | 30 | S36: Ports, volumes and environment variables; S37: Compose: the Spring Boot service and its database; S37: Service-name networking: why localhost fails between containers; S37: depends_on with health checks; S38: Adding the FastAPI service and its database to Compose; S38: Service-to-service calls: tokens, timeouts and failed downstream calls |
| C3 | Dockerfile practice | 15 | S36: Containers and images (Docker and Podman); S36: Multi-stage builds: back-end image, and front-end image built with Node and served with nginx |
| C4 | Milestone note, control evidence and repository hygiene | 20 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): S39 GitHub Actions and secrets in CI (Standard: optional evidence, no marks).

**Where to look**

- Track table: `docs/tracks/standard.md row W14`
- Milestone note: `docs/milestones/W14.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml x-security (roles, /auth/login, /me)`
- Supplied checks: `ui/tests/project/w14/**`, `deploy/tests/project/w14/**`
- Release row: RELEASES.md 'Week by week' row W14 (Standard column)
- Run: `scripts/check-milestone.sh standard 14` (selector `standard-w14`, areas: ui, deploy; cumulative)
- Milestone paths: `ui/src`, `deploy/compose.yaml`, `api/Dockerfile`, `pipeline/Dockerfile`, `deploy/tests/project/w14`, `ui/tests/project/w14`, `docs/milestones/W14.md`, `RELEASES.md`

## Standard W15 - Payment platform integration and walkthrough readiness

Days S41-S43 (partial week); taught cutoff S38 (Wed, programme day 68, 'Microservices with Compose'); due end of S42. This is the last Project 2 milestone and is part of the programme Final Assessment.

**Required deliverables**

- The whole platform works end to end on the final commit: full supplied suite passes in api, pipeline, web, ui and deploy with supplied paths unchanged, including the cross-area scenario (one payment per request, statement mismatch to an exception case, attributable resolution through /exception-cases)
- Consistent cross-service configuration (URLs, env names, tokens, PROVIDER_BASE_URL)
- docs/walkthrough.md covering the template headings, with no real-banking or compliance claims
- README.md 'Running the platform locally' section: how to run each area and start the services, env variable names only, synthetic sign-in
- docs/milestones/W15.md and docs/ai-use-log.md entry
- Optional, no marks: .github/workflows/ci.yml (CI/CD taught S39)

**Supplied for this week** (already in the one-snapshot repository)

- no new stubs; api/src/test/java/payments/project/w15/StandardW15ScenarioTest.java and api/src/test/resources/project/w15/scenario-statement.csv
- docs/templates/walkthrough.md

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | End-to-end payment invariants across services | 40 | S36: Ports, volumes and environment variables; S38: Service-to-service calls: tokens, timeouts and failed downstream calls; S4: Secrets from the environment |
| C2 | Walkthrough readiness | 30 | Completion work on topics taught by S38, using the supplied docs/templates/walkthrough.md and the scheduled S42 rehearsal (no separate topic teaches walkthrough writing). |
| C3 | README and run instructions | 20 | S4: Secrets from the environment; S36: Ports, volumes and environment variables |
| C4 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Scheduled completion time used (not taught topics): S40 Project day: Integrating all track topics into Project 2: back end; S41 Project day: Integrating all track topics into Project 2: front end; S42 Project day: Walkthrough rehearsal and recording (scheduled time, not a new taught topic)

Not required (taught after the cutoff): S39 CI/CD (optional evidence, no marks).

**Where to look**

- Track table: `docs/tracks/standard.md row W15`
- Milestone note: `docs/milestones/W15.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W13-exception-cases.yaml (run against the learner's API by the W15 scenario check)`, `all earlier contracts`
- Supplied checks: `all Standard supplied checks, cumulative (standard-w15 selector)`, `api/src/test/java/payments/project/w15/**`
- Release row: RELEASES.md 'Week by week' row W15 (Standard column)
- Run: `scripts/check-milestone.sh standard 15` (selector `standard-w15`, areas: api, pipeline, web, ui, deploy; cumulative)
- Milestone paths: `api`, `pipeline`, `web`, `ui`, `deploy`, `README.md`, `docs/walkthrough.md`, `docs/milestones/W15.md`, `RELEASES.md`
