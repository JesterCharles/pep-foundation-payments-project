"""FastAPI reconciliation service (see CONTRACT.md).

Run locally from pipeline/ (after your implementation works):
    uvicorn --factory payments_pipeline.service:app_from_env --app-dir src --port 8000
"""

import os

from fastapi import FastAPI

from .spring_client import SpringApiClient


def create_app(spring, statements_dir):
    app = FastAPI(title="Payment reconciliation service (training project)")
    # TODO: GET /health, GET /discrepancies?statement=NAME and GET /discrepancies/{reference}?statement=NAME
    return app


def app_from_env():
    return create_app(SpringApiClient.from_env(), os.environ.get("STATEMENTS_DIR", "data/samples"))
