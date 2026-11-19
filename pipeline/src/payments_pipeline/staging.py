"""Idempotent PostgreSQL staging of imported invoices and settlements (see CONTRACT.md)."""


def create_staging_tables(conn):
    raise NotImplementedError("TODO: create staging_invoices and staging_settlements if missing")


def load_invoices(conn, rows, source_file):
    """Inserts rows not staged yet; returns how many were inserted."""
    raise NotImplementedError("TODO: idempotent invoice load")


def load_settlements(conn, rows):
    """Inserts settlement lines not staged yet (keyed by reference); returns how many were inserted."""
    raise NotImplementedError("TODO: idempotent settlement load")


def staging_totals(conn):
    """{'invoice_count', 'invoice_cents', 'settlement_count', 'settlement_cents'} as ints."""
    raise NotImplementedError("TODO: staged counts and exact cent totals")
