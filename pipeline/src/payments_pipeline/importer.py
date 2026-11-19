"""Reads synthetic supplier-invoice exports and provider settlement statements (see CONTRACT.md).

The dataclasses below are the shapes the supplied checks read; keep their field names.
"""

from dataclasses import dataclass, field

INVOICE_COLUMNS = ("supplier_id", "invoice_number", "amount", "currency", "description")
SETTLEMENT_COLUMNS = ("reference", "provider_payment_id", "amount", "currency", "settled_on")


@dataclass(frozen=True)
class InvoiceRow:
    line: int
    supplier_id: str
    invoice_number: str
    invoice_key: str
    amount_cents: int
    currency: str
    description: str


@dataclass(frozen=True)
class SettlementRow:
    line: int
    reference: str
    provider_payment_id: str
    amount_cents: int
    currency: str
    settled_on: str


@dataclass(frozen=True)
class Rejected:
    line: int
    reason: str


@dataclass
class InvoiceImport:
    valid: list = field(default_factory=list)
    rejected: list = field(default_factory=list)


@dataclass
class SettlementImport:
    valid: list = field(default_factory=list)
    rejected: list = field(default_factory=list)


def read_invoice_export(path):
    """Reads a .csv or .json invoice export into an InvoiceImport."""
    raise NotImplementedError("TODO: read, validate and de-duplicate an invoice export")


def read_settlement_statement(path):
    """Reads a .csv or .json settlement statement into a SettlementImport."""
    raise NotImplementedError("TODO: read and validate a settlement statement")
