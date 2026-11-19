package payments.domain;

/**
 * Approval-limit control: an approver may approve only amounts within their limit.
 * Equal to the limit is allowed; one cent over is refused.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source).
 */
public final class WithinLimitPolicy implements ApprovalPolicy {

    @Override
    public boolean allows(Approver approver, PaymentRequest request) {
        return request.getAmountCents() <= approver.limitCents();
    }

    @Override
    public String name() {
        return "within-limit";
    }
}
