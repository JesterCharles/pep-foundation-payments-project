package payments.domain;

/**
 * Separation-of-duties control: nobody approves a request they submitted themselves.
 *
 * <p>LEARNER-OWNED (milestone M3). Actor ids come from a supplied directory stub, which is
 * not authentication.
 */
public final class NotOwnRequestPolicy implements ApprovalPolicy {

    @Override
    public boolean allows(Approver approver, PaymentRequest request) {
        // TODO(M3): allow only when the approver id differs from the request's requestedBy,
        //   ignoring case ("apr-0002" and "APR-0002" are the same person).
        throw new UnsupportedOperationException("TODO M3: NotOwnRequestPolicy.allows");
    }

    @Override
    public String name() {
        // TODO(M3): return the stable name "not-own-request".
        throw new UnsupportedOperationException("TODO M3: NotOwnRequestPolicy.name");
    }
}
