package payments.release.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import payments.domain.AllOfPolicy;
import payments.domain.ApprovalPolicy;
import payments.domain.Approver;
import payments.domain.NotOwnRequestPolicy;
import payments.domain.PaymentEvent;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.domain.Policies;
import payments.domain.WithinLimitPolicy;

/**
 * Release 3 public tests for the domain model (supplied; do not edit).
 * Run with {@code mvn -q test -Pm3}. All data is synthetic.
 */
class DomainReleaseTest {

    /** The allowed moves, exactly as the M3 brief states them. */
    private static final Set<String> ALLOWED = Set.of(
            "SUBMITTED>APPROVED", "SUBMITTED>REJECTED", "SUBMITTED>CANCELLED",
            "APPROVED>PAID", "APPROVED>CANCELLED");

    static Stream<Arguments> allStatusPairs() {
        List<Arguments> pairs = new ArrayList<>();
        for (PaymentStatus from : PaymentStatus.values()) {
            for (PaymentStatus to : PaymentStatus.values()) {
                pairs.add(Arguments.of(from, to, ALLOWED.contains(from + ">" + to)));
            }
        }
        return pairs.stream();
    }

    @ParameterizedTest(name = "{0} -> {1} allowed = {2}")
    @MethodSource("allStatusPairs")
    @DisplayName("canMoveTo matches the lifecycle table for all 25 status pairs")
    void lifecycleTable(PaymentStatus from, PaymentStatus to, boolean allowed) {
        assertEquals(allowed, from.canMoveTo(to), from + " -> " + to);
    }

    // ---------------------------------------------------------------- construction

    @Test
    @DisplayName("a new request is SUBMITTED, normalised and has one attributable event")
    void newRequest() {
        PaymentRequest r = new PaymentRequest(7, " sup-0042 ", "inv 7781", 123_450L, " usd ", "REQ-0001");
        assertEquals(7, r.getId());
        assertEquals("SUP-0042", r.getSupplierId());
        assertEquals("INV7781", r.getInvoiceNumber());
        assertEquals("SUP-0042|INV7781", r.getInvoiceKey());
        assertEquals(123_450L, r.getAmountCents());
        assertEquals("USD", r.getCurrency());
        assertEquals("REQ-0001", r.getRequestedBy());
        assertEquals(PaymentStatus.SUBMITTED, r.getStatus());
        assertEquals(List.of(new PaymentEvent(7, 1, "REQ-0001", "submitted", "1234.50")), r.events());
    }

    @ParameterizedTest(name = "rejects id={0} supplier={1} invoice={2} cents={3} currency={4} requester={5}")
    @CsvSource(delimiter = ';', nullValues = "NULL", value = {
        "0;SUP-0001;INV-1;100;USD;REQ-0001",
        "-1;SUP-0001;INV-1;100;USD;REQ-0001",
        "1;SUP-01;INV-1;100;USD;REQ-0001",
        "1;XYZ-0001;INV-1;100;USD;REQ-0001",
        "1;NULL;INV-1;100;USD;REQ-0001",
        "1;SUP-0001;'   ';100;USD;REQ-0001",
        "1;SUP-0001;NULL;100;USD;REQ-0001",
        "1;SUP-0001;INV-1;0;USD;REQ-0001",
        "1;SUP-0001;INV-1;-100;USD;REQ-0001",
        "1;SUP-0001;INV-1;1000000000;USD;REQ-0001",
        "1;SUP-0001;INV-1;100;EUR;REQ-0001",
        "1;SUP-0001;INV-1;100;US;REQ-0001",
        "1;SUP-0001;INV-1;100;NULL;REQ-0001",
        "1;SUP-0001;INV-1;100;USD;'  '",
        "1;SUP-0001;INV-1;100;USD;NULL",
    })
    @DisplayName("the constructor rejects every invalid field with IllegalArgumentException")
    void constructorRejects(int id, String supplier, String invoice, long cents, String currency, String requester) {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentRequest(id, supplier, invoice, cents, currency, requester));
    }

    @Test
    @DisplayName("amount boundaries 1 and 999999999 cents are accepted")
    void amountBoundaries() {
        assertEquals(1L, new PaymentRequest(1, "SUP-0001", "A", 1L, "USD", "REQ-0001").getAmountCents());
        assertEquals(999_999_999L,
                new PaymentRequest(2, "SUP-0001", "B", 999_999_999L, "USD", "REQ-0001").getAmountCents());
    }

    // ---------------------------------------------------------------- transitions

    @Test
    @DisplayName("approve, then markPaid with the exact amount and USD, records each actor")
    void happyPath() {
        PaymentRequest r = request(500_000L);
        r.approve("APR-0001");
        assertEquals(PaymentStatus.APPROVED, r.getStatus());
        r.markPaid("APR-0003", "STL-1001", 500_000L, "USD");
        assertEquals(PaymentStatus.PAID, r.getStatus());
        List<PaymentEvent> events = r.events();
        assertEquals(3, events.size());
        assertEvent(events.get(1), 2, "APR-0001", "approved", null);
        assertEvent(events.get(2), 3, "APR-0003", "paid", "STL-1001");
    }

    @Test
    @DisplayName("reject and cancel record the reason; cancel works from SUBMITTED and APPROVED")
    void rejectAndCancel() {
        PaymentRequest rejected = request(100L);
        rejected.reject("APR-0001", "invoice not recognised");
        assertEquals(PaymentStatus.REJECTED, rejected.getStatus());
        assertEvent(last(rejected), 2, "APR-0001", "rejected", "invoice not recognised");

        PaymentRequest cancelledEarly = request(100L);
        cancelledEarly.cancel("REQ-0001", "raised in error");
        assertEquals(PaymentStatus.CANCELLED, cancelledEarly.getStatus());
        assertEvent(last(cancelledEarly), 2, "REQ-0001", "cancelled", "raised in error");

        PaymentRequest cancelledLate = request(100L);
        cancelledLate.approve("APR-0001");
        cancelledLate.cancel("APR-0002", "supplier asked to hold");
        assertEquals(PaymentStatus.CANCELLED, cancelledLate.getStatus());
        assertEvent(last(cancelledLate), 3, "APR-0002", "cancelled", "supplier asked to hold");
    }

    @Test
    @DisplayName("every invalid move throws and leaves status and history unchanged")
    void invalidMovesChangeNothing() {
        for (PaymentStatus start : PaymentStatus.values()) {
            for (String move : List.of("approve", "reject", "cancel", "markPaid")) {
                PaymentRequest r = requestIn(start);
                PaymentStatus target = targetOf(move);
                if (ALLOWED.contains(start + ">" + target)) {
                    continue;
                }
                List<PaymentEvent> before = r.events();
                assertRefused(() -> apply(r, move), start + " " + move);
                assertEquals(start, r.getStatus(), start + " " + move + " must not change the status");
                assertEquals(before, r.events(), start + " " + move + " must not add events");
            }
        }
    }

    @Test
    @DisplayName("markPaid with a different amount or currency throws and changes nothing")
    void settlementMustMatch() {
        for (Object[] attempt : new Object[][] {
                {499_999L, "USD"}, {500_001L, "USD"}, {0L, "USD"}, {500_000L, "EUR"}, {500_000L, null}}) {
            PaymentRequest r = request(500_000L);
            r.approve("APR-0001");
            List<PaymentEvent> before = r.events();
            assertRefused(() -> r.markPaid("APR-0003", "STL-2001", (Long) attempt[0], (String) attempt[1]),
                    "settled " + attempt[0] + " " + attempt[1]);
            assertEquals(PaymentStatus.APPROVED, r.getStatus());
            assertEquals(before, r.events());
        }
    }

    // ---------------------------------------------------------------- corrections and history

    @Test
    @DisplayName("correctAmount while SUBMITTED records who changed it, old and new amounts and why")
    void correctAmount() {
        PaymentRequest r = request(123_450L);
        r.correctAmount("REQ-0001", 120_000L, "credit note applied");
        assertEquals(120_000L, r.getAmountCents());
        assertEquals(PaymentStatus.SUBMITTED, r.getStatus());
        PaymentEvent event = last(r);
        assertEquals(2, event.seq());
        assertEquals("REQ-0001", event.actorId());
        assertEquals("amount-corrected", event.action());
        assertTrue(event.detail().equals("1234.50->1200.00: credit note applied")
                        || event.detail().equals("123450->120000: credit note applied"),
                "detail must be \"old->new: reason\", got: " + event.detail());
    }

    @Test
    @DisplayName("correctAmount is refused outside SUBMITTED, for invalid cents and blank reasons")
    void correctAmountRefusals() {
        PaymentRequest approved = request(10_000L);
        approved.approve("APR-0001");
        List<PaymentEvent> before = approved.events();
        assertRefused(() -> approved.correctAmount("REQ-0001", 9_000L, "late change"), "after approval");
        assertEquals(10_000L, approved.getAmountCents());
        assertEquals(before, approved.events());

        for (long cents : new long[] {0L, -1L, 1_000_000_000L}) {
            PaymentRequest r = request(10_000L);
            assertRefused(() -> r.correctAmount("REQ-0001", cents, "typo"), "cents " + cents);
            assertEquals(10_000L, r.getAmountCents());
            assertEquals(1, r.events().size());
        }
        for (String reason : new String[] {"", "   ", null}) {
            PaymentRequest r = request(10_000L);
            assertRefused(() -> r.correctAmount("REQ-0001", 9_000L, reason), "reason '" + reason + "'");
            assertEquals(10_000L, r.getAmountCents());
            assertEquals(1, r.events().size());
        }
    }

    @Test
    @DisplayName("recordApprovalRefused adds an attributable event without changing the status")
    void refusalIsRecorded() {
        PaymentRequest r = request(10_000L);
        r.recordApprovalRefused("APR-0001", "within-limit");
        assertEquals(PaymentStatus.SUBMITTED, r.getStatus());
        assertEquals(new PaymentEvent(r.getId(), 2, "APR-0001", "approval-refused", "within-limit"), last(r));
    }

    @Test
    @DisplayName("events() is an unmodifiable copy")
    void eventsAreAnUnmodifiableCopy() {
        PaymentRequest r = request(10_000L);
        List<PaymentEvent> snapshot = r.events();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new PaymentEvent(r.getId(), 99, "X", "forged", "")));
        r.approve("APR-0001");
        assertEquals(1, snapshot.size(), "a list already returned must not grow when history grows");
        assertEquals(2, r.events().size());
    }

    @Test
    @DisplayName("PaymentRequest is encapsulated: private fields and no setters")
    void encapsulation() {
        for (Field field : PaymentRequest.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(Modifier.isPrivate(field.getModifiers()), "field " + field.getName() + " must be private");
            }
        }
        for (Method method : PaymentRequest.class.getMethods()) {
            assertFalse(method.getName().startsWith("set"), "no setters allowed: " + method.getName());
        }
    }

    // ---------------------------------------------------------------- policies

    @Test
    @DisplayName("WithinLimitPolicy allows the exact limit and refuses one cent over")
    void withinLimit() {
        ApprovalPolicy policy = new WithinLimitPolicy();
        Approver approver = new Approver("APR-0001", 500_000L);
        assertTrue(policy.allows(approver, request(499_999L)));
        assertTrue(policy.allows(approver, request(500_000L)), "equal to the limit is allowed");
        assertFalse(policy.allows(approver, request(500_001L)), "one cent over is refused");
        assertEquals("within-limit", policy.name());
    }

    @Test
    @DisplayName("NotOwnRequestPolicy refuses the requester, ignoring case")
    void notOwnRequest() {
        ApprovalPolicy policy = new NotOwnRequestPolicy();
        PaymentRequest own = new PaymentRequest(3, "SUP-0001", "OWN-1", 100L, "USD", "apr-0002");
        assertFalse(policy.allows(new Approver("APR-0002", 999_999_999L), own));
        assertFalse(policy.allows(new Approver("Apr-0002", 999_999_999L), own));
        assertTrue(policy.allows(new Approver("APR-0001", 999_999_999L), own));
        assertEquals("not-own-request", policy.name());
    }

    @Test
    @DisplayName("Policies.named builds limit-only and the composed standard policy")
    void policyFactory() {
        assertTrue(Policies.named("limit-only") instanceof WithinLimitPolicy);
        ApprovalPolicy standard = Policies.named("standard");
        assertTrue(standard instanceof AllOfPolicy, "standard must be an AllOfPolicy");
        assertEquals("standard", standard.name());
        PaymentRequest small = new PaymentRequest(4, "SUP-0001", "S-1", 10_000L, "USD", "REQ-0001");
        PaymentRequest big = new PaymentRequest(5, "SUP-0001", "S-2", 600_000L, "USD", "REQ-0001");
        PaymentRequest own = new PaymentRequest(6, "SUP-0001", "S-3", 10_000L, "USD", "APR-0001");
        Approver approver = new Approver("APR-0001", 500_000L);
        assertTrue(standard.allows(approver, small));
        assertFalse(standard.allows(approver, big), "standard includes the limit control");
        assertFalse(standard.allows(approver, own), "standard includes the own-request control");
        assertTrue(Policies.named("limit-only").allows(approver, own), "limit-only ignores ownership");
        for (String name : new String[] {"", "unknown", "strict", "limit_only"}) {
            assertThrows(IllegalArgumentException.class, () -> Policies.named(name), "name '" + name + "'");
        }
        assertSame(AllOfPolicy.class, standard.getClass());
    }

    // ---------------------------------------------------------------- helpers

    private static int nextId = 100;

    private static PaymentRequest request(long cents) {
        nextId++;
        return new PaymentRequest(nextId, "SUP-0001", "INV-" + nextId, cents, "USD", "REQ-0001");
    }

    private static PaymentRequest requestIn(PaymentStatus status) {
        PaymentRequest r = request(10_000L);
        switch (status) {
            case SUBMITTED -> { }
            case APPROVED -> r.approve("APR-0001");
            case REJECTED -> r.reject("APR-0001", "no");
            case CANCELLED -> r.cancel("REQ-0001", "no");
            case PAID -> {
                r.approve("APR-0001");
                r.markPaid("APR-0003", "STL-" + r.getId(), 10_000L, "USD");
            }
            default -> fail("unexpected status " + status);
        }
        assertEquals(status, r.getStatus());
        return r;
    }

    private static PaymentStatus targetOf(String move) {
        return switch (move) {
            case "approve" -> PaymentStatus.APPROVED;
            case "reject" -> PaymentStatus.REJECTED;
            case "cancel" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PAID;
        };
    }

    private static void apply(PaymentRequest r, String move) {
        switch (move) {
            case "approve" -> r.approve("APR-0002");
            case "reject" -> r.reject("APR-0002", "late");
            case "cancel" -> r.cancel("APR-0002", "late");
            default -> r.markPaid("APR-0003", "STL-X" + r.getId(), r.getAmountCents(), "USD");
        }
    }

    private static PaymentEvent last(PaymentRequest r) {
        List<PaymentEvent> events = r.events();
        return events.get(events.size() - 1);
    }

    private static void assertEvent(PaymentEvent event, int seq, String actor, String action, String detail) {
        assertEquals(seq, event.seq(), "seq");
        assertEquals(actor, event.actorId(), "actor");
        assertEquals(action, event.action(), "action");
        if (detail != null) {
            assertEquals(detail, event.detail(), "detail");
        }
    }

    /** A refusal must be an unchecked exception other than the starter's TODO marker. */
    static void assertRefused(Executable call, String what) {
        try {
            call.execute();
        } catch (UnsupportedOperationException todo) {
            fail(what + ": still a TODO stub (" + todo.getMessage() + ")");
        } catch (RuntimeException expected) {
            return;
        } catch (Throwable other) {
            fail(what + ": expected an unchecked exception but got " + other);
        }
        fail(what + ": expected the call to be refused with an unchecked exception");
    }
}
