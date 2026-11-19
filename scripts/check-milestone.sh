#!/bin/sh
# Supplied (do not edit). Runs every supplied check of one milestone, in every area that has checks by then.
#   scripts/check-milestone.sh standard 9     Standard W09 (api)
#   scripts/check-milestone.sh fast 11        Fast W11 (api, pipeline, web, ui, deploy)
# Checks are cumulative: a week also runs the earlier weeks' checks of the same track.
# Prerequisites: run scripts/prefetch.sh once (it needs the network); afterwards everything runs offline.
set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TRACK=${1:-}; WEEK=${2:-}
case "$TRACK" in standard) LAST=15 ;; fast) LAST=11 ;; *) echo "usage: $0 standard|fast <week 7..15|7..11>" >&2; exit 2 ;; esac
case "$WEEK" in ''|*[!0-9]*) echo "usage: $0 $TRACK <week>" >&2; exit 2 ;; esac
WEEK=$(expr "$WEEK" + 0)
if [ "$WEEK" -lt 7 ] || [ "$WEEK" -gt "$LAST" ]; then echo "$TRACK weeks are 7..$LAST" >&2; exit 2; fi
SEL=$(printf '%s-w%02d' "$TRACK" "$WEEK")
# First week each area has supplied checks, per track.
first() { case "$TRACK:$1" in
  standard:api) echo 7 ;; standard:pipeline) echo 10 ;; standard:web) echo 11 ;; standard:ui) echo 12 ;; standard:deploy) echo 14 ;;
  fast:api) echo 7 ;; fast:pipeline) echo 9 ;; fast:web) echo 9 ;; fast:ui) echo 10 ;; fast:deploy) echo 11 ;; esac; }
STATUS=0; SUMMARY=""
run() { # area command...
  AREA=$1; shift
  if [ "$WEEK" -lt "$(first "$AREA")" ]; then return; fi
  echo "==> $AREA: $*"
  if ( cd "$ROOT/$AREA" && "$@" ); then SUMMARY="$SUMMARY\n  PASS  $AREA"; else SUMMARY="$SUMMARY\n  FAIL  $AREA"; STATUS=1; fi
}
PY=python3; [ -x "$ROOT/pipeline/.venv/bin/python" ] && PY="$ROOT/pipeline/.venv/bin/python"
run api mvn -q test "-Dmilestone=$SEL"
run pipeline "$PY" -m pytest --milestone "$SEL"
run web npm test -- "$SEL"
run ui npm test -- "$SEL"
run deploy npm test -- "$SEL"
printf "\n%s supplied checks:%b\n" "$SEL" "$SUMMARY"
exit $STATUS
