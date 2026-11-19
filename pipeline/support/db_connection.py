"""Supplied PostgreSQL connection helper (do not edit).

Fast track: use this instead of writing connection code (the Fast track does not grade it).
Standard track: write your own connection code in src/payments_pipeline/db.py; the Standard
checks require that nothing under src/ imports this module.

Settings come from the environment only (see ../.env.example); nothing secret is stored here.
"""

import os

import psycopg


def connect(**kwargs) -> psycopg.Connection:
    """Opens a new psycopg connection (close it yourself, or use `with connect() as conn:`)."""
    url = (os.environ.get("PIPELINE_DATABASE_URL") or "").strip()
    if url:
        return psycopg.connect(url, connect_timeout=5, **kwargs)
    return psycopg.connect(connect_timeout=5, **kwargs)
