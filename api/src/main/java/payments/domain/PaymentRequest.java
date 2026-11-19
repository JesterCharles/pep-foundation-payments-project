package payments.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import payments.intake.PaymentRules;

/**
 * One request to pay one supplier invoice, with its attributable history.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). Rules:
 * <ul>
 *   <li>All state is private. There are no setters: state changes only through the transition
 *       methods below, and each change appends a {@link PaymentEvent}.</li>
 *   <li>Every rule is checked BEFORE anything changes. A refused call throws and leaves the
 *       status, amount and events exactly as they were.</li>
 *   <li>Money stays in long cents; amounts in event details are formatted with
 *       {@link PaymentRules#formatCents(long)}.</li>
 *   <li>Events are numbered seq 1, 2, 3, ... per request and carry this request's id.</li>
 * </ul>
 *
 * <pre>
 * action             detail
 * "submitted"        formatted amount, e.g. "1234.50"
 * "approved"         formatted amount
 * "rejected"         the reason
 * "cancelled"        the reason
 * "paid"             the settlement reference
 * "amount-corrected" "old->new: reason", e.g. "1234.50->1200.00: credit note applied"
 * "approval-refused" the refusing policy's name()
 * </pre>
 */
public final class PaymentRequest {

    private final int id;
    private final String supplierId;
    private final String invoiceNumber;
    private long amountCents;
    private final String currency;
    private final String requestedBy;
    private PaymentStatus status;
    private final List<PaymentEvent> events;

    /**
     * Creates a SUBMITTED request with one event (requestedBy, "submitted", formatted amount).
     *
     * @throws IllegalArgumentException when id <= 0; when the supplier id or invoice number is
     *         invalid; when amountCents is outside 1..999999999; when the currency is not "USD"
     *         (after trimming and upper-casing); when requestedBy is blank
     */
    public PaymentRequest(int id, String supplierId, String invoiceNumber, long amountCents,
                          String currency, String requestedBy) {
        this(id, supplierId, invoiceNumber, amountCents, currency, requestedBy, PaymentStatus.SUBMITTED,
                List.of());
        events.add(new PaymentEvent(id, 1, this.requestedBy, "submitted", PaymentRules.formatCents(amountCents)));
    }

    private PaymentRequest(int id, String supplierId, String invoiceNumber, long amountCents, String currency,
                           String requestedBy, PaymentStatus status, List<PaymentEvent> storedEvents) {
        if (id <= 0) {
            throw new IllegalArgumentException("request id must be positive");
        }
        String key = PaymentRules.invoiceKey(supplierId, invoiceNumber);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("supplier id must be SUP- and four digits, invoice number non-blank");
        }
        checkAmount(amountCents);
        if (currency == null || !currency.trim().toUpperCase(Locale.ROOT).equals("USD")) {
            throw new IllegalArgumentException("currency must be USD");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("requestedBy must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.id = id;
        this.supplierId = key.substring(0, key.indexOf('|'));
        this.invoiceNumber = key.substring(key.indexOf('|') + 1);
        this.amountCents = amountCents;
        this.currency = "USD";
        this.requestedBy = requestedBy.trim();
        this.status = status;
        this.events = new ArrayList<>(storedEvents == null ? List.of() : storedEvents);
    }

    /**
     * Rebuilds a request that was already stored (for example by a JDBC repository) without
     * re-running the submission or adding events.
     */
    public static PaymentRequest restore(int id, String supplierId, String invoiceNumber, long amountCents,
                                         String currency, String requestedBy, PaymentStatus status,
                                         List<PaymentEvent> events) {
        return new PaymentRequest(id, supplierId, invoiceNumber, amountCents, currency, requestedBy, status,
                events);
    }

    public int getId() {
        return id;
    }

    /** The supplier id, trimmed and upper-cased, e.g. "SUP-0042". */
    public String getSupplierId() {
        return supplierId;
    }

    /** The normalised invoice number (upper-cased, spaces removed), e.g. "INV7781". */
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    /** The duplicate-detection key from PaymentRules.invoiceKey, e.g. "SUP-0042|INV7781". */
    public String getInvoiceKey() {
        return supplierId + "|" + invoiceNumber;
    }

    public long getAmountCents() {
        return amountCents;
    }

    /** Always "USD". */
    public String getCurrency() {
        return currency;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    /** An unmodifiable COPY of the history: later events never appear in a list already returned. */
    public List<PaymentEvent> events() {
        return List.copyOf(events);
    }

    /** SUBMITTED to APPROVED; records (actorId, "approved", formatted amount). */
    public void approve(String actorId) {
        String actor = actor(actorId);
        moveTo(PaymentStatus.APPROVED);
        record(actor, "approved", PaymentRules.formatCents(amountCents));
    }

    /** SUBMITTED to REJECTED; records (actorId, "rejected", reason). */
    public void reject(String actorId, String reason) {
        String actor = actor(actorId);
        String why = reason(reason);
        moveTo(PaymentStatus.REJECTED);
        record(actor, "rejected", why);
    }

    /** SUBMITTED or APPROVED to CANCELLED; records (actorId, "cancelled", reason). */
    public void cancel(String actorId, String reason) {
        String actor = actor(actorId);
        String why = reason(reason);
        moveTo(PaymentStatus.CANCELLED);
        record(actor, "cancelled", why);
    }

    /**
     * APPROVED to PAID; records (actorId, "paid", settlementRef). Allowed only when settledCents
     * equals the amount exactly and settledCurrency is "USD".
     */
    public void markPaid(String actorId, String settlementRef, long settledCents, String settledCurrency) {
        String actor = actor(actorId);
        if (settlementRef == null || settlementRef.isBlank()) {
            throw new IllegalArgumentException("settlement reference is required");
        }
        if (!status.canMoveTo(PaymentStatus.PAID)) {
            throw new IllegalStateException("request " + id + " cannot move from " + status + " to PAID");
        }
        if (settledCents != amountCents) {
            throw new IllegalStateException("settled amount does not match request " + id);
        }
        if (settledCurrency == null || !settledCurrency.trim().equals("USD")) {
            throw new IllegalStateException("settled currency must be USD for request " + id);
        }
        status = PaymentStatus.PAID;
        record(actor, "paid", settlementRef.trim());
    }

    /**
     * Corrects the amount while SUBMITTED only. newCents must be in 1..999999999 and the reason
     * non-blank. Records (actorId, "amount-corrected", "old->new: reason").
     */
    public void correctAmount(String actorId, long newCents, String reason) {
        String actor = actor(actorId);
        if (status != PaymentStatus.SUBMITTED) {
            throw new IllegalStateException("request " + id + " amount can change only while SUBMITTED");
        }
        checkAmount(newCents);
        String why = reason(reason);
        String detail = PaymentRules.formatCents(amountCents) + "->" + PaymentRules.formatCents(newCents) + ": " + why;
        amountCents = newCents;
        record(actor, "amount-corrected", detail);
    }

    /** Records a refused approval: (approverId, "approval-refused", policyName). Status unchanged. */
    public void recordApprovalRefused(String approverId, String policyName) {
        String actor = actor(approverId);
        record(actor, "approval-refused", policyName == null ? "" : policyName);
    }

    private void moveTo(PaymentStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("request " + id + " cannot move from " + status + " to " + next);
        }
        status = next;
    }

    private void record(String actor, String action, String detail) {
        events.add(new PaymentEvent(id, events.size() + 1, actor, action, detail));
    }

    private static void checkAmount(long cents) {
        if (cents < 1 || cents > PaymentRules.MAX_CENTS) {
            throw new IllegalArgumentException("amount must be between 0.01 and 9999999.99");
        }
    }

    private static String actor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actor id must not be blank");
        }
        return actorId.trim();
    }

    private static String reason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("a reason is required");
        }
        return reason.trim();
    }
}
