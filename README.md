# Payment Approval and Reconciliation Platform

Project 2 for the **Standard** and **Fast** tracks. You start from a working payments service (Javalin and plain
JDBC, in `api/`) and turn it, week by week, into a platform:

- a Spring Boot API with JPA, Flyway and token security
- a Python import pipeline and FastAPI reconciliation service
- a plain HTML/CSS/JS client and a React client
- container and CI configuration

Everything is synthetic: a made-up company pays made-up supplier invoices in US dollars. No real money, bank,
payment provider or AI service is involved. Never put real names, account numbers, passwords or keys in any file.

You do not need to have done a payments project before. Read [docs/onboarding.md](docs/onboarding.md) first: it
explains the business words and tours the baseline.

## Start here

1. **Install** JDK 25, Maven 3.9, Python 3.12 or newer, and Node 24.15 or newer (with npm 11).
2. **Fetch dependencies once** (needs the network; everything works offline afterwards):

   ```sh
   scripts/prefetch.sh
   ```

3. **Run the baseline's tests.** They pass from the start:

   ```sh
   (cd api && mvn -q test)
   ```

4. **Open your track's weekly table**:
   - [docs/tracks/standard.md](docs/tracks/standard.md): nine milestones, W07-W15
   - [docs/tracks/fast.md](docs/tracks/fast.md): five milestones, W07-W11

   Each row gives the days, the taught cutoff, the due day, what you change, the criteria names and marks, the paths
   and the check command.
5. **Run your first milestone's checks.** They fail until you do the work; that is expected:

   ```sh
   scripts/check-milestone.sh standard 7     # or: scripts/check-milestone.sh fast 7
   ```

## How every week works

1. **Read** the week's row in your track table and the contract it names in `docs/contracts/`.
2. **Edit** only learner-owned paths ([LEARNER-OWNED.md](LEARNER-OWNED.md)). Search for `TODO(W..)` markers.
3. **Run** `scripts/check-milestone.sh <track> <week>`. Read each failure message: it says what the check expected.
   Write your own tests too; the bare commands (`mvn -q test`, `npm test`, `python -m pytest`) run them.
4. **Write** `docs/milestones/WNN.md` and your `docs/ai-use-log.md` entry. Commit on a branch, open a pull request,
   and merge it before the due day.

Checks accumulate: a week's selector also runs the earlier weeks' checks of your track, so finished work stays
checked. [RELEASES.md](RELEASES.md) explains the selectors and the two `api/pom.xml` additions you paste in yourself.

## Payment rules (every week)

- USD only. Money is exact: whole cents (`long` / `BIGINT`), never `double`. Valid amounts are 0.01 to 9,999,999.99.
- A supplier invoice (`SUPPLIER|INVOICE` key) is paid at most once.
- No approval above the approver's limit; nobody approves their own request; only payables clerks send payments.
- Statuses move only along the allowed lifecycle; every change and every refusal is attributable history.
  Nothing is deleted.
- Secrets come from the environment and are never committed.

## Repository layout

| Path | What it is |
|---|---|
| `api/` | Baseline (Javalin + JDBC, `payments.*`) and your Spring Boot API (`payments.PaymentsApplication`) |
| `pipeline/` | Your Python import pipeline and FastAPI service; contract in `pipeline/CONTRACT.md` |
| `web/` | Your plain HTML/CSS/JS client (Vite) |
| `ui/` | Your React client (Vite) |
| `deploy/` | Your Compose description, with static checks |
| `docs/` | Contracts, track tables, onboarding, milestone notes and templates |
| `scripts/` | Prefetch, milestone checks, reset |

## Running the platform locally

<!-- TODO(W15 Standard / W11 Fast): replace this section with your own run instructions: how to start the API,
     the pipeline service and the clients, which environment variables each needs (names only, never values),
     and how to sign in with a synthetic user. -->

## For course maintainers

`scripts/verify-starter.sh` checks the starter as released: every bare command passes, and every milestone
selection fails until the learner-owned work is done. Supplied-file hashes are in `SUPPLIED.sha256`.
