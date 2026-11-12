package payments.domain;

/**
 * One attributable entry in a payment request's history (supplied in release 3; do not edit).
 *
 * @param requestId the payment request this event belongs to
 * @param seq       1 for the first event of a request, then 2, 3, ... with no gaps
 * @param actorId   who did it (a synthetic id from the supplied ActorDirectory stub)
 * @param action    what happened, for example "submitted", "approved" or "approval-refused"
 * @param detail    the detail for that action, never null (use "" when there is nothing to add)
 */
public record PaymentEvent(int requestId, int seq, String actorId, String action, String detail) {
}
