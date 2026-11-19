"""Client for your Spring payment API: configured URL, token and timeout (see CONTRACT.md)."""


class SpringApiError(Exception):
    """kind is 'unauthorized', 'timeout', 'unavailable' or 'bad_response'."""

    def __init__(self, kind, message=""):
        super().__init__(message or kind)
        self.kind = kind


class SpringApiClient:
    def __init__(self, base_url, token, timeout_seconds=2.0):
        self.base_url = base_url
        self.token = token
        self.timeout_seconds = timeout_seconds

    @classmethod
    def from_env(cls):
        raise NotImplementedError("TODO: read SPRING_API_BASE_URL, SPRING_API_TOKEN, SPRING_API_TIMEOUT_SECONDS")

    def sent_requests(self):
        raise NotImplementedError("TODO: GET /payment-requests?status=SENT with the bearer token")

    def get_request(self, request_id):
        raise NotImplementedError("TODO: GET /payment-requests/{id}; None on 404")
