"""Checks invoice and settlement parsing with generated input rows (supplied)."""

from pathlib import Path

import pytest

from payments_pipeline import importer

from .generated import invoice_set, write_invoices

SAMPLES = Path(__file__).resolve().parents[3] / "data" / "samples"


class ImportChecks:
    def test_clean_invoice_export(self):
        result = importer.read_invoice_export(SAMPLES / "invoices-clean.csv")
        assert result.rejected == []
        assert len(result.valid) == 8
        assert sum(r.amount_cents for r in result.valid) == 1_003_249_724  # the eight amounts in invoices-clean.csv
        first = result.valid[0]
        assert (first.line, first.supplier_id, first.invoice_key, first.amount_cents, first.currency) == (
            1, "SUP-0001", "SUP-0001|INV-1001", 123450, "USD")

    def test_dirty_invoice_csv_reasons_in_line_order(self):
        result = importer.read_invoice_export(SAMPLES / "invoices-dirty.csv")
        reasons = [(r.line, r.reason) for r in result.rejected]
        assert reasons == [
            (2, "duplicate"), (3, "malformed_amount"), (4, "malformed_amount"), (5, "malformed_amount"),
            (6, "malformed_amount"), (7, "malformed_amount"), (8, "non_usd"), (10, "bad_supplier"),
            (11, "missing_invoice"), (12, "malformed_row"), (14, "malformed_amount"),
        ], reasons
        assert [r.line for r in result.valid] == [1, 9, 13, 15]
        assert [r.invoice_key for r in result.valid][0] == "SUP-0011|INV7781"
        assert result.valid[1].currency == "USD" and result.valid[1].amount_cents == 9900
        assert sum(r.amount_cents for r in result.valid) == 120000 + 9900 + 31040 + 4510
        markup = result.valid[2]
        assert markup.description == "<b>Monitor stands</b> for 1234.50 total" and markup.amount_cents == 31040

    def test_dirty_invoice_json(self):
        result = importer.read_invoice_export(SAMPLES / "invoices-dirty.json")
        assert [(r.line, r.reason) for r in result.rejected] == [
            (2, "duplicate"), (3, "malformed_amount"), (4, "non_usd"), (5, "malformed_row"), (6, "malformed_row")]
        assert [(r.line, r.amount_cents) for r in result.valid] == [(1, 64000), (7, 150000)]

    @pytest.mark.parametrize("seed", [11, 23, 47, 2026])
    @pytest.mark.parametrize("fmt", ["csv", "json"])
    def test_generated_invoice_sets_match_the_oracle(self, tmp_path, seed, fmt):
        rows, oracle = invoice_set(seed)
        result = importer.read_invoice_export(write_invoices(tmp_path / f"invoices.{fmt}", rows, fmt))
        assert [r.invoice_key for r in result.valid] == oracle["valid_keys"]
        assert {r.line: r.reason for r in result.rejected} == oracle["rejected"]
        total = sum(r.amount_cents for r in result.valid)
        assert type(total) is int and total == oracle["total_cents"]
        assert all(type(r.amount_cents) is int for r in result.valid)

    def test_settlement_statement_keeps_repeats_and_currencies(self):
        result = importer.read_settlement_statement(SAMPLES / "settlements-mixed.csv")
        assert [(r.line, r.reference, r.amount_cents, r.currency) for r in result.valid] == [
            (1, "PR-1", 123450, "USD"), (2, "PR-2", 8990, "USD"), (3, "PR-3", 500000, "EUR"),
            (4, "PR-99", 1000, "USD"), (5, "PR-1", 123450, "USD")]
        assert [(r.line, r.reason) for r in result.rejected] == [
            (6, "missing_reference"), (7, "malformed_amount"), (8, "malformed_row")]
        json_result = importer.read_settlement_statement(SAMPLES / "settlements-mixed.json")
        assert [(r.reference, r.currency) for r in json_result.valid][1] == ("PR-2", "USD")
        assert [(r.line, r.reason) for r in json_result.rejected] == [(5, "malformed_row")]

    def test_reading_does_not_modify_the_export(self, tmp_path):
        source = (SAMPLES / "invoices-dirty.csv").read_bytes()
        copy = tmp_path / "copy.csv"
        copy.write_bytes(source)
        importer.read_invoice_export(copy)
        importer.read_invoice_export(copy)
        assert copy.read_bytes() == source
