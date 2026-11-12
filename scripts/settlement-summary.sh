#!/bin/sh
# Settlement-export summary for Northwind Components (fictional). Synthetic data only.
#
# Usage: scripts/settlement-summary.sh <export-file>
#
# Each export line looks like:   2026-11-02 SETTLED PR-000123 125000 USD
#   field 1 date, field 2 outcome (SETTLED, RETURNED or PENDING), field 3 request reference,
#   field 4 amount in cents, field 5 currency code.
#
# Prints exactly four lines, in this order:
#   SETTLED n
#   RETURNED n
#   PENDING n
#   NON_USD n
#
# SUPPLIED (release 1): the argument and missing-file checks below. Keep them.
# LEARNER-OWNED: only the lines between the TODO(M1) BEGIN and END markers.

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <export-file>" >&2
  exit 2
fi

export_file=$1

if [ ! -f "$export_file" ]; then
  echo "error: export file not found: $export_file" >&2
  exit 1
fi

# ---- TODO(M1) BEGIN: replace the two placeholder lines below with your pipeline ----
# Count ONLY lines whose SECOND field is exactly SETTLED, RETURNED or PENDING (a word elsewhere
# in the line, such as inside a reference, must not count). NON_USD counts lines whose FIFTH
# field is not exactly USD (EUR, usd in lower case and a missing fifth field all count).
# The last line may have no final newline; it still counts. An empty file prints four zero lines.
# Tools from F1 are enough: awk, grep, cut, wc. Read "$export_file" only; never modify it.
echo "TODO M1: settlement summary is not implemented yet" >&2
exit 3
# ---- TODO(M1) END ----
