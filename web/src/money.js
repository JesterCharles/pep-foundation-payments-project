// LEARNER-OWNED (Standard W11 / Fast W09): exact money helpers. Money is a whole number of cents.
// Never use floating-point arithmetic on amounts; the API is the authority for every rule.

/**
 * Parses amount text such as "1234.50" into integer cents (123450).
 * Accepts ASCII digits, a '.', then exactly two digits; 0.01 up to 9999999.99.
 * Returns null for anything else (empty, "1.5", "1,000.00", "-5.00", "0.00", non-ASCII digits, non-strings).
 */
export function parseAmountToCents(text) {
  throw new Error('TODO: parseAmountToCents');
}

/** Formats integer cents as two-decimal text: 0 -> "0.00", 5 -> "0.05", -5 -> "-0.05". Throws for non-integers. */
export function formatCents(cents) {
  throw new Error('TODO: formatCents');
}

/** Adds integer cents exactly. [] -> 0. Throws when any value is not an integer. */
export function sumCents(values) {
  throw new Error('TODO: sumCents');
}
