// Supplied check support (do not edit): an in-test stand-in for the Spring API. No network is used.
import { problem, request } from './fixtures.js';

function json(status, body, type = 'application/json') {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': type },
  });
}

/**
 * Builds a fetch stub. `scenario` picks how the queue load and each action answer:
 *   queue: 'ok' | 'empty' | 401 | 500 | 'network'
 *   approve: 200 | 403 | 409 | 500
 *   submit: 201 | 400 | 409
 */
export function stubApi({ requests = [], queue = 'ok', approve = 200, submit = 201 } = {}) {
  const calls = [];
  const state = requests.map((r) => ({ ...r }));

  async function fetchStub(input, init = {}) {
    const url = new URL(typeof input === 'string' ? input : input.url, 'http://localhost');
    const method = (init.method || (typeof input === 'object' && input.method) || 'GET').toUpperCase();
    const headers = new Headers(init.headers || (typeof input === 'object' ? input.headers : undefined));
    const body = typeof init.body === 'string' ? init.body : undefined;
    calls.push({ method, path: url.pathname, search: url.search, headers, body });

    if (method === 'GET' && (url.pathname === '/payment-requests' || url.pathname === '/approval-queue')) {
      if (queue === 'network') throw new TypeError('Failed to fetch');
      if (queue === 401) return json(401, problem(401, 'Unauthorized', 'sign in required'), 'application/problem+json');
      if (queue === 500) return json(500, problem(500, 'Internal Server Error', 'unexpected failure'), 'application/problem+json');
      if (url.pathname === '/approval-queue') {
        // The queue holds SUBMITTED requests only (docs/contracts/W08-openapi.yaml); it takes no status filter.
        const waiting = queue === 'empty' ? [] : state.filter((r) => r.status === 'SUBMITTED');
        return json(200, { content: waiting, page: 0, size: 20, totalElements: waiting.length, totalPages: waiting.length ? 1 : 0 });
      }
      const status = url.searchParams.get('status');
      const list = queue === 'empty' ? [] : state.filter((r) => !status || r.status === status.toUpperCase());
      return json(200, list);
    }

    const action = /^\/payment-requests\/(\d+)\/approve$/.exec(url.pathname);
    if (method === 'POST' && action) {
      const target = state.find((r) => r.id === Number(action[1]));
      if (approve === 403) {
        return json(403, problem(403, 'Forbidden', 'over approval limit'), 'application/problem+json');
      }
      if (approve === 409) return json(409, problem(409, 'Conflict', 'request is not awaiting approval'), 'application/problem+json');
      if (approve === 500) return json(500, problem(500, 'Internal Server Error', 'unexpected failure'), 'application/problem+json');
      target.status = 'APPROVED';
      target.events = [...target.events, { seq: target.events.length + 1, actorId: 'APR-0002', action: 'approved', detail: target.amount }];
      return json(200, target);
    }

    if (method === 'POST' && url.pathname === '/payment-requests') {
      if (submit === 400) {
        return json(400, problem(400, 'Bad Request', 'invalid payment request', [
          { field: 'amount', message: 'amount must have exactly two decimals' },
        ]), 'application/problem+json');
      }
      if (submit === 409) return json(409, problem(409, 'Conflict', 'duplicate invoice'), 'application/problem+json');
      const sent = JSON.parse(body || '{}');
      const created = request(100, { ...sent, status: 'SUBMITTED' });
      state.push(created);
      return json(201, created);
    }

    return json(404, problem(404, 'Not Found', `no stub for ${method} ${url.pathname}`), 'application/problem+json');
  }

  return { fetch: fetchStub, calls, state };
}
