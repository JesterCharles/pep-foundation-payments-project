// Supplied check support (do not edit): structure and validation attributes of web/index.html.
import { describe, expect, it } from 'vitest';
import { htmlPattern, labelText, loadIndexDocument } from './paths.js';

const FIELDS = ['supplierId', 'invoiceNumber', 'amount'];

export function formStructureChecks(week) {
  describe(`${week}: page structure and landmarks (web/index.html)`, () => {
    it('declares a language, a title and a mobile viewport', () => {
      const doc = loadIndexDocument();
      expect(doc.documentElement.getAttribute('lang') || '', 'html lang').not.toBe('');
      expect(doc.title.trim(), '<title>').not.toBe('');
      const viewport = doc.querySelector('meta[name="viewport"]');
      expect(viewport?.getAttribute('content') || '', 'meta viewport').toMatch(/width=device-width/);
    });

    it('has one banner header, one main and a footer, and exactly one h1', () => {
      const doc = loadIndexDocument();
      expect(doc.querySelectorAll('main, [role="main"]').length, 'main landmarks').toBe(1);
      expect(doc.querySelector('body > header, [role="banner"]'), 'page header').not.toBeNull();
      expect(doc.querySelector('body > footer, [role="contentinfo"]'), 'page footer').not.toBeNull();
      expect(doc.querySelectorAll('h1').length, 'h1 count').toBe(1);
    });

    it('has an approval queue table with column headers for amount and status', () => {
      const doc = loadIndexDocument();
      const table = doc.querySelector('main table');
      expect(table, 'a table inside main').not.toBeNull();
      const named = (table.querySelector('caption')?.textContent || table.getAttribute('aria-label') || '').trim()
        || (table.getAttribute('aria-labelledby') && doc.getElementById(table.getAttribute('aria-labelledby'))?.textContent);
      expect(named, 'the table needs a caption or accessible name').toBeTruthy();
      const headers = [...table.querySelectorAll('th[scope="col"]')].map((th) => th.textContent.trim().toLowerCase());
      expect(headers.some((h) => h.includes('amount')), `column headers ${headers}`).toBe(true);
      expect(headers.some((h) => h.includes('status')), `column headers ${headers}`).toBe(true);
    });
  });

  describe(`${week}: payment-request form`, () => {
    function form() {
      const doc = loadIndexDocument();
      const forms = [...doc.querySelectorAll('form')].filter((f) => f.querySelector('[name="amount"]'));
      expect(forms.length, 'a form with an amount field').toBe(1);
      return { doc, form: forms[0] };
    }

    it.each(FIELDS)('field %s exists, is labelled and required', (name) => {
      const { doc, form: f } = form();
      const field = f.querySelector(`[name="${name}"]`);
      expect(field, `field name="${name}"`).not.toBeNull();
      expect(labelText(doc, field), `label for ${name}`).not.toBe('');
      expect(field.hasAttribute('required'), `${name} required`).toBe(true);
    });

    it('supplier id pattern accepts SUP-#### only', () => {
      const field = form().form.querySelector('[name="supplierId"]');
      const pattern = field.getAttribute('pattern');
      expect(pattern, 'supplierId pattern').toBeTruthy();
      const re = htmlPattern(pattern);
      for (const ok of ['SUP-0001', 'SUP-0042', 'SUP-9999']) expect(re.test(ok), ok).toBe(true);
      for (const bad of ['SUP-001', 'SUP-00011', 'XYZ-0001', 'SUP0001', 'SUP-00A1', ' SUP-0001']) {
        expect(re.test(bad), bad).toBe(false);
      }
    });

    it('amount is a text field with decimal keyboard and a two-decimal pattern', () => {
      const field = form().form.querySelector('[name="amount"]');
      expect((field.getAttribute('type') || 'text').toLowerCase(), 'type=number loses exact text').toBe('text');
      expect(field.getAttribute('inputmode'), 'inputmode').toBe('decimal');
      const pattern = field.getAttribute('pattern');
      expect(pattern, 'amount pattern').toBeTruthy();
      const re = htmlPattern(pattern);
      for (const ok of ['0.01', '5.00', '1234.50', '9999999.99']) expect(re.test(ok), ok).toBe(true);
      for (const bad of ['1.5', '1,000.00', '-5.00', '12', '.50', '1.234', 'abc', '1e3']) {
        expect(re.test(bad), bad).toBe(false);
      }
    });

    it('offers USD as the only currency and has a labelled submit button', () => {
      const { form: f } = form();
      const currency = f.querySelector('[name="currency"]');
      if (currency) {
        const values = currency.tagName === 'SELECT'
          ? [...currency.querySelectorAll('option')].map((o) => o.value || o.textContent.trim())
          : [currency.getAttribute('value') || ''];
        expect(values, 'currency values').toEqual(['USD']);
      } else {
        expect(f.textContent, 'state the currency when there is no currency field').toMatch(/USD/);
      }
      const submit = f.querySelector('button[type="submit"], button:not([type]), input[type="submit"]');
      expect(submit, 'submit button').not.toBeNull();
      expect((submit.textContent || submit.getAttribute('value') || '').trim(), 'submit button text').not.toBe('');
    });
  });
}
