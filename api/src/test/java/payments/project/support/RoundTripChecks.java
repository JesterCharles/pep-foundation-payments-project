package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * W08 persistence round trip (supplied; do not edit): generated requests, approvals, refusals and amount
 * corrections are stored in the contract tables with exact cents and complete, ordered history.
 */
public abstract class RoundTripChecks extends DevProfileCheck {

    @Test
    @DisplayName("generated requests, approvals, refusals and corrections are stored exactly")
    void roundTrip() {
        Tables tables = new Tables(dataSource());
        for (int i = 0; i < 12; i++) {
            long cents = data.cents(1, 2_000_000L);
            int id = submitted("REQ-0002", data.supplier(), data.invoice(), Generated.amount(cents));
            List<String> expectedActions = new ArrayList<>(List.of("submitted"));
            if (i % 3 == 0) {
                long corrected = cents + 1;
                assertStatus(200, api.post("/payment-requests/" + id + "/correct", "REQ-0002",
                        Api.toJson(Map.of("amount", Generated.amount(corrected), "reason", "credit note applied"))), "correct");
                cents = corrected;
                expectedActions.add("amount-corrected");
            }
            if (cents > 500_000L) {
                assertStatus(403, api.post("/payment-requests/" + id + "/approve", "APR-0001", null), "refusal");
                expectedActions.add("approval-refused");
            }
            assertStatus(200, api.post("/payment-requests/" + id + "/approve", "APR-0002", null), "approve");
            expectedActions.add("approved");
            Map<String, Object> row = tables.rows("SELECT amount_cents, status FROM payment_requests WHERE id = ?", id).get(0);
            assertEquals(cents, ((Number) row.get("amount_cents")).longValue(), "exact cents for " + id);
            assertEquals("APPROVED", row.get("status"));
            List<String> stored = new ArrayList<>();
            for (Map<String, Object> event : tables.rows(
                    "SELECT action FROM payment_events WHERE request_id = ? ORDER BY seq", id)) {
                stored.add((String) event.get("action"));
            }
            assertEquals(expectedActions, stored, "stored history of " + id);
            assertEquals(1, tables.count("SELECT COUNT(*) FROM approvals WHERE request_id = ? AND approver_id = 'APR-0002'", id));
            Map<String, Object> view = api.get("/payment-requests/" + id, "REQ-0002").json();
            assertEquals(Generated.amount(cents), view.get("amount"));
            assertEquals(expectedActions.size(), events(view).size());
        }
        Map<String, Object> corrected = tables.rows(
                "SELECT detail FROM payment_events WHERE action = 'amount-corrected' ORDER BY request_id LIMIT 1").get(0);
        assertEquals(true, String.valueOf(corrected.get("detail")).endsWith(": credit note applied"),
                "amount-corrected detail is \"old->new: reason\": " + corrected);
    }
}
