#!/bin/sh
# Release 1 public test for Foundation Project 1, milestone M1 (supplied; do not edit).
#
# Run from anywhere inside your clone:   sh tests/m1/run-m1.sh
# Exit status 0 means every check passed. Each FAIL line says what to fix next.
# Set M1_SEED=<number> to repeat a generated-export run exactly.
# All data used here is synthetic. Nothing is written inside the repository.

set -u

here=$(cd "$(dirname "$0")" && pwd) || exit 2
cd "$here/../.." || exit 2

SCRIPT=scripts/settlement-summary.sh
FIX=tests/m1/fixtures
pass=0
fail=0

tmp=$(mktemp -d 2>/dev/null || mktemp -d -t run-m1)
trap 'rm -rf "$tmp"' EXIT
trap 'exit 130' INT TERM

ok() {
  pass=$((pass + 1))
  echo "PASS  $1"
}

bad() {
  fail=$((fail + 1))
  echo "FAIL  $1"
  if [ "$#" -gt 1 ] && [ -n "$2" ]; then
    printf '      %s\n' "$2"
  fi
}

sha256_stdin() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum | awk '{print $1}'
  else
    shasum -a 256 | awk '{print $1}'
  fi
}

oneline() {
  tr '\n' ';' | cut -c1-300
}

# ---------------------------------------------------------------- summary script behaviour

expect_summary() {
  label=$1
  input=$2
  expected=$3
  sh "$SCRIPT" "$input" >"$tmp/out" 2>"$tmp/err"
  status=$?
  if [ "$status" -ne 0 ]; then
    bad "summary of $label" "exit status $status; stderr: $(oneline <"$tmp/err")"
    return
  fi
  lines=$(awk 'END { print NR }' "$tmp/out")
  if [ "$(cat "$tmp/out")" = "$expected" ] && [ "$lines" -eq 4 ]; then
    ok "summary of $label"
  else
    bad "summary of $label" "expected: $(printf '%s' "$expected" | oneline)  got: $(oneline <"$tmp/out")"
  fi
}

if [ ! -f "$SCRIPT" ]; then
  bad "$SCRIPT exists" "the supplied skeleton is missing; restore it from the release"
else
  expect_summary "mixed-outcomes.txt" "$FIX/mixed-outcomes.txt" "SETTLED 5
RETURNED 2
PENDING 2
NON_USD 0"
  expect_summary "outcome-words-in-references.txt (match field 2 exactly)" "$FIX/outcome-words-in-references.txt" "SETTLED 1
RETURNED 0
PENDING 3
NON_USD 1"
  expect_summary "non-usd-lines.txt (EUR, usd, missing currency all count)" "$FIX/non-usd-lines.txt" "SETTLED 5
RETURNED 1
PENDING 2
NON_USD 6"
  expect_summary "empty.txt (four zero lines)" "$FIX/empty.txt" "SETTLED 0
RETURNED 0
PENDING 0
NON_USD 0"
  expect_summary "no-final-newline.txt (last line still counts)" "$FIX/no-final-newline.txt" "SETTLED 1
RETURNED 1
PENDING 1
NON_USD 1"

  # A generated export: the expected counts are recorded while each line is generated.
  seed=${M1_SEED:-$(date +%s)}
  awk -v seed="$seed" -v data="$tmp/generated.txt" -v want="$tmp/generated.expected" 'BEGIN {
    srand(seed)
    split("SETTLED RETURNED PENDING HELD settled SETTLED_LATE", outcome, " ")
    split("USD USD USD EUR usd GBP USDX", currency, " ")
    n = 40 + int(rand() * 260)
    settled = 0; returned = 0; pending = 0; nonusd = 0
    for (i = 1; i <= n; i++) {
      o = outcome[1 + int(rand() * 6)]
      c = 1 + int(rand() * 8)
      ref = (rand() < 0.25) ? sprintf("PR-%s-%06d", outcome[1 + int(rand() * 3)], i) : sprintf("PR-%06d", i)
      if (c == 8) {
        line = sprintf("2026-11-%02d %s %s %d", 1 + int(rand() * 28), o, ref, 1 + int(rand() * 999999))
        nonusd++
      } else {
        line = sprintf("2026-11-%02d %s %s %d %s", 1 + int(rand() * 28), o, ref, 1 + int(rand() * 999999), currency[c])
        if (currency[c] != "USD") nonusd++
      }
      if (o == "SETTLED") settled++
      if (o == "RETURNED") returned++
      if (o == "PENDING") pending++
      if (i < n) print line > data
      else printf "%s", line > data
    }
    printf "SETTLED %d\nRETURNED %d\nPENDING %d\nNON_USD %d\n", settled, returned, pending, nonusd > want
  }'
  expect_summary "generated export (M1_SEED=$seed)" "$tmp/generated.txt" "$(cat "$tmp/generated.expected")"

  # Supplied argument and missing-file checks must still work.
  sh "$SCRIPT" >"$tmp/out" 2>"$tmp/err"
  status=$?
  if [ "$status" -ne 0 ] && [ -s "$tmp/err" ] && [ ! -s "$tmp/out" ]; then
    ok "no argument: non-zero exit and a message on stderr"
  else
    bad "no argument: non-zero exit and a message on stderr" "exit $status; keep the supplied usage check"
  fi
  sh "$SCRIPT" "$tmp/does-not-exist.txt" >"$tmp/out" 2>"$tmp/err"
  status=$?
  if [ "$status" -ne 0 ] && [ -s "$tmp/err" ] && [ ! -s "$tmp/out" ]; then
    ok "missing file: non-zero exit and a message on stderr"
  else
    bad "missing file: non-zero exit and a message on stderr" "exit $status; keep the supplied missing-file check"
  fi
  sh "$SCRIPT" "$FIX/empty.txt" "$FIX/empty.txt" >"$tmp/out" 2>"$tmp/err"
  status=$?
  if [ "$status" -ne 0 ]; then
    ok "two arguments: non-zero exit"
  else
    bad "two arguments: non-zero exit" "keep the supplied usage check"
  fi

  expected_skeleton=$(awk 'NR == 1 { print $1 }' release/m1/skeleton-outside-markers.sha256 2>/dev/null)
  actual_skeleton=$(awk '/TODO\(M1\) BEGIN/ { skip = 1 } !skip { print } /TODO\(M1\) END/ { skip = 0 }' "$SCRIPT" | sha256_stdin)
  if [ -n "$expected_skeleton" ] && [ "$expected_skeleton" = "$actual_skeleton" ]; then
    ok "only the lines between the TODO(M1) markers were changed"
  else
    bad "only the lines between the TODO(M1) markers were changed" "restore the supplied lines outside the BEGIN/END markers"
  fi
fi

# ---------------------------------------------------------------- repository hygiene (needs Git)

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  bad "inside a Git repository" "run this from your clone of the project"
else
  mode=$(git ls-files -s -- "$SCRIPT" | awk '{ print $1 }')
  if [ "$mode" = "100755" ]; then
    ok "$SCRIPT is committed with its execute bit"
  else
    bad "$SCRIPT is committed with its execute bit" "git mode is '${mode:-untracked}'; run: git update-index --chmod=+x $SCRIPT"
  fi

  if git ls-files --error-unmatch .env >/dev/null 2>&1; then
    bad ".env is not tracked" "remove it with: git rm --cached .env (and check its history with your trainer)"
  elif [ -n "$(git log --all --format=%h -- .env 2>/dev/null)" ]; then
    bad ".env never appears in history" "a commit added .env; ask your trainer how to clean history"
  else
    ok ".env is not tracked and never appears in history"
  fi

  missing=""
  for p in .env target/classes/payments/App.class .idea/workspace.xml .vscode/settings.json .DS_Store docs/.DS_Store; do
    if ! git check-ignore -q --no-index "$p"; then
      missing="$missing $p"
    fi
  done
  if [ -z "$missing" ]; then
    ok ".gitignore ignores .env, target/, .idea/, .vscode/ and .DS_Store"
  else
    bad ".gitignore ignores .env, target/, .idea/, .vscode/ and .DS_Store" "not ignored:$missing"
  fi

  wrongly=""
  for p in .env.example scripts/settlement-summary.sh README.md docs/user-stories.md src/main/java/payments/App.java; do
    if git check-ignore -q --no-index "$p"; then
      wrongly="$wrongly $p"
    fi
  done
  if [ -z "$wrongly" ]; then
    ok ".gitignore does not hide project files"
  else
    bad ".gitignore does not hide project files" "ignored by mistake:$wrongly"
  fi

  if git merge-base --is-ancestor teammate/ap-contact HEAD 2>/dev/null \
      || git merge-base --is-ancestor origin/teammate/ap-contact HEAD 2>/dev/null; then
    ok "the teammate/ap-contact branch is merged"
  elif git rev-parse -q --verify teammate/ap-contact >/dev/null 2>&1 \
      || git rev-parse -q --verify origin/teammate/ap-contact >/dev/null 2>&1; then
    bad "the teammate/ap-contact branch is merged" "merge it and resolve the README conflict keeping both sections"
  else
    echo "SKIP  teammate/ap-contact branch not present in this clone (checked by human review)"
  fi
fi

# ---------------------------------------------------------------- learner files

if grep -Eq '^[[:space:]]*PAYMENTS_PORT=[^[:space:]#]+' .env.example 2>/dev/null \
    && grep -Eq '^[[:space:]]*PAYMENTS_LOG_LEVEL=[^[:space:]#]+' .env.example 2>/dev/null; then
  ok ".env.example sets PAYMENTS_PORT and PAYMENTS_LOG_LEVEL to example values"
else
  bad ".env.example sets PAYMENTS_PORT and PAYMENTS_LOG_LEVEL to example values" "add two uncommented NAME=value lines with safe example values"
fi

if grep -q '^## Accounts-payable contact' README.md 2>/dev/null; then
  ok "README.md keeps the teammate's Accounts-payable contact section"
else
  bad "README.md keeps the teammate's Accounts-payable contact section" "merge teammate/ap-contact and keep both sections"
fi

stories=$(grep -Eic '^[[:space:]>*-]*as an? ' docs/user-stories.md 2>/dev/null)
if grep -Eq '<(role|capability|benefit)>' docs/user-stories.md 2>/dev/null; then
  bad "docs/user-stories.md has at least three 'As a ...' stories" "template placeholders such as <role> are still there"
elif [ "${stories:-0}" -ge 3 ]; then
  ok "docs/user-stories.md has at least three 'As a ...' stories"
else
  bad "docs/user-stories.md has at least three 'As a ...' stories" "found ${stories:-0}; follow the headings in the file (human review checks the criteria)"
fi

left=""
for f in README.md docs/user-stories.md docs/ai-use-log.md .gitignore .env.example; do
  if grep -q 'TODO(M1)' "$f" 2>/dev/null; then
    left="$left $f"
  fi
done
if [ -z "$left" ]; then
  ok "no TODO(M1) placeholders left in README, docs, .gitignore or .env.example"
else
  bad "no TODO(M1) placeholders left in README, docs, .gitignore or .env.example" "still present in:$left"
fi

# ---------------------------------------------------------------- released files unchanged

if [ -f release/m1/MANIFEST.sha256 ]; then
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum -c --quiet release/m1/MANIFEST.sha256 >"$tmp/manifest" 2>&1
  else
    shasum -a 256 -c --quiet release/m1/MANIFEST.sha256 >"$tmp/manifest" 2>&1
  fi
  if [ "$?" -eq 0 ]; then
    ok "released M1 files match release/m1/MANIFEST.sha256"
  else
    bad "released M1 files match release/m1/MANIFEST.sha256" "$(oneline <"$tmp/manifest")"
  fi
else
  bad "release/m1/MANIFEST.sha256 exists" "restore the release files"
fi

echo
echo "M1 checks: $pass passed, $fail failed."
if [ "$fail" -ne 0 ]; then
  echo "Fix the FAIL lines above, then run: sh tests/m1/run-m1.sh"
  exit 1
fi
exit 0
