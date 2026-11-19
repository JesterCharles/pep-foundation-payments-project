// LEARNER-OWNED (Standard W11): queue helpers over PaymentRequestView objects (docs/contracts).
// Both return a NEW array and never change the input.

/** Requests with exactly this status, in the original order; '' or undefined keeps all. */
export function filterByStatus(requests, status) {
  throw new Error('TODO: filterByStatus');
}

/** Sorted by exact amount ('desc' largest first, the default, or 'asc'); equal amounts by id ascending. */
export function sortByAmount(requests, direction = 'desc') {
  throw new Error('TODO: sortByAmount');
}
