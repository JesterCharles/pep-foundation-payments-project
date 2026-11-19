package payments.project.support;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Parses the {@code milestone} system property, e.g. {@code standard-w08} (supplied; do not edit). */
public final class Milestones {

    /** Valid selections per track: Standard W07-W15 and Fast W07-W11. */
    public static final List<String> VALID = List.of(
            "standard-w07", "standard-w08", "standard-w09", "standard-w10", "standard-w11", "standard-w12",
            "standard-w13", "standard-w14", "standard-w15",
            "fast-w07", "fast-w08", "fast-w09", "fast-w10", "fast-w11");

    /** A track ("standard" or "fast") and a week number. */
    public record Selection(String track, int week) {
        @Override
        public String toString() {
            return track + "-w" + (week < 10 ? "0" : "") + week;
        }
    }

    private Milestones() {
    }

    /** The current selection, or empty when no {@code -Dmilestone} was given. */
    public static Optional<Selection> selected() {
        String raw = System.getProperty("milestone", "").trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parse(raw));
    }

    /** True when exactly this milestone is selected (for checks that stop applying in later weeks). */
    public static boolean isExactly(String milestone) {
        return selected().map(s -> s.equals(parse(milestone))).orElse(false);
    }

    public static Selection parse(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (!VALID.contains(value)) {
            throw new IllegalArgumentException("unknown milestone '" + raw + "'; valid values: " + VALID);
        }
        int dash = value.indexOf("-w");
        return new Selection(value.substring(0, dash), Integer.parseInt(value.substring(dash + 2)));
    }
}
