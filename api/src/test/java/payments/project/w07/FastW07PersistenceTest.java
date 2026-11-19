package payments.project.w07;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import payments.project.support.Api;
import payments.project.support.DevProfileCheck;
import payments.project.support.Generated;
import payments.project.support.Milestone;
import payments.project.support.Tables;

/**
 * Fast W07 persistence checks (supplied; do not edit): requests and their history are stored through
 * Spring Data JPA in the contract tables, with exact cents, and the list filters use derived queries.
 */
@Milestone("fast-w07")
@DisplayName("Fast W07: requests and their events stored and reloaded through JPA")
class FastW07PersistenceTest extends DevProfileCheck {

    @Test
    @DisplayName("each generated request is one payment_requests row with exact amount_cents and its history rows")
    void roundTrip() {
        Tables tables = new Tables(dataSource());
        for (int i = 0; i < 10; i++) {
            long cents = i == 0 ? 999_999_999L : data.cents(1, 99_999_999L);
            String supplier = data.supplier();
            int id = submitted("REQ-0001", supplier, data.invoice(), Generated.amount(cents));
            if (i % 2 == 0) {
                api.post("/payment-requests/" + id + "/approve", "APR-0003", null);
            }
            Map<String, Object> view = api.get("/payment-requests/" + id, "REQ-0001").json();
            List<Map<String, Object>> rows = tables.rows(
                    "SELECT supplier_id, amount_cents, currency, requested_by, status FROM payment_requests WHERE id = ?", id);
            assertEquals(1, rows.size(), "one payment_requests row for request " + id);
            Map<String, Object> row = rows.get(0);
            assertEquals(cents, ((Number) row.get("amount_cents")).longValue(), "amount_cents must be exact");
            assertEquals(supplier, row.get("supplier_id"));
            assertEquals("USD", String.valueOf(row.get("currency")).trim());
            assertEquals(view.get("status"), row.get("status"));
            long history = tables.count("SELECT COUNT(*) FROM payment_events WHERE request_id = ?", id);
            assertEquals(events(view).size(), history, "one payment_events row per history entry of " + id);
        }
    }

    @Test
    @DisplayName("status and supplierId filters return exactly the matching stored requests")
    void derivedQueries() {
        String supplier = data.supplier();
        int a = submitted("REQ-0001", supplier, data.invoice(), "50.00");
        int b = submitted("REQ-0002", supplier, data.invoice(), "51.00");
        int other = submitted("REQ-0002", "SUP-0001", data.invoice(), "52.00");
        assertStatus(200, api.post("/payment-requests/" + b + "/approve", "APR-0001", null), "approve");
        assertEquals(Set.of((long) a, (long) b), ids(api.get("/payment-requests?supplierId=" + supplier, "REQ-0001")));
        Set<Long> approved = ids(api.get("/payment-requests?status=APPROVED&supplierId=" + supplier, "REQ-0001"));
        assertEquals(Set.of((long) b), approved, "status and supplier together");
        assertTrue(!ids(api.get("/payment-requests?status=SUBMITTED", "REQ-0001")).contains((long) b));
        assertTrue(ids(api.get("/payment-requests?supplierId=SUP-0001", "REQ-0001")).contains((long) other));
    }

    private static Set<Long> ids(Api.Response response) {
        assertEquals(200, response.status(), response.toString());
        Set<Long> ids = new HashSet<>();
        for (Object item : response.list()) {
            ids.add((Long) ((Map<?, ?>) item).get("id"));
        }
        return ids;
    }
}
