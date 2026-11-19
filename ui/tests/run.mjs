// Supplied (do not edit). Runs your own tests, plus the supplied checks of one milestone selection.
//   npm test                    your tests only (exits 0 when you have none yet)
//   npm test -- standard-w13    your tests + supplied Standard checks for weeks 12 and 13
//   npm test -- fast-w11        your tests + supplied Fast checks for weeks 10 and 11
// Each track has its own folders; a selection never runs the other track's checks.
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const TRACKS = {
  standard: ['w12', 'w13', 'w14'],
  fast: ['w10', 'w11'],
};

const uiDir = fileURLToPath(new URL('..', import.meta.url));
const args = process.argv.slice(2);
const selector = args.find((arg) => !arg.startsWith('-'));
const passThrough = args.filter((arg) => arg !== selector);

let dirs = [];
if (selector) {
  const match = /^(standard|fast)-w(\d\d)$/.exec(selector);
  // Every week of each track is valid, so one selector works in every area; a week before this area's first
  // check folder runs your own tests only.
  const lastWeek = { standard: 15, fast: 11 };
  const valid = Object.entries(lastWeek).flatMap(([track, last]) =>
    Array.from({ length: last - 6 }, (_, i) => `${track}-w${String(i + 7).padStart(2, '0')}`));
  if (!match || !valid.includes(selector)) {
    console.error(`Unknown milestone "${selector}". Valid values: ${valid.join(', ')}`);
    process.exit(2);
  }
  const [, track, week] = match;
  dirs = TRACKS[track].filter((dir) => Number(dir.slice(1)) <= Number(week));
  console.log(dirs.length
    ? `Running your tests plus supplied ${track} checks: ${dirs.map((d) => `tests/project/${d}`).join(', ')}`
    : `No supplied ui checks for ${selector} yet: running your own tests only.`);
} else {
  console.log('Running your own tests only. Add a milestone (for example: npm test -- standard-w13) to run supplied checks.');
}

const vitest = fileURLToPath(new URL('../node_modules/vitest/vitest.mjs', import.meta.url));
if (!existsSync(vitest)) {
  console.error('vitest is not installed: run "npm ci" in ui/ first.');
  process.exit(2);
}
const result = spawnSync(process.execPath, [vitest, 'run', ...passThrough], {
  cwd: uiDir,
  stdio: 'inherit',
  env: { ...process.env, UI_MILESTONE_DIRS: JSON.stringify(dirs) },
});
process.exit(result.status ?? 1);
