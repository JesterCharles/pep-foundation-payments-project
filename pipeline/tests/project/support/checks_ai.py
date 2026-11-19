"""Checks categorisation requests and responses against a stub AI provider (supplied)."""

import json

from payments_pipeline import categorise
from payments_pipeline.importer import InvoiceRow

from .stub_server import StubServer, closed_port_url

ROW = InvoiceRow(line=3, supplier_id="SUP-0042", invoice_number="INV-ZX-9917", invoice_key="SUP-0042|INV-ZX-9917",
                 amount_cents=7_654_321, currency="USD", description="Replacement keyboards for the support desk")

BAD_OUTPUTS = [
    {"category": "groceries"},
    {"category": "software", "amount": "0.00"},
    {"category": "software", "status": "APPROVED"},
    {"category": "1234.50"},
    {"category": ""},
    {"category": None},
    {"category": ["software"]},
    {"Category": "software"},
    {},
    ["software"],
    "software",
    42,
    None,
]


def _stub(answer, status=200, delay=0):
    def route(request):
        return status, answer, delay
    return StubServer({("POST", "/v1/categorise"): route})


class AiChecks:
    def test_allowed_categories_are_the_contract_list(self):
        assert tuple(categorise.ALLOWED_CATEGORIES) == ("office_supplies", "it_hardware", "software", "facilities",
                                                        "professional_services", "travel", "logistics")

    def test_valid_output_is_accepted(self):
        for category in categorise.ALLOWED_CATEGORIES:
            result = categorise.validate_ai_output({"category": category})
            assert (result.category, result.needs_review) == (category, False)

    def test_invalid_output_defaults_to_review(self):
        for raw in BAD_OUTPUTS:
            result = categorise.validate_ai_output(raw)
            assert (result.category, result.needs_review) == (None, True), f"{raw!r} must be flagged for review"

    def test_prompt_payload_carries_only_the_description(self):
        payload = categorise.build_prompt_payload(ROW.description)
        assert set(payload) == {"instruction", "categories", "text"}
        assert payload["text"] == ROW.description
        assert list(payload["categories"]) == list(categorise.ALLOWED_CATEGORIES)
        text = json.dumps(payload)
        for secret in ("SUP-0042", "INV-ZX-9917", "76543.21", "7654321"):
            assert secret not in text, f"{secret} must not reach the AI provider"

    def test_categorise_sends_only_the_payload_and_never_changes_money(self):
        for answer, expected in [({"category": "it_hardware"}, ("it_hardware", False)),
                                 ({"category": "it_hardware", "amount": "1.00"}, (None, True)),
                                 ({"category": "unknown"}, (None, True))]:
            with _stub(answer) as stub:
                result = categorise.categorise_invoice(ROW, categorise.AiClient(stub.base_url, timeout_seconds=2))
            assert result.row == ROW and result.row.amount_cents == 7_654_321
            assert (result.category, result.needs_review) == expected
            assert len(stub.requests) == 1
            sent = stub.requests[0]
            assert sent.json() == categorise.build_prompt_payload(ROW.description)
            assert b"SUP-0042" not in sent.body and b"INV-ZX-9917" not in sent.body and b"76543.21" not in sent.body

    def test_provider_failures_flag_for_review(self):
        cases = [_stub({"error": "boom"}, status=500), _stub(b"not json"), _stub({"category": "software"}, delay=1.5)]
        for server in cases:
            with server as stub:
                result = categorise.categorise_invoice(ROW, categorise.AiClient(stub.base_url, timeout_seconds=0.4))
            assert (result.category, result.needs_review) == (None, True)
            assert result.row == ROW
        refused = categorise.categorise_invoice(ROW, categorise.AiClient(closed_port_url(), timeout_seconds=0.4))
        assert refused.needs_review is True and refused.row == ROW
