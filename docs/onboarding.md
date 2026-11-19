# Onboarding: the payment domain and the supplied baseline

You do not need to have built a payments project before. This page explains the business words used everywhere in
Project 2 and tours the working baseline you start from. Everything is synthetic: a fictional company pays fictional
supplier invoices in US dollars. No real money, bank details, providers or accounting rules are involved.

## The business in one paragraph

A **requester** submits a **payment request** to pay a supplier's invoice. An **approver** approves or rejects it, but
only up to their **approval limit** and never for a request they submitted themselves (**separation of duties**).
A **payables clerk** sends approved payments to a **payment provider** (a stub here), which answers ACCEPTED or REJECTED.
Later the provider's **settlement statement** lists what was actually paid. **Reconciliation** compares that statement
with the requests; every difference becomes an **exception case** that a payables clerk resolves with a note.

## Glossary

| Term | Meaning in this project |
|---|---|
| Payment request | One invoice to pay: supplier id, invoice number, amount, currency, requester, status, history |
| Invoice key | `SUPPLIER|INVOICE`, upper-cased with spaces removed. Two requests with the same key are a duplicate (409) |
| Cents | Money is a whole number of cents in a `long` / `BIGINT`. `"1234.50"` is 123450. Never `double` |
| Approval limit | The largest amount an approver may approve: APR-0001 5,000.00; APR-0002 25,000.00; APR-0003 9,999,999.99 |
| Separation of duties | Requesters cannot approve; nobody approves their own request; only payables clerks send payments |
| History | Every change and every refusal is an event `(seq, actorId, action, detail)`. Nothing is deleted |
| Idempotency key | One key per request (`payment-request-<id>`) sent on every attempt, so a retry never pays twice |
| Settlement statement | The provider's export of settled payments (`reference,provider_payment_id,amount,currency,settled_on`) |
| Reconciliation | Matching statement lines to requests: matched, amount mismatch, currency mismatch, unknown reference, duplicate line |
| Exception case | A payment needing attention (provider REJECTED or a reconciliation difference), OPEN until resolved with a note |

## Statuses

`SUBMITTED -> APPROVED | REJECTED | CANCELLED`, `APPROVED -> CANCELLED | SENT | EXCEPTION`, `SENT -> PAID | EXCEPTION`, `PAID -> EXCEPTION` (a reconciliation difference).
SENT and EXCEPTION arrive with the provider client (Standard W09, Fast W08). The baseline also moves
APPROVED -> PAID when a settlement for exactly the amount is recorded. Any other move is refused with 409.

## Tour of the baseline (`api/src/main/java/payments/`)

| Package | What it holds |
|---|---|
| `intake` | `PaymentRules`: parse amount text to cents, format cents, build invoice keys |
| `domain` | `PaymentRequest` (lifecycle and history), `PaymentStatus`, approval policies (`WithinLimitPolicy`, `NotOwnRequestPolicy`, `AllOfPolicy`) |
| `identity` | `ActorDirectory`: the synthetic actors. A stub, not authentication |
| `desk` | `PaymentDesk`: the service that applies the rules; `PaymentRepository` with in-memory and JDBC implementations |
| `jdbc` | `JdbcPaymentRepository` (transactions with plain JDBC) and `DbConfig` |
| `reports` | `Reconciler` and `PaymentReports` over a statement |
| `api` | `PaymentApi`: the Javalin HTTP routes your Spring port replaces |

Run its tests with `mvn -q test` in `api/` (202 tests, no database server needed: they use H2 in PostgreSQL mode).
Read `api/src/test/java/payments/baseline/m4/ApiReleaseTest.java` to see the HTTP behaviour your port must keep.
To run the baseline service itself: `mvn -q compile exec:java -Dexec.mainClass=payments.App` (port 7070; the first run
downloads the exec plugin), then for example
`curl -s -H 'X-Actor-Id: REQ-0001' -H 'Content-Type: application/json' -d '{"supplierId":"SUP-0042","invoiceNumber":"INV 1","amount":"10.00","currency":"USD"}' 127.0.0.1:7070/payment-requests`.

## Synthetic users

REQ-0001, REQ-0002, REQ-0003 (requesters); APR-0001, APR-0002, APR-0003 (approvers); PAY-0001 (payables clerk,
from the security week). Once sign-in exists, every user's local development password is `change-me-local-only`.

## Where the rules are written down

The contracts in `docs/contracts/` are the authority for routes, JSON shapes, status codes and tables. Your track's
weekly table is in `docs/tracks/standard.md` or `docs/tracks/fast.md`.
