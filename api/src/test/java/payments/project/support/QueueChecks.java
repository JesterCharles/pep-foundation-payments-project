package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * The paginated approval queue of the W08 contract (supplied; do not edit). Each subclass supplies the
 * seed requests (amount text, requester); the queue holds SUBMITTED requests only.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class QueueChecks extends DevProfileCheck {

    /** One seed request. */
    public record Seed(String amount, String requester) {
    }

    private final List<long[]> seeded = new ArrayList<>(); // {id, cents}
    private boolean ready;

    /** At least 23 seed requests; the checks assume 23 or more. */
    protected abstract List<Seed> seeds();

    @BeforeEach
    void seedQueue() {
        if (ready) {
            return;
        }
        for (Seed seed : seeds()) {
            int id = submitted(seed.requester(), data.supplier(), data.invoice(), seed.amount());
            seeded.add(new long[] {id, cents(seed.amount())});
        }
        int notQueued = submitted("REQ-0001", data.supplier(), data.invoice(), "1.00");
        assertStatus(200, api.post("/payment-requests/" + notQueued + "/approve", "APR-0001", null), "approve");
        ready = true;
    }

    @Test
    @DisplayName("page 0, the last page and one past the end, with totals")
    void pageBoundaries() {
        int total = seeded.size();
        int size = 10;
        int pages = (total + size - 1) / size;
        Map<String, Object> first = page("/approval-queue?page=0&size=" + size);
        assertEquals((long) total, first.get("totalElements"), "only SUBMITTED requests are queued: " + first);
        assertEquals((long) pages, first.get("totalPages"));
        assertEquals((long) 0, first.get("page"));
        assertEquals((long) size, first.get("size"));
        assertEquals(size, content(first).size());
        Map<String, Object> last = page("/approval-queue?page=" + (pages - 1) + "&size=" + size);
        assertEquals(total - (pages - 1) * size, content(last).size(), "last page");
        Map<String, Object> past = page("/approval-queue?page=" + pages + "&size=" + size);
        assertTrue(content(past).isEmpty(), "one page past the end is empty: " + past);
        assertEquals((long) total, past.get("totalElements"));
    }

    @Test
    @DisplayName("size limits: 1 and 100 are accepted; 0, 101 and a negative page answer 400")
    void sizeLimits() {
        assertEquals(1, content(page("/approval-queue?page=0&size=1")).size());
        assertEquals(seeded.size(), content(page("/approval-queue?page=0&size=100")).size());
        assertStatus(400, api.get("/approval-queue?page=0&size=0", "APR-0003"), "size=0");
        assertStatus(400, api.get("/approval-queue?page=0&size=101", "APR-0003"), "size=101");
        assertStatus(400, api.get("/approval-queue?page=-1&size=10", "APR-0003"), "page=-1");
    }

    @Test
    @DisplayName("sort=amount is largest first (ties by id) across pages; sort=submittedAt is oldest first")
    void sorting() {
        List<long[]> byAmount = new ArrayList<>(seeded);
        byAmount.sort(Comparator.<long[]>comparingLong(r -> -r[1]).thenComparingLong(r -> r[0]));
        List<Long> expected = byAmount.stream().map(r -> r[0]).toList();
        List<Long> actual = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            for (Map<String, Object> item : content(page("/approval-queue?page=" + p + "&size=10&sort=amount"))) {
                actual.add((Long) item.get("id"));
                assertTrue(item.get("submittedAt") instanceof String, "queue items carry submittedAt: " + item);
                assertTrue(!events(item).isEmpty(), "queue items carry their history: " + item);
            }
        }
        assertEquals(expected, actual, "sort=amount order");
        List<Long> bySubmission = seeded.stream().map(r -> r[0]).toList();
        List<Long> submittedOrder = new ArrayList<>();
        for (Map<String, Object> item : content(page("/approval-queue?page=0&size=100&sort=submittedAt"))) {
            submittedOrder.add((Long) item.get("id"));
        }
        assertEquals(bySubmission, submittedOrder, "sort=submittedAt order");
    }

    private Map<String, Object> page(String path) {
        Api.Response response = api.get(path, "APR-0003");
        assertStatus(200, response, path);
        return response.json();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> content(Map<String, Object> page) {
        Object content = page.get("content");
        assertTrue(content instanceof List, "expected a content array: " + page);
        return (List<Map<String, Object>>) content;
    }

    private static long cents(String amount) {
        String[] parts = amount.split("\\.");
        return Long.parseLong(parts[0]) * 100 + Long.parseLong(parts[1]);
    }
}
