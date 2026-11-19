"""Exact money helpers: amounts are whole cents in an int (see CONTRACT.md)."""

MAX_CENTS = 999_999_999


def parse_amount_cents(text):
    """'1234.50' -> 123450; None for anything that is not ASCII digits, '.', two digits, or is zero/too large."""
    raise NotImplementedError("TODO: parse two-decimal text into int cents")


def format_cents(cents):
    """123450 -> '1234.50'; 5 -> '0.05'; -5 -> '-0.05'."""
    raise NotImplementedError("TODO: format int cents as two-decimal text")
