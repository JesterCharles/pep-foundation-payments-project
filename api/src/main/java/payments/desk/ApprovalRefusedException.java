package payments.desk;

/**
 * Thrown when an approval control refuses an approval (HTTP 403).
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). It extends IllegalStateException so callers that
 * expect the general exception keep working. Messages name the request id, the actor and the rule,
 * never a full request body.
 */
public class ApprovalRefusedException extends IllegalStateException {

    public ApprovalRefusedException(String message) {
        super(message);
    }
}
