#!/bin/sh
# Supplied (do not edit). Deletes local, git-ignored run data so you can start the services from a clean state:
# the H2 database files under api/data/ and build output. It never touches tracked files or your commits.
set -eu
ROOT=$(cd "$(dirname "$0")/.." && pwd)
rm -rf "$ROOT/api/data" "$ROOT/api/target" "$ROOT/data"
echo "Local data and build output removed. Tracked files are unchanged:"
git -C "$ROOT" status --short || true
