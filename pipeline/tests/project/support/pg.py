"""PostgreSQL staging fixtures (supplied). Checks that need PostgreSQL skip when none is configured."""

import os
import uuid
from contextlib import contextmanager

import pytest


def postgres_configured():
    return bool((os.environ.get("PIPELINE_DATABASE_URL") or "").strip() or (os.environ.get("PGHOST") or "").strip())


SKIP_REASON = ("PostgreSQL staging checks skipped: set PIPELINE_DATABASE_URL or the PG* variables "
               "(see README.md) to run them against your local PostgreSQL")


@contextmanager
def isolated_schema(connect):
    """Opens a connection with the learner's/supplied connect() and a throw-away schema as search_path."""
    conn = connect()
    schema = "p2check_" + uuid.uuid4().hex[:12]
    try:
        with conn.cursor() as cur:
            cur.execute(f'CREATE SCHEMA "{schema}"')
            cur.execute(f'SET search_path TO "{schema}"')
        conn.commit()
        yield conn
    finally:
        try:
            conn.rollback()
            with conn.cursor() as cur:
                cur.execute(f'DROP SCHEMA IF EXISTS "{schema}" CASCADE')
            conn.commit()
        finally:
            conn.close()


def require_postgres():
    if not postgres_configured():
        pytest.skip(SKIP_REASON)
