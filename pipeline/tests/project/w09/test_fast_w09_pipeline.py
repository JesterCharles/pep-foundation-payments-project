"""Fast W09 supplied checks: import pipeline and FastAPI reconciliation service (do not edit).

Run from pipeline/:  python -m pytest --milestone fast-w09
Fast uses the supplied connection helper (support/db_connection.py); connection code is not graded.
PostgreSQL staging checks run when PIPELINE_DATABASE_URL or PG* is set, and skip otherwise.
"""

import pytest

from project.support.checks_ai import AiChecks
from project.support.checks_amounts import AmountChecks
from project.support.checks_import import ImportChecks
from project.support.checks_service import ReconcileChecks, ServiceChecks
from project.support.checks_spring import SpringClientChecks
from project.support.checks_staging import StagingChecks


@pytest.fixture
def connect():
    from support import db_connection

    return db_connection.connect


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
