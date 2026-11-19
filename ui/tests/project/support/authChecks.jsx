// Supplied (do not edit). Signed-in role screens against a stubbed auth API: 401 sends you to sign in,
// a screen for another role sends you to your own role screen, sign-out clears the session, approvers
// never see their own requests in the queue, and approve is hidden above the approver's limit while the
// server still refuses a forced call with 403.
import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { installStubApi } from './stubApi.js';
import { currentPath, expectPath, renderApp, signIn } from './render.jsx';

const ROLE_HOME = { 'REQ-0001': '/requests', 'APR-0001': '/queue', 'PAY-0001': '/exceptions' };

function storedValues() {
  const values = [];
  for (const storage of [window.localStorage, window.sessionStorage]) {
    for (let i = 0; i < storage.length; i += 1) values.push(storage.getItem(storage.key(i)));
  }
  return values.join('\n');
}

async function signedInAs(userId, startPath = '/') {
  const api = installStubApi({ auth: 'token' });
  const view = renderApp(startPath);
  await expectPath('/sign-in');
  await signIn(view.user, userId);
  await expectPath(ROLE_HOME[userId]);
  return { api, ...view };
}

export function authSuite() {
  describe('sign-in and 401 handling', () => {
    it('sends an unauthenticated visitor to the sign-in screen', async () => {
      const api = installStubApi({ auth: 'token' });
      renderApp('/queue');
      await expectPath('/sign-in');
      expect(await screen.findByLabelText(/user id/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toHaveAttribute('type', 'password');
      expect(api.calls.some((c) => c.headers.get('authorization'))).toBe(false);
    });

    it('shows a refusal for wrong credentials and stays on sign-in', async () => {
      installStubApi({ auth: 'token' });
      const { user } = renderApp('/sign-in');
      await signIn(user, 'APR-0001', 'not-the-password');
      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(currentPath()).toBe('/sign-in');
    });

    it.each(Object.entries(ROLE_HOME))('signs %s in to the role screen %s and sends the token', async (userId, home) => {
      const { api } = await signedInAs(userId);
      expect(api.callsTo('POST', '/auth/login')[0].body).toEqual({ userId, password: 'change-me-local-only' });
      const token = api.tokenFor(userId);
      await waitFor(() => {
        const after = api.calls.filter((c) => c.path !== '/auth/login');
        expect(after.length).toBeGreaterThan(0);
        expect(after.at(-1).headers.get('authorization')).toBe(`Bearer ${token}`);
      });
      expect(currentPath()).toBe(home);
    });

    it('returns to sign-in when the session expires (401 on a later call)', async () => {
      const { api, user } = await signedInAs('APR-0001');
      api.expireTokens();
      await user.click(await screen.findByRole('link', { name: /INV 7781/ }));
      await expectPath('/sign-in');
    });
  });

  describe('role screens', () => {
    it('sends a requester who opens the exceptions screen to the requester screen', async () => {
      await signedInAs('REQ-0001', '/exceptions');
      expect(screen.queryByRole('article', { name: /exception case/i })).not.toBeInTheDocument();
    });

    it('sends a payables clerk who opens the approval queue to the exceptions screen', async () => {
      await signedInAs('PAY-0001', '/queue');
      expect(await screen.findByRole('article', { name: /exception case 3/i })).toBeInTheDocument();
    });

    it('never shows approvers their own requests in the queue', async () => {
      await signedInAs('APR-0001');
      expect(await screen.findByRole('link', { name: /INV 7781/ })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /INV-2001/ })).toBeInTheDocument();
      expect(screen.queryByText(/INV-5001/)).not.toBeInTheDocument();
    });

    it('hides approve above the approver limit, and a forced call is still refused with 403', async () => {
      const { api, user } = await signedInAs('APR-0001');
      await user.click(await screen.findByRole('link', { name: /INV-2001/ }));
      await expectPath('/requests/8');
      expect(await screen.findByLabelText(/^status$/i)).toHaveTextContent('SUBMITTED');
      expect(screen.queryByRole('button', { name: /^approve$/i })).not.toBeInTheDocument();

      const { approveRequest } = await import('../../../src/api/client.js');
      const forced = await approveRequest(8, api.tokenFor('APR-0001')).then(
        () => null,
        (error) => error,
      );
      expect(forced, 'approveRequest must reject when the server refuses').toBeInstanceOf(Error);
      expect(forced.status).toBe(403);
      expect(api.state.requests.find((r) => r.id === 8).status).toBe('SUBMITTED');
    });

    it('shows approve within the limit', async () => {
      const { user } = await signedInAs('APR-0001');
      await user.click(await screen.findByRole('link', { name: /INV 7781/ }));
      expect(await screen.findByRole('button', { name: /^approve$/i })).toBeInTheDocument();
    });
  });

  describe('sign-out', () => {
    it('clears the session: back to sign-in, no stored token, no further authorised calls', async () => {
      const { api, user } = await signedInAs('APR-0001');
      const token = api.tokenFor('APR-0001');
      await screen.findByRole('link', { name: /INV 7781/ });
      await user.click(screen.getByRole('button', { name: /sign out/i }));
      await expectPath('/sign-in');
      expect(storedValues()).not.toContain(token);
      const before = api.calls.length;
      await new Promise((resolve) => setTimeout(resolve, 50));
      const later = api.calls.slice(before);
      expect(later.filter((c) => c.headers.get('authorization')?.includes(token))).toHaveLength(0);
      expect(within(document.body).queryByText(/APR-0001/)).not.toBeInTheDocument();
    });
  });
}
