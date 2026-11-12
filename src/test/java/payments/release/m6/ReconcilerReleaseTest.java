package payments.release.m6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.reports.PaymentReports;
import payments.reports.ReconciliationResult;
import payments.reports.Reconciler;
import payments.reports.StatementLine;

/**
 * Release 6 public tests for reconciliation and reports (supplied; do not edit).
 * Run with {@code mvn -q test} (the complete suite). Expected results are written out by hand
 * for the fixtures and built while generating the seeded data. All data is synthetic.
 */
class ReconcilerReleaseTest {

    private static final Comparator<StatementLine> BY_REQUEST_THEN_REFERENCE =
            Comparator.comparingInt(StatementLine::requestId).thenComparing(StatementLine::reference);

    /** The seven requests the statement fixtures refer to (ids 1-7). */
    static List<PaymentRequest> fixtureRequests() {
        List<PaymentRequest> requests = new ArrayList<>();
        requests.add(paid(new PaymentRequest(1, "SUP-0101", "INV-A1", 125_000L, "USD", "REQ-0001"), "STL-1001"));
        requests.add(paid(new PaymentRequest(2, "SUP-0101", "INV-A2", 30_000L, "USD", "REQ-0001"), "STL-1002"));
        requests.add(paid(new PaymentRequest(3, "SUP-0202", "INV-B1", 499_999L, "USD", "REQ-0002"), "STL-1003"));
        requests.add(paid(new PaymentRequest(4, "SUP-0202", "INV-B2", 7_525L, "USD", "REQ-0002"), "STL-1004"));
        requests.add(paid(new PaymentRequest(5, "SUP-0303", "INV-C1", 1_000L, "USD", "REQ-0001"), "STL-1005"));
        PaymentRequest approved = new PaymentRequest(6, "SUP-0303", "INV-C2", 64_000L, "USD", "REQ-0002");
        approved.approve("APR-0003");
        requests.add(approved);
        requests.add(new PaymentRequest(7, "SUP-0404", "INV-D1", 9_999L, "USD", "REQ-0001"));
        return requests;
    }

    @Test
    @DisplayName("precedence: duplicate, then unknown, then currency, then amount, then matched")
    void precedence() {
        List<StatementLine> lines = List.of(
                line("STL-2001", 98, 1_000L, "EUR"),
                line("STL-2001", 98, 1_000L, "EUR"),
                line("STL-2002", 4, 7_526L, "GBP"),
                line("STL-2003", 5, 1_000L, "USD"),
                line("STL-2003", 5, 1_000L, "USD"),
                line("STL-2004", 7, 9_999L, "USD"),
                line("STL-2005", 2, 29_999L, "USD"),
                line("STL-2006", 3, 499_999L, "usd"));
        ReconciliationResult result = Reconciler.reconcile(fixtureRequests(), lines);
        assertEquals(List.of(line("STL-2003", 5, 1_000L, "USD"), line("STL-2004", 7, 9_999L, "USD")), result.matched());
        assertEquals(List.of(line("STL-2005", 2, 29_999L, "USD")), result.amountMismatch());
        assertEquals(List.of(line("STL-2006", 3, 499_999L, "usd"), line("STL-2002", 4, 7_526L, "GBP")),
                result.currencyMismatch(), "currency wins over amount; only exact \"USD\" matches");
        assertEquals(List.of(line("STL-2001", 98, 1_000L, "EUR")), result.unknownReference());
        assertEquals(List.of(line("STL-2003", 5, 1_000L, "USD"), line("STL-2001", 98, 1_000L, "EUR")),
                result.duplicateLine(), "a repeated EUR line for an unknown request is only a duplicate");
        assertEquals(List.of(1), ids(result.paidWithoutLine()));
        assertEachLineOnce(lines, result);
    }

    @Test
    @DisplayName("every list is ordered by request id, then reference, whatever the statement order")
    void ordering() {
        List<StatementLine> lines = List.of(
                line("STL-Z", 5, 1_000L, "USD"),
                line("STL-B", 99, 1L, "USD"),
                line("STL-A", 2, 30_000L, "USD"),
                line("STL-M", 98, 1L, "USD"),
                line("STL-C", 1, 125_000L, "USD"),
                line("STL-A2", 99, 2L, "USD"));
        ReconciliationResult result = Reconciler.reconcile(fixtureRequests(), lines);
        assertEquals(List.of(1, 2, 5), lineIds(result.matched()));
        assertEquals(List.of("STL-M", "STL-A2", "STL-B"), references(result.unknownReference()));
        assertEquals(List.of(3, 4), ids(result.paidWithoutLine()));
    }

    @Test
    @DisplayName("empty inputs give empty lists, never null")
    void emptyInputs() {
        ReconciliationResult none = Reconciler.reconcile(List.of(), List.of());
        assertTrue(none.matched().isEmpty() && none.amountMismatch().isEmpty() && none.currencyMismatch().isEmpty()
                && none.unknownReference().isEmpty() && none.duplicateLine().isEmpty() && none.paidWithoutLine().isEmpty());
        ReconciliationResult noLines = Reconciler.reconcile(fixtureRequests(), List.of());
        assertEquals(List.of(1, 2, 3, 4, 5), ids(noLines.paidWithoutLine()));
        ReconciliationResult noRequests = Reconciler.reconcile(List.of(), List.of(line("STL-1", 1, 100L, "USD")));
        assertEquals(1, noRequests.unknownReference().size());
    }

    @Test
    @DisplayName("the supplied statement fixtures reconcile to the documented categories")
    void suppliedFixtures() throws IOException, URISyntaxException {
        ReconciliationResult mixed = Reconciler.reconcile(fixtureRequests(), StatementLine.readCsv(fixture("mixed-discrepancies.csv")));
        assertEquals(List.of(1, 6), lineIds(mixed.matched()));
        assertEquals(List.of(2), lineIds(mixed.amountMismatch()));
        assertEquals(List.of(3), lineIds(mixed.currencyMismatch()));
        assertEquals(List.of(99), lineIds(mixed.unknownReference()));
        assertEquals(List.of(1), lineIds(mixed.duplicateLine()));
        assertEquals(List.of(4, 5), ids(mixed.paidWithoutLine()));

        ReconciliationResult clean = Reconciler.reconcile(fixtureRequests(), StatementLine.readCsv(fixture("clean-match.csv")));
        assertEquals(List.of(1, 2, 3, 4, 5), lineIds(clean.matched()));
        assertTrue(clean.paidWithoutLine().isEmpty());
    }

    @Test
    @DisplayName("seeded statements (0-500 lines) match categories assigned while generating them")
    void seededStatements() {
        for (long seed = 1; seed <= 40; seed++) {
            Random random = new Random(seed * 7919L);
            List<PaymentRequest> requests = new ArrayList<>();
            int requestCount = random.nextInt(61);
            for (int id = 1; id <= requestCount; id++) {
                long cents = 1 + random.nextInt(2_000_000);
                PaymentRequest r = new PaymentRequest(id, "SUP-" + (1000 + random.nextInt(20)), "GEN-" + id, cents, "USD",
                        "REQ-0001");
                int status = random.nextInt(5);
                if (status >= 1) {
                    r.approve("APR-0003");
                }
                if (status == 2) {
                    r.markPaid("APR-0002", "STL-P" + id, cents, "USD");
                }
                if (status == 3) {
                    r.cancel("APR-0002", "seeded");
                }
                if (status == 4) {
                    r.markPaid("APR-0002", "STL-P" + id, cents, "USD");
                }
                requests.add(r);
            }
            Collections.shuffle(requests, random);

            Map<Integer, PaymentRequest> byId = new HashMap<>();
            for (PaymentRequest r : requests) {
                byId.put(r.getId(), r);
            }
            List<StatementLine> lines = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            Set<Integer> covered = new HashSet<>();
            List<StatementLine> matched = new ArrayList<>();
            List<StatementLine> amount = new ArrayList<>();
            List<StatementLine> currency = new ArrayList<>();
            List<StatementLine> unknown = new ArrayList<>();
            List<StatementLine> duplicate = new ArrayList<>();
            int lineCount = random.nextInt(501);
            for (int i = 0; i < lineCount; i++) {
                int kind = random.nextInt(10);
                if (kind == 0 && !seen.isEmpty()) {
                    int anyId = random.nextInt(requestCount + 10) + 1;
                    StatementLine dup = line(seen.get(random.nextInt(seen.size())), anyId, random.nextInt(100_000),
                            random.nextBoolean() ? "USD" : "EUR");
                    lines.add(dup);
                    duplicate.add(dup);
                    continue;
                }
                String reference = "STL-" + seed + "-" + i;
                seen.add(reference);
                if (kind == 1 || requestCount == 0) {
                    StatementLine u = line(reference, requestCount + 1 + random.nextInt(50), random.nextInt(100_000),
                            random.nextBoolean() ? "USD" : "CAD");
                    lines.add(u);
                    unknown.add(u);
                    continue;
                }
                PaymentRequest target = byId.get(1 + random.nextInt(requestCount));
                covered.add(target.getId());
                if (kind == 2) {
                    StatementLine c = line(reference, target.getId(),
                            random.nextBoolean() ? target.getAmountCents() : target.getAmountCents() + 1,
                            random.nextBoolean() ? "EUR" : "GBP");
                    lines.add(c);
                    currency.add(c);
                } else if (kind == 3) {
                    long delta = 1 + random.nextInt(500);
                    long cents = random.nextBoolean() || target.getAmountCents() <= delta
                            ? target.getAmountCents() + delta : target.getAmountCents() - delta;
                    StatementLine a = line(reference, target.getId(), cents, "USD");
                    lines.add(a);
                    amount.add(a);
                } else {
                    StatementLine m = line(reference, target.getId(), target.getAmountCents(), "USD");
                    lines.add(m);
                    matched.add(m);
                }
            }
            List<Integer> paidWithoutLine = new ArrayList<>();
            for (int id = 1; id <= requestCount; id++) {
                if (byId.get(id).getStatus() == PaymentStatus.PAID && !covered.contains(id)) {
                    paidWithoutLine.add(id);
                }
            }
            ReconciliationResult result = Reconciler.reconcile(requests, lines);
            String where = "seed " + seed + " (" + requestCount + " requests, " + lineCount + " lines)";
            assertEquals(sorted(matched), result.matched(), where + " matched");
            assertEquals(sorted(amount), result.amountMismatch(), where + " amountMismatch");
            assertEquals(sorted(currency), result.currencyMismatch(), where + " currencyMismatch");
            assertEquals(sorted(unknown), result.unknownReference(), where + " unknownReference");
            assertEquals(sorted(duplicate), result.duplicateLine(), where + " duplicateLine");
            assertEquals(paidWithoutLine, ids(result.paidWithoutLine()), where + " paidWithoutLine");
        }
    }

    // ---------------------------------------------------------------- PaymentReports

    @Nested
    @DisplayName("PaymentReports")
    class Reports {

        @Test
        @DisplayName("totalsByStatus has all five statuses with exact cents, including zeros")
        void totalsByStatus() {
            Map<PaymentStatus, Long> empty = PaymentReports.totalsByStatus(List.of());
            assertEquals(5, empty.size());
            for (PaymentStatus status : PaymentStatus.values()) {
                assertEquals(Long.valueOf(0L), empty.get(status), status.name());
            }
            Map<PaymentStatus, Long> expected = new EnumMap<>(PaymentStatus.class);
            expected.put(PaymentStatus.SUBMITTED, 9_999L);
            expected.put(PaymentStatus.APPROVED, 64_000L);
            expected.put(PaymentStatus.REJECTED, 0L);
            expected.put(PaymentStatus.CANCELLED, 0L);
            expected.put(PaymentStatus.PAID, 125_000L + 30_000L + 499_999L + 7_525L + 1_000L);
            assertEquals(expected, PaymentReports.totalsByStatus(fixtureRequests()));
        }

        @Test
        @DisplayName("totalsBySupplier sums exactly per supplier for one status")
        void totalsBySupplier() {
            Map<String, Long> paid = PaymentReports.totalsBySupplier(fixtureRequests(), PaymentStatus.PAID);
            assertEquals(Map.of("SUP-0101", 155_000L, "SUP-0202", 507_524L, "SUP-0303", 1_000L), paid);
            assertEquals(Map.of("SUP-0303", 64_000L), PaymentReports.totalsBySupplier(fixtureRequests(), PaymentStatus.APPROVED));
            assertTrue(PaymentReports.totalsBySupplier(fixtureRequests(), PaymentStatus.REJECTED).isEmpty());
            List<PaymentRequest> many = new ArrayList<>();
            long expectedTotal = 0;
            for (int i = 1; i <= 1000; i++) {
                PaymentRequest r = new PaymentRequest(i, "SUP-7777", "TEN-" + i, 10L, "USD", "REQ-0001");
                r.approve("APR-0003");
                many.add(r);
                expectedTotal += 10L;
            }
            assertEquals(Long.valueOf(expectedTotal), PaymentReports.totalsBySupplier(many, PaymentStatus.APPROVED).get("SUP-7777"));
        }

        @Test
        @DisplayName("largestOutstanding takes APPROVED only, largest first, ties by id, with n checks")
        void largestOutstanding() {
            List<PaymentRequest> requests = new ArrayList<>();
            long[] amounts = {50_000L, 90_000L, 50_000L, 90_000L, 10_000L};
            for (int i = 0; i < amounts.length; i++) {
                PaymentRequest r = new PaymentRequest(i + 1, "SUP-0001", "OUT-" + i, amounts[i], "USD", "REQ-0001");
                r.approve("APR-0003");
                requests.add(r);
            }
            requests.add(new PaymentRequest(6, "SUP-0001", "OUT-6", 999_000L, "USD", "REQ-0001"));
            PaymentRequest paid = new PaymentRequest(7, "SUP-0001", "OUT-7", 998_000L, "USD", "REQ-0001");
            paid.approve("APR-0003");
            paid.markPaid("APR-0002", "STL-7", 998_000L, "USD");
            requests.add(paid);
            Collections.reverse(requests);
            assertThrows(IllegalArgumentException.class, () -> PaymentReports.largestOutstanding(requests, -1));
            assertEquals(List.of(), ids(PaymentReports.largestOutstanding(requests, 0)));
            assertEquals(List.of(2, 4, 1), ids(PaymentReports.largestOutstanding(requests, 3)));
            assertEquals(List.of(2, 4, 1, 3, 5), ids(PaymentReports.largestOutstanding(requests, 5)));
            assertEquals(List.of(2, 4, 1, 3, 5), ids(PaymentReports.largestOutstanding(requests, 10)));
        }

        @Test
        @DisplayName("findByInvoice uses the normalised invoice key and returns Optional")
        void findByInvoice() {
            List<PaymentRequest> requests = fixtureRequests();
            Optional<PaymentRequest> found = PaymentReports.findByInvoice(requests, " sup-0202 ", "inv - b2");
            assertNotNull(found);
            assertTrue(found.isPresent(), "case and spaces must not matter");
            assertEquals(4, found.get().getId());
            assertEquals(Optional.empty(), PaymentReports.findByInvoice(requests, "SUP-0202", "INV-B9"));
            assertEquals(Optional.empty(), PaymentReports.findByInvoice(requests, "SUP-22", "INV-B2"));
            assertEquals(Optional.empty(), PaymentReports.findByInvoice(requests, "SUP-0202", "  "));
            assertEquals(Optional.empty(), PaymentReports.findByInvoice(List.of(), "SUP-0202", "INV-B2"));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static PaymentRequest paid(PaymentRequest r, String reference) {
        r.approve("APR-0003");
        r.markPaid("APR-0002", reference, r.getAmountCents(), "USD");
        return r;
    }

    static StatementLine line(String reference, int requestId, long cents, String currency) {
        return new StatementLine(reference, requestId, cents, currency);
    }

    static Path fixture(String name) throws URISyntaxException {
        return Path.of(ReconcilerReleaseTest.class.getResource("/payments/release/m6/statements/" + name).toURI());
    }

    private static List<StatementLine> sorted(List<StatementLine> lines) {
        List<StatementLine> copy = new ArrayList<>(lines);
        copy.sort(BY_REQUEST_THEN_REFERENCE);
        return copy;
    }

    private static List<Integer> ids(List<PaymentRequest> requests) {
        List<Integer> ids = new ArrayList<>();
        for (PaymentRequest r : requests) {
            ids.add(r.getId());
        }
        return ids;
    }

    private static List<Integer> lineIds(List<StatementLine> lines) {
        List<Integer> ids = new ArrayList<>();
        for (StatementLine l : lines) {
            ids.add(l.requestId());
        }
        return ids;
    }

    private static List<String> references(List<StatementLine> lines) {
        List<String> refs = new ArrayList<>();
        for (StatementLine l : lines) {
            refs.add(l.reference());
        }
        return refs;
    }

    private static void assertEachLineOnce(List<StatementLine> lines, ReconciliationResult result) {
        int classified = result.matched().size() + result.amountMismatch().size() + result.currencyMismatch().size()
                + result.unknownReference().size() + result.duplicateLine().size();
        assertEquals(lines.size(), classified, "every statement line is classified exactly once");
    }
}
