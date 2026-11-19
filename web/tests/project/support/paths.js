// Supplied check support (do not edit).
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

export const WEB_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');

/** web/index.html parsed WITHOUT running its scripts. */
export function loadIndexDocument() {
  const file = join(WEB_ROOT, 'index.html');
  if (!existsSync(file)) throw new Error('web/index.html is missing');
  return new JSDOM(readFileSync(file, 'utf8')).window.document;
}

/** Text of the element's accessible label (label[for], wrapping label, aria-labelledby or aria-label). */
export function labelText(doc, element) {
  const parts = [];
  if (element.getAttribute('aria-label')) parts.push(element.getAttribute('aria-label'));
  const labelledBy = element.getAttribute('aria-labelledby');
  if (labelledBy) {
    for (const id of labelledBy.split(/\s+/)) {
      const ref = doc.getElementById(id);
      if (ref) parts.push(ref.textContent);
    }
  }
  if (element.id) {
    for (const label of doc.querySelectorAll('label')) {
      if (label.htmlFor === element.id) parts.push(label.textContent);
    }
  }
  const wrapping = element.closest('label');
  if (wrapping) parts.push(wrapping.textContent);
  return parts.join(' ').replace(/\s+/g, ' ').trim();
}

/** Compiles an HTML pattern attribute the way browsers do (whole value, 'v' flag). */
export function htmlPattern(pattern) {
  return new RegExp(`^(?:${pattern})$`, 'v');
}
