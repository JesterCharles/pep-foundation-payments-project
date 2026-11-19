#!/bin/sh
# Supplied (do not edit). Downloads every dependency once so the project builds and tests offline afterwards.
# Needs the network. Safe to rerun. Requires JDK 25 + Maven 3.9, Python 3.12+, Node 24.15+.
set -eu
ROOT=$(cd "$(dirname "$0")/.." && pwd)
echo "==> api: Maven dependencies"; ( cd "$ROOT/api" && mvn -q dependency:go-offline && mvn -q -DskipTests test-compile )
echo "==> pipeline: virtual environment in pipeline/.venv"
( cd "$ROOT/pipeline" && python3 -m venv .venv && .venv/bin/python -m pip install -q -e ".[test]" )
for area in web ui deploy; do echo "==> $area: npm ci"; ( cd "$ROOT/$area" && npm ci --no-audit --no-fund ); done
echo "==> web: headless Chromium for the responsive check"; ( cd "$ROOT/web" && npm run prefetch )
echo "Done. Later runs work offline (mvn -o, npm and pytest use the local caches)."
