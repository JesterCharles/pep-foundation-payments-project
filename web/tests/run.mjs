#!/usr/bin/env node
// Supplied (do not edit). Runs your own tests, plus the supplied checks of one milestone.
//
//   npm test                    your own tests only (exits 0 when there are none)
//   npm test -- standard-w12    plus the supplied Standard checks of weeks 11 and 12
//   npm test -- fast-w09        plus the supplied Fast checks of week 9
//
// Your own tests: any *.test.js / *.test.mjs file under src/ or tests/ outside tests/project/.
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const WEB = dirname(dirname(fileURLToPath(import.meta.url)));

// Which supplied check directories belong to which track, in week order.
const TRACKS = {
  standard: ['w11', 'w12'],
  fast: ['w09', 'w10'],
};

// Every week of each track is a valid selection (so one selector works in every area); a week before this
// area's first check directory runs your own tests only.
const LAST_WEEK = { standard: 15, fast: 11 };

function validSelectors() {
  return Object.entries(LAST_WEEK).flatMap(([track, last]) =>
    Array.from({ length: last - 6 }, (_, i) => `${track}-w${String(i + 7).padStart(2, '0')}`));
}

function walk(dir, out = []) {
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    if (name === 'node_modules' || name.startsWith('.')) continue;
    const path = join(dir, name);
    if (statSync(path).isDirectory()) walk(path, out);
    else if (/\.test\.m?js$/.test(name)) out.push(path);
  }
  return out;
}

const selector = (process.argv[2] || process.env.MILESTONE || '').trim().toLowerCase();
let projectDirs = [];
if (selector) {
  if (!validSelectors().includes(selector)) {
    console.error(`Unknown milestone "${selector}". Valid values: ${validSelectors().join(', ')}`);
    process.exit(2);
  }
  const [track, week] = selector.split('-');
  projectDirs = TRACKS[track].filter((dir) => Number(dir.slice(1)) <= Number(week.slice(1)));
}

const projectRoot = join(WEB, 'tests', 'project');
const learnerFiles = [...walk(join(WEB, 'src')), ...walk(join(WEB, 'tests'))].filter(
  (file) => !file.startsWith(projectRoot + '/'),
);
const projectFiles = projectDirs.flatMap((week) => walk(join(projectRoot, week)));
const files = [...learnerFiles, ...projectFiles].map((file) => relative(WEB, file));

console.log(
  selector
    ? `Milestone ${selector}: supplied checks in tests/project/{${projectDirs.join(',')}} plus ${learnerFiles.length} learner test file(s).`
    : `No milestone selected: running ${learnerFiles.length} learner test file(s). Add e.g. "-- standard-w11" for supplied checks.`,
);
if (files.length === 0) {
  console.log('No test files to run.');
  process.exit(0);
}

const vitest = join(WEB, 'node_modules', 'vitest', 'vitest.mjs');
if (!existsSync(vitest)) {
  console.error('Vitest is not installed. Run `npm ci` in web/ first.');
  process.exit(1);
}
const result = spawnSync(process.execPath, [vitest, 'run'], {
  cwd: WEB,
  stdio: 'inherit',
  env: { ...process.env, WEB_TEST_FILES: JSON.stringify(files), WEB_MILESTONE: selector },
});
process.exit(result.status ?? 1);
