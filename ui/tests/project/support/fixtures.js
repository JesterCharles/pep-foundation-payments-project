// Supplied (do not edit). Synthetic users, payment requests and exception cases for the supplied UI
// checks. Shapes follow docs/contracts (PaymentRequestView, ExceptionCaseView). No real data.

export const PASSWORD = 'change-me-local-only';

export const USERS = {
  'REQ-0001': { userId: 'REQ-0001', roles: ['REQUESTER'], approvalLimit: null },
  'REQ-0002': { userId: 'REQ-0002', roles: ['REQUESTER'], approvalLimit: null },
  'APR-0001': { userId: 'APR-0001', roles: ['APPROVER'], approvalLimit: '5000.00' },
  'APR-0002': { userId: 'APR-0002', roles: ['APPROVER'], approvalLimit: '25000.00' },
  'PAY-0001': { userId: 'PAY-0001', roles: ['PAYABLES_CLERK'], approvalLimit: null },
};

function request(id, supplierId, invoiceNumber, amount, requestedBy, status, extraEvents = []) {
  return {
    id,
    supplierId,
    invoiceNumber,
    amount,
    currency: 'USD',
    requestedBy,
    status,
    submittedAt: `2026-11-1${id % 10}T09:00:00Z`,
    events: [{ seq: 1, actorId: requestedBy, action: 'submitted', detail: amount }, ...extraEvents],
  };
}

/** A fresh copy every call, so tests never share mutated state. */
export function sampleRequests() {
  return [
    request(7, 'SUP-0042', 'INV 7781', '1234.50', 'REQ-0001', 'SUBMITTED'),
    request(8, 'SUP-0007', 'INV-2001', '7000.00', 'REQ-0002', 'SUBMITTED'),
    request(9, 'SUP-0042', 'INV-3001', '250.00', 'REQ-0001', 'APPROVED', [
      { seq: 2, actorId: 'APR-0002', action: 'approved', detail: '250.00' },
    ]),
    request(10, 'SUP-0013', 'INV-4001', '99.99', 'REQ-0002', 'EXCEPTION', [
      { seq: 2, actorId: 'APR-0001', action: 'approved', detail: '99.99' },
      { seq: 3, actorId: 'PAY-0001', action: 'sent', detail: 'PP-000010' },
      { seq: 4, actorId: 'PAY-0001', action: 'exception-opened', detail: 'statement amount 98.99 differs from 99.99' },
    ]),
    request(11, 'SUP-0050', 'INV-5001', '100.00', 'APR-0001', 'SUBMITTED'),
  ];
}

export function sampleExceptionCases() {
  return [
    {
      id: 3,
      paymentRequestId: 10,
      status: 'OPEN',
      reason: 'statement amount 98.99 differs from 99.99',
      openedBy: 'PAY-0001',
      openedAt: '2026-11-18T10:00:00Z',
      resolvedBy: null,
      resolvedAt: null,
      note: null,
    },
  ];
}

export function problem(status, title, detail, errors) {
  const body = { type: 'about:blank', title, status, detail };
  if (errors) body.errors = errors;
  return body;
}
