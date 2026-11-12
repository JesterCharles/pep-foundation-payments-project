package payments.domain;

/**
 * An approval control (supplied in release 3; do not edit).
 *
 * <p>The PaymentDesk depends only on this interface, so controls can be composed
 * (see {@link AllOfPolicy}) and chosen by name (see {@code Policies.named}).
 */
public interface ApprovalPolicy {

    /** Returns true when this approver may approve this request under this control. */
    boolean allows(Approver approver, PaymentRequest request);

    /** A short, stable name recorded in the history when this control refuses an approval. */
    String name();
}
