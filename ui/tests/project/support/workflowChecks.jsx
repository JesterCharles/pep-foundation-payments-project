// Supplied (do not edit). Routed request workflows with a fixed synthetic user (no sign-in yet):
// submit, list, detail by route parameter, correct with a reason, approve, reject with a reason,
// server refusals shown without changing status, clear pending and failure feedback, and no delete anywhere.
import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { installStubApi } from './stubApi.js';
import { problem } from './fixtures.js';
import { expectNoSuccessMessage, expectPath, renderApp, statusText } from './render.jsx';

export function workflowSuite() {
  describe('requester submits a payment request', () => {
    it('posts exact text amounts in USD and shows the new request', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      const { user } = renderApp('/requests/new');
      await user.type(await screen.findByLabelText(/supplier id/i), 'SUP-0042');
      await user.type(screen.getByLabelText(/invoice number/i), 'INV-9001');
      await user.type(screen.getByLabelText(/^amount$/i), '1500.10');
      await user.click(screen.getByRole('button', { name: /submit request/i }));
      await expectPath('/requests/13');
      const [post] = api.callsTo('POST', '/payment-requests');
      expect(post.body).toEqual({ supplierId: 'SUP-0042', invoiceNumber: 'INV-9001', amount: '1500.10', currency: 'USD' });
      expect(await statusText()).toContain('SUBMITTED');
    });

    it('shows a 409 duplicate refusal and stays on the form with the values kept', async () => {
      installStubApi({ user: 'REQ-0001' });
      const { user } = renderApp('/requests/new');
      await user.type(await screen.findByLabelText(/supplier id/i), 'SUP-0042');
      await user.type(screen.getByLabelText(/invoice number/i), 'inv7781');
      await user.type(screen.getByLabelText(/^amount$/i), '10.00');
      await user.click(screen.getByRole('button', { name: /submit request/i }));
      expect(await screen.findByRole('alert')).toHaveTextContent(/duplicate invoice/i);
      expect(screen.getByLabelText(/invoice number/i)).toHaveValue('inv7781');
      await expectPath('/requests/new');
      expectNoSuccessMessage();
    });

    it('shows the field messages of a 400 validation ProblemDetail', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      api.override('POST /payment-requests', {
        status: 400,
        body: problem(400, 'Bad Request', 'invalid payment request', [
          { field: 'amount', message: 'must be a two-decimal amount' },
        ]),
      });
      const { user } = renderApp('/requests/new');
      await user.type(await screen.findByLabelText(/supplier id/i), 'SUP-0042');
      await user.type(screen.getByLabelText(/invoice number/i), 'INV-9002');
      await user.type(screen.getByLabelText(/^amount$/i), '12.00');
      await user.click(screen.getByRole('button', { name: /submit request/i }));
      expect(await screen.findByRole('alert')).toHaveTextContent(/must be a two-decimal amount/i);
      await expectPath('/requests/new');
    });
  });

  describe('list and detail by route parameter', () => {
    it('lists requests with links that open the detail route', async () => {
      installStubApi({ user: 'REQ-0001' });
      const { user } = renderApp('/requests');
      await user.click(await screen.findByRole('link', { name: /INV 7781/ }));
      await expectPath('/requests/7');
      expect(await statusText()).toContain('SUBMITTED');
    });

    it('loads the request named in the URL and shows its full history', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      renderApp('/requests/9');
      const history = await screen.findByRole('list', { name: /history/i });
      const items = within(history).getAllByRole('listitem');
      expect(items).toHaveLength(2);
      expect(items[1]).toHaveTextContent('approved');
      expect(items[1]).toHaveTextContent('APR-0002');
      expect(screen.getAllByText(/INV-3001/).length).toBeGreaterThan(0);
      expect(await statusText()).toContain('APPROVED');
      expect(api.callsTo('GET', '/payment-requests/9')).not.toHaveLength(0);
    });

    it('shows "Loading" while the request is on its way, then the request', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      const gate = api.override('GET /payment-requests/7', { pending: true });
      renderApp('/requests/7');
      await waitFor(() => expect(api.callsTo('GET', '/payment-requests/7')).toHaveLength(1));
      expect(await screen.findByText(/loading/i)).toBeInTheDocument();
      gate.release();
      expect(await statusText()).toContain('SUBMITTED');
      await waitFor(() => expect(screen.queryByText(/loading/i)).not.toBeInTheDocument());
    });

    it.each([
      ['a 404 ProblemDetail', { status: 404, body: problem(404, 'Not Found', 'payment request 7 not found') }, /not found/i],
      ['a 500 error', { status: 500, body: problem(500, 'Internal Server Error', 'unexpected failure') }, /./],
      ['a network failure', { networkError: true }, /./],
    ])('shows an error alert for %s and no stale request data', async (_label, answer, text) => {
      const api = installStubApi({ user: 'REQ-0001' });
      api.override('GET /payment-requests/7', answer);
      renderApp('/requests/7');
      expect(await screen.findByRole('alert')).toHaveTextContent(text);
      expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
      expect(screen.queryByLabelText(/^status$/i)).not.toBeInTheDocument();
    });
  });

  describe('requester corrects an amount with a reason', () => {
    it('sends the new amount and reason, then shows the corrected amount and history', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      const { user } = renderApp('/requests/7');
      await user.type(await screen.findByLabelText(/new amount/i), '1200.00');
      await user.type(screen.getByLabelText(/correction reason/i), 'credit note applied');
      await user.click(screen.getByRole('button', { name: /correct amount/i }));
      await waitFor(() => expect(api.callsTo('POST', '/payment-requests/7/correct')).toHaveLength(1));
      expect(api.callsTo('POST', '/payment-requests/7/correct')[0].body).toEqual({
        amount: '1200.00',
        reason: 'credit note applied',
      });
      const history = await screen.findByRole('list', { name: /history/i });
      await waitFor(() => expect(history).toHaveTextContent('amount-corrected'));
      expect(screen.getAllByText(/1200\.00/).length).toBeGreaterThan(0);
    });

    it('never sends a correction without a reason', async () => {
      const api = installStubApi({ user: 'REQ-0001' });
      const { user } = renderApp('/requests/7');
      await user.type(await screen.findByLabelText(/new amount/i), '1200.00');
      await user.click(screen.getByRole('button', { name: /correct amount/i }));
      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(api.callsTo('POST', '/payment-requests/7/correct')).toHaveLength(0);
      expect(await statusText()).toContain('SUBMITTED');
    });

    it('offers no approve control to a requester', async () => {
      installStubApi({ user: 'REQ-0001' });
      renderApp('/requests/7');
      await statusText();
      expect(screen.queryByRole('button', { name: /^approve$/i })).not.toBeInTheDocument();
    });
  });

  describe('approver approves or rejects', () => {
    it('approves and shows APPROVED with the approver in the history', async () => {
      const api = installStubApi({ user: 'APR-0002' });
      const { user } = renderApp('/requests/7');
      await user.click(await screen.findByRole('button', { name: /^approve$/i }));
      await waitFor(async () => expect(await statusText()).toContain('APPROVED'));
      expect(api.callsTo('POST', '/payment-requests/7/approve')).toHaveLength(1);
      const history = screen.getByRole('list', { name: /history/i });
      expect(within(history).getAllByRole('listitem').at(-1)).toHaveTextContent('APR-0002');
    });

    it.each([
      [403, 'Forbidden', 'over approval limit'],
      [409, 'Conflict', 'cannot approve a APPROVED request'],
    ])('shows a %s refusal without changing the displayed status', async (status, title, detail) => {
      const api = installStubApi({ user: 'APR-0002' });
      api.override('POST /payment-requests/7/approve', { status, body: problem(status, title, detail) });
      const { user } = renderApp('/requests/7');
      await user.click(await screen.findByRole('button', { name: /^approve$/i }));
      expect(await screen.findByRole('alert')).toHaveTextContent(detail);
      expect(await statusText()).toContain('SUBMITTED');
      expectNoSuccessMessage();
    });

    it('rejects with a reason and never rejects without one', async () => {
      const api = installStubApi({ user: 'APR-0002' });
      const { user } = renderApp('/requests/7');
      await user.click(await screen.findByRole('button', { name: /^reject$/i }));
      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(api.callsTo('POST', '/payment-requests/7/reject')).toHaveLength(0);
      await user.type(screen.getByLabelText(/rejection reason/i), 'supplier on hold');
      await user.click(screen.getByRole('button', { name: /^reject$/i }));
      await waitFor(async () => expect(await statusText()).toContain('REJECTED'));
      expect(api.callsTo('POST', '/payment-requests/7/reject')[0].body).toEqual({ reason: 'supplier on hold' });
    });

    it('offers no correction control on someone else\'s request', async () => {
      installStubApi({ user: 'APR-0002' });
      renderApp('/requests/7');
      await statusText();
      expect(screen.queryByRole('button', { name: /correct amount/i })).not.toBeInTheDocument();
    });
  });

  describe('nothing can be deleted', () => {
    it('shows no delete or remove control on the list or detail screens', async () => {
      for (const [userId, path] of [
        ['REQ-0001', '/requests'],
        ['REQ-0001', '/requests/7'],
        ['APR-0002', '/requests/8'],
      ]) {
        const api = installStubApi({ user: userId });
        const { unmount } = renderApp(path);
        await screen.findAllByText(/INV/);
        expect(screen.queryByRole('button', { name: /delete|remove/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /delete|remove/i })).not.toBeInTheDocument();
        expect(api.calls.filter((c) => c.method === 'DELETE')).toHaveLength(0);
        unmount();
      }
    });

  });
}
