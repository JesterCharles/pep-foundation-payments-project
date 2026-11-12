package payments.release.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static payments.release.m3.DomainReleaseTest.assertRefused;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import payments.desk.PaymentDesk;
import payments.domain.ApprovalPolicy;
import payments.domain.Approver;
import payments.domain.PaymentEvent;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.domain.Policies;
import payments.identity.InMemoryActorDirectory;

/**
 * Release 3 public tests for the payment desk (supplied; do not edit).
 * Run with {@code mvn -q test -Pm3}. All actors, suppliers and amounts are synthetic.
 */
class DeskReleaseTest {

    private static final Set<String> ALLOWED = Set.of(
            "SUBMITTED>APPROVED", "SUBMITTED>REJECTED", "SUBMITTED>CANCELLED",
            "APPROVED>PAID", "APPROVED>CANCELLED");

    private static InMemoryActorDirectory directory() {
        return new InMemoryActorDirectory()
                .addRequester("REQ-0001")
                .addRequester("REQ-0002")
                .addApprover("APR-0001", 500_000L)
                .addApprover("APR-0002", 2_500_000L)
                .addApprover("APR-0003", 999_999_999L);
    }

    private static PaymentDesk standardDesk() {
        return PaymentDesk.create(directory(), Policies.named("standard"));
    }

    @Test
    @DisplayName("submit parses the amount and hands out ids 1, 2, 3")
    void submitAssignsIds() {
        PaymentDesk desk = standardDesk();
        PaymentRequest first = desk.submit("SUP-0001", "INV-100", "1234.50", "USD", "REQ-0001");
        PaymentRequest second = desk.submit("sup-0002", "inv 200", "0.01", " usd ", "REQ-0002");
        PaymentRequest third = desk.submit("SUP-0001", "INV-101", "9999999.99", "USD", "REQ-0001");
        assertEquals(List.of(1, 2, 3), List.of(first.getId(), second.getId(), third.getId()));
        assertEquals(123_450L, first.getAmountCents());
        assertEquals(1L, second.getAmountCents());
        assertEquals("SUP-0002|INV200", second.getInvoiceKey());
        assertEquals(PaymentStatus.SUBMITTED, third.getStatus());
        assertEquals("REQ-0001", first.events().get(0).actorId());
        assertEquals(first.getAmountCents(), desk.find(1).getAmountCents());
        assertNull(desk.find(4));
        assertNull(desk.find(0));
        assertEquals(3, desk.all().size());
    }

    @Test
    @DisplayName("an invalid amount text or supplier is refused with IllegalArgumentException")
    void submitRejectsInvalidInput() {
        PaymentDesk desk = standardDesk();
        for (String amount : new String[] {"12.5", "abc", "0.00", "-1.00", "1,000.00", "10000000.00", null}) {
            assertThrows(IllegalArgumentException.class,
                    () -> desk.submit("SUP-0001", "INV-1", amount, "USD", "REQ-0001"), "amount " + amount);
        }
        assertThrows(IllegalArgumentException.class, () -> desk.submit("SUP-1", "INV-1", "1.00", "USD", "REQ-0001"));
        assertThrows(IllegalArgumentException.class, () -> desk.submit("SUP-0001", "INV-1", "1.00", "EUR", "REQ-0001"));
        assertTrue(desk.all().isEmpty(), "refused submissions are not stored");
    }

    @Test
    @DisplayName("a reused invoice key is refused with case and space variety, even after reject or cancel")
    void duplicateInvoiceRefused() {
        PaymentDesk desk = standardDesk();
        PaymentRequest original = desk.submit("SUP-0001", "INV 500", "100.00", "USD", "REQ-0001");
        assertRefused(() -> desk.submit("sup-0001", " inv500 ", "100.00", "USD", "REQ-0002"), "same key");
        assertRefused(() -> desk.submit("SUP-0001", "I N V 5 0 0", "1.00", "USD", "REQ-0001"), "spaced key");
        desk.reject(original.getId(), "APR-0001", "wrong supplier");
        assertRefused(() -> desk.submit("SUP-0001", "INV500", "100.00", "USD", "REQ-0001"), "after reject");
        PaymentRequest other = desk.submit("SUP-0001", "INV-600", "5.00", "USD", "REQ-0001");
        desk.cancel(other.getId(), "REQ-0001", "raised twice");
        assertRefused(() -> desk.submit("SUP-0001", "inv-600", "5.00", "USD", "REQ-0001"), "after cancel");
        assertEquals(2, desk.all().size(), "duplicates are never stored");
        assertNotNull(desk.submit("SUP-0002", "INV500", "100.00", "USD", "REQ-0001"),
                "the same invoice number for another supplier is a different key");
    }

    @Test
    @DisplayName("approval at the limit succeeds; one cent over is refused, recorded and leaves status unchanged")
    void approvalLimitBoundary() {
        PaymentDesk desk = standardDesk();
        PaymentRequest atLimit = desk.submit("SUP-0001", "L-1", "5000.00", "USD", "REQ-0001");
        PaymentRequest over = desk.submit("SUP-0001", "L-2", "5000.01", "USD", "REQ-0001");
        desk.approve(atLimit.getId(), "APR-0001");
        assertEquals(PaymentStatus.APPROVED, desk.find(atLimit.getId()).getStatus());
        assertRefused(() -> desk.approve(over.getId(), "APR-0001"), "one cent over the limit");
        PaymentRequest after = desk.find(over.getId());
        assertEquals(PaymentStatus.SUBMITTED, after.getStatus());
        assertEquals(new PaymentEvent(over.getId(), 2, "APR-0001", "approval-refused", "standard"), lastEvent(after));
        desk.approve(over.getId(), "APR-0002");
        assertEquals(PaymentStatus.APPROVED, desk.find(over.getId()).getStatus());
    }

    @Test
    @DisplayName("nobody approves their own request (case-insensitive), and the refusal is recorded")
    void ownRequestRefused() {
        PaymentDesk desk = standardDesk();
        PaymentRequest own = desk.submit("SUP-0003", "OWN-1", "10.00", "USD", "apr-0002");
        assertRefused(() -> desk.approve(own.getId(), "APR-0002"), "own request");
        PaymentRequest after = desk.find(own.getId());
        assertEquals(PaymentStatus.SUBMITTED, after.getStatus());
        assertEquals("approval-refused", lastEvent(after).action());
        assertEquals("APR-0002", lastEvent(after).actorId());
        desk.approve(own.getId(), "APR-0001");
        assertEquals(PaymentStatus.APPROVED, desk.find(own.getId()).getStatus());
    }

    @Test
    @DisplayName("the desk asks its injected ApprovalPolicy and records that policy's name")
    void deskUsesTheInjectedPolicy() {
        PaymentDesk limitOnly = PaymentDesk.create(directory(), Policies.named("limit-only"));
        PaymentRequest own = limitOnly.submit("SUP-0003", "OWN-2", "10.00", "USD", "APR-0002");
        limitOnly.approve(own.getId(), "APR-0002");
        assertEquals(PaymentStatus.APPROVED, limitOnly.find(own.getId()).getStatus(), "limit-only ignores ownership");

        ApprovalPolicy refuseAll = new ApprovalPolicy() {
            @Override
            public boolean allows(Approver approver, PaymentRequest request) {
                return false;
            }

            @Override
            public String name() {
                return "test-refuse-all";
            }
        };
        PaymentDesk strict = PaymentDesk.create(directory(), refuseAll);
        PaymentRequest small = strict.submit("SUP-0004", "P-1", "1.00", "USD", "REQ-0001");
        assertRefused(() -> strict.approve(small.getId(), "APR-0003"), "refuse-all policy");
        assertEquals("test-refuse-all", lastEvent(strict.find(small.getId())).detail());

        ApprovalPolicy allowAll = new ApprovalPolicy() {
            @Override
            public boolean allows(Approver approver, PaymentRequest request) {
                return true;
            }

            @Override
            public String name() {
                return "test-allow-all";
            }
        };
        PaymentDesk lenient = PaymentDesk.create(directory(), allowAll);
        PaymentRequest big = lenient.submit("SUP-0004", "P-2", "9000.00", "USD", "REQ-0001");
        lenient.approve(big.getId(), "APR-0001");
        assertEquals(PaymentStatus.APPROVED, lenient.find(big.getId()).getStatus(),
                "limits are the policy's job, not hard-coded in the desk");
    }

    @Test
    @DisplayName("unknown ids and unknown approvers give IllegalArgumentException and change nothing")
    void unknownIdsAndApprovers() {
        PaymentDesk desk = standardDesk();
        PaymentRequest r = desk.submit("SUP-0001", "U-1", "10.00", "USD", "REQ-0001");
        assertThrows(IllegalArgumentException.class, () -> desk.approve(99, "APR-0001"));
        assertThrows(IllegalArgumentException.class, () -> desk.reject(99, "APR-0001", "x"));
        assertThrows(IllegalArgumentException.class, () -> desk.cancel(99, "APR-0001", "x"));
        assertThrows(IllegalArgumentException.class, () -> desk.markPaid(99, "APR-0003", "STL-1", 1000L, "USD"));
        assertThrows(IllegalArgumentException.class, () -> desk.correctAmount(99, "REQ-0001", 1000L, "x"));
        for (String approver : new String[] {"APR-9999", "REQ-0001", "", null}) {
            assertThrows(IllegalArgumentException.class, () -> desk.approve(r.getId(), approver), "approver " + approver);
        }
        assertEquals(PaymentStatus.SUBMITTED, desk.find(r.getId()).getStatus());
        assertEquals(1, desk.find(r.getId()).events().size(), "no event for an unknown approver");
    }

    @Test
    @DisplayName("reject, cancel, markPaid and correctAmount delegate; approvedUnpaidCents sums APPROVED only")
    void delegationAndTotals() {
        PaymentDesk desk = standardDesk();
        PaymentRequest a = desk.submit("SUP-0001", "T-1", "100.00", "USD", "REQ-0001");
        PaymentRequest b = desk.submit("SUP-0001", "T-2", "250.25", "USD", "REQ-0001");
        PaymentRequest c = desk.submit("SUP-0001", "T-3", "40.00", "USD", "REQ-0002");
        PaymentRequest d = desk.submit("SUP-0001", "T-4", "7.00", "USD", "REQ-0002");
        assertEquals(0L, desk.approvedUnpaidCents());
        desk.correctAmount(c.getId(), "REQ-0002", 3_500L, "discount");
        desk.approve(a.getId(), "APR-0001");
        desk.approve(b.getId(), "APR-0001");
        desk.approve(c.getId(), "APR-0001");
        assertEquals(10_000L + 25_025L + 3_500L, desk.approvedUnpaidCents());
        assertRefused(() -> desk.markPaid(a.getId(), "APR-0003", "STL-9", 9_999L, "USD"), "settled amount differs");
        assertEquals(PaymentStatus.APPROVED, desk.find(a.getId()).getStatus());
        desk.markPaid(a.getId(), "APR-0003", "STL-10", 10_000L, "USD");
        desk.cancel(b.getId(), "APR-0002", "supplier merged");
        desk.reject(d.getId(), "APR-0001", "no purchase order");
        assertEquals(3_500L, desk.approvedUnpaidCents());
        assertEquals(PaymentStatus.PAID, desk.find(a.getId()).getStatus());
        assertEquals("paid", lastEvent(desk.find(a.getId())).action());
        assertEquals(PaymentStatus.CANCELLED, desk.find(b.getId()).getStatus());
        assertEquals(PaymentStatus.REJECTED, desk.find(d.getId()).getStatus());
        assertEquals("amount-corrected", desk.find(c.getId()).events().get(1).action());
        List<PaymentRequest> all = desk.all();
        assertEquals(List.of(1, 2, 3, 4), List.of(all.get(0).getId(), all.get(1).getId(), all.get(2).getId(),
                all.get(3).getId()));
    }

    @Test
    @DisplayName("a fixed scripted sequence gives hand-checked statuses, amounts, totals, history and actors")
    void scriptedSequence() {
        PaymentDesk desk = standardDesk();
        int a = desk.submit("SUP-0001", "S-1", "4999.99", "USD", "REQ-0001").getId();
        int b = desk.submit("SUP-0001", "S-2", "5000.01", "USD", "REQ-0002").getId();
        int c = desk.submit("sup-0002", "s 3", "25000.00", "USD", "APR-0002").getId();
        int d = desk.submit("SUP-0002", "S-4", "100.00", "USD", "REQ-0001").getId();
        assertRefused(() -> desk.submit("sup-0001", " s-1 ", "1.00", "USD", "REQ-0002"), "duplicate of S-1");

        desk.approve(a, "APR-0001");
        assertRefused(() -> desk.approve(b, "APR-0001"), "one cent over APR-0001's limit");
        desk.approve(b, "APR-0002");
        assertRefused(() -> desk.approve(c, "APR-0002"), "own request");
        desk.approve(c, "APR-0003");
        assertEquals(3_500_000L, desk.approvedUnpaidCents(), "after approvals");

        assertRefused(() -> desk.markPaid(b, "APR-0003", "STL-9001", 500_002L, "USD"), "settled amount differs");
        assertEquals(PaymentStatus.APPROVED, desk.find(b).getStatus());
        desk.markPaid(b, "APR-0003", "STL-9001", 500_001L, "USD");
        assertEquals(2_999_999L, desk.approvedUnpaidCents(), "after paying b");
        desk.cancel(a, "REQ-0001", "supplier hold");
        assertEquals(2_500_000L, desk.approvedUnpaidCents(), "after cancelling a");

        assertRefused(() -> desk.correctAmount(c, "REQ-0001", 1_000L, "too late"), "correction after approval");
        assertRefused(() -> desk.correctAmount(d, "REQ-0001", 0L, "zero"), "zero correction");
        desk.correctAmount(d, "REQ-0001", 25_000L, "invoice says 250.00");
        desk.reject(d, "APR-0001", "duplicate of S-1");
        assertRefused(() -> desk.approve(d, "APR-0003"), "approve after reject");
        assertRefused(() -> desk.cancel(b, "APR-0003", "too late"), "cancel after paid");

        assertEquals(List.of(PaymentStatus.CANCELLED, PaymentStatus.PAID, PaymentStatus.APPROVED, PaymentStatus.REJECTED),
                List.of(desk.find(a).getStatus(), desk.find(b).getStatus(), desk.find(c).getStatus(), desk.find(d).getStatus()));
        assertEquals(List.of(499_999L, 500_001L, 2_500_000L, 25_000L), List.of(desk.find(a).getAmountCents(),
                desk.find(b).getAmountCents(), desk.find(c).getAmountCents(), desk.find(d).getAmountCents()));
        assertEquals(List.of(3, 4, 3, 3), List.of(desk.find(a).events().size(), desk.find(b).events().size(),
                desk.find(c).events().size(), desk.find(d).events().size()));
        assertEquals("approval-refused", desk.find(b).events().get(1).action());
        assertEquals("approval-refused", desk.find(c).events().get(1).action());
        assertEquals(List.of("REQ-0001", "APR-0003", "APR-0003", "APR-0001"), List.of(lastEvent(desk.find(a)).actorId(),
                lastEvent(desk.find(b)).actorId(), lastEvent(desk.find(c)).actorId(), lastEvent(desk.find(d)).actorId()));
        assertEquals(2_500_000L, desk.approvedUnpaidCents(), "final approved-unpaid total");
    }

    // ---------------------------------------------------------------- helpers

    private static PaymentEvent lastEvent(PaymentRequest r) {
        List<PaymentEvent> events = r.events();
        return events.get(events.size() - 1);
    }
}
