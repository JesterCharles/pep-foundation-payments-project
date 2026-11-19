// Supplied (do not edit). Payables clerk resolves exception cases with a note; the resolver is recorded
// and shown, and the request history shows the resolution. Works with a fixed user (Standard W13) or
// after signing in (Fast W11), chosen by the `signedIn` option.
import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { installStubApi } from './stubApi.js';
import { problem } from './fixtures.js';
import { expectPath, renderApp, signIn } from './render.jsx';

async function openExceptions(signedIn) {
  const api = signedIn ? installStubApi({ auth: 'token' }) : installStubApi({ user: 'PAY-0001' });
  const view = renderApp('/exceptions');
  if (signedIn) {
    await expectPath('/sign-in');
    await signIn(view.user, 'PAY-0001');
    await expectPath('/exceptions');
  }
  const caseCard = await screen.findByRole('article', { name: /exception case 3/i });
  return { api, ...view, caseCard };
}

export function exceptionSuite({ signedIn = false } = {}) {
  describe('payables clerk resolves exception cases', () => {
    it('lists the open case with its request and reason', async () => {
      const { api, caseCard } = await openExceptions(signedIn);
      expect(caseCard).toHaveTextContent('statement amount 98.99 differs from 99.99');
      expect(within(caseCard).getByRole('link', { name: /10/ })).toBeInTheDocument();
      expect(api.calls.some((c) => c.method === 'GET' && c.path === '/exception-cases')).toBe(true);
    });

    it('never sends a resolution without a note', async () => {
      const { api, user, caseCard } = await openExceptions(signedIn);
      await user.click(within(caseCard).getByRole('button', { name: /resolve/i }));
      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(api.callsTo('POST', '/exception-cases/3/resolve')).toHaveLength(0);
      expect(caseCard).not.toHaveTextContent(/resolved by/i);
    });

    it('resolves with a note, shows the resolver, and the request history records it', async () => {
      const { api, user, caseCard } = await openExceptions(signedIn);
      await user.type(within(caseCard).getByLabelText(/resolution note/i), 'supplier confirmed the statement fee');
      await user.click(within(caseCard).getByRole('button', { name: /resolve/i }));
      await waitFor(() => expect(api.callsTo('POST', '/exception-cases/3/resolve')).toHaveLength(1));
      expect(api.callsTo('POST', '/exception-cases/3/resolve')[0].body).toEqual({
        note: 'supplier confirmed the statement fee',
      });
      const resolved = await screen.findByRole('article', { name: /exception case 3/i });
      await waitFor(() => expect(resolved).toHaveTextContent(/resolved by\s*PAY-0001/i));
      await user.click(within(resolved).getByRole('link', { name: /10/ }));
      await expectPath('/requests/10');
      const history = await screen.findByRole('list', { name: /history/i });
      const last = within(history).getAllByRole('listitem').at(-1);
      expect(last).toHaveTextContent('exception-resolved');
      expect(last).toHaveTextContent('PAY-0001');
      expect(last).toHaveTextContent('supplier confirmed the statement fee');
    });

    it('shows a server refusal and leaves the case open', async () => {
      const { api, user, caseCard } = await openExceptions(signedIn);
      api.override('POST /exception-cases/3/resolve', {
        status: 409,
        body: problem(409, 'Conflict', 'exception case already resolved'),
      });
      await user.type(within(caseCard).getByLabelText(/resolution note/i), 'retry');
      await user.click(within(caseCard).getByRole('button', { name: /resolve/i }));
      expect(await screen.findByRole('alert')).toHaveTextContent('exception case already resolved');
      expect(screen.getByRole('article', { name: /exception case 3/i })).not.toHaveTextContent(/resolved by/i);
    });

    it('offers no delete or remove control for exception cases', async () => {
      const { api } = await openExceptions(signedIn);
      expect(screen.queryByRole('button', { name: /delete|remove/i })).not.toBeInTheDocument();
      expect(api.calls.filter((c) => c.method === 'DELETE')).toHaveLength(0);
    });
  });
}
