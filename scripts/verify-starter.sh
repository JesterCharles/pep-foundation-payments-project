#!/bin/sh
# For course maintainers (supplied). Checks the starter as released:
#  1. every bare command passes (supplied baseline suite + learner tests only), and
#  2. every milestone selection FAILS on the untouched starter, because the learner-owned stubs are TODOs.
# A failing selection here is the intended fail-first state, not a defect. Run after scripts/prefetch.sh.
set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
BAD=0
bare() { echo "==> bare $1"; if ( cd "$ROOT/$1" && shift && "$@" ) >/dev/null 2>&1; then echo "  PASS (expected)"; else echo "  FAIL (unexpected)"; BAD=1; fi; }
PY=python3; [ -x "$ROOT/pipeline/.venv/bin/python" ] && PY="$ROOT/pipeline/.venv/bin/python"
bare api mvn -q test
bare pipeline "$PY" -m pytest
bare web npm test
bare ui npm test
bare deploy npm test
for sel in "standard 7" "standard 8" "standard 9" "standard 10" "standard 11" "standard 12" "standard 13" "standard 14" "standard 15" "fast 7" "fast 8" "fast 9" "fast 10" "fast 11"; do
  if "$ROOT/scripts/check-milestone.sh" $sel >/dev/null 2>&1; then echo "  $sel: PASSED on the starter (unexpected)"; BAD=1; else echo "  $sel: fails on the starter (expected)"; fi
done
exit $BAD
