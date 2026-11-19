// SUPPLIED (do not edit). File access and small parsers shared by the deploy/ checks.
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

/** The repository root (deploy/..). */
export const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', '..', '..');
export const deployDir = join(repoRoot, 'deploy');

export function pathOf(rel) {
  return join(repoRoot, rel);
}

export function readText(rel) {
  const path = pathOf(rel);
  if (!existsSync(path)) throw new Error(`${rel} does not exist`);
  return readFileSync(path, 'utf8');
}

/** Parses a YAML file; throws a readable error when it is missing or invalid. */
export function readYaml(rel) {
  const text = readText(rel);
  try {
    return { text, doc: parse(text) ?? {} };
  } catch (e) {
    throw new Error(`${rel} is not valid YAML: ${e.message.split('\n')[0]}`);
  }
}

/**
 * Splits a Dockerfile into stages. Each stage: { image, name, index, instructions: [{ op, args, line }] }.
 * Handles comments, blank lines and backslash line continuations. Parser directives are ignored.
 */
export function parseDockerfile(text) {
  const logical = [];
  let buffer = '';
  let startLine = 0;
  text.split(/\r?\n/).forEach((raw, i) => {
    const line = raw.trim();
    if (buffer === '' && (line === '' || line.startsWith('#'))) return;
    if (buffer !== '' && line.startsWith('#')) return;
    if (buffer === '') startLine = i + 1;
    if (line.endsWith('\\')) {
      buffer += line.slice(0, -1) + ' ';
    } else {
      buffer += line;
      logical.push({ text: buffer.trim(), line: startLine });
      buffer = '';
    }
  });
  if (buffer.trim()) logical.push({ text: buffer.trim(), line: startLine });

  const stages = [];
  for (const { text: l, line } of logical) {
    const match = /^(\S+)\s*(.*)$/.exec(l);
    const op = match[1].toUpperCase();
    const args = match[2];
    if (op === 'FROM') {
      const parts = args.split(/\s+/).filter((p) => !p.startsWith('--'));
      const asIndex = parts.findIndex((p) => p.toUpperCase() === 'AS');
      stages.push({
        image: parts[0] ?? '',
        name: asIndex >= 0 ? (parts[asIndex + 1] ?? '').toLowerCase() : null,
        index: stages.length,
        line,
        instructions: [],
      });
    } else if (stages.length > 0) {
      stages[stages.length - 1].instructions.push({ op, args, line });
    } else {
      stages.push({ image: '', name: null, index: 0, line, instructions: [{ op, args, line }], beforeFrom: true });
    }
  }
  return stages.filter((s) => !s.beforeFrom || s.instructions.length > 0);
}

/** Normalises a Compose `environment` (map or list form) to a plain object of strings. */
export function environmentOf(service) {
  const env = service?.environment;
  if (!env) return {};
  if (Array.isArray(env)) {
    const out = {};
    for (const item of env) {
      const text = String(item);
      const eq = text.indexOf('=');
      if (eq < 0) out[text] = '';
      else out[text.slice(0, eq)] = text.slice(eq + 1);
    }
    return out;
  }
  const out = {};
  for (const [k, v] of Object.entries(env)) out[k] = v === null || v === undefined ? '' : String(v);
  return out;
}

/** Normalises `depends_on` (list or map form) to { name: condition|null }. */
export function dependsOnOf(service) {
  const deps = service?.depends_on;
  if (!deps) return {};
  if (Array.isArray(deps)) return Object.fromEntries(deps.map((d) => [String(d), null]));
  return Object.fromEntries(Object.entries(deps).map(([k, v]) => [k, v?.condition ?? null]));
}

/** Names that hold credentials or signing material. */
export const SECRET_NAME = /(PASSWORD|PASSWD|SECRET|TOKEN|API_?KEY|PRIVATE_?KEY|CREDENTIAL)/i;

/** True when a value only refers to an environment variable, with no literal fallback: ${NAME} or ${NAME:?message}. */
export function isPureIndirection(value) {
  return /^\$\{[A-Za-z_][A-Za-z0-9_]*(:?\?[^}]*)?\}$/.test(String(value).trim());
}

/** True when a value refers to the named environment variable (a non-secret default is allowed). */
export function refersTo(value, name) {
  return new RegExp(`\\$\\{${name}(?:[:?\\-][^}]*)?\\}`).test(String(value));
}

/** Text patterns that look like real credentials and never belong in a tracked file. */
export const CREDENTIAL_PATTERNS = [
  /-----BEGIN [A-Z ]*PRIVATE KEY-----/,
  /\bAKIA[0-9A-Z]{16}\b/,
  /\bgh[pousr]_[A-Za-z0-9]{20,}\b/,
  /\bxox[abpr]-[A-Za-z0-9-]{10,}\b/,
  /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{5,}/,
  /change-me-local-only/,
];
