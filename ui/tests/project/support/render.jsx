// Supplied (do not edit). Renders the learner's <App /> inside a MemoryRouter at a starting path, with a
// hidden probe that exposes the current path, so checks can follow redirects without a browser.
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router';
import App from '../../../src/App.jsx';
import { PASSWORD } from './fixtures.js';

function PathProbe() {
  const location = useLocation();
  return (
    <div hidden data-testid="current-path">
      {location.pathname}
    </div>
  );
}

export function renderApp(path = '/') {
  const user = userEvent.setup();
  const view = render(
    <MemoryRouter initialEntries={[path]}>
      <App />
      <PathProbe />
    </MemoryRouter>,
  );
  return { user, ...view };
}

export const currentPath = () => screen.getByTestId('current-path').textContent;

export async function expectPath(path) {
  await waitFor(() => {
    if (currentPath() !== path) throw new Error(`expected the UI at ${path} but it is at ${currentPath()}`);
  });
}

/** Signs in through the learner's sign-in screen (the app must already be showing it). */
export async function signIn(user, userId, password = PASSWORD) {
  await user.type(await screen.findByLabelText(/user id/i), userId);
  await user.type(screen.getByLabelText(/password/i), password);
  await user.click(screen.getByRole('button', { name: /sign in/i }));
}

/** Text of the element labelled "Status" on a request detail screen. */
export async function statusText() {
  return (await screen.findByLabelText(/^status$/i)).textContent;
}

/** No success message is shown (success messages use role="status"). */
export function expectNoSuccessMessage(pattern = /approved|rejected|corrected|resolved|submitted|success/i) {
  for (const element of screen.queryAllByRole('status')) {
    if (pattern.test(element.textContent)) {
      throw new Error(`a success message is shown after a refused action: "${element.textContent}"`);
    }
  }
}
