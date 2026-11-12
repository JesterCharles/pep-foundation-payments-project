package payments.identity;

import payments.domain.Approver;

/**
 * Supplied identity STUB (release 3; do not edit). THIS IS NOT AUTHENTICATION.
 *
 * <p>It only maps synthetic actor ids (REQ-xxxx requesters, APR-xxxx approvers) to what
 * they may do. Nothing proves that a caller is who the id says. In M4 the HTTP layer reads
 * the id from an X-Actor-Id header, which is equally unauthenticated. Real authentication
 * arrives in the Standard/Fast continuation. Lookups ignore case.
 */
public interface ActorDirectory {

    /** True when the id names any actor in the directory (requester or approver). */
    boolean isKnown(String actorId);

    /** The approver with this id, or null when the id is unknown or cannot approve. */
    Approver approverFor(String actorId);
}
