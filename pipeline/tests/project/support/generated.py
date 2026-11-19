"""Seeded generators of synthetic invoice and settlement exports with an integer oracle (supplied)."""

import csv
import json
import random

CATEGORIES_TEXT = [
    "Printer paper and toner", "Annual software licence", "Replacement laptops", "Office cleaning",
    "Consulting workshop", "Train tickets for a site visit", "Courier deliveries", "Desk chairs",
]

BAD_AMOUNTS = ["1,000.00", "1.5", "12.345", "-5.00", "+5.00", "0.00", "", "abc", "5", ".50", "10000000.00",
               "١٢.00", "1 000.00", "12.5O"]


def amount_text(cents):
    return f"{cents // 100}.{cents % 100:02d}"


def invoice_set(seed, size=120):
    """Returns (rows as dicts, oracle) where oracle holds the expected valid keys, rejections and totals."""
    rng = random.Random(seed)
    rows, valid_keys, rejected, total = [], [], {}, 0
    for i in range(size):
        line = i + 1
        supplier = f"SUP-{rng.randint(1, 60):04d}"
        invoice = f"INV-{seed % 1000:03d}{i:04d}"
        cents = rng.choice([1, 10, 99, 100, 12345, 999_999_999, rng.randint(1, 5_000_000)])
        kind = rng.random()
        row = {"supplier_id": supplier, "invoice_number": invoice, "amount": amount_text(cents),
               "currency": "USD", "description": rng.choice(CATEGORIES_TEXT)}
        if kind < 0.12 and valid_keys:
            dup_key = rng.choice(valid_keys)
            s, inv = dup_key.split("|")
            row["supplier_id"] = rng.choice([s, s.lower(), f" {s} "])
            row["invoice_number"] = rng.choice([inv, inv.lower(), inv[:4] + " " + inv[4:]])
            rejected[line] = "duplicate"
        elif kind < 0.24:
            row["amount"] = rng.choice(BAD_AMOUNTS)
            rejected[line] = "malformed_amount"
        elif kind < 0.32:
            row["currency"] = rng.choice(["EUR", "GBP", "CAD", "US D", ""])
            rejected[line] = "non_usd"
        else:
            if rng.random() < 0.2:
                row["currency"] = "usd"
            if rng.random() < 0.2:
                row["amount"] = f"  {row['amount']} "
            valid_keys.append(f"{supplier}|{invoice.upper().replace(' ', '')}")
            total += cents
        rows.append(row)
    return rows, {"valid_keys": valid_keys, "rejected": rejected, "total_cents": total}


def write_invoices(path, rows, fmt="csv"):
    columns = ["supplier_id", "invoice_number", "amount", "currency", "description"]
    if fmt == "json":
        path.write_text(json.dumps(rows), encoding="utf-8")
        return path
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)
    return path


def settlement_set(seed, requests, extra_unknown=5):
    """Lines for SENT requests with seeded mismatches. Returns (rows, expected kinds by line)."""
    rng = random.Random(seed)
    rows, expected = [], {}
    for request in requests:
        if request["status"] != "SENT" or rng.random() < 0.15:
            continue
        cents = int(request["amount"].replace(".", ""))
        currency = "USD"
        roll = rng.random()
        kind = None
        if roll < 0.15:
            cents = cents + rng.choice([-1, 1, 100]) if cents > 100 else cents + 1
            kind = "amount_mismatch"
        elif roll < 0.25:
            currency = rng.choice(["EUR", "GBP"])
            kind = "currency_mismatch"
        rows.append({"reference": f"PR-{request['id']}", "provider_payment_id": f"PP-{request['id']:06d}",
                     "amount": amount_text(cents), "currency": currency, "settled_on": "2026-11-17"})
        if kind:
            expected[len(rows)] = kind
        if rng.random() < 0.08:
            rows.append(dict(rows[-1]))
            expected[len(rows)] = "duplicate_line"
    for n in range(extra_unknown):
        rows.append({"reference": f"PR-{90000 + n}", "provider_payment_id": f"PP-9{n:05d}", "amount": "1.00",
                     "currency": "USD", "settled_on": "2026-11-17"})
        expected[len(rows)] = "unknown_reference"
    return rows, expected


def write_settlements(path, rows, fmt="csv"):
    columns = ["reference", "provider_payment_id", "amount", "currency", "settled_on"]
    if fmt == "json":
        path.write_text(json.dumps(rows), encoding="utf-8")
        return path
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)
    return path


def spring_requests(seed, size=40):
    rng = random.Random(seed)
    requests = []
    for i in range(1, size + 1):
        cents = rng.randint(1, 3_000_000)
        requests.append({"id": i, "supplierId": f"SUP-{rng.randint(1, 60):04d}", "invoiceNumber": f"INV-G{i:04d}",
                         "amount": amount_text(cents), "currency": "USD", "requestedBy": "REQ-0001",
                         "status": rng.choice(["SENT", "SENT", "SENT", "APPROVED", "PAID"]), "events": []})
    return requests
