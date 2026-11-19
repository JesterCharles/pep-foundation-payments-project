"""Reports differences between a settlement statement and the Spring API's payment requests."""

from dataclasses import dataclass


@dataclass(frozen=True)
class Discrepancy:
    kind: str
    line: int | None
    reference: str | None
    request_id: int | None
    statement_cents: int | None
    expected_cents: int | None


def reconcile(statement, requests):
    raise NotImplementedError("TODO: classify statement lines and find SENT requests with no line")
