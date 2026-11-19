package payments.project.w08;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;

import payments.project.support.Generated;
import payments.project.support.Milestone;
import payments.project.support.QueueChecks;

/** Fast W08 supplied checks: the paginated approval queue over generated requests (supplied; do not edit). */
@Milestone("fast-w08")
@DisplayName("Fast W08: paginated approval queue")
class FastW08QueueTest extends QueueChecks {

    @Override
    protected List<Seed> seeds() {
        List<Seed> seeds = new ArrayList<>();
        long[] fixed = {1, 500_000, 500_000, 999_999_999};
        for (int i = 0; i < 24; i++) {
            long cents = i < fixed.length ? fixed[i] : data.cents(1, 4_000_000);
            seeds.add(new Seed(Generated.amount(cents), "REQ-000" + (1 + i % 3)));
        }
        return seeds;
    }
}
