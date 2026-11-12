# Payment Approval and Reconciliation Platform

Foundation Project 1 for the PEP programme. Over six weekly milestones (M1 to M6) you build one
payment approval and reconciliation service for **Northwind Components**, a fictional manufacturer
that pays supplier invoices. It starts as a Git repository with payment user stories and a
settlement-export summary script, and finishes as a tested Java HTTP service over PostgreSQL that
refuses duplicate invoices and limit bypasses, keeps an attributable history, records settlements
atomically and reconciles a synthetic bank-statement export.

## Synthetic data only: no real money

Everything in this repository is invented for training: suppliers, invoices, people, actor ids,
amounts, settlement references and statements. No real funds move, no bank or payment provider is
contacted, and no real account numbers, banking details or credentials belong in any file. Nothing
here is a regulatory or compliance claim, and no accounting expertise is assumed.

Domain rules you will meet:

- Single currency: USD. Any other code is a discrepancy to report, never to convert.
- Money is exact: whole cents in a `long` (BIGINT in the database), never `double` or `float`.
  A valid amount is 0.01 to 9,999,999.99 USD.
- Supplier ids are `SUP-` plus four digits. The invoice key `SUPPLIER|INVOICE` (upper case, invoice
  without spaces) may be requested only once, ever.
- Lifecycle: SUBMITTED to APPROVED, REJECTED or CANCELLED; APPROVED to PAID or CANCELLED. Nothing else.
- No approval above the approver's limit, and nobody approves their own request.
- Every change, refused approval and correction is an event naming the actor, action and detail.
- A request becomes PAID only with a settlement for exactly its amount in USD; the status, the
  settlement and its event are stored together or not at all.

**Identity is a stub, not authentication.** Actor ids (REQ-xxxx requesters, APR-xxxx approvers)
come from the supplied `ActorDirectory`, and from M4 the HTTP API reads them from an `X-Actor-Id`
header that anyone can set. Real authentication arrives in the Standard/Fast continuation.

## How the milestones work

- `main` already holds the scaffold for all six milestones: supplied files, stubs with
  `TODO(Mx)` comments, and the public released tests. Stubs throw
  `UnsupportedOperationException("TODO Mx: ...")` until you implement them.
- [LEARNER-OWNED.md](LEARNER-OWNED.md) lists every file you edit. Everything else is supplied:
  do not edit released tests, fixtures or manifests (`release/mN/MANIFEST.sha256` lets reviewers check).
- [RELEASES.md](RELEASES.md) explains the release model. A release only ever adds files under
  supplied paths; changes to your own files (for example the M5 `saveSettlement` addition) arrive
  as written instructions that you apply.
- Work on a branch per milestone (`m1/settlement-summary`, `m2/payment-rules`, ...), commit in
  small steps, and merge into `main`. Log any AI help in `docs/ai-use-log.md`.
- The released tests are the whole executable contract. There are no hidden tests; human
  reviewers also read your code, stories and notes.
- M1 needs both branches: `main` and `teammate/ap-contact` (a teammate's README edit that conflicts
  with yours on purpose). After cloning or importing, check it is there with
  `git branch -a --list '*teammate/ap-contact'`; if only `origin/teammate/ap-contact` is listed,
  run `git branch teammate/ap-contact origin/teammate/ap-contact`; if nothing is listed (an import
  or clone that brought only `main`), run `git fetch origin teammate/ap-contact:teammate/ap-contact`. Merge it with `git merge teammate/ap-contact`,
  keep both README sections, and commit.

**Wednesday cutoff rule.** Each milestone uses only topics taught up to the Wednesday of its
week. Topics taught on Thursday or Friday become available the following week. That is why, for
example, M2 uses `-1` and `""` sentinels instead of exceptions, and streams and `Optional` appear
only in M6.

| Milestone | What you build | Test command |
|---|---|---|
| M1 | User stories, `.gitignore`/`.env.example` hygiene, `scripts/settlement-summary.sh`, teammate merge | `sh tests/m1/run-m1.sh` |
| M2 | `PaymentRules`: exact cents parsing and formatting, invoice key, supplier totals, binary search | `mvn -q test -Pm2` |
| M3 | Payment request lifecycle, approval policies, attributable history, the payment desk | `mvn -q test -Pm3` |
| M4 | Repository seam, custom exceptions, JSON API with honest errors, your own desk tests | `mvn -q test -Pm4` |
| M5 | PostgreSQL schema, JDBC repository, atomic settlement, your database test | `mvn -q test -Pm5` (local PostgreSQL via PG* variables) |
| M6 | Reconciliation, exact reports, the reconciliation route, run/demo notes and walkthrough | `mvn -q test` |

Each Maven profile runs the released tests due by that milestone, so earlier milestones keep being
checked. `pom.xml` already holds every dependency (Javalin for M4, the PostgreSQL driver for M5);
before M5 apply only `release/m5/saveSettlement-interface-addition.md`. Until you finish a milestone,
its tests fail with `TODO Mn: ...` messages: that is the intended starting point, not a broken setup.

**Time.** Milestone time boxes are planning caps, not measured learner times, and the counted-once
programme schedule does not yet prove fit (see the programme's timing reconciliation record).

## Later tracks extend this same repository

This Foundation repository is the base of one cumulative application. Later Standard and Fast
milestone releases continue the same payment platform: they add supplied support files, new
tests and new files, and evolve the same application, but never overwrite a learner-owned file
(changes to your files arrive as written instructions, as in M5). Advanced-track TODOs and tests
are not part of the Foundation test selection above. AWS integration is not part of this project:
it is absent here and, if offered later, optional.

## What you need

- JDK 25 and Maven (as installed in the Java pages). The project targets Java 25.
- From M5, a local PostgreSQL reachable with the same PG* variables `psql` uses (as in the JDBC
  lab). Keep the password in `~/.pgpass`, never in the project. No container engine is used.

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
