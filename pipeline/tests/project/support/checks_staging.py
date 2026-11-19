"""Idempotent PostgreSQL staging checks (supplied). The test module provides a `connect` fixture."""

from pathlib import Path

from payments_pipeline import importer, staging

from .generated import invoice_set, settlement_set, spring_requests, write_invoices, write_settlements
from .pg import isolated_schema, require_postgres

SAMPLES = Path(__file__).resolve().parents[3] / "data" / "samples"


def _rows(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT invoice_key, supplier_id, invoice_number, amount_cents, currency FROM staging_invoices"
                    " ORDER BY invoice_key")
        invoices = cur.fetchall()
        cur.execute("SELECT reference, amount_cents, currency FROM staging_settlements ORDER BY reference")
        settlements = cur.fetchall()
    conn.commit()
    return invoices, settlements


class StagingChecks:
    def test_loading_the_same_files_twice_changes_nothing(self, connect, tmp_path):
        require_postgres()
        rows, oracle = invoice_set(314)
        invoices = importer.read_invoice_export(write_invoices(tmp_path / "inv.csv", rows))
        settlement_rows, _ = settlement_set(314, spring_requests(314))
        statement = importer.read_settlement_statement(write_settlements(tmp_path / "st.csv", settlement_rows))
        with isolated_schema(connect) as conn:
            staging.create_staging_tables(conn)
            staging.create_staging_tables(conn)
            first_inv = staging.load_invoices(conn, invoices.valid, "inv.csv")
            first_st = staging.load_settlements(conn, statement.valid)
            totals = staging.staging_totals(conn)
            snapshot = _rows(conn)
            assert first_inv == len(oracle["valid_keys"])
            assert first_st == len({r.reference for r in statement.valid})
            assert staging.load_invoices(conn, invoices.valid, "inv.csv") == 0
            assert staging.load_settlements(conn, statement.valid) == 0
            again = importer.read_invoice_export(write_invoices(tmp_path / "inv.json", rows, "json"))
            assert staging.load_invoices(conn, again.valid, "inv.json") == 0
            assert staging.staging_totals(conn) == totals
            assert _rows(conn) == snapshot

    def test_staged_totals_equal_the_integer_oracle(self, connect, tmp_path):
        require_postgres()
        rows, oracle = invoice_set(2718, size=300)
        invoices = importer.read_invoice_export(write_invoices(tmp_path / "inv.csv", rows))
        with isolated_schema(connect) as conn:
            staging.create_staging_tables(conn)
            staging.load_invoices(conn, invoices.valid, "inv.csv")
            totals = staging.staging_totals(conn)
            assert totals["invoice_count"] == len(oracle["valid_keys"])
            assert totals["invoice_cents"] == oracle["total_cents"] and type(totals["invoice_cents"]) is int
            with conn.cursor() as cur:
                cur.execute("SELECT data_type FROM information_schema.columns"
                            " WHERE table_name = 'staging_invoices' AND column_name = 'amount_cents'"
                            " AND table_schema = current_schema()")
                assert cur.fetchone()[0] == "bigint"
            conn.commit()

    def test_repeated_settlement_reference_keeps_the_first_line(self, connect):
        require_postgres()
        statement = importer.read_settlement_statement(SAMPLES / "settlements-mixed.csv")
        with isolated_schema(connect) as conn:
            staging.create_staging_tables(conn)
            assert staging.load_settlements(conn, statement.valid) == 4
            totals = staging.staging_totals(conn)
            assert totals == {"invoice_count": 0, "invoice_cents": 0, "settlement_count": 4,
                              "settlement_cents": 123450 + 8990 + 500000 + 1000}
