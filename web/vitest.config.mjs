// Supplied (do not edit). `npm test` (tests/run.mjs) passes the selected files in WEB_TEST_FILES.
// Running `npx vitest` directly runs only your own tests.
import { defineConfig } from 'vitest/config';

const LEARNER_TESTS = ['src/**/*.test.{js,mjs}', 'tests/*.test.{js,mjs}', 'tests/!(project)/**/*.test.{js,mjs}'];
const selected = process.env.WEB_TEST_FILES ? JSON.parse(process.env.WEB_TEST_FILES) : null;

export default defineConfig({
  test: {
    environment: 'jsdom',
    include: selected && selected.length > 0 ? selected : LEARNER_TESTS,
    exclude: ['**/node_modules/**'],
    passWithNoTests: true,
    testTimeout: 20000,
  },
});
