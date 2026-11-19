# Learner-owned paths

Project 2 has three kinds of path. The supplied checks and reviewers rely on this split, and
[SUPPLIED.sha256](SUPPLIED.sha256) lists the SHA-256 of every supplied file so supplied bytes can be told apart from yours.

## 1. Starting source (yours to change from day one)

The **supplied payments baseline** is a complete, working Javalin + JDBC payment approval service with its passing
test suite. It defines the behaviour your Spring Boot API must keep. It is participant-editable starting source, not a
protected supplied path: port it, refactor it, and retire the Javalin parts when your Spring API replaces them.

- `api/src/main/java/payments/**` (the baseline packages `api`, `desk`, `domain`, `identity`, `intake`, `jdbc`,
  `reports`, the baseline entry point `App.java`, and the Spring entry point `PaymentsApplication.java`)
- `api/src/main/resources/db/schema.sql` (the baseline's JDBC schema)
- `api/src/test/java/payments/baseline/**` and `api/src/test/resources/payments/baseline/**` (the baseline's tests;
  keep them passing while the baseline code exists, delete or adapt them when you retire it)

## 2. Learner-owned files (yours; no release overwrites them)

| First week (Standard / Fast) | Path | What you do |
|---|---|---|
| W07 / W07 | `api/src/main/java/**` | The Spring Boot port and every later API feature |
| W07 / W07 | `api/pom.xml` | Supplied once as a skeleton; you paste the written dependency additions from RELEASES.md |
| W08 / W08 | `api/src/main/resources/application*.yml`, `api/src/main/resources/db/migration/**` | Configuration (secrets as `${NAME}` only) and Flyway migrations |
| W09 / W08 | `api/src/test/java/**` outside `payments/project/` | Your own control tests |
| W10 / W09 | `pipeline/src/**` | The import pipeline and FastAPI service (`payments_pipeline` package) |
| W10 / W09 | `pipeline/pyproject.toml` | Supplied skeleton; you may add dependencies |
| W10 / W09 | `pipeline/tests/**` outside `tests/project/` and `tests/conftest.py` | Your own pipeline tests (optional) |
| W11 / W09 | `web/index.html`, `web/src/**` | The browser client |
| W12 / W10 | `web/tests/**` outside `tests/project/`, or `web/src/**/*.test.js` | Your Vitest test of a helper |
| W12 / W10 | `ui/src/**` | The React client |
| W13 / W11 | `ui/tests/**` outside `tests/project/` | Your Testing Library tests |
| W14 / W11 | `deploy/compose.yaml`, `api/Dockerfile`, `pipeline/Dockerfile` | Compose description and Dockerfiles (static checks only; no container engine runs) |
| - / W11 | `.github/workflows/ci.yml` | Fast: the CI workflow (ships inert, `workflow_dispatch` only, until you write it). Standard: optional |
| every week | `docs/milestones/WNN.md`, `docs/ai-use-log.md` | Your milestone note and AI-use log |
| W15 / W11 | `README.md`, `docs/walkthrough.md` | Run instructions and walkthrough notes |
| any | `docs/decisions.md`, `.gitignore`, `.env.example` | Optional notes; ignore rules; safe example settings (never real secrets) |

## 3. Supplied paths (do not edit)

- `RELEASES.md`, `LEARNER-OWNED.md`, `.gitattributes`, `scripts/**`
- `docs/prd.md`, `docs/milestone-maps/**`, `docs/contracts/**`, `docs/tracks/**`, `docs/onboarding.md`, `docs/templates/**`
- `api/src/test/java/payments/project/**`, `api/src/test/resources/project/**`
- `pipeline/CONTRACT.md`, `pipeline/README.md`, `pipeline/.gitignore`, `pipeline/support/**`,
  `pipeline/data/samples/**`, `pipeline/tests/conftest.py`, `pipeline/tests/project/**`
- `web/package.json`, `web/package-lock.json`, `web/vite.config.js`, `web/vitest.config.mjs`, `web/README.md`,
  `web/tests/run.mjs`, `web/tests/project/**`
- `ui/package.json`, `ui/package-lock.json`, `ui/index.html`, `ui/vite.config.js`, `ui/vitest.config.js`, `ui/README.md`,
  `ui/tests/run.mjs`, `ui/tests/setup.js`, `ui/tests/project/**`
- `deploy/package.json`, `deploy/package-lock.json`, `deploy/README.md`, `deploy/tests/**`

Supplied checks under `*/tests/project/**` and `api/src/test/java/payments/project/**` are public: they are the whole
executable part of each milestone. Reference solutions are not in this repository.
