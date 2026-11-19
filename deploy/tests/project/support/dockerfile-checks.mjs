// SUPPLIED (do not edit). Static checks of api/Dockerfile and pipeline/Dockerfile shared by Standard W14 and Fast W11.
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { CREDENTIAL_PATTERNS, SECRET_NAME, parseDockerfile, readText } from './files.mjs';

const TARGETS = [
  { file: 'api/Dockerfile', port: '8080', build: /\b(mvn|mvnw)\b/, buildWhat: 'a Maven build (mvn ... package)', fatImage: /maven|jdk/i },
  { file: 'pipeline/Dockerfile', port: '8000', build: /\bpip\b|\buv\b/, buildWhat: 'a pip install of the pipeline', fatImage: null },
];

function stagesOf(file) {
  const stages = parseDockerfile(readText(file));
  assert.ok(stages.length > 0 && stages[0].image, `${file} has no FROM instruction yet`);
  return stages;
}

function finalStage(stages) {
  return stages[stages.length - 1];
}

export function registerDockerfileChecks() {
  for (const t of TARGETS) {
    describe(`${t.file} (static)`, () => {
      test('multi-stage: a named build stage, then a runtime stage that copies only the build output', () => {
        const stages = stagesOf(t.file);
        assert.ok(stages.length >= 2, `${t.file} needs at least two stages (build, then runtime)`);
        const named = stages.slice(0, -1).filter((s) => s.name);
        assert.ok(named.length > 0, `name the build stage (FROM <image> AS <name>)`);
        const last = finalStage(stages);
        const copiesFrom = last.instructions.filter((i) => i.op === 'COPY' && /--from=/.test(i.args))
          .map((i) => /--from=(\S+)/.exec(i.args)[1].toLowerCase());
        assert.ok(copiesFrom.some((f) => named.some((s) => s.name === f) || /^\d+$/.test(f)),
          'the runtime stage must COPY --from=<build stage> the built artefact');
        const buildRuns = stages.slice(0, -1).flatMap((s) => s.instructions).filter((i) => i.op === 'RUN');
        assert.ok(buildRuns.some((i) => t.build.test(i.args)), `an earlier stage should run ${t.buildWhat}`);
      });

      test('base images are pinned and the runtime image is slim', () => {
        const stages = stagesOf(t.file);
        for (const s of stages) {
          if (stages.some((o) => o.name && o.name === s.image.toLowerCase())) continue;
          assert.match(s.image, /[:@][^/]+$/, `pin ${s.image} to a version tag or digest`);
          assert.ok(!/:latest$/.test(s.image), `do not use the "latest" tag (${s.image})`);
        }
        const last = finalStage(stages);
        if (t.fatImage) {
          assert.notEqual(last.image, stages[0].image, 'the runtime stage should use a smaller image than the build stage');
          assert.ok(!/maven/i.test(last.image), 'the runtime stage must not ship the Maven build image');
        }
        if (t.file.startsWith('api')) assert.ok(/jre|distroless|slim|alpine/i.test(last.image), 'use a JRE-only or slim runtime image for the api');
        else assert.ok(/slim|alpine|distroless/i.test(last.image), 'use a slim Python runtime image for the pipeline');
      });

      test('runtime stage: non-root user, exposed port, start command', () => {
        const last = finalStage(stagesOf(t.file));
        const users = last.instructions.filter((i) => i.op === 'USER');
        assert.ok(users.length > 0, 'the runtime stage must switch to a non-root USER');
        const user = users[users.length - 1].args.trim().split(':')[0];
        assert.ok(user && user !== 'root' && user !== '0', `the runtime USER must not be root (found "${user}")`);
        const exposed = last.instructions.filter((i) => i.op === 'EXPOSE').flatMap((i) => i.args.split(/\s+/));
        assert.ok(exposed.some((p) => p.split('/')[0] === t.port), `EXPOSE ${t.port} in the runtime stage`);
        assert.ok(last.instructions.some((i) => i.op === 'CMD' || i.op === 'ENTRYPOINT'), 'give the runtime stage a CMD or ENTRYPOINT');
      });

      test('no secret is baked into the image', () => {
        const text = readText(t.file);
        const stages = stagesOf(t.file);
        for (const i of stages.flatMap((s) => s.instructions)) {
          if (i.op === 'ARG' || i.op === 'ENV') {
            const names = i.args.split(/\s+/).map((p) => p.split('=')[0]);
            for (const n of names) {
              assert.ok(!SECRET_NAME.test(n), `${t.file}:${i.line} ${i.op} ${n}: secrets come from the runtime environment, never the image`);
            }
          }
          if (i.op === 'COPY' || i.op === 'ADD') {
            assert.ok(!/(^|\s|\/)\.env(\s|$)/.test(i.args), `${t.file}:${i.line} copies a .env file into the image`);
          }
        }
        for (const pattern of CREDENTIAL_PATTERNS) {
          assert.ok(!pattern.test(text), `${t.file} contains text that looks like a credential (${pattern})`);
        }
      });
    });
  }
}
