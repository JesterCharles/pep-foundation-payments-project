package payments.domain;

import java.util.List;

/**
 * One request to pay one supplier invoice, with its attributable history.
 *
 * <p>LEARNER-OWNED (milestone M3; {@link #restore} is added in M5). Keep every public
 * signature. Rules:
 * <ul>
 *   <li>All state is private. There are no setters: state changes only through the
 *       transition methods below, and each change appends a {@link PaymentEvent}.</li>
 *   <li>Check every rule BEFORE changing anything. A refused call throws and leaves the
 *       status, amount and events exactly as they were.</li>
 *   <li>Money stays in long cents. Reuse {@code payments.intake.PaymentRules} for the
 *       invoice key and for formatting amounts in event details.</li>
 *   <li>Events are numbered seq 1, 2, 3, ... per request, and carry this request's id.</li>
 * </ul>
 *
 * <p>Event actions and details used by the released tests:
 * <pre>
 * action             detail
 * "submitted"        PaymentRules.formatCents(amountCents), e.g. "1234.50"
 * "approved"         PaymentRules.formatCents(amountCents)
 * "rejected"         the reason
 * "cancelled"        the reason
 * "paid"             the settlement reference
 * "amount-corrected" "old->new: reason", e.g. "1234.50->1200.00: credit note applied"
 * "approval-refused" the refusing policy's name()
 * </pre>
 *
 * <p>Test command for M3: {@code mvn -q test -Pm3}
 */
public final class PaymentRequest {

    // TODO(M3): declare private fields for the id, supplier id, invoice number, amount in
    //   cents, currency, requester, status and the list of events.

    /**
     * Creates a SUBMITTED request with one event (requestedBy, "submitted", formatted amount).
     *
     * @throws IllegalArgumentException when id <= 0; when PaymentRules.invoiceKey(supplierId,
     *         invoiceNumber) is ""; when amountCents is outside 1..999999999; when the currency
     *         is not exactly "USD" after trimming and upper-casing; when requestedBy is blank
     */
    public PaymentRequest(int id, String supplierId, String invoiceNumber, long amountCents,
                          String currency, String requestedBy) {
        // TODO(M3): validate every argument first (IllegalArgumentException), then store:
        //   supplier id trimmed and upper-cased; invoice number in its normalised form (the
        //   part of the invoice key after '|'); currency as "USD"; status SUBMITTED; and the
        //   first event (requestedBy, "submitted", formatted amount).
        throw new UnsupportedOperationException("TODO M3: PaymentRequest constructor");
    }

    /**
     * Rebuilds a request that was already stored, for example by a JDBC repository, without
     * re-running the submission or adding events.
     */
    public static PaymentRequest restore(int id, String supplierId, String invoiceNumber, long amountCents,
                                         String currency, String requestedBy, PaymentStatus status,
                                         List<PaymentEvent> events) {
        // TODO(M5): implement a static factory that validates like the constructor and then
        //   sets the stored status and events exactly as given (a private constructor helps).
        throw new UnsupportedOperationException("TODO M5: PaymentRequest.restore");
    }

    public int getId() {
        // TODO(M3): return the id.
        throw new UnsupportedOperationException("TODO M3: getId");
    }

    /** The supplier id, trimmed and upper-cased, e.g. "SUP-0042". */
    public String getSupplierId() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getSupplierId");
    }

    /** The normalised invoice number (upper-cased, spaces removed), e.g. "INV7781". */
    public String getInvoiceNumber() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getInvoiceNumber");
    }

    /** The duplicate-detection key from PaymentRules.invoiceKey, e.g. "SUP-0042|INV7781". */
    public String getInvoiceKey() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getInvoiceKey");
    }

    public long getAmountCents() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getAmountCents");
    }

    /** Always "USD". */
    public String getCurrency() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getCurrency");
    }

    public String getRequestedBy() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getRequestedBy");
    }

    public PaymentStatus getStatus() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: getStatus");
    }

    /** An unmodifiable COPY of the history: later events must not appear in a list already returned. */
    public List<PaymentEvent> events() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: events");
    }

    /** SUBMITTED to APPROVED; records (actorId, "approved", formatted amount). */
    public void approve(String actorId) {
        // TODO(M3): refuse any move PaymentStatus.canMoveTo does not allow with an unchecked
        //   exception (IllegalStateException recommended) and change nothing.
        throw new UnsupportedOperationException("TODO M3: approve");
    }

    /** SUBMITTED to REJECTED; records (actorId, "rejected", reason). */
    public void reject(String actorId, String reason) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: reject");
    }

    /** SUBMITTED or APPROVED to CANCELLED; records (actorId, "cancelled", reason). */
    public void cancel(String actorId, String reason) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: cancel");
    }

    /**
     * APPROVED to PAID; records (actorId, "paid", settlementRef). Allowed only when
     * settledCents equals the amount exactly and settledCurrency is "USD"; otherwise throw an
     * unchecked exception and change nothing.
     */
    public void markPaid(String actorId, String settlementRef, long settledCents, String settledCurrency) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: markPaid");
    }

    /**
     * Corrects the amount while SUBMITTED only. newCents must be in 1..999999999 and the reason
     * non-blank. Records (actorId, "amount-corrected", "old->new: reason") using formatted amounts.
     */
    public void correctAmount(String actorId, long newCents, String reason) {
        // TODO(M3): check status, amount and reason first; throw and change nothing if any fails.
        throw new UnsupportedOperationException("TODO M3: correctAmount");
    }

    /**
     * Records a refused approval: (approverId, "approval-refused", policyName). The status does
     * not change. Called by PaymentDesk when its ApprovalPolicy refuses.
     */
    public void recordApprovalRefused(String approverId, String policyName) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: recordApprovalRefused");
    }
}
