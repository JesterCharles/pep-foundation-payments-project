package payments.domain;

/**
 * The lifecycle of a payment request.
 *
 * <p>LEARNER-OWNED (milestone M3). Keep the five constants and their order.
 */
public enum PaymentStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    PAID;

    /**
     * Returns true only for an allowed move.
     *
     * <pre>
     * SUBMITTED -> APPROVED, REJECTED or CANCELLED
     * APPROVED  -> PAID or CANCELLED
     * </pre>
     * Every other pair is false, including staying in the same status. REJECTED,
     * CANCELLED and PAID are terminal.
     */
    public boolean canMoveTo(PaymentStatus next) {
        // TODO(M3): implement the lifecycle table above (a switch on `this` works well).
        throw new UnsupportedOperationException("TODO M3: PaymentStatus.canMoveTo");
    }
}
