package payments.desk;

/**
 * Thrown when an approval control refuses (approval limit or own request; mapped to HTTP 403).
 *
 * <p>LEARNER-OWNED (milestone M4). It extends IllegalStateException so the release 3
 * expectations keep holding. Keep that superclass.
 */
public class ApprovalRefusedException extends IllegalStateException {

    // TODO(M4): add a constructor that takes a message and passes it to super(...).
    //   The message names the request id, the approver id and the refusing policy name.
}
