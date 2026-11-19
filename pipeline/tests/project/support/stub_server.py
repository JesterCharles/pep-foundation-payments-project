"""A tiny in-test HTTP server on 127.0.0.1 (supplied; do not edit). No network leaves the machine."""

import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit


class Recorded:
    def __init__(self, method, path, query, headers, body):
        self.method = method
        self.path = path
        self.query = query
        self.headers = headers
        self.body = body

    def json(self):
        return json.loads(self.body.decode("utf-8"))


class StubServer:
    """Routes map (method, path) to a function(Recorded) -> (status, body, delay_seconds).

    body may be a dict/list (sent as JSON), bytes, or str.
    """

    def __init__(self, routes):
        self.routes = routes
        self.requests = []
        stub = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def _handle(self):
                parts = urlsplit(self.path)
                length = int(self.headers.get("Content-Length") or 0)
                body = self.rfile.read(length) if length else b""
                recorded = Recorded(self.command, parts.path, parse_qs(parts.query),
                                    {k.lower(): v for k, v in self.headers.items()}, body)
                stub.requests.append(recorded)
                route = stub.routes.get((self.command, parts.path))
                if route is None:
                    status, payload, delay = 404, {"error": "no stub route"}, 0
                else:
                    status, payload, delay = route(recorded)
                if delay:
                    time.sleep(delay)
                if isinstance(payload, (dict, list)):
                    data = json.dumps(payload).encode("utf-8")
                    content_type = "application/json"
                elif isinstance(payload, bytes):
                    data, content_type = payload, "application/octet-stream"
                else:
                    data, content_type = str(payload).encode("utf-8"), "text/plain"
                try:
                    self.send_response(status)
                    self.send_header("Content-Type", content_type)
                    self.send_header("Content-Length", str(len(data)))
                    self.end_headers()
                    self.wfile.write(data)
                except (BrokenPipeError, ConnectionResetError):
                    pass

            do_GET = _handle
            do_POST = _handle

        self._server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self._server.daemon_threads = True
        self._server.block_on_close = False  # do not wait for deliberately slow handlers
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)

    @property
    def base_url(self):
        host, port = self._server.server_address[:2]
        return f"http://{host}:{port}"

    def __enter__(self):
        self._thread.start()
        return self

    def __exit__(self, *exc):
        self._server.shutdown()
        self._server.server_close()


def closed_port_url():
    """A localhost URL where nothing listens (connection refused)."""
    import socket

    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        port = s.getsockname()[1]
    return f"http://127.0.0.1:{port}"
