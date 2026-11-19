package payments.domain;

/**
 * Separation-of-duties control: nobody approves a request they submitted themselves
 * (ids compared ignoring case).
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). Actor ids come from a supplied
 * directory stub, which is not authentication.
 */
public final class NotOwnRequestPolicy implements ApprovalPolicy {

    @Override
    public boolean allows(Approver approver, PaymentRequest request) {
        return !approver.id().equalsIgnoreCase(request.getRequestedBy());
    }

    @Override
    public String name() {
        return "not-own-request";
    }
}
