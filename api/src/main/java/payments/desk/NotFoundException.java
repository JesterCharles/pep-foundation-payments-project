package payments.desk;

/**
 * Thrown when a payment request or an actor does not exist (HTTP 404).
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). It extends IllegalArgumentException so callers that
 * expect the general exception keep working. Messages name the request id, the actor and the rule,
 * never a full request body.
 */
public class NotFoundException extends IllegalArgumentException {

    public NotFoundException(String message) {
        super(message);
    }
}
