// SUPPLIED (do not edit). Runs the deploy/ checks with Node's built-in test runner.
//
//   npm test                    your own tests only (deploy/tests/**/*.test.mjs outside tests/project/)
//   npm test -- standard-w14    plus every supplied Standard check up to week 14
//   npm test -- fast-w11        plus every supplied Fast check up to week 11
//
// These are static structural checks: they read deploy/compose.yaml, api/Dockerfile,
// pipeline/Dockerfile and (Fast) .github/workflows/ci.yml as text/YAML. No container engine runs.
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const testsDir = dirname(fileURLToPath(import.meta.url));
const projectDir = join(testsDir, 'project');

// Which supplied check directories belong to which track, and the week each arrives.
const SUPPLIED = {
  standard: [{ dir: 'w14', week: 14 }],
  fast: [{ dir: 'w11', week: 11 }],
};
// Weeks with a Project 2 milestone on each track (a selection outside these is an error).
const WEEKS = { standard: [7, 15], fast: [7, 11] };

function validSelectors() {
  const out = [];
  for (const [track, [first, last]] of Object.entries(WEEKS)) {
    for (let w = first; w <= last; w++) out.push(`${track}-w${String(w).padStart(2, '0')}`);
  }
  return out;
}

function testFiles(dir, skipProject) {
  if (!existsSync(dir)) return [];
  const found = [];
  for (const name of readdirSync(dir).sort()) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      if (name === 'node_modules' || (skipProject && path === projectDir)) continue;
      found.push(...testFiles(path, skipProject));
    } else if (/\.test\.(mjs|js)$/.test(name)) {
      found.push(path);
    }
  }
  return found;
}

const selector = (process.argv[2] ?? process.env.MILESTONE ?? '').trim();
const files = testFiles(testsDir, true);
if (selector) {
  if (!validSelectors().includes(selector)) {
    console.error(`Unknown milestone "${selector}". Use one of: ${validSelectors().join(', ')}`);
    process.exit(2);
  }
  const [track, weekText] = selector.split('-w');
  const week = Number(weekText);
  const dirs = SUPPLIED[track].filter((d) => d.week <= week).map((d) => d.dir);
  for (const d of dirs) files.push(...testFiles(join(projectDir, d), false));
  console.log(`deploy: ${selector} -> supplied checks [${dirs.join(', ') || 'none for this week'}] + your tests`);
} else {
  console.log('deploy: no milestone selected -> your own tests only (npm test -- <track>-w<NN> adds supplied checks)');
}

if (files.length === 0) {
  console.log('deploy: no tests to run.');
  process.exit(0);
}
console.log(files.map((f) => '  ' + relative(testsDir, f).split(sep).join('/')).join('\n'));
const result = spawnSync(process.execPath, ['--test', '--test-reporter=spec', ...files], { stdio: 'inherit' });
process.exit(result.status ?? 1);
