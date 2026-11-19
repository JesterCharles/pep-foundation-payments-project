// Supplied (do not edit). Finds the learner's own test files: every *.test.js / *.test.jsx under src/ or
// tests/ that is not inside tests/project/. They run in the same `npm test` run as the supplied checks.
import { readdirSync } from 'node:fs';
import { join, relative } from 'node:path';

const uiDir = process.cwd();

function walk(dir, found = []) {
  let entries = [];
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return found;
  }
  for (const entry of entries) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'node_modules') continue;
      if (relative(uiDir, full).split(/[\\/]/).slice(0, 2).join('/') === 'tests/project') continue;
      walk(full, found);
    } else if (/\.test\.jsx?$/.test(entry.name)) {
      found.push(full);
    }
  }
  return found;
}

export function learnerTestFiles() {
  return [...walk(join(uiDir, 'src')), ...walk(join(uiDir, 'tests'))].map((file) => relative(uiDir, file));
}
