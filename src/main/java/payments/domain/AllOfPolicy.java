package payments.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Worked example of composition (supplied in release 3; do not edit).
 *
 * <p>An AllOfPolicy allows an approval only when every policy it holds allows it. It holds
 * other policies instead of extending them, so new controls can be combined without
 * changing existing classes. Use it from {@code Policies.named("standard")}:
 *
 * <pre>{@code
 * ApprovalPolicy standard = new AllOfPolicy("standard",
 *         new WithinLimitPolicy(), new NotOwnRequestPolicy());
 * }</pre>
 */
public final class AllOfPolicy implements ApprovalPolicy {

    private final String name;
    private final List<ApprovalPolicy> policies;

    public AllOfPolicy(String name, ApprovalPolicy... policies) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a policy needs a name");
        }
        if (policies == null || policies.length == 0) {
            throw new IllegalArgumentException("AllOfPolicy needs at least one policy");
        }
        List<ApprovalPolicy> copy = new ArrayList<>();
        for (ApprovalPolicy policy : policies) {
            if (policy == null) {
                throw new IllegalArgumentException("AllOfPolicy cannot hold a null policy");
            }
            copy.add(policy);
        }
        this.name = name;
        this.policies = Collections.unmodifiableList(copy);
    }

    @Override
    public boolean allows(Approver approver, PaymentRequest request) {
        for (ApprovalPolicy policy : policies) {
            if (!policy.allows(approver, request)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String name() {
        return name;
    }

    /** The composed policies, in the order they are checked (read-only). */
    public List<ApprovalPolicy> policies() {
        return policies;
    }

    @Override
    public String toString() {
        return "AllOfPolicy[" + name + "]";
    }
}
