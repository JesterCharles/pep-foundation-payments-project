package payments.domain;

/**
 * An approver and their approval limit in cents (supplied in release 3; do not edit).
 * Approvers come from the supplied ActorDirectory stub, which is not authentication.
 *
 * @param id         synthetic approver id such as "APR-0001"
 * @param limitCents the largest amount, in cents, this approver may approve
 */
public record Approver(String id, long limitCents) {
}
