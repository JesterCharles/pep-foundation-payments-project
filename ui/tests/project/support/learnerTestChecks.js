// Supplied (do not edit). Executes your own Testing Library tests outside tests/project/.
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync, rmSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import React from 'react';
import { act, render, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { learnerTestFiles } from './learnerTests.js';
import { installStubApi } from './stubApi.js';

const uiDir = process.cwd();

function runLearnerTests(files, libraryLog) {
  const vitest = join(uiDir, 'node_modules', 'vitest', 'vitest.mjs');
  const result = spawnSync(process.execPath, [vitest, 'run', '--config', 'tests/project/support/learner-vitest.config.js', '--reporter=json'], {
    cwd: uiDir,
    encoding: 'utf8',
    env: { ...process.env, UI_LEARNER_TEST_FILES: JSON.stringify(files), UI_LIBRARY_LOG: libraryLog },
  });
  try {
    return { report: JSON.parse(result.stdout), status: result.status };
  } catch {
    throw new Error(`learner test run did not return a Vitest report:\n${result.stderr || result.stdout}`);
  }
}

export function learnerUiTestSuite() {
  describe('your own Testing Library UI tests', () => {
    it('a learner Testing Library test outside tests/project exists and passes', () => {
      const uiTests = learnerTestFiles();
      const libraryLog = join(uiDir, `.learner-library-${process.pid}-${Date.now()}.log`);
      const { report, status } = runLearnerTests(uiTests, libraryLog);
      const supplied = (file) => {
        const rel = relative(uiDir, file.split('?')[0]).split(sep).join('/');
        return rel === 'tests/setup.js' || rel.startsWith('tests/project/') || rel.includes('node_modules/');
      };
      const imports = (existsSync(libraryLog) ? readFileSync(libraryLog, 'utf8').trim().split('\n').filter(Boolean) : [])
        .map((line) => JSON.parse(line))
        .filter(({ importer }) => !supplied(importer));
      rmSync(libraryLog, { force: true });
      expect(
        report.numTotalTests,
        'add e.g. src/pages/RequestDetail.test.jsx using render() and getByRole/getByLabelText',
      ).toBeGreaterThan(0);
      expect(status, 'your own UI tests must pass').toBe(0);
      expect(imports, 'your own UI test must use @testing-library/react').not.toEqual([]);
    });
  });
}

export function authContextSuite() {
  describe('signed-in user shared through React context', () => {
    it('src/auth/AuthContext.jsx shares the signed-in user through useAuth', async () => {
      const modules = import.meta.glob('../../../src/auth/AuthContext.jsx');
      const load = Object.values(modules)[0];
      expect(load, 'create src/auth/AuthContext.jsx').toBeTypeOf('function');
      const mod = await load();
      let auth;
      function Consumer() {
        auth = mod.useAuth();
        return React.createElement('span', null, auth?.user?.userId ?? 'signed out');
      }

      async function signInAs(userId, expectedUser) {
        const api = installStubApi({ auth: 'token' });
        const view = render(React.createElement(mod.AuthProvider, null, React.createElement(Consumer)));
        expect(auth?.user).toBeNull();
        await act(async () => auth.signIn(userId, 'change-me-local-only'));
        await waitFor(() => expect(auth.user).toEqual(expectedUser));
        expect(view.getByText(userId)).toBeInTheDocument();
        expect(api.callsTo('POST', '/auth/login').map((call) => call.body)).toEqual([{
          userId,
          password: 'change-me-local-only',
        }]);
        view.unmount();
      }

      await signInAs('APR-0001', {
        userId: 'APR-0001',
        roles: ['APPROVER'],
        approvalLimit: '5000.00',
      });
      await signInAs('REQ-0001', {
        userId: 'REQ-0001',
        roles: ['REQUESTER'],
        approvalLimit: null,
      });
      expect(auth.user.roles).toContain('REQUESTER');
      expect(auth.user.roles).not.toContain('APPROVER');
    });
  });
}
