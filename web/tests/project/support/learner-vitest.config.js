import { appendFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { defineConfig } from 'vitest/config';

const helperModules = new Set([resolve('src/money.js'), resolve('src/queue.js')]);

export default defineConfig({
  plugins: [{
    name: 'learner-helper-observer',
    transform(_code, id) {
      if (process.env.WEB_HELPER_LOG && helperModules.has(id)) appendFileSync(process.env.WEB_HELPER_LOG, `${id}\n`);
    },
  }],
  test: {
    environment: 'jsdom',
    include: JSON.parse(process.env.WEB_LEARNER_TEST_FILES || '[]'),
    passWithNoTests: true,
    testTimeout: 20000,
  },
});
