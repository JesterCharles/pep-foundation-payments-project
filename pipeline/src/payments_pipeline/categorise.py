"""Validated, minimised AI spend categorisation (see CONTRACT.md).

The AI only ever sees the invoice description and only ever suggests a category: it can never
change an amount, currency, supplier, invoice or status.
"""

from dataclasses import dataclass

ALLOWED_CATEGORIES = ("office_supplies", "it_hardware", "software", "facilities",
                      "professional_services", "travel", "logistics")


@dataclass(frozen=True)
class CategoryResult:
    category: str | None
    needs_review: bool


@dataclass(frozen=True)
class CategorisedInvoice:
    row: object
    category: str | None
    needs_review: bool


class AiProviderError(Exception):
    """Timeout, non-2xx status or non-JSON body from the AI provider."""


def build_prompt_payload(description):
    raise NotImplementedError("TODO: payload with exactly instruction, categories and text")


class AiClient:
    def __init__(self, base_url, timeout_seconds=2.0):
        self.base_url = base_url
        self.timeout_seconds = timeout_seconds

    @classmethod
    def from_env(cls):
        raise NotImplementedError("TODO: read AI_PROVIDER_BASE_URL and AI_PROVIDER_TIMEOUT_SECONDS")

    def complete(self, payload):
        raise NotImplementedError("TODO: POST the payload to {base_url}/v1/categorise")


def validate_ai_output(raw):
    raise NotImplementedError("TODO: accept only {'category': <allowed>}; anything else needs review")


def categorise_invoice(row, ai):
    raise NotImplementedError("TODO: categorise one invoice without changing it")
