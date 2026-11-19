# Project 2 Fast milestone map

Companion to the Project 2 PRD ([docs/prd.md](../prd.md)) and the Fast track table ([docs/tracks/fast.md](../tracks/fast.md)). For each milestone it lists what you deliver, what is already supplied, the taught topics each criterion builds on, what is not required yet, and where to look in this repository. Criteria are listed by reference number, short name and marks only; the full criteria are in your course materials for each milestone.

Five cumulative Fast milestones, one per Fast technology week W07-W11 (FT1-FT25). Cutoff = that week's Wednesday, inclusive; due = that Friday, except W11, due end of FT24 (the FT25 Final follows). AWS preparation A1-A7 (programme days 56-62) follows FT25 as a separate block with no Project 2 milestone, requirement or lab, and is never inside FT1-FT25. W07-W10 are formative; W11, the last milestone, is the Project 2 part of the programme Final Assessment (PRD section 10).

Legend: each topic is named as in the programme calendar, with the day it is taught; every listed day is on or before the milestone's taught cutoff. `(skim)` = the topic is covered at skim depth on this track.

| Week | Days | Cutoff | Due | Title | Marks (C1..Cn) |
|---|---|---|---|---|---|
| W07 | FT1-FT5 | FT3 | FT5 | Port the payment service to Spring Boot with JPA persistence | 30, 20, 30, 10, 10 |
| W08 | FT6-FT10 | FT8 | FT10 | Atomic approvals, authenticated separation of duties and the idempotent provider client | 13, 22, 15, 20, 20, 10 |
| W09 | FT11-FT15 | FT13 | FT15 | Invoice and settlement import pipeline, FastAPI reconciliation service and accessible payment form | 30, 30, 30, 10 |
| W10 | FT16-FT20 | FT18 | FT20 | Operational browser client and React approval workflows | 25, 10, 35, 20, 10 |
| W11 | FT21-FT25 | FT23 | end of FT24 | Signed-in role screens, exception workflow, Compose, CI and Project 2 completion | 25, 15, 25, 15, 10, 10 |

## Fast W07 - Port the payment service to Spring Boot with JPA persistence

Days FT1-FT5; taught cutoff FT3 (Wed, programme day 33, 'Spring Data JPA'); due FT5. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- Spring Boot port of the supplied baseline in api/src/main/java with REST controllers and ResponseEntity status codes (201/200/400/403/404/409), keeping every W07 contract rule
- Spring Data JPA entities and repositories (payment_requests, payment_events per W07 x-persistence) with BIGINT/long cents; history persisted as entity rows keyed by request id (relationship mapping optional until W08)
- Derived or @Query repository queries filtering by status and supplier
- Controller, service and repository beans wired by Spring DI (constructor injection not required); no payment rules in controllers
- docs/milestones/W07.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- supplied payments baseline (participant-editable starting source)
- api/pom.xml skeleton
- docs/contracts/W07-openapi.yaml (incl. x-persistence)
- api/src/test/java/payments/project/w07/FastW07PortTest.java, FastW07PersistenceTest.java

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Ported payment rules keep their behaviour | 30 | FT2: Porting a Javalin route, service and repository to Spring Boot; FT1: REST controllers; FT2: Path, query and body parameters; FT2: Reading an OpenAPI spec and exploring it in Swagger UI |
| C2 | Status codes | 20 | FT2: Response codes with ResponseEntity |
| C3 | JPA persistence of requests and history | 30 | FT3: Spring Data JPA with Hibernate; FT3: Entities and IDs; FT3: Repositories, derived queries and @Query; FT3: Seeing the generated SQL |
| C4 | Dependency injection and layering | 10 | FT1: Dependency injection replacing manual wiring; FT2: Porting a Javalin route, service and repository to Spring Boot |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Note: the topic 'Controller, service and repository layers' is taught on FT4, after the FT3 cutoff. C4 asks only for Spring-managed controller, service and repository beans wired by dependency injection, with payment rules kept out of controllers, as covered by the FT1 dependency-injection and FT2 porting topics.

Not required (taught after the cutoff): FT4 Controller/service/repository layers, DTOs, pagination, Flyway, MockMvc; FT5 Bean Validation, ProblemDetail; FT6 One-to-many relationships (history relationship graded in W08).

**Where to look**

- Track table: `docs/tracks/fast.md row W07`
- Milestone note: `docs/milestones/W07.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W07-openapi.yaml (x-persistence required from Fast W07)`
- Supplied checks: `api/src/test/java/payments/project/w07/FastW07*.java`
- Release row: RELEASES.md 'Week by week' row W07 (Fast column)
- Run: `scripts/check-milestone.sh fast 7` (selector `fast-w07`, areas: api; cumulative)
- Milestone paths: `api/src/main/java`, `api/src/test/java/payments/project/w07`, `docs/milestones/W07.md`, `RELEASES.md`

## Fast W08 - Atomic approvals, authenticated separation of duties and the idempotent provider client

Days FT6-FT10; taught cutoff FT8 (Wed, programme day 38, 'Spring review and check'); due FT10. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- Paste the FLYWAY and SECURITY dependency additions from RELEASES.md into api/pom.xml
- Flyway migrations with USD, amount-range, status and unique (supplier_id, invoice_number) constraints; application*.yml profiles with secrets only as ${NAME}
- DTOs at the boundary; POST /payment-requests/{id}/correct; paginated GET /approval-queue with history and no N+1
- PaymentRequest 1-* PaymentEvent; approve/reject/correct transactional with checks before writes and full rollback
- Bean Validation and global ProblemDetail errors
- POST /auth/login, GET /me; hashed passwords; HS256 JWT; role rules and separation of duties; actor from the token; dev profile keeps the X-Actor-Id stub
- POST /payment-requests/{id}/send: RestClient provider client with configured base URL, timeout and max attempts, one Idempotency-Key per request, bounded retries with the same key, REJECTED to EXCEPTION, retries exhausted without an answer give 502 and leave the request APPROVED, a second send answers 409
- At least three learner MockMvc tests with @MockitoBean (api/src/test/java outside payments/project), including a refusal case
- No Spring health check (Standard-only). docs/milestones/W08.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- docs/contracts/W08-openapi.yaml
- docs/contracts/W08-provider-stub.md
- api/src/test/java/payments/project/w08/FastW08*.java and payments/project/support/** (in-test ProviderStub)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Invariant-enforcing migrations, profiles and DTO queue | 13 | FT2: Configuration files and profiles (skim); FT2: Secrets from the environment (skim); FT4: Schema migrations with Flyway; FT4: Controller, service and repository layers; FT4: DTOs and mapping; FT4: Pagination and sorting |
| C2 | Atomic approval with history and N+1 | 22 | FT3: Seeing the generated SQL; FT6: One-to-many and many-to-one relationships; FT6: How transactions work in Spring; FT6: The N+1 query problem: spotting it in SQL logs; FT6: Fixing N+1 with JOIN FETCH; FT6: Lazy and eager loading (skim) |
| C3 | Validation and standard errors | 15 | FT5: Bean Validation on requests; FT5: Global error handling; FT5: Standard error responses with ProblemDetail |
| C4 | Authenticated separation of duties | 20 | FT7: Password hashing; FT7: Issuing and checking a JWT; FT7: Role-protected endpoints; FT7: A dev profile that keeps endpoints open during front-end work; FT7: Session cookies vs JWTs: trade-offs (skim); FT7: Common API security risks (OWASP) (skim) |
| C5 | Idempotent provider client and learner tests | 20 | FT8: Calling another service with RestClient: base URL and timeouts (skim); FT4: Web-layer tests with MockMvc; FT6: Mockito in Spring: @MockitoBean (skim); Idempotency key and bounded retry: not a separate topic; built from taught Java (a bounded loop with exception handling around the RestClient call) and taught persistence. No retry library is required. |
| C6 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): FT9-FT10 Python (builds ahead; graded in W09); Health checks (Standard-only, never required on Fast).

**Where to look**

- Track table: `docs/tracks/fast.md row W08`
- Milestone note: `docs/milestones/W08.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml (all items incl. x-required-from fast W08)`, `docs/contracts/W08-provider-stub.md`
- Supplied checks: `api/src/test/java/payments/project/w08/FastW08*.java`, `api/src/test/java/payments/project/support/**`
- Release row: RELEASES.md 'Week by week' row W08 (Fast column)
- Run: `scripts/check-milestone.sh fast 8` (selector `fast-w08`, areas: api; cumulative)
- Milestone paths: `api/src/main/java`, `api/src/main/resources`, `api/src/test/java`, `docs/milestones/W08.md`, `RELEASES.md`, `api/pom.xml`

## Fast W09 - Invoice and settlement import pipeline, FastAPI reconciliation service and accessible payment form

Days FT11-FT15; taught cutoff FT13 (Wed, programme day 43, 'JavaScript basics + Modern JS'); due FT15. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- pipeline/src/payments_pipeline per pipeline/CONTRACT.md: amounts, importer (generator readers, validation, rejection reasons), staging through the supplied support/db_connection.py with idempotent, parameterised upserts (no connection code graded)
- categorise (description-only prompt payload, validated output, needs_review otherwise), spring_client (URL, token, timeout from env), reconcile and the FastAPI service routes
- web/index.html: semantic, labelled payment-request form with HTML5 validation (supplier pattern, two-decimal amount) and a queue page
- web/src ES-module money helpers that parse and format cents without floating point
- docs/milestones/W09.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- pipeline/pyproject.toml skeleton
- pipeline/CONTRACT.md
- pipeline/support/db_connection.py (Fast-only helper)
- pipeline/data/samples/**
- pipeline/tests/project/w09/test_fast_w09_pipeline.py
- web/package.json and lockfile
- web/tests/project/w09/form.test.js, money.test.js

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Exact import and idempotent load | 30 | FT9: Python syntax for Java developers; FT9: Lists, dicts, sets and tuples; FT10: Reading CSV and JSON files with generators; FT10: Loading into a staging table with psycopg and upserts; FT10: Cleaning and validating records with Pydantic and type hints; FT10: Deduplicating: in SQL vs in Python (skim) |
| C2 | Validated AI categorisation and FastAPI discrepancy service | 30 | FT11: AI enrichment with validated outputs; FT11: Keeping sensitive data out of prompts (skim); FT11: FastAPI routes that serve pipeline results; FT11: Calling the Spring Boot API from FastAPI: service URL, token and timeouts |
| C3 | Accessible payment form and money helpers | 30 | FT12: Semantic HTML page structure; FT12: Forms and HTML5 validation; FT12: Accessibility basics; FT12: JavaScript for Java developers; FT13: Functions as values: arrow functions and callbacks; FT13: Arrays and objects: map, filter, reduce; FT13: Modules: import and export; FT13: Project setup with npm and Vite |
| C4 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): FT14 DOM, events, XSS, async; FT15 fetch, errors, CORS, Vitest.

**Where to look**

- Track table: `docs/tracks/fast.md row W09`
- Milestone note: `docs/milestones/W09.md (learner note template, not a spec)`
- Contracts: `pipeline/CONTRACT.md`
- Supplied checks: `pipeline/tests/project/w09/**`, `web/tests/project/w09/**`
- Release row: RELEASES.md 'Week by week' row W09 (Fast column)
- Run: `scripts/check-milestone.sh fast 9` (selector `fast-w09`, areas: pipeline, web; cumulative)
- Milestone paths: `pipeline/src`, `pipeline/tests/project/w09`, `web/index.html`, `web/src`, `web/tests/project/w09`, `docs/milestones/W09.md`, `RELEASES.md`, `pipeline/CONTRACT.md`

## Fast W10 - Operational browser client and React approval workflows

Days FT16-FT20; taught cutoff FT18 (Wed, programme day 48, 'Forms and shared state + CRUD screens and UI tests'); due FT20. Formative (marks do not count toward the programme Final Assessment).

**Required deliverables**

- web/src: approval queue loaded from the learner's API through the Vite proxy with the API address from environment settings; specific messages for HTTP, ProblemDetail (403, 409) and network errors; status unchanged after a refusal
- Untrusted text rendered through safe APIs (no innerHTML / dangerouslySetInnerHTML with untrusted data)
- At least one learner Vitest test of a money or queue helper
- ui/src: routed React submit, correct-with-reason, approve and reject workflows with controlled forms, history, loading/error states, no double submission, no delete
- docs/milestones/W10.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- ui/ Vite + React scaffold
- web/tests/project/w10/client.test.js, learner-test.test.js
- ui/tests/project/w10/workflows.test.jsx (stubbed API with refusals)

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Queue integration and refusal states | 25 | FT14: Promises; FT14: async and await; FT15: Fetching data; FT15: Handling HTTP errors and ProblemDetail responses; FT15: Fixing CORS: Spring Security config vs Vite dev proxy; FT15: Calling your Spring Boot API from the browser; FT15: Environment settings for the API address |
| C2 | XSS-safe rendering | 10 | FT14: Selecting and changing the DOM; FT14: Preventing cross-site scripting |
| C3 | React approval workflows | 35 | FT16: Components and JSX; FT16: Props; FT16: useState; FT16: Updating state immutably; FT16: Rendering lists with keys; FT17: Routing and route parameters; FT17: Navigation; FT17: Loading data with useEffect; FT17: Lifting state up; FT18: Controlled forms; FT18: Create, edit and delete against the API; FT18: Handling submission errors in forms |
| C4 | Loading states and learner test | 20 | FT17: Loading and error states; FT15: Testing a plain function with Vitest (skim) |
| C5 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Not required (taught after the cutoff): FT19 Context, login, protected pages; FT20 Testing Library UI tests.

**Where to look**

- Track table: `docs/tracks/fast.md row W10`
- Milestone note: `docs/milestones/W10.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W08-openapi.yaml (ProblemDetail and route shapes the stub reproduces)`
- Supplied checks: `web/tests/project/w10/**`, `ui/tests/project/w10/**`
- Release row: RELEASES.md 'Week by week' row W10 (Fast column)
- Run: `scripts/check-milestone.sh fast 10` (selector `fast-w10`, areas: web, ui; cumulative)
- Milestone paths: `web/src`, `ui/src`, `web/tests/project/w10`, `ui/tests/project/w10`, `docs/milestones/W10.md`, `RELEASES.md`

## Fast W11 - Signed-in role screens, exception workflow, Compose, CI and Project 2 completion

Days FT21-FT25; taught cutoff FT23 (Wed, programme day 53, 'CI/CD and module check'); due end of FT24. This is the last Project 2 milestone and is part of the programme Final Assessment.

**Required deliverables**

- ui/src sign-in, protected role screens, 401/403 handling, sign-out, approver queue without own requests, Context-shared auth state, dev profile off
- ui/src exception workflow (resolve with a required note, resolver shown in history) against the stubbed API per W13-exception-cases.yaml
- At least three learner Testing Library tests (ui/tests outside tests/project) by role and label, including a refusal or exception case
- deploy/compose.yaml with api, database and FastAPI service, health ordering, service-name URLs, env indirection incl. PROVIDER_BASE_URL, no secrets; multi-stage api/Dockerfile and pipeline/Dockerfile
- .github/workflows/ci.yml running each area's tests with secrets referenced, never inline
- Full cumulative supplied suite passes on the final commit; README run instructions and docs/walkthrough.md
- docs/milestones/W11.md and docs/ai-use-log.md entry

**Supplied for this week** (already in the one-snapshot repository)

- docs/contracts/W13-exception-cases.yaml (Fast uses it from W11)
- deploy/tests/project/w11/ci.test.mjs, compose.test.mjs, dockerfiles.test.mjs
- ui/tests/project/w11/auth.test.jsx, exceptions.test.jsx, learner-tests.test.js
- docs/templates/walkthrough.md

**Criteria and the taught topics each depends on**

| Ref | Criterion | Marks | Depends on (day: topic) |
|---|---|---|---|
| C1 | Signed-in role screens and exception workflow | 25 | FT18: Controlled forms; FT19: Sharing state with Context; FT19: Login with a session cookie or JWT; FT19: Protected pages and role-based screens; FT19: Handling 401 and 403: redirect and sign out; FT19: Turning off the dev profile |
| C2 | Learner UI tests | 15 | FT20: UI tests with Testing Library: querying by role and label |
| C3 | Compose and Dockerfiles (static) | 25 | FT21: Containers and images (Docker and Podman); FT21: Ports, volumes and environment variables; FT21: Multi-stage builds: back-end and front-end images; FT21: Compose: the Spring Boot service and its database; FT21: Service-name networking: why localhost fails between containers; FT22: Health checks and depends_on; FT22: Adding the FastAPI service and its database to Compose; FT22: Service-to-service calls: tokens, timeouts and failed downstream calls |
| C4 | CI workflow and secrets in CI | 15 | FT23: A GitHub Actions pipeline: tests, then image builds; FT23: Secrets in CI |
| C5 | Completion and walkthrough readiness | 10 | Completion work on topics taught by FT23, using the supplied docs/templates/walkthrough.md and the scheduled FT24 completion time. The FT24 polish-checklist reading falls after the cutoff and is not required. |
| C6 | Milestone note, control evidence and repository hygiene | 10 | daily ritual (PR user story, security check, AI note) |

Scheduled completion time used (not taught topics): FT24: Closing remaining Project 2 gaps: back end and front end; Reading: polish checklist (README, contract gaps, clean commits); Walkthrough rehearsal and recording (scheduled completion time, not a new taught topic)

Not required (taught after the cutoff): FT25 Final (separate sitting); A1-A7 AWS preparation (separate block, no Project 2 work).

**Where to look**

- Track table: `docs/tracks/fast.md row W11`
- Milestone note: `docs/milestones/W11.md (learner note template, not a spec)`
- Contracts: `docs/contracts/W13-exception-cases.yaml`
- Supplied checks: `ui/tests/project/w11/**`, `deploy/tests/project/w11/**`, `all earlier Fast supplied checks (cumulative fast-w11 selector)`
- Release row: RELEASES.md 'Week by week' row W11 (Fast column)
- Run: `scripts/check-milestone.sh fast 11` (selector `fast-w11`, areas: api, pipeline, web, ui, deploy; cumulative)
- Milestone paths: `ui/src`, `ui/tests`, `deploy`, `api/Dockerfile`, `pipeline/Dockerfile`, `.github/workflows/ci.yml`, `README.md`, `docs/walkthrough.md`, `docs/milestones/W11.md`, `RELEASES.md`
