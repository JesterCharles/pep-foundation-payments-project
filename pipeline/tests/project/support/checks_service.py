"""FastAPI discrepancy route checks with TestClient and a stub Spring API (supplied)."""

import shutil
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from payments_pipeline import importer, reconcile
from payments_pipeline.service import create_app
from payments_pipeline.spring_client import SpringApiClient

from .checks_spring import REQUESTS, TOKEN, spring_stub
from .generated import settlement_set, spring_requests, write_settlements

SAMPLES = Path(__file__).resolve().parents[3] / "data" / "samples"


def _client(base_url, statements, timeout=2.0):
    return TestClient(create_app(SpringApiClient(base_url, TOKEN, timeout_seconds=timeout), statements))


class ReconcileChecks:
    def test_mixed_statement_kinds_in_order(self):
        statement = importer.read_settlement_statement(SAMPLES / "settlements-mixed.csv")
        found = reconcile.reconcile(statement, REQUESTS)
        assert [(d.kind, d.line, d.reference, d.request_id) for d in found] == [
            ("amount_mismatch", 2, "PR-2", 2),
            ("currency_mismatch", 3, "PR-3", 3),
            ("unknown_reference", 4, "PR-99", None),
            ("duplicate_line", 5, "PR-1", 1),
            ("malformed_line", 6, None, None),
            ("malformed_line", 7, None, None),
            ("malformed_line", 8, None, None),
            ("missing_line", None, "PR-4", 4),
        ]
        mismatch = found[0]
        assert (mismatch.statement_cents, mismatch.expected_cents) == (8990, 8999)

    def test_clean_statement_reports_only_missing_sent_requests(self):
        statement = importer.read_settlement_statement(SAMPLES / "settlements-clean.csv")
        found = reconcile.reconcile(statement, REQUESTS)
        assert [(d.kind, d.reference) for d in found] == [("missing_line", "PR-4")]

    @pytest.mark.parametrize("seed", [5, 61, 977])
    def test_generated_statements(self, tmp_path, seed):
        requests = spring_requests(seed)
        rows, expected = settlement_set(seed, requests)
        statement = importer.read_settlement_statement(write_settlements(tmp_path / "s.csv", rows))
        found = reconcile.reconcile(statement, requests)
        assert {d.line: d.kind for d in found if d.line is not None} == expected
        referenced = {r["reference"] for r in rows}
        missing = sorted(r["id"] for r in requests if r["status"] == "SENT" and f"PR-{r['id']}" not in referenced)
        assert [d.request_id for d in found if d.kind == "missing_line"] == missing


class ServiceChecks:
    @pytest.fixture
    def statements(self, tmp_path):
        folder = tmp_path / "statements"
        folder.mkdir()
        for name in ("settlements-clean.csv", "settlements-mixed.csv", "settlements-mixed.json"):
            shutil.copy(SAMPLES / name, folder / name)
        (tmp_path / "outside.csv").write_text("reference,provider_payment_id,amount,currency,settled_on\n")
        return folder

    def test_health_does_not_call_spring(self, statements):
        with spring_stub() as stub:
            response = _client(stub.base_url, statements).get("/health")
        assert response.status_code == 200 and response.json() == {"status": "ok"}
        assert stub.requests == []

    def test_discrepancies_route(self, statements):
        with spring_stub() as stub:
            response = _client(stub.base_url, statements).get("/discrepancies",
                                                               params={"statement": "settlements-mixed.csv"})
        assert response.status_code == 200, response.text
        body = response.json()
        assert body["statement"] == "settlements-mixed.csv"
        assert body["summary"] == {"amount_mismatch": 1, "currency_mismatch": 1, "unknown_reference": 1,
                                   "duplicate_line": 1, "malformed_line": 3, "missing_line": 1}
        first = body["discrepancies"][0]
        assert first == {"kind": "amount_mismatch", "line": 2, "reference": "PR-2", "requestId": 2,
                         "statementAmount": "89.90", "expectedAmount": "89.99"}
        assert stub.requests[0].headers["authorization"] == f"Bearer {TOKEN}"

    def test_json_statement_and_reference_route(self, statements):
        with spring_stub() as stub:
            client = _client(stub.base_url, statements)
            body = client.get("/discrepancies", params={"statement": "settlements-mixed.json"}).json()
            assert body["summary"] == {"amount_mismatch": 1, "unknown_reference": 1, "malformed_line": 1,
                                       "missing_line": 1}
            one = client.get("/discrepancies/PR-3", params={"statement": "settlements-mixed.json"})
            assert one.status_code == 200
            assert [d["kind"] for d in one.json()["discrepancies"]] == ["amount_mismatch"]
            assert one.json()["discrepancies"][0]["statementAmount"] == "4999.99"
            none = client.get("/discrepancies/PR-1", params={"statement": "settlements-mixed.json"})
            assert none.status_code == 404

    def test_statement_names_are_confined_to_the_folder(self, statements):
        with spring_stub() as stub:
            client = _client(stub.base_url, statements)
            for name in ["", "../outside.csv", "..", "sub/x.csv", "..\\outside.csv", "/etc/passwd"]:
                assert client.get("/discrepancies", params={"statement": name}).status_code == 400, name
            assert client.get("/discrepancies", params={"statement": "missing.csv"}).status_code == 404

    def test_spring_failures_map_to_gateway_errors_without_the_token(self, statements):
        cases = [(spring_stub(token="other"), 2.0, 502), (spring_stub(delay=2.0), 0.3, 504),
                 (spring_stub(status=500), 2.0, 502)]
        for server, timeout, expected in cases:
            with server as stub:
                response = _client(stub.base_url, statements, timeout).get(
                    "/discrepancies", params={"statement": "settlements-clean.csv"})
            assert response.status_code == expected, response.text
            assert TOKEN not in response.text
            assert "detail" in response.json()
