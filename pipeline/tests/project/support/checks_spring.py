"""Spring API client checks against an in-test stub of the learner's Spring API (supplied)."""

import json
import secrets
import time
from pathlib import Path

import pytest

from payments_pipeline.spring_client import SpringApiClient, SpringApiError

from .stub_server import StubServer, closed_port_url

SAMPLES = Path(__file__).resolve().parents[3] / "data" / "samples"
TOKEN = "test-" + secrets.token_hex(16)  # generated at test time, never committed
REQUESTS = json.loads((SAMPLES / "spring-requests.json").read_text(encoding="utf-8"))


def spring_stub(status=200, delay=0, body=None, token=TOKEN):
    def queue(request):
        if request.headers.get("authorization") != f"Bearer {token}":
            return 401, {"title": "Unauthorized", "status": 401}, 0
        if status != 200:
            return status, {"status": status}, delay
        if body is not None:
            return 200, body, delay
        sent = [r for r in REQUESTS if r["status"] == request.query.get("status", [""])[0]]
        return 200, sent, delay

    def one(request_id):
        def route(request):
            if request.headers.get("authorization") != f"Bearer {token}":
                return 401, {"status": 401}, 0
            for r in REQUESTS:
                if r["id"] == request_id:
                    return 200, r, delay
            return 404, {"status": 404}, 0
        return route

    routes = {("GET", "/payment-requests"): queue}
    for n in (1, 2, 3, 4, 5, 404):
        routes[("GET", f"/payment-requests/{n}")] = one(n)
    return StubServer(routes)


class SpringClientChecks:
    def test_ok_sends_bearer_token_and_status_filter(self):
        with spring_stub() as stub:
            sent = SpringApiClient(stub.base_url, TOKEN, timeout_seconds=2).sent_requests()
        assert [r["id"] for r in sent] == [1, 2, 3, 4]
        request = stub.requests[0]
        assert request.path == "/payment-requests" and request.query == {"status": ["SENT"]}
        assert request.headers["authorization"] == f"Bearer {TOKEN}"

    def test_get_request_and_404(self):
        with spring_stub() as stub:
            client = SpringApiClient(stub.base_url, TOKEN, timeout_seconds=2)
            assert client.get_request(3)["amount"] == "5000.00"
            assert client.get_request(404) is None

    def test_401_is_unauthorized(self):
        with spring_stub(token="a-different-token") as stub:
            with pytest.raises(SpringApiError) as error:
                SpringApiClient(stub.base_url, TOKEN, timeout_seconds=2).sent_requests()
        assert error.value.kind == "unauthorized"

    def test_slow_api_times_out_within_the_configured_timeout(self):
        with spring_stub(delay=2.0) as stub:
            started = time.monotonic()
            with pytest.raises(SpringApiError) as error:
                SpringApiClient(stub.base_url, TOKEN, timeout_seconds=0.3).sent_requests()
            elapsed = time.monotonic() - started
        assert error.value.kind == "timeout"
        assert elapsed < 1.5, f"the client waited {elapsed:.2f}s; the configured timeout was 0.3s"

    def test_unavailable_and_bad_responses(self):
        with pytest.raises(SpringApiError) as refused:
            SpringApiClient(closed_port_url(), TOKEN, timeout_seconds=0.5).sent_requests()
        assert refused.value.kind == "unavailable"
        with spring_stub(status=503) as stub:
            with pytest.raises(SpringApiError) as down:
                SpringApiClient(stub.base_url, TOKEN, timeout_seconds=2).sent_requests()
        assert down.value.kind == "unavailable"
        with spring_stub(body=b"<html>not json</html>") as stub:
            with pytest.raises(SpringApiError) as bad:
                SpringApiClient(stub.base_url, TOKEN, timeout_seconds=2).sent_requests()
        assert bad.value.kind == "bad_response"

    def test_configuration_comes_from_the_environment(self, monkeypatch):
        monkeypatch.setenv("SPRING_API_BASE_URL", "http://spring.test:8080")
        monkeypatch.setenv("SPRING_API_TOKEN", "env-token")
        monkeypatch.setenv("SPRING_API_TIMEOUT_SECONDS", "1.5")
        client = SpringApiClient.from_env()
        assert (client.base_url.rstrip("/"), client.token, float(client.timeout_seconds)) == (
            "http://spring.test:8080", "env-token", 1.5)
