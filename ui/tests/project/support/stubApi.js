// Supplied (do not edit). An in-memory stand-in for the Spring API, installed as globalThis.fetch.
// It follows the routes and JSON shapes of docs/contracts and never touches the network.
//
//   const api = installStubApi({ user: 'REQ-0001' });            // fixed signed-in user (W10 Fast, W13 Standard)
//   const api = installStubApi({ auth: 'token' });                // real sign-in with /auth/login (W11 Fast, W14 Standard)
//   api.override('POST /payment-requests/7/approve', { status: 403, body: problem(...) });
//   api.calls                                                     // every request the UI made
//
// Deliberately, GET /approval-queue also returns the signed-in approver's own SUBMITTED requests: the UI
// must leave them out itself (the server rule is checked separately in api/).
import { PASSWORD, USERS, problem, sampleExceptionCases, sampleRequests } from './fixtures.js';

const JSON_TYPE = { 'Content-Type': 'application/json' };
const PROBLEM_TYPE = { 'Content-Type': 'application/problem+json' };

function cents(text) {
  const match = /^(\d+)\.(\d\d)$/.exec(String(text ?? '').trim());
  return match ? Number(match[1]) * 100 + Number(match[2]) : NaN;
}

function deferred() {
  let release;
  const promise = new Promise((resolve) => {
    release = resolve;
  });
  return { promise, release };
}

export function installStubApi(options = {}) {
  const mode = options.auth === 'token' ? 'token' : 'fixed';
  const state = {
    requests: options.requests ?? sampleRequests(),
    cases: options.exceptionCases ?? sampleExceptionCases(),
    tokens: new Map(),
    nextId: 13,
    fixedUser: options.user ?? 'REQ-0001',
  };
  const overrides = new Map();
  const calls = [];

  const json = (status, body, headers = JSON_TYPE) =>
    new Response(body === undefined ? null : JSON.stringify(body), { status, headers });
  const fail = (status, title, detail, errors) => json(status, problem(status, title, detail, errors), PROBLEM_TYPE);

  function currentUser(headers) {
    if (mode === 'fixed') return USERS[state.fixedUser];
    const header = headers.get('authorization') || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    const userId = state.tokens.get(token);
    return userId ? USERS[userId] : null;
  }

  const find = (id) => state.requests.find((r) => String(r.id) === String(id));
  const addEvent = (req, actorId, action, detail) =>
    req.events.push({ seq: req.events.length + 1, actorId, action, detail });

  async function route(method, path, query, headers, body) {
    if (method === 'DELETE') return fail(405, 'Method Not Allowed', 'nothing can be deleted');

    if (method === 'POST' && path === '/auth/login') {
      const known = USERS[body?.userId];
      if (!known || body?.password !== PASSWORD) return fail(401, 'Unauthorized', 'invalid credentials');
      const token = `stub-token-${body.userId}-${state.tokens.size + 1}`;
      state.tokens.set(token, body.userId);
      return json(200, { token, ...known });
    }

    const user = currentUser(headers);
    if (!user) return fail(401, 'Unauthorized', 'sign in required');
    const has = (role) => user.roles.includes(role);

    if (method === 'GET' && path === '/me') return json(200, user);

    if (method === 'GET' && path === '/payment-requests') {
      const status = (query.get('status') || '').toUpperCase();
      const supplier = (query.get('supplierId') || '').toUpperCase();
      return json(
        200,
        state.requests.filter((r) => (!status || r.status === status) && (!supplier || r.supplierId === supplier)),
      );
    }

    if (method === 'POST' && path === '/payment-requests') {
      if (!has('REQUESTER') && !has('APPROVER')) return fail(403, 'Forbidden', 'role');
      const errors = [];
      if (!/^SUP-\d{4}$/.test(body?.supplierId ?? '')) errors.push({ field: 'supplierId', message: 'must look like SUP-0001' });
      if (!String(body?.invoiceNumber ?? '').trim()) errors.push({ field: 'invoiceNumber', message: 'must not be blank' });
      if (Number.isNaN(cents(body?.amount))) errors.push({ field: 'amount', message: 'must be a two-decimal amount' });
      if (body?.currency !== 'USD') errors.push({ field: 'currency', message: 'must be USD' });
      if (errors.length) return fail(400, 'Bad Request', 'invalid payment request', errors);
      const key = (r) => `${r.supplierId}|${String(r.invoiceNumber).toUpperCase().replaceAll(' ', '')}`;
      if (state.requests.some((r) => key(r) === key(body))) return fail(409, 'Conflict', 'duplicate invoice');
      const created = {
        id: state.nextId++,
        supplierId: body.supplierId,
        invoiceNumber: body.invoiceNumber,
        amount: body.amount,
        currency: 'USD',
        requestedBy: user.userId,
        status: 'SUBMITTED',
        submittedAt: '2026-11-18T12:00:00Z',
        events: [],
      };
      addEvent(created, user.userId, 'submitted', body.amount);
      state.requests.push(created);
      return json(201, created);
    }

    let match = /^\/payment-requests\/([^/]+)$/.exec(path);
    if (method === 'GET' && match) {
      if (!/^\d+$/.test(match[1])) return fail(400, 'Bad Request', 'id must be a number');
      const req = find(match[1]);
      return req ? json(200, req) : fail(404, 'Not Found', `payment request ${match[1]} not found`);
    }

    match = /^\/payment-requests\/(\d+)\/(approve|reject|cancel|correct)$/.exec(path);
    if (method === 'POST' && match) {
      const req = find(match[1]);
      if (!req) return fail(404, 'Not Found', `payment request ${match[1]} not found`);
      const action = match[2];
      if (action === 'approve' || action === 'reject') {
        if (!has('APPROVER')) return fail(403, 'Forbidden', 'role');
        if (req.requestedBy === user.userId) return fail(403, 'Forbidden', 'own request');
        if (req.status !== 'SUBMITTED') return fail(409, 'Conflict', `cannot ${action} a ${req.status} request`);
        if (action === 'approve') {
          if (cents(req.amount) > cents(user.approvalLimit)) return fail(403, 'Forbidden', 'over approval limit');
          req.status = 'APPROVED';
          addEvent(req, user.userId, 'approved', req.amount);
        } else {
          if (!String(body?.reason ?? '').trim()) {
            return fail(400, 'Bad Request', 'reason required', [{ field: 'reason', message: 'must not be blank' }]);
          }
          req.status = 'REJECTED';
          addEvent(req, user.userId, 'rejected', body.reason);
        }
        return json(200, req);
      }
      if (action === 'correct') {
        if (req.requestedBy !== user.userId) return fail(403, 'Forbidden', 'only the requester can correct');
        if (req.status !== 'SUBMITTED') return fail(409, 'Conflict', `cannot correct a ${req.status} request`);
        const errors = [];
        if (Number.isNaN(cents(body?.amount))) errors.push({ field: 'amount', message: 'must be a two-decimal amount' });
        if (!String(body?.reason ?? '').trim()) errors.push({ field: 'reason', message: 'must not be blank' });
        if (errors.length) return fail(400, 'Bad Request', 'invalid correction', errors);
        addEvent(req, user.userId, 'amount-corrected', `${req.amount}->${body.amount}: ${body.reason}`);
        req.amount = body.amount;
        return json(200, req);
      }
      return fail(409, 'Conflict', 'not supported by the stub');
    }

    if (method === 'GET' && path === '/approval-queue') {
      // Reads are open to any signed-in user (docs/contracts/W08-openapi.yaml); the screens decide who sees what.
      const size = Number(query.get('size') || 20);
      const page = Number(query.get('page') || 0);
      const all = state.requests.filter((r) => r.status === 'SUBMITTED');
      const content = all.slice(page * size, page * size + size);
      return json(200, { content, page, size, totalElements: all.length, totalPages: Math.ceil(all.length / size) });
    }

    if (method === 'GET' && path === '/exception-cases') {
      const status = (query.get('status') || '').toUpperCase();
      return json(200, state.cases.filter((c) => !status || c.status === status));
    }

    match = /^\/exception-cases\/(\d+)\/resolve$/.exec(path);
    if (method === 'POST' && match) {
      if (!has('PAYABLES_CLERK')) return fail(403, 'Forbidden', 'role');
      const found = state.cases.find((c) => String(c.id) === match[1]);
      if (!found) return fail(404, 'Not Found', `exception case ${match[1]} not found`);
      if (!String(body?.note ?? '').trim()) {
        return fail(400, 'Bad Request', 'note required', [{ field: 'note', message: 'must not be blank' }]);
      }
      if (found.status !== 'OPEN') return fail(409, 'Conflict', 'exception case already resolved');
      Object.assign(found, { status: 'RESOLVED', resolvedBy: user.userId, resolvedAt: '2026-11-18T11:00:00Z', note: body.note });
      const req = find(found.paymentRequestId);
      if (req) addEvent(req, user.userId, 'exception-resolved', body.note);
      return json(200, found);
    }

    return fail(404, 'Not Found', `no route ${method} ${path}`);
  }

  async function stubFetch(input, init = {}) {
    const isRequest = typeof Request !== 'undefined' && input instanceof Request;
    const url = new URL(isRequest ? input.url : String(input), 'http://localhost');
    const method = (init.method || (isRequest ? input.method : 'GET')).toUpperCase();
    const headers = new Headers(init.headers || (isRequest ? input.headers : undefined));
    let raw = init.body;
    if (raw === undefined && isRequest && method !== 'GET') raw = await input.clone().text();
    let body = null;
    if (typeof raw === 'string' && raw.length) {
      try {
        body = JSON.parse(raw);
      } catch {
        body = raw;
      }
    }
    const call = { method, path: url.pathname, query: url.searchParams, headers, body };
    calls.push(call);

    const override = overrides.get(`${method} ${url.pathname}`);
    if (override) {
      if (override.once) overrides.delete(`${method} ${url.pathname}`);
      if (override.pending) await override.pending.promise;
      if (override.networkError) throw new TypeError('Failed to fetch');
      if (override.status) {
        return json(override.status, override.body, override.status >= 400 ? PROBLEM_TYPE : JSON_TYPE);
      }
    }
    return route(method, url.pathname, url.searchParams, headers, body);
  }

  globalThis.fetch = stubFetch;

  return {
    calls,
    state,
    /** Replace one route's answer: { status, body } | { networkError: true } | { pending: true } (then release()). */
    override(key, answer) {
      const entry = { ...answer };
      if (answer.pending) entry.pending = deferred();
      overrides.set(key, entry);
      return { release: () => entry.pending?.release() };
    },
    callsTo(method, path) {
      return calls.filter((c) => c.method === method && c.path === path);
    },
    tokenFor(userId) {
      return [...state.tokens.entries()].find(([, id]) => id === userId)?.[0];
    },
    expireTokens() {
      state.tokens.clear();
    },
  };
}
