package payments.desk;

/**
 * Thrown for a duplicate invoice, an invalid lifecycle move or a settlement mismatch
 * (mapped to HTTP 409 in PaymentApi).
 *
 * <p>LEARNER-OWNED (milestone M4). It extends IllegalStateException so the release 3
 * expectations keep holding. Keep that superclass.
 */
public class PaymentConflictException extends IllegalStateException {

    // TODO(M4): add a constructor that takes a message and passes it to super(...).
    //   Messages name the request id, the actor and the rule, never a full request body.
}
