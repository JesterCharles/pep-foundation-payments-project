# Pipeline: invoice and settlement import, AI categorisation, reconciliation service

Training project with synthetic data only: fictional suppliers and invoices, a stubbed payment
provider, a stubbed AI provider. No real money, bank details or live services. USD only.

This folder is due from Standard week 10 and Fast week 9 (it is in the repository from the start).

| Path | Owner |
|---|---|
| `src/payments_pipeline/**` | **You.** Starts as stubs that raise `NotImplementedError` |
| `tests/` outside `tests/project/` | **You.** Your own tests |
| `pyproject.toml` | Supplied skeleton; you may add dependencies |
| `CONTRACT.md` | Supplied: the names and behaviour the supplied checks call |
| `tests/conftest.py`, `tests/project/**` | Supplied checks (do not edit) |
| `support/db_connection.py` | Supplied PostgreSQL helper for the **Fast** track (Standard must not import it) |
| `data/samples/**` | Supplied synthetic exports (see `data/samples/README.md`) |

## Set up (once)

From `pipeline/`, with Python 3.12 or newer (tested on 3.14):

```sh
python3 -m venv .venv
. .venv/bin/activate            # Windows: .venv\Scripts\activate
python -m pip install -e ".[test]"
```

`.venv/` is git-ignored. The checks need no network: the AI provider and your Spring API are
stubbed inside the tests on `127.0.0.1`.

## Run the checks

```sh
python -m pytest                              # your own tests only
python -m pytest --milestone standard-w10     # Standard: plus the supplied week 10 checks
python -m pytest --milestone fast-w09         # Fast: plus the supplied week 9 checks
```

Later weeks of your track (for example `--milestone standard-w15`) run the same supplied pipeline
checks again. The selection only ever runs your own track's checks.

The supplied checks fail until you implement `src/payments_pipeline/` (that is expected).

### PostgreSQL staging checks

The staging checks need a local PostgreSQL and **skip with a message** when none is configured.
To run them, set either a full URL or the usual `PG*` variables in your shell (never commit them):

```sh
export PIPELINE_DATABASE_URL="postgresql://payments_dev@localhost:5432/payments_dev"
# or: export PGHOST=localhost PGPORT=5432 PGDATABASE=payments_dev PGUSER=payments_dev
```

Each check creates a throw-away schema and drops it afterwards. Put the password in `~/.pgpass`
or `PGPASSWORD`, never in code or in a committed file.

- **Standard:** write your own connection code in `src/payments_pipeline/db.py`
  (`conninfo_from_env` and `connect`). The Standard checks fail if anything in `src/` imports `support`.
- **Fast:** connection code is supplied (`support/db_connection.py`) and is not graded.

## Run the service by hand

After your implementation passes, start the reconciliation service against your Spring API:

```sh
export SPRING_API_BASE_URL=http://localhost:8080
export SPRING_API_TOKEN="<a token from POST /auth/login on your own API>"
export STATEMENTS_DIR=data/samples
uvicorn --factory payments_pipeline.service:app_from_env --app-dir src --port 8000
curl "http://localhost:8000/discrepancies?statement=settlements-mixed.csv"
```

The token is a local development token from your own API. Do not paste it into files or commits.
