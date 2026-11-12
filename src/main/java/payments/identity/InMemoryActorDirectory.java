package payments.identity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import payments.domain.Approver;

/**
 * Supplied in-memory identity STUB (release 3; do not edit). NOT AUTHENTICATION.
 *
 * <p>All ids, limits and personas are synthetic. Ids are stored upper-case and looked up
 * ignoring case.
 */
public final class InMemoryActorDirectory implements ActorDirectory {

    private final Map<String, Approver> approvers = new HashMap<>();
    private final Map<String, String> actors = new HashMap<>();

    /**
     * The synthetic sample directory used by App and the README demo.
     * <pre>
     * REQ-0001, REQ-0002, REQ-0003   requesters
     * APR-0001  limit   5,000.00 USD
     * APR-0002  limit  25,000.00 USD
     * APR-0003  limit 9,999,999.99 USD
     * </pre>
     */
    public static InMemoryActorDirectory withSampleActors() {
        return new InMemoryActorDirectory()
                .addRequester("REQ-0001")
                .addRequester("REQ-0002")
                .addRequester("REQ-0003")
                .addApprover("APR-0001", 500_000L)
                .addApprover("APR-0002", 2_500_000L)
                .addApprover("APR-0003", 999_999_999L);
    }

    /** Adds a requester id; returns this directory so calls can be chained. */
    public InMemoryActorDirectory addRequester(String actorId) {
        String key = normalise(actorId);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("actor id must not be blank");
        }
        actors.put(key, key);
        return this;
    }

    /** Adds an approver with a limit in cents; approvers are also known actors. */
    public InMemoryActorDirectory addApprover(String actorId, long limitCents) {
        String key = normalise(actorId);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("actor id must not be blank");
        }
        if (limitCents < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        actors.put(key, key);
        approvers.put(key, new Approver(key, limitCents));
        return this;
    }

    @Override
    public boolean isKnown(String actorId) {
        return actors.containsKey(normalise(actorId));
    }

    @Override
    public Approver approverFor(String actorId) {
        return approvers.get(normalise(actorId));
    }

    private static String normalise(String actorId) {
        return actorId == null ? "" : actorId.trim().toUpperCase(Locale.ROOT);
    }
}
