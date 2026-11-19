package payments.domain;

/**
 * Factory for approval policies by configuration name.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source).
 * <ul>
 *   <li>"limit-only" gives a {@link WithinLimitPolicy}.</li>
 *   <li>"standard" gives an {@link AllOfPolicy} named "standard" holding a WithinLimitPolicy and a
 *       NotOwnRequestPolicy.</li>
 * </ul>
 */
public final class Policies {

    private Policies() {
    }

    /** @throws IllegalArgumentException for any other name */
    public static ApprovalPolicy named(String name) {
        if ("limit-only".equals(name)) {
            return new WithinLimitPolicy();
        }
        if ("standard".equals(name)) {
            return new AllOfPolicy("standard", new WithinLimitPolicy(), new NotOwnRequestPolicy());
        }
        throw new IllegalArgumentException("unknown approval policy: " + name);
    }
}
