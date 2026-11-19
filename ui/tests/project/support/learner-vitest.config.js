import { appendFileSync } from 'node:fs';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react(), {
    // Runs before Vite's own resolver (which would otherwise answer first) and records who imports
    // @testing-library/react. The supplied tests/setup.js imports it too, so the check counts learner importers only.
    name: 'learner-testing-library-observer',
    enforce: 'pre',
    resolveId(source, importer) {
      if (process.env.UI_LIBRARY_LOG && source === '@testing-library/react' && importer) {
        appendFileSync(process.env.UI_LIBRARY_LOG, `${JSON.stringify({ source, importer })}\n`);
      }
    },
  }],
  test: {
    environment: 'jsdom',
    setupFiles: ['tests/setup.js'],
    include: JSON.parse(process.env.UI_LEARNER_TEST_FILES || '[]'),
    passWithNoTests: true,
    restoreMocks: true,
    testTimeout: 10000,
  },
});
