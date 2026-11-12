# Releases

This repository is the Foundation Project 1 starter: a complete stubbed `main` that already holds
the supplied files and stubs for all six milestones. Each milestone below is still called a
"release" because it names what was supplied for that week and which public tests become due.

## Rules of the release model

1. **Releases only add.** Release content is only ever added under supplied paths:
   `src/test/java/payments/release/**`, `src/test/java/payments/support/**`, `tests/**`,
   `release/**`, `docs/templates/**` and fixtures (`tests/m1/fixtures/**`,
   `src/test/resources/payments/release/**`). Merging a release can never conflict with your work.
2. **A later release never overwrites a learner-owned file** (see [LEARNER-OWNED.md](LEARNER-OWNED.md)).
   A stub becomes yours when it arrives and is never re-released. When a later milestone needs a
   change in a file you own, or in a supplied interface, it arrives as written instructions or a
   patch note that you apply yourself:
   - `release/m4/pom-additions.md` and `release/m5/pom-additions.md` document the dependencies the
     supplied `pom.xml` already contains (nothing to paste);
   - `release/m5/saveSettlement-interface-addition.md` (one method on `PaymentRepository`, plus the
     matching one-method in-memory addition).
3. **Released tests accumulate.** Tests under `src/test/java/payments/release/mN/` stay valid in
   every later milestone: desks are created through `PaymentDesk.create(directory, policy)`,
   `NotFoundException` extends `IllegalArgumentException`, and `PaymentConflictException` and
   `ApprovalRefusedException` extend `IllegalStateException`.
4. **Manifests.** `release/mN/MANIFEST.sha256` lists the SHA-256 of that milestone's supplied files
   that must stay byte-for-byte unchanged. Check them from the repository root:

   ```sh
   shasum -a 256 -c release/m3/MANIFEST.sha256      # macOS
   sha256sum -c release/m3/MANIFEST.sha256          # Linux / Git Bash
   ```

   Files that you are expected to edit (stubs, `pom.xml`, `README.md`, the M1 script's marked
   lines) are not in any manifest. `PaymentRepository.java` is the one supplied file you change
   (M5, by the written addition), so it is checked separately: before M5 against
   `release/m4/PaymentRepository.before-m5.sha256`, from M5 against
   `release/m5/PaymentRepository.after-m5.sha256`.
5. **No hidden tests.** The public tests in this repository are the whole executable contract.
   Human reviewers additionally read your code, stories and notes.

## Release 1 (M1): repository, user stories, settlement-export summary

Supplied: `RELEASES.md`, `LEARNER-OWNED.md`, `.gitattributes`, `scripts/settlement-summary.sh`
skeleton (argument and missing-file checks written; you edit only between the `TODO(M1)` markers),
`tests/m1/run-m1.sh`, `tests/m1/fixtures/*.txt`, `docs/templates/` (user stories, AI-use log,
PR description), `release/m1/skeleton-outside-markers.sha256`, `release/m1/MANIFEST.sha256`, and the
local branch `teammate/ap-contact`, which edits `README.md` so that it conflicts with your README edit.
Test: `sh tests/m1/run-m1.sh`.

## Release 2 (M2): exact amount parsing and payment-intake rules

Supplied: `pom.xml` (first and only release of it; yours afterwards), the `PaymentRules` stub,
`src/test/java/payments/release/m2/PaymentRulesReleaseTest.java`, `release/m2/MANIFEST.sha256`.
Test: `mvn -q test -Pm2`.

## Release 3 (M3): lifecycle, approval controls, attributable history

Supplied: `PaymentEvent`, `Approver`, `ApprovalPolicy`, `AllOfPolicy` (worked composition example),
the identity stub `ActorDirectory` and `InMemoryActorDirectory` (synthetic REQ-/APR- ids; NOT
authentication), stubs for `PaymentStatus`, `PaymentRequest`, `WithinLimitPolicy`,
`NotOwnRequestPolicy`, `Policies` and `PaymentDesk`, `docs/templates/decisions.md`,
`DomainReleaseTest`, `DeskReleaseTest`, `release/m3/MANIFEST.sha256`.
Test: `mvn -q test -Pm3`.

## Release 4 (M4): JSON API with stubbed actor identity, and your tests

Supplied: `PaymentRepository` (interface), the `PaymentApi` skeleton (start, POST
`/payment-requests`, GET `/payment-requests/{id}`, X-Actor-Id stub resolution), `PaymentView`,
`EventView`, stubs for `InMemoryPaymentRepository` and the three exceptions, the learner test
skeleton `PaymentDeskTest`, `release/m4/pom-additions.md` (notes on the Javalin 7.2.3 bundle and Mockito 5.24.0 already in `pom.xml`),
`ApiReleaseTest` (real HTTP on a free localhost port), `RepositoryContractTest`, the test helpers in
`src/test/java/payments/release/support/`, `release/m4/MANIFEST.sha256`.
Test: `mvn -q test -Pm4`.

## Release 5 (M5): PostgreSQL persistence and atomic settlement

Supplied: `src/test/java/payments/support/TestDatabase.java` (creates and drops a uniquely named
database through PG* variables), `App.java` (memory or jdbc storage from `PAYMENTS_STORE`),
`Settlement`, stubs for `DbConfig`, `JdbcPaymentRepository`, `schema.sql` and the learner test
`JdbcPaymentRepositoryTest`, `release/m5/pom-additions.md` (notes on the PostgreSQL JDBC driver 42.7.14 already in `pom.xml`),
`release/m5/saveSettlement-interface-addition.md` with its patch file, `JdbcRepositoryReleaseTest`,
`ApiOnJdbcReleaseTest`, `release/m5/MANIFEST.sha256`.
First: apply the written interface addition. Test: `mvn -q test -Pm5` with a local PostgreSQL. If no database
is reachable the database tests fail with a message naming the PG* variables to set.

## Release 6 (M6): reconciliation reports; Project 1 completion

Supplied: `StatementLine`, `ReconciliationResult`, stubs for `Reconciler` and `PaymentReports`,
`src/test/resources/payments/release/m6/statements/*.csv` (synthetic statement exports),
`ReconcilerReleaseTest`, `ReportsRouteReleaseTest`, `docs/templates/walkthrough.md`,
`release/m6/MANIFEST.sha256`.
Test: `mvn -q test` (the complete released suite plus your tests).

## Later tracks (Standard and Fast)

Standard and Fast continue this same application in this same repository. Their releases follow
the rules above: they add supplied support, tests and new files, never overwrite a learner-owned
file, and are not part of the Foundation test selection (M1-M6 commands above). AWS project
integration is not included; any later AWS step is an optional enhancement.
