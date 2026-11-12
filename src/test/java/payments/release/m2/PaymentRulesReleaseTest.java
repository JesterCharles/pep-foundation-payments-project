package payments.release.m2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import payments.intake.PaymentRules;

/**
 * Release 2 public tests (supplied; do not edit). Run with {@code mvn -q test -Pm2}.
 *
 * <p>Expected values come from the M2 brief. Generated cases build their expected result while
 * generating the input, so echoing sample outputs cannot pass them. All data is synthetic.
 */
class PaymentRulesReleaseTest {

    // ---------------------------------------------------------------- parseAmountCents

    @ParameterizedTest(name = "parseAmountCents(\"{0}\") = {1}")
    @CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = false, value = {
        "0.01;1",
        "1.00;100",
        "1234.50;123450",
        "9999999.99;999999999",
        "00012.30;1230",
        " 7.25 ;725",
        "\\t7.25\\n;725",
    })
    @DisplayName("valid amounts parse to exact cents")
    void validAmounts(String raw, long expected) {
        assertEquals(expected, PaymentRules.parseAmountCents(unescape(raw)), "parseAmountCents(\"" + raw + "\")");
    }

    @ParameterizedTest(name = "parseAmountCents(\"{0}\") = -1")
    @ValueSource(strings = {
        "0.00", "000.00", "10000000.00", "99999999999999999999.99",
        "18446744073709551716.00", "9223372036854775807.99",
        "1.5", "1.505", ".50", "12.", "12", "-5.00", "+5.00", "1,000.00", "1 000.00",
        "12..30", "12.3a", "1e3.00", "abc", ".", "1.0O", "١٢.٣٠x", "12.30 USD", "$12.30",
    })
    @DisplayName("invalid shapes, zero and out-of-range amounts give -1 without overflowing")
    void invalidAmounts(String raw) {
        assertEquals(-1L, PaymentRules.parseAmountCents(raw), "parseAmountCents(\"" + raw + "\") must be -1");
    }

    @ParameterizedTest(name = "parseAmountCents(blank \"{0}\") = -1")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void nullAndBlankAmounts(String raw) {
        assertEquals(-1L, PaymentRules.parseAmountCents(raw));
    }

    @ParameterizedTest(name = "parseAmountCents(\"{0}\") = {1}")
    @CsvSource(delimiter = ';', value = {
        "0.10;10", "0.99;99", "1.09;109", "10.01;1001", "90.90;9090", "100.00;10000", "4999.99;499999",
        "5000.01;500001", "24999.99;2499999", "123456.78;12345678", "9999999.98;999999998",
    })
    @DisplayName("more hand-checked amounts parse to exact cents (no floating point)")
    void moreExactAmounts(String raw, long expected) {
        assertEquals(expected, PaymentRules.parseAmountCents(raw), "parseAmountCents(\"" + raw + "\")");
    }

    @Test
    @DisplayName("one thousand lines of 0.10 add up to exactly 100.00 (no rounding drift)")
    void noRoundingDrift() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            lines.add("SUP-0010|DRIFT-" + i + "|0.10|USD");
        }
        Map<String, Long> totals = PaymentRules.totalsBySupplier(lines);
        assertEquals(Long.valueOf(10_000L), totals.get("SUP-0010"));
        assertEquals("100.00", PaymentRules.formatCents(totals.get("SUP-0010")));
    }

    // ---------------------------------------------------------------- formatCents

    @ParameterizedTest(name = "formatCents({0}) = \"{1}\"")
    @CsvSource({
        "0,0.00", "5,0.05", "99,0.99", "100,1.00", "101,1.01", "123450,1234.50",
        "999999999,9999999.99", "-5,-0.05", "-100,-1.00", "-123450,-1234.50", "-999999999,-9999999.99",
    })
    void formatCents(long cents, String expected) {
        assertEquals(expected, PaymentRules.formatCents(cents));
    }

    @Test
    @DisplayName("formatCents and parseAmountCents round-trip every generated valid amount")
    void formatParseRoundTrip() {
        Random random = new Random(7L);
        for (int i = 0; i < 300; i++) {
            long cents = 1 + random.nextInt(999_999_999);
            assertEquals(cents, PaymentRules.parseAmountCents(PaymentRules.formatCents(cents)), "round trip of " + cents);
        }
    }

    // ---------------------------------------------------------------- invoiceKey

    @ParameterizedTest(name = "invoiceKey(\"{0}\", \"{1}\") = \"{2}\"")
    @CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = false, value = {
        "SUP-0042;INV-7781;SUP-0042|INV-7781",
        "sup-0042;inv-7781;SUP-0042|INV-7781",
        " SUP-0042 ; INV 77 81 ;SUP-0042|INV7781",
        "Sup-0001;a b c;SUP-0001|ABC",
        "SUP-9999;7;SUP-9999|7",
    })
    @DisplayName("valid supplier and invoice give the normalised key")
    void invoiceKeyValid(String supplier, String invoice, String expected) {
        assertEquals(expected, PaymentRules.invoiceKey(supplier, invoice));
    }

    @ParameterizedTest(name = "invoiceKey(\"{0}\", \"{1}\") = \"\"")
    @CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = false, nullValues = "NULL", value = {
        "SUP-42;INV-1", "SUP-00421;INV-1", "SUP-004a;INV-1", "SUP0042;INV-1", "XYZ-0042;INV-1",
        "SUP-;INV-1", "SU P-0042;INV-1", "NULL;INV-1", ";INV-1",
        "SUP-0042;NULL", "SUP-0042;", "SUP-0042;   ",
    })
    @DisplayName("invalid supplier ids or blank invoices give an empty key")
    void invoiceKeyInvalid(String supplier, String invoice) {
        assertEquals("", PaymentRules.invoiceKey(supplier, invoice));
    }

    @Test
    @DisplayName("the one-line overload gives the same key as the two-argument form")
    void invoiceKeyLineOverload() {
        assertEquals("SUP-0042|INV7781", PaymentRules.invoiceKey("sup-0042| inv 7781 |12.00|USD"));
        assertEquals("SUP-0003|A-1", PaymentRules.invoiceKey("SUP-0003|a-1|0.00|EUR"));
        assertEquals("", PaymentRules.invoiceKey("SUP-42|INV-1|12.00|USD"));
        assertEquals("", PaymentRules.invoiceKey("SUP-0042|   |12.00|USD"));
        assertEquals("", PaymentRules.invoiceKey("SUP-0042|INV-1|12.00"));
        assertEquals("", PaymentRules.invoiceKey("SUP-0042|INV-1|12.00|USD|extra"));
        assertEquals("", PaymentRules.invoiceKey((String) null));
        Random random = new Random(42L);
        for (int i = 0; i < 100; i++) {
            String supplier = randomCase("sup-" + fourDigits(random.nextInt(10_000)), random);
            String invoice = randomCase(" in v-" + random.nextInt(100_000) + " ", random);
            assertEquals(PaymentRules.invoiceKey(supplier, invoice),
                    PaymentRules.invoiceKey(supplier + "|" + invoice + "|1.00|USD"),
                    "line overload must delegate to the two-argument form");
        }
    }

    // ---------------------------------------------------------------- totalsBySupplier

    @Test
    @DisplayName("totals skip malformed, non-USD and invalid lines, and merge supplier case")
    void totalsFixedExample() {
        List<String> lines = new ArrayList<>(List.of(
                "SUP-0001|INV-1|100.00|USD",
                "sup-0001|INV-2|0.50| usd ",
                "SUP-0002|INV-1|250.25|USD",
                "SUP-0002|INV-9|10.00|EUR",
                "SUP-0002|INV-8|10.00|usd",
                "SUP-0003|INV-1|1.5|USD",
                "SUP-0003|INV-2|0.00|USD",
                "SUP-03|INV-3|5.00|USD",
                "SUP-0004|   |5.00|USD",
                "SUP-0005|INV-1|5.00",
                "SUP-0005|INV-2|5.00|USD|extra",
                "",
                "SUP-0006|INV-1|5.00|US D"));
        Map<String, Long> totals = PaymentRules.totalsBySupplier(lines);
        Map<String, Long> expected = new HashMap<>();
        expected.put("SUP-0001", 10_050L);
        expected.put("SUP-0002", 26_025L);
        assertEquals(expected, totals);
    }

    @Test
    @DisplayName("a repeated invoice key later in the list is skipped (case and space variety)")
    void totalsSkipDuplicates() {
        List<String> lines = List.of(
                "SUP-0001|INV 100|40.00|USD",
                "sup-0001|inv100|40.00|USD",
                "SUP-0001| I N V 1 0 0 |99.00|USD",
                "SUP-0002|INV100|7.00|USD",
                "SUP-0001|INV-101|1.00|USD");
        Map<String, Long> totals = PaymentRules.totalsBySupplier(lines);
        assertEquals(Long.valueOf(4_100L), totals.get("SUP-0001"));
        assertEquals(Long.valueOf(700L), totals.get("SUP-0002"));
        assertEquals(2, totals.size());
    }

    @Test
    @DisplayName("null input gives an empty map; the result is a new HashMap; the input is unchanged")
    void totalsNullAndImmutability() {
        Map<String, Long> fromNull = PaymentRules.totalsBySupplier(null);
        assertTrue(fromNull.isEmpty(), "null gives an empty map");
        assertTrue(fromNull instanceof HashMap, "return a new HashMap");
        List<String> lines = new ArrayList<>(List.of("SUP-0001|INV-1|1.00|USD", "SUP-0001|INV-1|1.00|USD"));
        List<String> before = new ArrayList<>(lines);
        Map<String, Long> first = PaymentRules.totalsBySupplier(lines);
        Map<String, Long> second = PaymentRules.totalsBySupplier(lines);
        assertEquals(before, lines, "totalsBySupplier must not modify its input");
        assertTrue(first instanceof HashMap, "return a new HashMap");
        assertNotSame(first, second, "each call returns a new map");
        assertEquals(Long.valueOf(100L), first.get("SUP-0001"));
    }

    @Test
    @DisplayName("300 seeded intake lines match totals built while generating them")
    void totalsSeeded() {
        for (long seed : new long[] {1L, 2L, 3L, 2026L, 11L}) {
            Random random = new Random(seed);
            List<String> lines = new ArrayList<>();
            List<String[]> counted = new ArrayList<>();
            Map<String, Long> expected = new HashMap<>();
            for (int i = 0; i < 300; i++) {
                int kind = random.nextInt(10);
                String supplier = "SUP-" + fourDigits(random.nextInt(12));
                String[] picked = SEEDED_AMOUNTS[random.nextInt(SEEDED_AMOUNTS.length)];
                String amount = picked[0];
                long cents = Long.parseLong(picked[1]);
                if (kind <= 4 || counted.isEmpty()) {
                    String invoice = "INV-" + seed + "-" + i;
                    lines.add(randomCase(supplier, random) + "|" + spaced(invoice, random) + "|" + amount + "|"
                            + pick(random, "USD", " usd ", "Usd", "USD "));
                    counted.add(new String[] {supplier, invoice});
                    expected.merge(supplier, cents, Long::sum);
                } else if (kind == 5) {
                    String[] earlier = counted.get(random.nextInt(counted.size()));
                    lines.add(randomCase(earlier[0], random) + "|" + spaced(randomCase(earlier[1], random), random)
                            + "|" + amount + "|USD");
                } else {
                    String invoice = "BAD-" + seed + "-" + i;
                    String bad = switch (random.nextInt(6)) {
                        case 0 -> "SUP-" + random.nextInt(100) + "|" + invoice + "|" + amount + "|USD";
                        case 1 -> supplier + "|   |" + amount + "|USD";
                        case 2 -> supplier + "|" + invoice + "|" + pick(random, ONE_DECIMAL_AMOUNTS) + "|USD";
                        case 3 -> supplier + "|" + invoice + "|" + amount + "|" + pick(random, "EUR", "GBP", "US", "");
                        case 4 -> supplier + "|" + invoice + "|" + amount;
                        default -> supplier + "|" + invoice + "|" + amount + "|USD|x";
                    };
                    lines.add(bad);
                }
            }
            List<String> before = new ArrayList<>(lines);
            assertEquals(expected, PaymentRules.totalsBySupplier(lines), "seed " + seed);
            assertEquals(before, lines, "input unchanged, seed " + seed);
        }
    }

    // ---------------------------------------------------------------- indexOfRequest

    @Test
    @DisplayName("binary search: null, empty, single, first, last and absent ids")
    void indexOfRequestEdges() {
        assertEquals(-1, PaymentRules.indexOfRequest(null, 5));
        assertEquals(-1, PaymentRules.indexOfRequest(new int[0], 5));
        assertEquals(0, PaymentRules.indexOfRequest(new int[] {5}, 5));
        assertEquals(-1, PaymentRules.indexOfRequest(new int[] {5}, 4));
        assertEquals(-1, PaymentRules.indexOfRequest(new int[] {5}, 6));
        int[] ids = {3, 8, 15, 16, 23, 42, 100};
        assertEquals(0, PaymentRules.indexOfRequest(ids, 3));
        assertEquals(6, PaymentRules.indexOfRequest(ids, 100));
        assertEquals(3, PaymentRules.indexOfRequest(ids, 16));
        assertEquals(-1, PaymentRules.indexOfRequest(ids, 1), "absent below");
        assertEquals(-1, PaymentRules.indexOfRequest(ids, 20), "absent between");
        assertEquals(-1, PaymentRules.indexOfRequest(ids, 101), "absent above");
        int[] copy = Arrays.copyOf(ids, ids.length);
        PaymentRules.indexOfRequest(ids, 42);
        assertTrue(Arrays.equals(copy, ids), "the array must not be modified");
    }

    @Test
    @DisplayName("binary search over a large generated array finds every id and no gap")
    void indexOfRequestLarge() {
        Random random = new Random(99L);
        int[] ids = new int[100_000];
        int next = -50;
        for (int i = 0; i < ids.length; i++) {
            next += 2 + random.nextInt(3);
            ids[i] = next;
        }
        for (int i = 0; i < ids.length; i++) {
            assertEquals(i, PaymentRules.indexOfRequest(ids, ids[i]), "index of " + ids[i]);
            if (i > 0 && ids[i] - ids[i - 1] > 1) {
                assertEquals(-1, PaymentRules.indexOfRequest(ids, ids[i] - 1), "gap " + (ids[i] - 1));
            }
        }
        assertEquals(-1, PaymentRules.indexOfRequest(ids, Integer.MIN_VALUE));
        assertEquals(-1, PaymentRules.indexOfRequest(ids, Integer.MAX_VALUE));
    }

    // ---------------------------------------------------------------- helpers

    private static String unescape(String raw) {
        return raw.replace("\\t", "\t").replace("\\n", "\n");
    }

    /** Hand-checked amount text and its exact cents, picked by the seed in totalsSeeded. */
    private static final String[][] SEEDED_AMOUNTS = {
        {"0.01", "1"}, {"0.10", "10"}, {"1.00", "100"}, {"7.25", "725"}, {"12.50", "1250"},
        {"99.99", "9999"}, {"100.00", "10000"}, {"1234.56", "123456"}, {"5000.01", "500001"},
        {"49999.99", "4999999"},
    };

    /** Amounts with one decimal digit, which totalsBySupplier must skip. */
    private static final String[] ONE_DECIMAL_AMOUNTS = {"12.5", "7.0", "100.1", "0.5"};

    private static String fourDigits(int value) {
        String text = String.valueOf(value);
        while (text.length() < 4) {
            text = "0" + text;
        }
        return text;
    }

    private static String randomCase(String text, Random random) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            out.append(random.nextBoolean() ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static String spaced(String text, Random random) {
        StringBuilder out = new StringBuilder(random.nextBoolean() ? " " : "");
        for (char c : text.toCharArray()) {
            out.append(c);
            if (random.nextInt(5) == 0) {
                out.append(' ');
            }
        }
        return out.toString();
    }

    private static String pick(Random random, String... options) {
        return options[random.nextInt(options.length)];
    }
}
