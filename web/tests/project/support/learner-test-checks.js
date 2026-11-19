// Supplied check support (do not edit): your own Vitest test of a money or queue helper exists.
// It runs with every `npm test`; this check only confirms it is there and exercises web/src.
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync, readdirSync, rmSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';
import { WEB_ROOT } from './paths.js';

function learnerTestFiles(dir = WEB_ROOT, out = []) {
  for (const name of readdirSync(dir)) {
    if (name === 'node_modules' || name.startsWith('.')) continue;
    const path = join(dir, name);
    if (relative(WEB_ROOT, path) === join('tests', 'project')) continue;
    if (statSync(path).isDirectory()) learnerTestFiles(path, out);
    else if (/\.test\.m?js$/.test(name)) out.push(path);
  }
  return out;
}

export function learnerTestChecks(week) {
  describe(`${week}: your own helper test`, () => {
    it('a learner test outside tests/project exercises src/money.js or src/queue.js', () => {
      const found = learnerTestFiles().map((file) => relative(WEB_ROOT, file));
      const helperLog = join(WEB_ROOT, `.learner-helper-${process.pid}-${Date.now()}.log`);
      const run = spawnSync(process.execPath, ['node_modules/vitest/vitest.mjs', 'run', '--config', 'tests/project/support/learner-vitest.config.js', '--reporter=json'], {
        cwd: WEB_ROOT,
        encoding: 'utf8',
        env: { ...process.env, WEB_LEARNER_TEST_FILES: JSON.stringify(found), WEB_HELPER_LOG: helperLog },
      });
      let report;
      try {
        report = JSON.parse(run.stdout);
      } catch {
        throw new Error(`learner test run did not return a Vitest report:\n${run.stderr || run.stdout}`);
      }
      const helpers = existsSync(helperLog) ? readFileSync(helperLog, 'utf8').trim().split('\n').filter(Boolean) : [];
      rmSync(helperLog, { force: true });
      expect(
        report.numTotalTests,
        'add e.g. web/tests/money.test.js that imports ../src/money.js and uses expect(...)',
      ).toBeGreaterThan(0);
      expect(run.status, 'your own helper tests must pass').toBe(0);
      expect(helpers, 'your own test must load src/money.js or src/queue.js').not.toEqual([]);
    });
  });
}
