# Fast track: Project 2 weekly milestones (FT1-FT25)

Five cumulative milestones, one per Fast technology week W07-W11, each worth 100 marks. A milestone requires
only topics taught by its **taught cutoff** (that week's Wednesday, inclusive) and is due at the end of the
**Due** day. W11 is due at the end of FT24 (Thursday), the day before the FT25 Final, so milestone grading never
collides with the Final. Milestone marks are formative: the Final stands alone and milestone marks do not count
toward its capstone (24%) or walkthrough (16%) weights.

No Fast milestone requires a Standard-only topic: constructor injection across layers, Spring health checks
and writing psycopg connection code are not required (the W09 release supplies `pipeline/support/db_connection.py`).
Each week you also commit `docs/milestones/WNN.md` and keep `docs/ai-use-log.md` current. Supplied checks are
cumulative: `-Dmilestone=fast-w08` also runs the W07 checks.

| Week | Days | Taught cutoff | Due | Title | Required learner changes | Criteria (reference, marks; 100 total) | Repo paths | Supplied checks |
|---|---|---|---|---|---|---|---|---|
| W07 | FT1-FT5 | FT3 (Wed) | FT5 (Fri) | Port the payment service to Spring Boot with JPA persistence | Port the baseline to Spring Boot REST controllers with ResponseEntity status codes, and persist requests and history through Spring Data JPA entities with derived-query filters (status, supplier). No Flyway or DTO requirement yet. | C1 (30), C2 (20), C3 (30), C4 (10), C5 (10) | `api/src/main/java`<br>`api/src/test/java/payments/project/w07`<br>`docs/milestones/W07.md`<br>`RELEASES.md` | `mvn -q test -Dmilestone=fast-w07` in `api/` |
| W08 | FT6-FT10 | FT8 (Wed) | FT10 (Fri) | Atomic approvals, authenticated separation of duties and the idempotent provider client | Paste the Flyway and security dependency additions; add the Flyway schema, `/correct`, the paginated `/approval-queue` with history and no N+1, atomic approval, ProblemDetail errors, JWT roles and separation of duties (dev profile keeps the stub), the idempotent provider client, and your own control tests. No Spring health check. | C1 (13), C2 (22), C3 (15), C4 (20), C5 (20), C6 (10) | `api/src/main/java`<br>`api/src/main/resources`<br>`api/src/test/java`<br>`docs/milestones/W08.md`<br>`RELEASES.md`<br>`api/pom.xml` | `mvn -q test -Dmilestone=fast-w08` in `api/` |
| W09 | FT11-FT15 | FT13 (Wed) | FT15 (Fri) | Invoice and settlement import pipeline, FastAPI reconciliation service and accessible payment form | Write the import pipeline (exact cents, idempotent staging through the supplied `pipeline/support/db_connection.py`, validated AI categorisation, FastAPI discrepancy service calling your API) and the accessible payment-request form with money helpers in `web/`. | C1 (30), C2 (30), C3 (30), C4 (10) | `pipeline/src`<br>`pipeline/tests/project/w09`<br>`web/index.html`<br>`web/src`<br>`web/tests/project/w09`<br>`docs/milestones/W09.md`<br>`RELEASES.md`<br>`pipeline/CONTRACT.md` | `python -m pytest --milestone fast-w09` in `pipeline/`; `npm test -- fast-w09` in `web/` |
| W10 | FT16-FT20 | FT18 (Wed) | FT20 (Fri) | Operational browser client and React approval workflows | Load the queue from your API in `web/` with safe rendering and clear refusal messages plus a Vitest helper test; build the routed React submit, correct, approve and reject workflows with history, clear pending and failure feedback and no delete path. Context and Testing Library tests come in W11. | C1 (25), C2 (10), C3 (35), C4 (20), C5 (10) | `web/src`<br>`ui/src`<br>`web/tests/project/w10`<br>`ui/tests/project/w10`<br>`docs/milestones/W10.md`<br>`RELEASES.md` | `npm test -- fast-w10` in `web/` and in `ui/` |
| W11 | FT21-FT25 | FT23 (Wed) | end of FT24 (Thu) | Signed-in role screens, exception workflow, Compose, CI and Project 2 completion | Add sign-in and role screens, the exception workflow with Context-shared state and your own Testing Library tests; describe the platform in `deploy/compose.yaml` with multi-stage Dockerfiles; turn `.github/workflows/ci.yml` into a CI workflow that keeps secrets out; finish README and walkthrough. | C1 (25), C2 (15), C3 (25), C4 (15), C5 (10), C6 (10) | `ui/src`<br>`ui/tests`<br>`deploy`<br>`api/Dockerfile`<br>`pipeline/Dockerfile`<br>`.github/workflows/ci.yml`<br>`README.md`<br>`docs/walkthrough.md`<br>`docs/milestones/W11.md`<br>`RELEASES.md` | `scripts/check-milestone.sh fast 11` (all five areas) |

## After the Final: AWS preparation (A1-A7)

A1-A7 (programme days 56-62) follow FT25 as a separate certification-preparation block. It has no Project 2
milestone, no project requirement and no coding lab; nothing in this repository is due during it.

Criteria are listed by reference number and marks only; this repository does not hold the full criteria. Your course materials for the milestone describe them. The supplied checks are public
and are the whole executable part of each milestone.
Schedule source: frozen calendar `curriculum/snapshot/pep-calendar.json` (SHA-256 `634da8de...ce00`).
