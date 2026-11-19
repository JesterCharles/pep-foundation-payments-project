"""Exact-cents checks shared by Standard W10 and Fast W09 (supplied)."""

import random

from payments_pipeline import amounts

from .generated import BAD_AMOUNTS, amount_text


class AmountChecks:
    def test_parses_two_decimal_text_to_exact_cents(self):
        cases = {"0.01": 1, "1.00": 100, "1234.50": 123450, " 89.99 ": 8999, "9999999.99": 999_999_999,
                 "0005.00": 500}
        for text, cents in cases.items():
            result = amounts.parse_amount_cents(text)
            assert result == cents and type(result) is int, f"{text!r} -> {result!r}, expected {cents}"

    def test_rejects_every_malformed_amount(self):
        for text in BAD_AMOUNTS + [None, "1.5e2", "NaN", "12.", "1.0.00"]:
            assert amounts.parse_amount_cents(text) is None, f"{text!r} must be rejected"

    def test_generated_amounts_round_trip_and_sum_to_the_integer_oracle(self):
        rng = random.Random(1007)
        total = 0
        parsed_total = 0
        for _ in range(2000):
            cents = rng.randint(1, amounts.MAX_CENTS)
            total += cents
            parsed = amounts.parse_amount_cents(amount_text(cents))
            assert parsed == cents
            parsed_total += parsed
            assert amounts.format_cents(cents) == amount_text(cents)
        assert parsed_total == total
        tenths = sum(amounts.parse_amount_cents("0.10") for _ in range(1000))
        assert tenths == 10_000 and amounts.format_cents(tenths) == "100.00"

    def test_formats_zero_small_and_negative(self):
        assert amounts.format_cents(0) == "0.00"
        assert amounts.format_cents(5) == "0.05"
        assert amounts.format_cents(123450) == "1234.50"
        assert amounts.format_cents(-5) == "-0.05"
        assert amounts.format_cents(-123450) == "-1234.50"
