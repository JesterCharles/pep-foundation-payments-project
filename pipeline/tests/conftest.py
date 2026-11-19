"""Supplied milestone selector for the pipeline checks (do not edit).

    python -m pytest                              learner tests only (tests/ outside tests/project/)
    python -m pytest --milestone standard-w10     plus the supplied checks for that track up to that week
    python -m pytest --milestone fast-w09

A selection runs that track's supplied directories up to the week (standard-w15 still runs w10;
fast-w08 runs none). Each track has its own directories; a selection never runs the other track's.
With no selection, a run that collects no tests still exits 0.
"""

from pathlib import Path

import pytest

# Supplied check directories per track, in week order (explicit map; never inferred from numbers).
MILESTONE_DIRS = {
    "standard": ["w10"],
    "fast": ["w09"],
}

# The Project 2 weeks of each track; any of them is a valid selection (cumulative).
TRACK_WEEKS = {
    "standard": range(7, 16),
    "fast": range(7, 12),
}

PROJECT_DIR = Path(__file__).parent / "project"


def _valid_values():
    return [f"{track}-w{week:02d}" for track, weeks in TRACK_WEEKS.items() for week in weeks]


def pytest_addoption(parser):
    parser.addoption(
        "--milestone",
        default=None,
        help="run the supplied checks for a track up to a week, e.g. standard-w10 or fast-w09",
    )


def _selected_dirs(config):
    value = config.getoption("--milestone")
    if not value:
        return set()
    value = value.strip().lower()
    if value not in _valid_values():
        raise pytest.UsageError(
            f"unknown --milestone {value!r}; valid values: {', '.join(_valid_values())}"
        )
    track, week = value.split("-", 1)
    return {d for d in MILESTONE_DIRS[track] if int(d[1:]) <= int(week[1:])}


def pytest_configure(config):
    config._payments_selected = _selected_dirs(config)


def pytest_ignore_collect(collection_path, config):
    path = Path(collection_path)
    try:
        relative = path.relative_to(PROJECT_DIR)
    except ValueError:
        return None
    if not relative.parts:
        return None
    first = relative.parts[0]
    if first == "support" or first.startswith("__"):
        return None
    if first not in config._payments_selected:
        return True
    return None


def pytest_sessionfinish(session, exitstatus):
    # pytest exits 5 when nothing is collected (no learner tests yet, or a week before the pipeline); that is green.
    if exitstatus == pytest.ExitCode.NO_TESTS_COLLECTED:
        session.exitstatus = pytest.ExitCode.OK


def pytest_report_header(config):
    selected = sorted(config._payments_selected)
    if selected:
        return f"payments milestone checks: {config.getoption('--milestone')} -> tests/project/{', '.join(selected)}"
    return "payments milestone checks: none selected (learner tests only)"
