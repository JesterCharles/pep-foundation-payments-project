// Supplied (do not edit). Test setup for every ui/ test: jest-dom matchers, a clean DOM and storage
// between tests, and no real network: tests must stub fetch (see tests/project/support/stubApi.js).
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach } from 'vitest';

const realFetch = globalThis.fetch;

beforeEach(() => {
  globalThis.fetch = async (input) => {
    throw new Error(`Network access is disabled in tests (attempted ${String(input?.url ?? input)}); stub fetch instead.`);
  };
});

afterEach(() => {
  cleanup();
  globalThis.fetch = realFetch;
  try {
    window.localStorage.clear();
    window.sessionStorage.clear();
  } catch {
    // storage may be unavailable; nothing to clear
  }
});
