package payments.domain;

/**
 * Approval-limit control: an approver may approve only amounts within their limit.
 *
 * <p>LEARNER-OWNED (milestone M3).
 */
public final class WithinLimitPolicy implements ApprovalPolicy {

    @Override
    public boolean allows(Approver approver, PaymentRequest request) {
        // TODO(M3): allow when the request amount is at most the approver's limit.
        //   Equal to the limit is allowed; one cent over is refused.
        throw new UnsupportedOperationException("TODO M3: WithinLimitPolicy.allows");
    }

    @Override
    public String name() {
        // TODO(M3): return the stable name "within-limit".
        throw new UnsupportedOperationException("TODO M3: WithinLimitPolicy.name");
    }
}
