"""Standard W10 supplied checks: import pipeline and FastAPI reconciliation service (do not edit).

Run from pipeline/:  python -m pytest --milestone standard-w10
PostgreSQL staging checks run when PIPELINE_DATABASE_URL or PG* is set, and skip otherwise.
"""

import importlib
import sys

import pytest

from project.support.checks_ai import AiChecks
from project.support.checks_amounts import AmountChecks
from project.support.checks_import import ImportChecks
from project.support.checks_service import ReconcileChecks, ServiceChecks
from project.support.checks_spring import SpringClientChecks
from project.support.checks_staging import StagingChecks

@pytest.fixture
def connect():
    """Standard writes its own connection code (payments_pipeline.db)."""
    from payments_pipeline import db

    return db.connect


class TestAmounts(AmountChecks):
    pass


class TestImport(ImportChecks):
    pass


class TestStaging(StagingChecks):
    pass


class TestAiCategorisation(AiChecks):
    pass


class TestSpringClient(SpringClientChecks):
    pass


class TestReconcile(ReconcileChecks):
    pass


class TestService(ServiceChecks):
    pass


class TestOwnConnectionCode:
    def test_conninfo_prefers_the_database_url(self):
        from payments_pipeline import db

        url = "postgresql://payments_dev@localhost:5432/payments_dev"
        assert db.conninfo_from_env({"PIPELINE_DATABASE_URL": url, "PGHOST": "elsewhere"}) == url

    def test_conninfo_from_pg_variables_with_defaults(self):
        import psycopg

        from payments_pipeline import db

        parsed = psycopg.conninfo.conninfo_to_dict(db.conninfo_from_env({}))
        assert (parsed.get("host"), str(parsed.get("port")), parsed.get("dbname")) == ("localhost", "5432", "postgres")
        parsed = psycopg.conninfo.conninfo_to_dict(db.conninfo_from_env(
            {"PGHOST": "db.internal.example", "PGPORT": "6543", "PGDATABASE": "payments_dev", "PGUSER": "payments_dev"}))
        assert (parsed["host"], str(parsed["port"]), parsed["dbname"], parsed["user"]) == (
            "db.internal.example", "6543", "payments_dev", "payments_dev")

    def test_src_does_not_import_the_fast_helper(self):
        class BlockFastHelper:
            def find_spec(self, fullname, path=None, target=None):
                if fullname == "support" or fullname.startswith("support."):
                    raise ImportError("Fast-track support package is unavailable")
                return None

        blocked = BlockFastHelper()
        saved = {name: module for name, module in sys.modules.items()
                 if name == "support" or name.startswith("support.")}
        cached_db = sys.modules.pop("payments_pipeline.db", None)
        for name in saved:
            sys.modules.pop(name, None)
        sys.meta_path.insert(0, blocked)
        try:
            db = importlib.import_module("payments_pipeline.db")
            import psycopg
            parsed = psycopg.conninfo.conninfo_to_dict(db.conninfo_from_env({}))
            assert (parsed.get("host"), str(parsed.get("port")), parsed.get("dbname")) == (
                "localhost", "5432", "postgres")
        finally:
            sys.meta_path.remove(blocked)
            sys.modules.pop("payments_pipeline.db", None)
            if cached_db is not None:
                sys.modules["payments_pipeline.db"] = cached_db
            sys.modules.update(saved)
