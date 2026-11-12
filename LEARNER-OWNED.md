# Learner-owned paths

You edit these paths, and only these. A file becomes yours when it arrives; no later release
overwrites it. Every other path in the repository is supplied: leave it as released.

| Milestone | Path | What you do |
|---|---|---|
| M1 | `README.md` | Team line, then keep both sections when merging `teammate/ap-contact`; later M5 configuration and M6 run/demo sections |
| M1 | `docs/user-stories.md` | At least three stories with testable acceptance criteria |
| M1 | `docs/ai-use-log.md` | Log AI help (or state none was used), every milestone |
| M1 | `docs/pr-m1.md` | Only if no hosted pull request is available (template in `docs/templates/pr-m1.md`) |
| M1 | `.gitignore` | Rules for `.env`, `target/`, `.idea/`, `.vscode/`, `.DS_Store` |
| M1 | `.env.example` | `PAYMENTS_PORT`, `PAYMENTS_LOG_LEVEL` example values; M5 adds `PAYMENTS_STORE` |
| M1 | `scripts/settlement-summary.sh` | Only the lines between the `TODO(M1) BEGIN` and `END` markers |
| M2 | `pom.xml` | Supplied once in M2; afterwards you apply the written additions in `release/m4/` and `release/m5/` |
| M2 | `src/main/java/payments/intake/PaymentRules.java` | Method bodies |
| M3 | `src/main/java/payments/domain/PaymentStatus.java` | `canMoveTo` |
| M3 | `src/main/java/payments/domain/PaymentRequest.java` | Fields, constructor, transitions, history; M5 adds `restore(...)` |
| M3 | `src/main/java/payments/domain/WithinLimitPolicy.java` | Limit control |
| M3 | `src/main/java/payments/domain/NotOwnRequestPolicy.java` | Own-request control |
| M3 | `src/main/java/payments/domain/Policies.java` | Factory by name |
| M3 | `src/main/java/payments/desk/PaymentDesk.java` | Methods marked TODO; M4 repository constructor; M5 settle via `saveSettlement` |
| M3 | `docs/decisions.md` | One design choice and its trade-off (at most 200 words) |
| M4 | `src/main/java/payments/desk/InMemoryPaymentRepository.java` | Pass the repository contract; M5 adds `saveSettlement` |
| M4 | `src/main/java/payments/desk/NotFoundException.java` | Constructor and messages |
| M4 | `src/main/java/payments/desk/PaymentConflictException.java` | Constructor and messages |
| M4 | `src/main/java/payments/desk/ApprovalRefusedException.java` | Constructor and messages |
| M4 | `src/main/java/payments/api/PaymentApi.java` | Supplied skeleton; you add error mapping, action routes, logging (M4) and the report route (M6) |
| M4 | `src/test/java/payments/learner/PaymentDeskTest.java` | At least three tests of the money controls |
| M5 | `src/main/resources/db/schema.sql` | Tables and constraints |
| M5 | `src/main/java/payments/jdbc/DbConfig.java` | `jdbcUrl` |
| M5 | `src/main/java/payments/jdbc/JdbcPaymentRepository.java` | JDBC repository with transactions |
| M5 | `src/main/java/payments/desk/PaymentRepository.java` | Apply the one-method addition from `release/m5/saveSettlement-interface-addition.md` only |
| M5 | `src/test/java/payments/learner/JdbcPaymentRepositoryTest.java` | At least one database test |
| M6 | `src/main/java/payments/reports/Reconciler.java` | Reconciliation |
| M6 | `src/main/java/payments/reports/PaymentReports.java` | Exact reports and `Optional` lookup |
| M6 | `docs/walkthrough.md` | Five-heading walkthrough outline |

## Supplied paths (do not edit)

- `RELEASES.md`, `LEARNER-OWNED.md`, `.gitattributes`, `release/**`, `docs/templates/**`
- `tests/m1/**` (runner and fixtures)
- `src/test/java/payments/release/**`, `src/test/java/payments/support/**`, `src/test/resources/payments/release/**`
- `src/main/java/payments/domain/{PaymentEvent,Approver,ApprovalPolicy,AllOfPolicy,Settlement}.java`
- `src/main/java/payments/identity/**` (identity stub, not authentication)
- `src/main/java/payments/desk/PaymentRepository.java` (except the one M5 addition you apply)
- `src/main/java/payments/api/{PaymentView,EventView}.java`
- `src/main/java/payments/reports/{StatementLine,ReconciliationResult}.java`
- `src/main/java/payments/App.java`
- the lines of `scripts/settlement-summary.sh` outside the `TODO(M1)` markers
