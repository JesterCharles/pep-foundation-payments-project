// Supplied check support (do not edit): exact money helpers in web/src/money.js.
import { describe, expect, it } from 'vitest';
import * as money from '../../../src/money.js';
import { generatedAmounts } from './fixtures.js';

export function moneyChecks(week) {
  describe(`${week}: parseAmountToCents (web/src/money.js)`, () => {
    it.each([
      ['0.01', 1],
      ['5.00', 500],
      ['1234.50', 123450],
      ['9999999.99', 999999999],
    ])('parses %j to %d cents', (text, cents) => {
      expect(money.parseAmountToCents(text)).toBe(cents);
    });

    it.each([
      ['1.5'], ['1,000.00'], ['-5.00'], [''], ['   '], ['0.00'], ['10000000.00'], ['12'], ['.50'], ['1.234'],
      ['1e3'], ['abc'], ['١٢.٠٠'], ['１２.００'], ['१.००'],
    ])('returns null for %j', (text) => {
      expect(money.parseAmountToCents(text)).toBeNull();
    });

    it('returns null for null, undefined and numbers', () => {
      expect(money.parseAmountToCents(null)).toBeNull();
      expect(money.parseAmountToCents(undefined)).toBeNull();
      expect(money.parseAmountToCents(12.5)).toBeNull();
    });
  });

  describe(`${week}: formatCents`, () => {
    it.each([
      [0, '0.00'],
      [5, '0.05'],
      [50, '0.50'],
      [123450, '1234.50'],
      [999999999, '9999999.99'],
      [-5, '-0.05'],
      [-123450, '-1234.50'],
    ])('formats %d as %j', (cents, text) => {
      expect(money.formatCents(cents)).toBe(text);
    });

    it('refuses non-integer cents', () => {
      expect(money.formatCents(100)).toBe('1.00');
      expect(() => money.formatCents(1.5)).toThrow();
      expect(() => money.formatCents('100')).toThrow();
      expect(() => money.formatCents(Number.NaN)).toThrow();
    });
  });

  describe(`${week}: sumCents`, () => {
    it('adds a thousand 0.10 amounts to exactly 100.00', () => {
      const cents = Array.from({ length: 1000 }, () => money.parseAmountToCents('0.10'));
      const total = money.sumCents(cents);
      expect(total).toBe(10000);
      expect(money.formatCents(total)).toBe('100.00');
    });

    it('matches an integer oracle on generated amounts', () => {
      for (const seed of [1, 7, 42]) {
        const { texts, oracleCents } = generatedAmounts(seed, 250);
        const total = money.sumCents(texts.map((t) => money.parseAmountToCents(t)));
        expect(total, `seed ${seed}`).toBe(oracleCents);
      }
    });

    it('sums an empty list to 0 and refuses non-integers', () => {
      expect(money.sumCents([])).toBe(0);
      expect(() => money.sumCents([100, 0.5])).toThrow();
    });
  });
}
