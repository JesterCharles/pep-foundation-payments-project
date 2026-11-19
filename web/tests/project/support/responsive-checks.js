// Supplied check support (do not edit): responsive layout of web/index.html in pinned headless Chromium.
// Needs the Playwright Chromium build: run `npm run prefetch` once (network), then it works offline.
import { createReadStream, existsSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';
import { chromium } from 'playwright';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { WEB_ROOT } from './paths.js';

const TYPES = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.mjs': 'text/javascript', '.svg': 'image/svg+xml' };

function browserInstalled() {
  try {
    return existsSync(chromium.executablePath());
  } catch {
    return false;
  }
}

export function responsiveChecks(week) {
  const installed = browserInstalled();
  if (!installed) {
    console.warn(`${week}: SKIPPED responsive checks: Playwright Chromium is not installed. Run \`npm run prefetch\` in web/.`);
  }
  describe.skipIf(!installed)(`${week}: responsive layout in headless Chromium`, () => {
    let server;
    let browser;
    let base;

    beforeAll(async () => {
      server = createServer((req, res) => {
        const path = normalize(decodeURIComponent(new URL(req.url, 'http://x').pathname)).replace(/^\/+/, '');
        const file = join(WEB_ROOT, path || 'index.html');
        if (!file.startsWith(WEB_ROOT) || file.includes('node_modules') || !existsSync(file) || statSync(file).isDirectory()) {
          res.writeHead(404).end();
          return;
        }
        res.writeHead(200, { 'Content-Type': TYPES[extname(file)] || 'application/octet-stream' });
        createReadStream(file).pipe(res);
      });
      await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
      base = `http://127.0.0.1:${server.address().port}/index.html`;
      browser = await chromium.launch({ headless: true });
    });

    afterAll(async () => {
      await browser?.close();
      await new Promise((resolve) => (server ? server.close(resolve) : resolve()));
    });

    async function measure(width, height) {
      const page = await browser.newPage({ viewport: { width, height } });
      await page.goto(base, { waitUntil: 'load' });
      const result = await page.evaluate(() => {
        const fields = ['supplierId', 'invoiceNumber', 'amount'].map((name) => {
          const el = document.querySelector(`[name="${name}"]`);
          const box = el?.getBoundingClientRect();
          return { name, left: box?.left ?? -1, right: box?.right ?? 1e9, height: box?.height ?? 0 };
        });
        let mediaRules = 0;
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) if (rule instanceof CSSMediaRule) mediaRules++;
          } catch {
            /* cross-origin sheet */
          }
        }
        return {
          scrollWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          fields,
          mediaRules,
        };
      });
      await page.close();
      return result;
    }

    it('fits a 360px phone screen without sideways scrolling, every field fully visible', async () => {
      const m = await measure(360, 740);
      expect(m.scrollWidth, 'page wider than the phone screen').toBeLessThanOrEqual(m.clientWidth);
      for (const f of m.fields) {
        expect(f.left, `${f.name} starts off-screen`).toBeGreaterThanOrEqual(0);
        expect(f.right, `${f.name} ends off-screen`).toBeLessThanOrEqual(m.clientWidth);
        expect(f.height, `${f.name} too small to tap`).toBeGreaterThanOrEqual(24);
      }
    });

    it('fits a 1280px desktop screen and uses at least one media query', async () => {
      const m = await measure(1280, 800);
      expect(m.scrollWidth).toBeLessThanOrEqual(m.clientWidth);
      expect(m.mediaRules, 'a @media rule in a stylesheet linked from index.html').toBeGreaterThan(0);
    });
  });
}
