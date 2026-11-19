// Starter (yours to edit). One place for every call to your Spring API. Use relative paths
// (/payment-requests ...): the Vite dev server proxies them to http://localhost:8080.
//
// Contract used by the supplied checks (see ui/README.md):
//   approveRequest(id, token) -> Promise of the updated PaymentRequestView; when the server refuses,
//   the promise rejects with an Error that has `status` (e.g. 403) and `problem` (the ProblemDetail body).

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || `request failed with ${status}`);
    this.status = status;
    this.problem = problem;
  }
}

export async function approveRequest(id, token) {
  // TODO: POST /payment-requests/{id}/approve with the Authorization header when a token is given.
  throw new Error(`TODO: approveRequest(${id}) is not implemented yet`);
}
