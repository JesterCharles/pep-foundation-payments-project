package payments.desk;

/**
 * Thrown when a payment request or an actor does not exist (mapped to HTTP 404 in PaymentApi).
 *
 * <p>LEARNER-OWNED (milestone M4). It extends IllegalArgumentException so the release 3
 * expectations ("unknown id throws IllegalArgumentException") keep holding. Keep that superclass.
 */
public class NotFoundException extends IllegalArgumentException {

    // TODO(M4): add a constructor that takes a message and passes it to super(...).
    //   Messages name the request id or actor id and the rule, never a full request body.
}
