// Supplied check support (do not edit): the operational browser client against a stubbed API.
// Contract (web/README.md):
//   startQueuePage(root, { fetch, token }) in web/src/queue-page.js
//   startRequestForm(form, { fetch, token }) in web/src/request-form.js
import { describe, expect, it, vi } from 'vitest';
import { startQueuePage } from '../../../src/queue-page.js';
import { startRequestForm } from '../../../src/request-form.js';
import { request } from './fixtures.js';
import { loadIndexDocument } from './paths.js';
import { stubApi } from './stub-api.js';

const MARKUP = '<img src=x onerror="window.__xss=1">INV-<b>9</b>';

function freshRoot() {
  document.body.innerHTML = '<main><section id="queue"></section></main>';
  return document.getElementById('queue');
}

function alerts() {
  return [...document.querySelectorAll('[role="alert"]')].map((e) => e.textContent).join(' ').trim();
}

function statusMessages() {
  return [...document.querySelectorAll('[role="status"]')].map((e) => e.textContent).join(' ').trim();
}

function row(id) {
  return document.querySelector(`[data-request-id="${id}"]`);
}

function visibleRows() {
  return [...document.querySelectorAll('[data-request-id]')].filter((r) => !r.hidden && !r.closest('[hidden]'));
}

function approveButton(id) {
  return [...row(id).querySelectorAll('button')].find((b) => /approve/i.test(b.textContent || b.getAttribute('aria-label') || ''));
}

const QUEUE = [
  request(1, { amount: '1234.50', status: 'SUBMITTED', supplierId: 'SUP-0001' }),
  request(2, { amount: '99.99', status: 'APPROVED', supplierId: 'SUP-0002' }),
  request(3, { amount: '0.01', status: 'SUBMITTED', supplierId: 'SUP-0003', invoiceNumber: MARKUP }),
];

export function queueClientChecks(week, { withFilter }) {
  describe(`${week}: queue page against the API (web/src/queue-page.js)`, () => {
    it('loads requests with GET and shows supplier, invoice, exact amount and status per row', async () => {
      const api = stubApi({ requests: QUEUE });
      startQueuePage(freshRoot(), { fetch: api.fetch, token: 'test-token' });
      await vi.waitFor(() => expect(visibleRows().length).toBe(3));
      expect(api.calls[0].method).toBe('GET');
      expect(api.calls[0].headers.get('Authorization'), 'token sent as a Bearer header').toBe('Bearer test-token');
      const text = row(1).textContent;
      for (const part of ['SUP-0001', 'INV-1', '1234.50', 'SUBMITTED']) expect(text).toContain(part);
      expect(row(3).textContent).toContain('0.01');
    });

    it('renders invoice text as text, never as markup', async () => {
      const api = stubApi({ requests: QUEUE });
      startQueuePage(freshRoot(), { fetch: api.fetch });
      await vi.waitFor(() => expect(row(3)).not.toBeNull());
      expect(row(3).querySelector('img, b'), 'markup must not become elements').toBeNull();
      expect(row(3).textContent).toContain('<img src=x');
      expect(window.__xss).toBeUndefined();
    });

    it('says so when the queue is empty', async () => {
      const api = stubApi({ queue: 'empty' });
      startQueuePage(freshRoot(), { fetch: api.fetch });
      await vi.waitFor(() => expect(document.body.textContent).toMatch(/no payment requests/i));
      expect(visibleRows()).toHaveLength(0);
    });

    it.each([
      [401, /sign in/i],
      [500, /./],
      ['network', /./],
    ])('shows an alert when loading answers %s', async (queue, pattern) => {
      const api = stubApi({ queue });
      startQueuePage(freshRoot(), { fetch: api.fetch });
      await vi.waitFor(() => expect(alerts()).toMatch(pattern));
      expect(visibleRows()).toHaveLength(0);
      expect(document.body.textContent, 'no stack traces or raw errors').not.toMatch(/TypeError|at \w+ \(|undefined/);
    });

    it('approves a request and shows the new status from the response', async () => {
      const api = stubApi({ requests: QUEUE, approve: 200 });
      startQueuePage(freshRoot(), { fetch: api.fetch });
      await vi.waitFor(() => expect(row(1)).not.toBeNull());
      approveButton(1).click();
      await vi.waitFor(() => expect(row(1).textContent).toContain('APPROVED'));
      const post = api.calls.find((c) => c.method === 'POST');
      expect(post.path).toBe('/payment-requests/1/approve');
      expect(alerts()).toBe('');
    });

    it.each([
      [403, 'over approval limit'],
      [409, 'request is not awaiting approval'],
      [500, ''],
    ])('a refused approval (%s) explains why and leaves the status unchanged', async (approve, detail) => {
      const api = stubApi({ requests: QUEUE, approve });
      startQueuePage(freshRoot(), { fetch: api.fetch });
      await vi.waitFor(() => expect(row(1)).not.toBeNull());
      approveButton(1).click();
      await vi.waitFor(() => expect(alerts()).not.toBe(''));
      if (detail) expect(alerts()).toContain(detail);
      expect(row(1).textContent).toContain('SUBMITTED');
      expect(row(1).textContent).not.toContain('APPROVED');
      expect(statusMessages(), 'no success message after a refusal').not.toMatch(/approved|success|done/i);
    });

    if (withFilter) {
      it('filters the visible rows by status with a labelled control', async () => {
        const api = stubApi({ requests: QUEUE });
        const root = freshRoot();
        startQueuePage(root, { fetch: api.fetch });
        await vi.waitFor(() => expect(visibleRows().length).toBe(3));
        const select = [...root.querySelectorAll('select')].find((s) => {
          const label = s.labels?.[0]?.textContent || s.getAttribute('aria-label') || '';
          return /status/i.test(label);
        });
        expect(select, 'a <select> labelled "Status"').toBeTruthy();
        select.value = 'APPROVED';
        select.dispatchEvent(new Event('change', { bubbles: true }));
        await vi.waitFor(() => expect(visibleRows().map((r) => r.dataset.requestId)).toEqual(['2']));
      });
    }
  });

  describe(`${week}: payment-request form against the API (web/src/request-form.js)`, () => {
    function mountForm(api) {
      const doc = loadIndexDocument();
      document.body.innerHTML = doc.body.innerHTML;
      const form = [...document.querySelectorAll('form')].find((f) => f.querySelector('[name="amount"]'));
      startRequestForm(form, { fetch: api.fetch });
      form.querySelector('[name="supplierId"]').value = 'SUP-0042';
      form.querySelector('[name="invoiceNumber"]').value = 'INV 7781';
      form.querySelector('[name="amount"]').value = '1234.50';
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      return form;
    }

    it('posts the amount as exact two-decimal text and confirms the created request', async () => {
      const api = stubApi({ submit: 201 });
      mountForm(api);
      await vi.waitFor(() => expect(statusMessages()).toMatch(/100|submitted/i));
      const post = api.calls.find((c) => c.method === 'POST');
      expect(post.path).toBe('/payment-requests');
      const sent = JSON.parse(post.body);
      expect(sent).toMatchObject({ supplierId: 'SUP-0042', invoiceNumber: 'INV 7781', amount: '1234.50', currency: 'USD' });
      expect(alerts()).toBe('');
    });

    it.each([
      [400, 'amount must have exactly two decimals'],
      [409, 'duplicate invoice'],
    ])('a %s answer shows the reason and no success message', async (submit, reason) => {
      const api = stubApi({ submit });
      mountForm(api);
      await vi.waitFor(() => expect(alerts()).toContain(reason));
      expect(statusMessages()).not.toMatch(/submitted|success|created/i);
    });
  });
}
