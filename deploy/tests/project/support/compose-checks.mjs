// SUPPLIED (do not edit). Static checks of deploy/compose.yaml shared by Standard W14 and Fast W11.
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { describe, test } from 'node:test';
import {
  CREDENTIAL_PATTERNS, SECRET_NAME, deployDir, dependsOnOf, environmentOf, isPureIndirection, readYaml, refersTo,
} from './files.mjs';

const COMPOSE = 'deploy/compose.yaml';

function load() {
  const { text, doc } = readYaml(COMPOSE);
  assert.ok(doc && typeof doc === 'object' && doc.services && typeof doc.services === 'object',
    `${COMPOSE} must have a top-level "services" map`);
  return { text, services: doc.services };
}

function service(services, name) {
  const s = services[name];
  assert.ok(s && typeof s === 'object', `${COMPOSE} must describe a service named "${name}"`);
  return s;
}

function buildDir(svc, name) {
  const build = svc.build;
  assert.ok(build, `service "${name}" must be built from its Dockerfile (a "build" entry)`);
  const context = typeof build === 'string' ? build : build.context ?? '.';
  const dockerfile = typeof build === 'string' ? 'Dockerfile' : build.dockerfile ?? 'Dockerfile';
  return { dir: resolve(deployDir, context), dockerfile };
}

function healthTest(svc) {
  const t = svc.healthcheck?.test;
  if (!t || svc.healthcheck?.disable) return '';
  return Array.isArray(t) ? t.join(' ') : String(t);
}

function assertHealthcheck(svc, name) {
  const t = healthTest(svc);
  assert.ok(t && !/^NONE\b/.test(t), `service "${name}" needs a healthcheck with a test command`);
}

function assertWaitsHealthy(svc, name, dependency) {
  const deps = dependsOnOf(svc);
  assert.ok(dependency in deps, `service "${name}" must depend on "${dependency}"`);
  assert.equal(deps[dependency], 'service_healthy',
    `service "${name}" must wait for "${dependency}" to be healthy (depends_on condition service_healthy), not just started`);
}

function assertNoLocalhost(env, name) {
  for (const [k, v] of Object.entries(env)) {
    assert.ok(!/\b(localhost|127\.0\.0\.1)\b/.test(v),
      `service "${name}" ${k} points at localhost; inside Compose, services reach each other by service name`);
  }
}

export function registerComposeChecks() {
  describe('Compose description (deploy/compose.yaml, static)', () => {
    test('the file parses and describes the api, db and pipeline services', () => {
      const { services } = load();
      for (const name of ['api', 'db', 'pipeline']) service(services, name);
    });

    test('db: pinned PostgreSQL image, readiness healthcheck, password from the environment', () => {
      const { services } = load();
      const db = service(services, 'db');
      assert.ok(typeof db.image === 'string' && /postgres/i.test(db.image), 'db must use a PostgreSQL image');
      assert.match(db.image, /:[^:]+$/, 'pin the db image to a version tag');
      assert.ok(!/:latest$/.test(db.image), 'do not use the "latest" tag for db');
      assertHealthcheck(db, 'db');
      assert.match(healthTest(db), /pg_isready/, 'the db healthcheck should ask PostgreSQL whether it is ready (pg_isready)');
      const env = environmentOf(db);
      assert.ok('POSTGRES_PASSWORD' in env, 'db must take POSTGRES_PASSWORD from the environment');
      assert.ok(isPureIndirection(env.POSTGRES_PASSWORD),
        'db POSTGRES_PASSWORD must be ${VARIABLE} indirection with no literal value or default');
    });

    test('api: built from api/Dockerfile, healthy before dependants, waits for a healthy db', () => {
      const { services } = load();
      const api = service(services, 'api');
      const { dir, dockerfile } = buildDir(api, 'api');
      assert.ok(existsSync(join(dir, dockerfile)), `the api build context must contain ${dockerfile} (expected api/Dockerfile)`);
      assert.ok(dir.endsWith('/api') || dir.endsWith('\\api'), 'the api build context should be the api/ directory');
      assertHealthcheck(api, 'api');
      assertWaitsHealthy(api, 'api', 'db');
    });

    test('api: database by service name, provider URL and signing secret from the environment, dev profile off', () => {
      const { services } = load();
      const api = service(services, 'api');
      const env = environmentOf(api);
      const url = env.SPRING_DATASOURCE_URL ?? '';
      assert.match(url, /^jdbc:postgresql:\/\/db(:\d+)?\//, 'api SPRING_DATASOURCE_URL must reach PostgreSQL at the "db" service name');
      assert.ok('PROVIDER_BASE_URL' in env && refersTo(env.PROVIDER_BASE_URL, 'PROVIDER_BASE_URL'),
        'api PROVIDER_BASE_URL must come from the environment (${PROVIDER_BASE_URL...}), not a hard-coded provider');
      assert.ok('SECURITY_JWT_SECRET' in env && isPureIndirection(env.SECURITY_JWT_SECRET),
        'api SECURITY_JWT_SECRET must be ${VARIABLE} indirection with no literal value or default');
      const profiles = env.SPRING_PROFILES_ACTIVE ?? '';
      assert.ok(!/\bdev\b/.test(profiles), 'the Compose api must not run the dev profile (header identity stub)');
      assertNoLocalhost(Object.fromEntries(Object.entries(env).filter(([k]) => k !== 'PROVIDER_BASE_URL')), 'api');
    });

    test('pipeline: built from pipeline/Dockerfile, waits for healthy db and api, calls the api by service name', () => {
      const { services } = load();
      const pipeline = service(services, 'pipeline');
      const { dir, dockerfile } = buildDir(pipeline, 'pipeline');
      assert.ok(existsSync(join(dir, dockerfile)), `the pipeline build context must contain ${dockerfile} (expected pipeline/Dockerfile)`);
      assert.ok(dir.endsWith('/pipeline') || dir.endsWith('\\pipeline'), 'the pipeline build context should be the pipeline/ directory');
      assertHealthcheck(pipeline, 'pipeline');
      assertWaitsHealthy(pipeline, 'pipeline', 'db');
      assertWaitsHealthy(pipeline, 'pipeline', 'api');
      const env = environmentOf(pipeline);
      assert.match(env.SPRING_API_BASE_URL ?? '', /^http:\/\/api:\d+\/?$/, 'pipeline SPRING_API_BASE_URL must be http://api:<port>');
      assert.equal(env.PGHOST, 'db', 'pipeline PGHOST must be the "db" service name');
      assert.ok('SPRING_API_TOKEN' in env && isPureIndirection(env.SPRING_API_TOKEN),
        'pipeline SPRING_API_TOKEN must be ${VARIABLE} indirection with no literal value or default');
      assertNoLocalhost(env, 'pipeline');
    });

    test('no secret is written into the file', () => {
      const { text, services } = load();
      for (const [name, svc] of Object.entries(services)) {
        for (const [k, v] of Object.entries(environmentOf(svc))) {
          if (SECRET_NAME.test(k)) {
            assert.ok(isPureIndirection(v), `service "${name}" ${k} must be \${VARIABLE} indirection, never a literal or a default value`);
          }
        }
        for (const [k, v] of Object.entries(svc.build?.args ?? {})) {
          assert.ok(!SECRET_NAME.test(k), `service "${name}" passes ${k} as a build argument; secrets never go into images`);
          void v;
        }
      }
      for (const pattern of CREDENTIAL_PATTERNS) {
        assert.ok(!pattern.test(text), `${COMPOSE} contains text that looks like a credential (${pattern})`);
      }
    });
  });
}
