package payments.project.support;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generated synthetic suppliers, invoices and amounts (supplied; do not edit). Seeded from the
 * {@code checks.seed} system property (default: a new seed per run, printed on failure through the values).
 */
public final class Generated {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final long SEED = Long.getLong("checks.seed", System.nanoTime());
    private final Random random = new Random(SEED ^ COUNTER.incrementAndGet());
    private final String run = Long.toString(Math.abs(SEED % 46_656), 36).toUpperCase();

    /** A supplier id SUP-0100..SUP-0999. */
    public String supplier() {
        return String.format("SUP-%04d", 100 + random.nextInt(900));
    }

    /** A unique invoice number for this run. */
    public String invoice() {
        return "GEN-" + run + "-" + COUNTER.incrementAndGet();
    }

    /** Cents in [min, max]. */
    public long cents(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min + 1));
    }

    public static String amount(long cents) {
        return (cents / 100) + "." + (cents % 100 < 10 ? "0" : "") + (cents % 100);
    }

    public Random random() {
        return random;
    }
}
