"""PostgreSQL connection code (Standard track writes this; Fast uses support/db_connection.py).

Settings come from the environment only: PIPELINE_DATABASE_URL, else PGHOST/PGPORT/PGDATABASE/
PGUSER/PGPASSWORD with defaults localhost, 5432, postgres. Never hard-code a password.
"""


def conninfo_from_env(env=None):
    """Returns a libpq conninfo string (PIPELINE_DATABASE_URL as-is, else built from PG*)."""
    raise NotImplementedError("TODO (Standard): build the connection string from the environment")


def connect(env=None, **kwargs):
    """Opens a new psycopg connection."""
    raise NotImplementedError("TODO (Standard): open a psycopg connection")
