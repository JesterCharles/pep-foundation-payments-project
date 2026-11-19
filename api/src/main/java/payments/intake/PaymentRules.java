package payments.intake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Payment-intake rules for Northwind Components (fictional) supplier invoices.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). Money is always a whole number of
 * cents held in a {@code long}; {@code double}, {@code float} and {@code BigDecimal} are never used
 * for amounts. Invalid input returns a sentinel ({@code -1} or {@code ""}) instead of throwing.
 */
public final class PaymentRules {

    /** The largest valid amount: 9,999,999.99 USD as cents. */
    public static final long MAX_CENTS = 999_999_999L;

    private PaymentRules() {
    }

    /**
     * Parses an amount such as {@code "1234.50"} into cents ({@code 123450}).
     * Accepted text (after trimming): one or more ASCII digits, a '.', then exactly two digits.
     *
     * @return the amount in cents, or {@code -1} when the text is invalid, zero or above MAX_CENTS
     */
    public static long parseAmountCents(String raw) {
        if (raw == null) {
            return -1;
        }
        String text = raw.trim();
        int dot = text.indexOf('.');
        if (dot < 1 || dot != text.length() - 3) {
            return -1;
        }
        long cents = 0;
        for (int i = 0; i < text.length(); i++) {
            if (i == dot) {
                continue;
            }
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            cents = cents * 10 + (c - '0');
            if (cents > MAX_CENTS) {
                return -1;
            }
        }
        return cents == 0 ? -1 : cents;
    }

    /**
     * Formats cents as text with exactly two decimals: 123450 gives "1234.50",
     * 5 gives "0.05", 0 gives "0.00" and -5 gives "-0.05".
     */
    public static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        // Long.MIN_VALUE has no positive counterpart; work on the quotient/remainder signs instead.
        long whole = Math.abs(cents / 100);
        long part = Math.abs(cents % 100);
        return sign + whole + "." + (part < 10 ? "0" : "") + part;
    }

    /**
     * Builds the duplicate-detection key {@code SUPPLIER|INVOICE}, for example
     * {@code invoiceKey(" sup-0042 ", "inv 7781")} gives {@code "SUP-0042|INV7781"}.
     *
     * @return the key, or {@code ""} when the supplier id or invoice number is invalid
     */
    public static String invoiceKey(String supplierId, String invoiceNumber) {
        if (supplierId == null || invoiceNumber == null || invoiceNumber.isBlank()) {
            return "";
        }
        String supplier = supplierId.trim().toUpperCase(Locale.ROOT);
        if (!isSupplierId(supplier)) {
            return "";
        }
        String invoice = invoiceNumber.toUpperCase(Locale.ROOT).replace(" ", "");
        if (invoice.isBlank()) {
            return "";
        }
        return supplier + "|" + invoice;
    }

    /**
     * Overload for one intake line {@code supplier|invoice|amount|currency}.
     *
     * @return the invoice key of the line, or {@code ""} when the line is invalid
     */
    public static String invoiceKey(String line) {
        if (line == null) {
            return "";
        }
        String[] fields = line.split("\\|", -1);
        if (fields.length != 4) {
            return "";
        }
        return invoiceKey(fields[0], fields[1]);
    }

    /**
     * Adds up valid USD intake lines per supplier, skipping duplicate invoice keys.
     *
     * @param lines intake lines {@code supplier|invoice|amount|currency}; never modified
     * @return a new {@code HashMap} from upper-case supplier id to total cents
     */
    public static Map<String, Long> totalsBySupplier(List<String> lines) {
        Map<String, Long> totals = new HashMap<>();
        if (lines == null) {
            return totals;
        }
        Set<String> seen = new HashSet<>();
        for (String line : new ArrayList<>(lines)) {
            if (line == null) {
                continue;
            }
            String[] fields = line.split("\\|", -1);
            if (fields.length != 4) {
                continue;
            }
            String key = invoiceKey(fields[0], fields[1]);
            long cents = parseAmountCents(fields[2]);
            String currency = fields[3].trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty() || cents < 0 || !currency.equals("USD")) {
                continue;
            }
            if (!seen.add(key)) {
                continue;
            }
            String supplier = fields[0].trim().toUpperCase(Locale.ROOT);
            totals.merge(supplier, cents, Long::sum);
        }
        return totals;
    }

    /**
     * Finds a request id in an ascending array of distinct ids with a binary search.
     *
     * @return the index of {@code id}, or {@code -1} when absent (also for null or empty arrays)
     */
    public static int indexOfRequest(int[] sortedIds, int id) {
        if (sortedIds == null) {
            return -1;
        }
        int low = 0;
        int high = sortedIds.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (sortedIds[mid] == id) {
                return mid;
            }
            if (sortedIds[mid] < id) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    private static boolean isSupplierId(String supplier) {
        if (supplier.length() != 8 || !supplier.startsWith("SUP-")) {
            return false;
        }
        for (int i = 4; i < 8; i++) {
            char c = supplier.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
