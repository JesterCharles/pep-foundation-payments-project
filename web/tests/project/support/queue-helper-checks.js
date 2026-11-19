// Supplied check support (do not edit): filter and sort helpers in web/src/queue.js.
import { describe, expect, it } from 'vitest';
import * as queue from '../../../src/queue.js';
import { request } from './fixtures.js';

function sample() {
  return [
    request(1, { amount: '999.99', status: 'SUBMITTED' }),
    request(2, { amount: '1000.00', status: 'APPROVED' }),
    request(3, { amount: '10.00', status: 'SUBMITTED' }),
    request(4, { amount: '9.50', status: 'REJECTED' }),
    request(5, { amount: '1000.00', status: 'SUBMITTED' }),
  ];
}

const ids = (list) => list.map((r) => r.id);

export function queueHelperChecks(week) {
  describe(`${week}: filterByStatus (web/src/queue.js)`, () => {
    it('keeps only the given status, in the original order', () => {
      expect(ids(queue.filterByStatus(sample(), 'SUBMITTED'))).toEqual([1, 3, 5]);
      expect(ids(queue.filterByStatus(sample(), 'APPROVED'))).toEqual([2]);
    });

    it('returns everything for an empty or missing status, and [] when nothing matches', () => {
      expect(ids(queue.filterByStatus(sample(), ''))).toEqual([1, 2, 3, 4, 5]);
      expect(ids(queue.filterByStatus(sample(), undefined))).toEqual([1, 2, 3, 4, 5]);
      expect(queue.filterByStatus(sample(), 'PAID')).toEqual([]);
      expect(queue.filterByStatus([], 'SUBMITTED')).toEqual([]);
    });

    it('returns a new array and leaves the input untouched', () => {
      const input = sample();
      const before = JSON.stringify(input);
      const result = queue.filterByStatus(input, '');
      expect(result).not.toBe(input);
      expect(JSON.stringify(input)).toBe(before);
    });
  });

  describe(`${week}: sortByAmount`, () => {
    it('sorts by exact amount, largest first by default, ties by id', () => {
      expect(ids(queue.sortByAmount(sample()))).toEqual([2, 5, 1, 3, 4]);
      expect(ids(queue.sortByAmount(sample(), 'desc'))).toEqual([2, 5, 1, 3, 4]);
    });

    it('sorts smallest first with "asc", ties by id', () => {
      expect(ids(queue.sortByAmount(sample(), 'asc'))).toEqual([4, 3, 1, 2, 5]);
    });

    it('handles empty lists and does not mutate the input', () => {
      expect(queue.sortByAmount([])).toEqual([]);
      const input = sample();
      const before = JSON.stringify(input);
      expect(queue.sortByAmount(input)).not.toBe(input);
      expect(JSON.stringify(input)).toBe(before);
    });
  });
}
