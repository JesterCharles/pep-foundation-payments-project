// Supplied (do not edit). tests/run.mjs chooses which supplied check folders run by setting
// UI_MILESTONE_DIRS; your own tests (any *.test.js / *.test.jsx outside tests/project/) always run.
import { readdirSync } from 'node:fs';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

const selected = JSON.parse(process.env.UI_MILESTONE_DIRS || '[]');
const allWeeks = readdirSync(new URL('./tests/project/', import.meta.url), { withFileTypes: true })
  .filter((entry) => entry.isDirectory() && /^w\d\d$/.test(entry.name))
  .map((entry) => entry.name);

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['tests/setup.js'],
    include: [
      'src/**/*.test.{js,jsx}',
      'tests/**/*.test.{js,jsx}',
      ...selected.map((week) => `tests/project/${week}/**/*.test.{js,jsx}`),
    ],
    exclude: [
      'node_modules/**',
      'dist/**',
      'tests/project/support/**',
      ...allWeeks.filter((week) => !selected.includes(week)).map((week) => `tests/project/${week}/**`),
    ],
    passWithNoTests: true,
    restoreMocks: true,
    testTimeout: 10000,
  },
});
