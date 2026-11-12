# Payment Approval and Reconciliation Platform

Foundation Project 1. Over six milestones (M1 to M6) you build a small payment service for
**Northwind Components**, a made-up company that pays its suppliers' invoices. Staff submit payment
requests, managers approve or reject them, and the service records when each one is paid. At the end
it checks those payments against a bank statement and reports anything that does not match.

All the data is invented for training. No real money moves and no bank is contacted. Never put real
names, account numbers, passwords or keys in any file.

## Start here

1. **Open the project.** Open the project folder you were given in VS Code.
2. **Open a terminal.** In VS Code choose **Terminal > New Terminal**. Run every command below in
   that terminal, from the project folder.
3. **Run the first tests.** You need no setup command to start.

   ```sh
   sh tests/m1/run-m1.sh
   mvn -q test -Pm2
   ```

   Both runs fail at first, and that is expected. The M1 script prints a `PASS` or `FAIL` line for
   each check, and each `FAIL` line says what to do next. The Maven run reports failing tests whose
   messages contain `TODO M2:`. These messages mark work you have not done yet. They do not mean
   your setup is broken.
4. **Work through M1 to M6 in order.** Follow the checklist below.

```text
Open project -> Run tests -> M1 -> M2 -> M3 -> M4 -> M5 -> M6 -> Final test
```

Progress checklist (tick each box when its test command passes):

- [ ] M1 - Git hygiene, user stories, settlement summary, teammate merge
- [ ] M2 - Exact money rules
- [ ] M3 - Payment requests, approvals and history
- [ ] M4 - Storage seam, errors and the HTTP API
- [ ] M5 - PostgreSQL storage and settlements
- [ ] M6 - Reconciliation and reports
- [ ] Final test - `sh tests/m1/run-m1.sh` and `mvn -q test` both pass

## How every milestone works

1. **Edit.** Open the files listed for the milestone and search them for its `TODO` comments, such
   as `TODO(M2)`. Edit only those files. [LEARNER-OWNED.md](LEARNER-OWNED.md) lists every file you
   may edit.
2. **Run** the milestone's test command.
3. **Read** the feedback. A failure whose message contains `TODO Mn:` (for example `TODO M2:`) is a
   stub you have not written yet.
   Any other failure tells you what your code got wrong.
4. **Stop** when the command passes. Then commit your work and move on.

Each Maven command also re-runs the tests of the earlier milestones, so finished work stays checked.
The tests in this repository are the whole automated check; there are no hidden tests. Reviewers also
read your code, stories and notes. Do not edit the supplied tests, fixtures, `release/` files,
`RELEASES.md` or `LEARNER-OWNED.md`. Log any AI help in `docs/ai-use-log.md` at every milestone.
Commit in small steps, on a branch per milestone (for example `m2/payment-rules`) that you merge into
`main`.

## Business rules

These rules hold in every milestone:

- Single currency: USD. Any other currency is a discrepancy to report, never to convert.
- Money is exact: whole cents in a `long` (BIGINT in the database), never `double` or `float`.
  A valid amount is 0.01 to 9,999,999.99 USD.
- Supplier ids are `SUP-` plus four digits. The invoice key `SUPPLIER|INVOICE` (upper case, invoice
  without spaces) may be requested only once, ever.
- Lifecycle: SUBMITTED to APPROVED, REJECTED or CANCELLED; APPROVED to PAID or CANCELLED. Nothing else.
- No approval above the approver's limit, and nobody approves their own request.
- Every change, refused approval and correction is an event naming the actor, action and detail.
- A request becomes PAID only with a settlement for exactly its amount in USD. The status, the
  settlement and its event are stored together or not at all.

Actor ids (REQ-xxxx requesters, APR-xxxx approvers) come from the supplied `ActorDirectory`. This is
a stand-in for identity, not real authentication.

## The milestones

### M1 - Git hygiene, user stories, settlement summary, teammate merge

- **Goal:** set up the repository safely, describe the work as user stories, write a small shell
  summary of a settlement export, and merge a teammate's change.
- **Edit:** `docs/user-stories.md`, `docs/ai-use-log.md`, `.gitignore`, `.env.example`,
  `scripts/settlement-summary.sh` (only between its two M1 markers), and the Team line in this README.
- **Run:** `sh tests/m1/run-m1.sh`
- **Done when:** the output has no `FAIL` line.

The teammate merge needs the branch `teammate/ap-contact`. Check that it is there:

```sh
git branch -a --list '*teammate/ap-contact'
```

- If `teammate/ap-contact` is listed, it is ready.
- If only `remotes/origin/teammate/ap-contact` is listed, run
  `git branch teammate/ap-contact origin/teammate/ap-contact`.
- If nothing is listed, run `git fetch origin teammate/ap-contact:teammate/ap-contact`.

Commit your Team line first, then run `git merge teammate/ap-contact`. Git stops with a conflict in
`README.md`; that conflict is part of the exercise. Keep both your Team line and the teammate's
"Accounts-payable contact" section, delete the conflict marker lines (`<<<<<<<`, `=======`,
`>>>>>>>`), then run `git add README.md` and `git commit`.

If you cannot use a hosted pull request, describe your M1 change in `docs/pr-m1.md` using
`docs/templates/pr-m1.md`.

### M2 - Exact money rules

- **Goal:** turn amount text into exact cents and back, build the invoice key, total amounts per
  supplier and search a sorted list.
- **Edit:** `src/main/java/payments/intake/PaymentRules.java` (method bodies).
- **Run:** `mvn -q test -Pm2`
- **Done when:** the command finishes with no failing tests.

### M3 - Payment requests, approvals and history

- **Goal:** model the payment lifecycle, the approval controls and an attributable history, and
  run them through the payment desk.
- **Edit:** `PaymentStatus.java`, `PaymentRequest.java`, `WithinLimitPolicy.java`,
  `NotOwnRequestPolicy.java`, `Policies.java` (in `src/main/java/payments/domain/`),
  `src/main/java/payments/desk/PaymentDesk.java`, and `docs/decisions.md` (one design choice and its
  trade-off, at most 200 words).
- **Run:** `mvn -q test -Pm3`
- **Done when:** the command finishes with no failing tests.

### M4 - Storage seam, errors and the HTTP API

- **Goal:** put storage behind a repository, add clear exceptions, and offer the desk as a JSON HTTP
  API that returns honest error codes. The API reads the actor from an `X-Actor-Id` header that
  anyone can set, so it is still not authentication.
- **Edit:** `InMemoryPaymentRepository.java`, `NotFoundException.java`,
  `PaymentConflictException.java`, `ApprovalRefusedException.java`, the M4 markers in
  `PaymentDesk.java` (all in `src/main/java/payments/desk/`), `src/main/java/payments/api/PaymentApi.java`,
  and `src/test/java/payments/learner/PaymentDeskTest.java` (at least three tests of your own for the
  money controls).
- **Run:** `mvn -q test -Pm4`
- **Done when:** the command finishes with no failing tests.

### M5 - PostgreSQL storage and settlements

- **Goal:** store payments in PostgreSQL and record each settlement atomically.
- **First:** follow `release/m5/saveSettlement-interface-addition.md`. It adds one method to
  `PaymentRepository.java` with a supplied patch; change nothing else in that file.
- **Edit:** `src/main/resources/db/schema.sql`, `src/main/java/payments/jdbc/DbConfig.java`,
  `src/main/java/payments/jdbc/JdbcPaymentRepository.java`, the M5 markers in `PaymentDesk.java` and
  `PaymentRequest.java`, `InMemoryPaymentRepository.java` (its `saveSettlement`),
  `src/test/java/payments/learner/JdbcPaymentRepositoryTest.java` (at least one database test of your
  own), `PAYMENTS_STORE` in `.env.example`, and the Configuration section of this README.
- **Run:** `mvn -q test -Pm5`
- **Needs:** a PostgreSQL database that the tests can reach through the `PGHOST`, `PGPORT`, `PGUSER`
  and `PGDATABASE` variables, with the password in `~/.pgpass` (never in a project file). Ask your
  trainer which database to use. Each database test creates its own temporary database and drops
  it afterwards.
- **Done when:** the command finishes with no failing tests.

### M6 - Reconciliation and reports

- **Goal:** compare recorded settlements with a bank-statement file, produce exact reports, serve
  the reconciliation report over HTTP, and write your run and walkthrough notes.
- **Edit:** `Reconciler.java` and `PaymentReports.java` (in `src/main/java/payments/reports/`),
  the M6 markers in `src/main/java/payments/api/PaymentApi.java`, `docs/walkthrough.md`, and the Run
  and demo section of this README.
- **Run:** `mvn -q test`
- **Done when:** the command finishes with no failing tests.

### Final test

Run both commands. You are finished when both pass:

```sh
sh tests/m1/run-m1.sh
mvn -q test
```

Later course tracks continue this same repository. They add new files and tests, and they never
overwrite the files you own ([RELEASES.md](RELEASES.md)).

## Your project notes

You fill in the three sections below during M1, M5 and M6.

## Team

TODO(M1): replace this line with your name or initials and the user story you own.

## Configuration

Copy `.env.example` to `.env` for your own machine; `.env` is never committed.

TODO(M5): document `PAYMENTS_STORE=memory|jdbc` here (what each value does and which PG* variables jdbc needs).

## Run and demo

TODO(M6): write how to start the service with environment variables only (for example
`PAYMENTS_STORE`, `PAYMENTS_PORT`), and a short demo: submit, approve, see a refusal, and fetch
`GET /reports/reconciliation?statement=mixed-discrepancies.csv`. Use the synthetic actor ids from
`InMemoryActorDirectory.withSampleActors()`.

## Optional: working on your own computer

This section is optional. Use it only if you choose to work on your own computer instead of the
project you were given.

- Install JDK 25 and Maven as in the Java pages, and for M5 a local PostgreSQL as in the JDBC lab.
  No container engine is used.
- Clone the project with `git clone https://github.com/JesterCharles/pep-foundation-payments-project.git`,
  open the folder in VS Code, then follow "Start here". Get the teammate branch as described in M1.

## Troubleshooting

- **`mvn` or `java` is not found.** The terminal cannot see Java or Maven. Check with
  `java -version` (the project needs Java 25) and `mvn -v`, and ask your trainer.
- **Maven test failures mention `TODO M2:` (or a later milestone).** That is expected until you finish
  that milestone's stubs.
- **`COMPILATION ERROR`.** Read the first `[ERROR]` line. It names the file and line to fix.
- **M5 tests say PostgreSQL is not reachable.** Set `PGHOST`, `PGPORT`, `PGUSER` and `PGDATABASE`
  in your terminal, keep the password in `~/.pgpass`, and check that `psql -c 'select 1'` works in
  the same terminal. Use `PGHOST=localhost`, not a socket folder.
- **The M1 script prints `SKIP ... teammate/ap-contact branch not present`.** Get the branch with
  the steps in M1. If `git fetch` fails, ask your trainer.
- **A `FAIL` line says a released file changed, or you edited a supplied file by mistake.** Run
  `git restore <file>` to bring back the last committed version of that file. If you already
  committed the change, ask your trainer.
