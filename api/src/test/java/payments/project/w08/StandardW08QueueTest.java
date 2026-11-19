package payments.project.w08;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;

import payments.project.support.Milestone;
import payments.project.support.QueueChecks;

/** Standard W08 supplied checks: the paginated approval queue, seeded from project/w08/queue-seed.csv (supplied; do not edit). */
@Milestone("standard-w08")
@DisplayName("Standard W08: paginated approval queue")
class StandardW08QueueTest extends QueueChecks {

    @Override
    protected List<Seed> seeds() {
        try (InputStream in = getClass().getResourceAsStream("/project/w08/queue-seed.csv")) {
            List<Seed> seeds = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("amount")) {
                    continue;
                }
                String[] fields = line.trim().split(",");
                seeds.add(new Seed(fields[0], fields[1]));
            }
            return seeds;
        } catch (IOException | NullPointerException e) {
            throw new AssertionError("missing supplied seed api/src/test/resources/project/w08/queue-seed.csv", e);
        }
    }
}
