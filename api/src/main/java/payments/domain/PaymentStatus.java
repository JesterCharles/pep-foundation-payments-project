package payments.domain;

/**
 * The lifecycle of a payment request in the supplied baseline.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source).
 *
 * <pre>
 * SUBMITTED -> APPROVED, REJECTED or CANCELLED
 * APPROVED  -> PAID or CANCELLED
 * </pre>
 * Every other pair is refused, including staying in the same status. REJECTED, CANCELLED and
 * PAID are terminal.
 */
public enum PaymentStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    PAID;

    /** Returns true only for an allowed move. */
    public boolean canMoveTo(PaymentStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case SUBMITTED -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == PAID || next == CANCELLED;
            case REJECTED, CANCELLED, PAID -> false;
        };
    }
}
