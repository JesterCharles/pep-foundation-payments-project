# Releases

Project 2 (Standard and Fast tracks) ships as **one snapshot**: every supplied file for every week is already in this
repository on day one, so nothing has to be merged in later and no release can conflict with your work. A "release"
names what becomes **due** that week on your track and which supplied checks start to apply. Your track's weekly
table, with taught cutoffs and due days, is in [docs/tracks/standard.md](docs/tracks/standard.md) or
[docs/tracks/fast.md](docs/tracks/fast.md).

## Rules

1. **Supplied files stay byte-for-byte unchanged.** [SUPPLIED.sha256](SUPPLIED.sha256) lists the SHA-256 of every
   supplied file ([LEARNER-OWNED.md](LEARNER-OWNED.md), section 3). Check them from the repository root:

   ```sh
   shasum -a 256 -c SUPPLIED.sha256      # macOS
   sha256sum -c SUPPLIED.sha256          # Linux / Git Bash
   ```

2. **Your files are never overwritten.** When a later week needs a change in a file you own, it arrives as written
   instructions that you apply yourself: the two API dependency additions below.
3. **Checks are selected, and they accumulate.** A selector `<track>-wNN` runs that track's supplied checks of week
   NN and of every earlier week. Without a selector only the baseline suite and your own tests run, and they pass.
   A track never runs the other track's checks.

   | Area | Command (run in the area's folder) | Supplied check weeks: Standard | Fast |
   |---|---|---|---|
   | `api/` | `mvn -q test -Dmilestone=<track>-wNN` | W07, W08, W09, W15 | W07, W08 |
   | `pipeline/` | `python -m pytest --milestone <track>-wNN` | W10 | W09 |
   | `web/` | `npm test -- <track>-wNN` | W11, W12 | W09, W10 |
   | `ui/` | `npm test -- <track>-wNN` | W12, W13, W14 | W10, W11 |
   | `deploy/` | `npm test -- <track>-wNN` | W14 | W11 |

   Every selector from `standard-w07` to `standard-w15` and `fast-w07` to `fast-w11` is valid in every area; a week
   before an area's first check week runs your own tests only. `scripts/check-milestone.sh <track> <week>` runs every
   area at once.
4. **No hidden tests.** The supplied checks are public and are the whole executable part of each milestone. Reviewers
   also read your code, notes and pull requests. A green check is necessary, not sufficient.

## API dependency additions (written instructions)

`api/pom.xml` is yours. It arrives without Flyway and Spring Security, because each takes effect as soon as it is on the
classpath: Flyway takes over schema creation from Hibernate, and Spring Security locks every endpoint. Paste each block
below the line `<!-- API dependency additions (RELEASES.md) go below this line. -->` when your track reaches it.

**FLYWAY: Standard W08, Fast W08** (with your first migration in `api/src/main/resources/db/migration/`):

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
```

**SECURITY: Standard W09, Fast W08** (with your security configuration and a `dev` profile that keeps the X-Actor-Id stub):

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
```

Versions come from the Spring Boot parent; do not add `<version>` lines. Run `mvn -q dependency:go-offline` once
after pasting, while you have network.

## Week by week

Standard week = schedule week W07-W15 (days S1-S43). Fast week = W07-W11 (days FT1-FT25).

| Week | Standard: due this week | Fast: due this week |
|---|---|---|
| W07 | Spring Boot port, in memory (`api/`); contract `docs/contracts/W07-openapi.yaml` | Spring Boot port with JPA persistence (`api/`); contract `W07-openapi.yaml` incl. `x-persistence` |
| W08 | FLYWAY addition; JPA, migrations, atomic approval, `/correct`, `/approval-queue` (`W08-openapi.yaml`) | FLYWAY and SECURITY additions; everything in `W08-openapi.yaml` and `W08-provider-stub.md` |
| W09 | SECURITY addition; `W09-openapi.yaml`, `W09-provider-stub.md`; your control tests | Pipeline (`pipeline/`, `pipeline/CONTRACT.md`, Fast uses `pipeline/support/db_connection.py`); `web/` form |
| W10 | Pipeline (`pipeline/`, `pipeline/CONTRACT.md`; you write the psycopg connection code) | `web/` API calls and a Vitest test; `ui/` routed workflows |
| W11 | `web/` form and queue page | `ui/` sign-in, roles, exceptions (`W13-exception-cases.yaml`), Testing Library tests; `deploy/`; CI; README and walkthrough |
| W12 | `web/` API calls and a Vitest test; first `ui/` components | - |
| W13 | `ui/` routed workflows and exceptions (`W13-exception-cases.yaml`); Testing Library tests | - |
| W14 | `ui/` sign-in and roles; `deploy/` Compose and Dockerfiles | - |
| W15 | The whole platform end to end; README and walkthrough (due end of S42; S43 is the Final) | - |

Every week also: `docs/milestones/WNN.md` (template in `docs/templates/milestone-note.md`) and `docs/ai-use-log.md`.

## What is not here

Reference solutions, reviewer material and grading data are not in this repository. Docker and Compose files are
checked statically only: no container engine, registry, cloud account or paid CI runner is needed or used. The CI
workflow ships inert (`workflow_dispatch` only).
