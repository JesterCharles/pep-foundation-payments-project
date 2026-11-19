// Supplied check support (do not edit): synthetic, generated test data. No real suppliers or money.

/** Small deterministic generator so failures are reproducible. */
export function random(seed) {
  let state = seed >>> 0 || 1;
  return () => {
    state = (Math.imul(state, 1664525) + 1013904223) >>> 0;
    return state / 2 ** 32;
  };
}

/** Amount texts with two decimals and their exact integer total. */
export function generatedAmounts(seed, count) {
  const next = random(seed);
  const texts = [];
  let oracleCents = 0;
  for (let i = 0; i < count; i++) {
    const cents = 1 + Math.floor(next() * 99_999_999);
    oracleCents += cents;
    texts.push(`${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`);
  }
  return { texts, oracleCents };
}

/** A PaymentRequestView as the API returns it (docs/contracts). */
export function request(id, overrides = {}) {
  return {
    id,
    supplierId: `SUP-${String(1000 + id).slice(-4)}`,
    invoiceNumber: `INV-${id}`,
    amount: '100.00',
    currency: 'USD',
    requestedBy: 'REQ-0001',
    status: 'SUBMITTED',
    submittedAt: '2026-11-18T09:00:00Z',
    events: [{ seq: 1, actorId: 'REQ-0001', action: 'submitted', detail: overrides.amount || '100.00' }],
    ...overrides,
  };
}

export function problem(status, title, detail, errors) {
  return { type: 'about:blank', title, status, detail, ...(errors ? { errors } : {}) };
}
