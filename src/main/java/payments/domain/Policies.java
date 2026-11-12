package payments.domain;

/**
 * Factory for approval policies by configuration name.
 *
 * <p>LEARNER-OWNED (milestone M3).
 */
public final class Policies {

    private Policies() {
    }

    /**
     * Returns the policy for a configuration name.
     * <ul>
     *   <li>"limit-only" gives a {@link WithinLimitPolicy}.</li>
     *   <li>"standard" gives an {@link AllOfPolicy} named "standard" holding a
     *       WithinLimitPolicy and a NotOwnRequestPolicy.</li>
     * </ul>
     *
     * @throws IllegalArgumentException for any other name
     */
    public static ApprovalPolicy named(String name) {
        // TODO(M3): implement the factory described above.
        throw new UnsupportedOperationException("TODO M3: Policies.named");
    }
}
