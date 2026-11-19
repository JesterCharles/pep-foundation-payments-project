package payments.desk;

/**
 * Thrown when a duplicate invoice, an invalid lifecycle move or a settlement mismatch (HTTP 409).
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). It extends IllegalStateException so callers that
 * expect the general exception keep working. Messages name the request id, the actor and the rule,
 * never a full request body.
 */
public class PaymentConflictException extends IllegalStateException {

    public PaymentConflictException(String message) {
        super(message);
    }
}
