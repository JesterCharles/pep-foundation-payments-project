// SUPPLIED (do not edit). Static checks of .github/workflows/ci.yml (Fast W11 only).
// Nothing here talks to GitHub: the workflow is read as YAML.
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { CREDENTIAL_PATTERNS, SECRET_NAME, readYaml } from './files.mjs';

const CI = '.github/workflows/ci.yml';

/** Each area's test command, and how a step can show it runs in that area. */
const AREAS = [
  { area: 'api', command: /\bmvn\b[^\n]*\btest\b/, inline: /(-f|--file)\s+\.?\/?api\/pom\.xml|cd\s+\.?\/?api\b/ },
  { area: 'pipeline', command: /\bpython3?\s+-m\s+pytest\b|\bpytest\b/, inline: /cd\s+\.?\/?pipeline\b/ },
  { area: 'web', command: /\bnpm\s+(test|run\s+test)\b/, inline: /cd\s+\.?\/?web\b|--prefix\s+\.?\/?web\b/ },
  { area: 'ui', command: /\bnpm\s+(test|run\s+test)\b/, inline: /cd\s+\.?\/?ui\b|--prefix\s+\.?\/?ui\b/ },
  { area: 'deploy', command: /\bnpm\s+(test|run\s+test)\b/, inline: /cd\s+\.?\/?deploy\b|--prefix\s+\.?\/?deploy\b/ },
];

function load() {
  const { text, doc } = readYaml(CI);
  assert.ok(doc.jobs && typeof doc.jobs === 'object' && Object.keys(doc.jobs).length > 0, `${CI} must define at least one job`);
  return { text, doc };
}

function triggersOf(doc) {
  const on = doc.on ?? doc.true; // YAML 1.1 readers turn a bare `on` into true
  if (!on) return [];
  if (typeof on === 'string') return [on];
  if (Array.isArray(on)) return on.map(String);
  return Object.keys(on);
}

function stepsOf(doc) {
  const out = [];
  for (const [jobName, job] of Object.entries(doc.jobs)) {
    const jobDir = job?.defaults?.run?.['working-directory'] ?? doc.defaults?.run?.['working-directory'] ?? '';
    for (const step of job?.steps ?? []) {
      out.push({ jobName, job, step, dir: String(step['working-directory'] ?? jobDir ?? '') });
    }
  }
  return out;
}

function normaliseDir(dir) {
  return dir.replace(/^\.\//, '').replace(/\/$/, '');
}

export function registerCiChecks() {
  describe('CI workflow (.github/workflows/ci.yml, static)', () => {
    test('runs automatically on pushes and pull requests, never on pull_request_target', () => {
      const { doc } = load();
      const triggers = triggersOf(doc);
      assert.ok(triggers.includes('push') || triggers.includes('pull_request'),
        `${CI} must run on push and/or pull_request (found: ${triggers.join(', ') || 'no triggers'})`);
      assert.ok(!triggers.includes('pull_request_target'), 'do not use pull_request_target: it exposes secrets to fork code');
    });

    test('runs the test command of every area: api, pipeline, web, ui and deploy', () => {
      const { doc } = load();
      const steps = stepsOf(doc).filter((s) => typeof s.step.run === 'string');
      for (const a of AREAS) {
        const ok = steps.some((s) => a.command.test(s.step.run)
          && (normaliseDir(s.dir) === a.area || a.inline.test(s.step.run)));
        assert.ok(ok, `no step runs the ${a.area} tests (run the ${a.area}/ test command with working-directory: ${a.area})`);
      }
    });

    test('every action is pinned to a version tag or commit SHA', () => {
      const { doc } = load();
      const uses = stepsOf(doc).map((s) => s.step.uses).filter(Boolean).map(String);
      assert.ok(uses.length > 0, 'use the checkout and setup actions (actions/checkout, setup-java, setup-python, setup-node)');
      for (const u of uses) {
        if (u.startsWith('./') || u.startsWith('docker://')) continue;
        assert.match(u, /@(v\d+(\.\d+){0,2}|[0-9a-f]{40})$/, `pin ${u} to a version tag (e.g. @v4) or a full commit SHA`);
      }
    });

    test('secrets come only from ${{ secrets.* }} and are never printed', () => {
      const { text, doc } = load();
      const envBlocks = [doc.env, ...Object.values(doc.jobs).map((j) => j?.env), ...stepsOf(doc).map((s) => s.step.env)];
      for (const env of envBlocks.filter(Boolean)) {
        for (const [k, v] of Object.entries(env)) {
          if (SECRET_NAME.test(k)) {
            assert.match(String(v).trim(), /^\$\{\{\s*secrets\.[A-Za-z0-9_]+\s*\}\}$/,
              `${k} must be set from \${{ secrets.NAME }}, never a literal value`);
          }
        }
      }
      for (const { step } of stepsOf(doc)) {
        if (typeof step.run !== 'string') continue;
        for (const line of step.run.split('\n')) {
          if (/\b(echo|printf|cat)\b/.test(line)) {
            assert.ok(!/secrets\.|\$\{?[A-Z_]*(SECRET|TOKEN|PASSWORD)[A-Z_]*\}?/.test(line),
              `a step prints a secret: "${line.trim()}"`);
          }
          assert.ok(!/\bset\s+-x\b/.test(line), 'avoid set -x: it echoes commands with their secret values');
        }
      }
      for (const pattern of CREDENTIAL_PATTERNS) {
        assert.ok(!pattern.test(text), `${CI} contains text that looks like a credential (${pattern})`);
      }
    });
  });
}
