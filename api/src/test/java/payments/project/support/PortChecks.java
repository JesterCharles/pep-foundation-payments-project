package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The W07 port contract (docs/contracts/W07-openapi.yaml), shared by Standard W07 and Fast W07
 * (supplied; do not edit). The Spring API must keep the payment rules of the supplied baseline.
 */
public abstract class PortChecks extends DevProfileCheck {

    @Test
    @DisplayName("generated requests: submit answers 201, GET and the list read them back with exact cents")
    void submitGetList() {
        List<Integer> ids = new ArrayList<>();
        long[] fixed = {1L, 500_000L, 999_999_999L, 10L, 123_450L};
        for (int i = 0; i < 15; i++) {
            long cents = i < fixed.length ? fixed[i] : data.cents(1, 999_999_999L);
            String supplier = data.supplier();
            String invoice = data.invoice();
            String requester = "REQ-000" + (1 + i % 3);
            Api.Response created = submit(requester, supplier, invoice, Generated.amount(cents));
            assertStatus(201, created, "submit " + Generated.amount(cents));
            Map<String, Object> view = created.json();
            assertEquals(supplier, view.get("supplierId"), created.toString());
            assertEquals(invoice, view.get("invoiceNumber"), created.toString());
            assertEquals(Generated.amount(cents), view.get("amount"), "amount must round-trip exactly: " + created);
            assertEquals("USD", view.get("currency"), created.toString());
            assertEquals("SUBMITTED", view.get("status"), created.toString());
            assertEquals(requester, view.get("requestedBy"), created.toString());
            Map<String, Object> first = events(view).get(0);
            assertEquals("submitted", first.get("action"), created.toString());
            assertEquals(requester, first.get("actorId"), created.toString());
            int id = idOf(created);
            ids.add(id);
            Api.Response read = api.get("/payment-requests/" + id, "REQ-0002");
            assertStatus(200, read, "GET " + id);
            assertEquals(Generated.amount(cents), read.json().get("amount"), read.toString());
        }
        Api.Response list = api.get("/payment-requests", "REQ-0001");
        assertStatus(200, list, "GET /payment-requests");
        Set<Long> listed = new HashSet<>();
        for (Object item : list.list()) {
            listed.add((Long) ((Map<?, ?>) item).get("id"));
        }
        for (int id : ids) {
            assertTrue(listed.contains((long) id), "list is missing request " + id + ": " + list);
        }
    }

    @Test
    @DisplayName("approval at the limit succeeds; one cent over and own requests are refused with 403 and change nothing")
    void approvalRules() {
        int atLimit = submitted("REQ-0001", data.supplier(), data.invoice(), "5000.00");
        assertStatus(200, api.post("/payment-requests/" + atLimit + "/approve", "APR-0001", null), "at limit");
        assertEquals("APPROVED", statusOf(atLimit));

        int over = submitted("REQ-0001", data.supplier(), data.invoice(), "5000.01");
        assertStatus(403, api.post("/payment-requests/" + over + "/approve", "APR-0001", null), "one cent over");
        assertEquals("SUBMITTED", statusOf(over), "a refused approval must leave the status unchanged");

        int own = submitted("APR-0002", data.supplier(), data.invoice(), "10.00");
        assertStatus(403, api.post("/payment-requests/" + own + "/approve", "APR-0002", null), "own request");
        assertEquals("SUBMITTED", statusOf(own));

        int big = submitted("REQ-0002", data.supplier(), data.invoice(), "25000.00");
        assertStatus(200, api.post("/payment-requests/" + big + "/approve", "APR-0002", null), "APR-0002 at 25000.00");
        int bigger = submitted("REQ-0002", data.supplier(), data.invoice(), "25000.01");
        assertStatus(403, api.post("/payment-requests/" + bigger + "/approve", "APR-0002", null), "APR-0002 over");
        assertStatus(200, api.post("/payment-requests/" + bigger + "/approve", "APR-0003", null), "APR-0003");
    }

    @Test
    @DisplayName("refusals are recorded in the history with the approver; approvals record who approved")
    void history() {
        int id = submitted("REQ-0003", data.supplier(), data.invoice(), "9000.00");
        assertStatus(403, api.post("/payment-requests/" + id + "/approve", "APR-0001", null), "over limit");
        assertStatus(200, api.post("/payment-requests/" + id + "/approve", "APR-0002", null), "within limit");
        List<String> trail = new ArrayList<>();
        for (Map<String, Object> event : events(api.get("/payment-requests/" + id, "REQ-0003").json())) {
            trail.add(event.get("actorId") + ":" + event.get("action"));
        }
        assertEquals(List.of("REQ-0003:submitted", "APR-0001:approval-refused", "APR-0002:approved"), trail);
    }

    @Test
    @DisplayName("duplicate invoice keys (case and space variety) answer 409 and are not stored")
    void duplicates() {
        String supplier = data.supplier();
        String invoice = "INV " + data.invoice();
        submitted("REQ-0001", supplier, invoice, "10.00");
        int before = api.get("/payment-requests", "REQ-0001").list().size();
        String[][] variants = {
            {supplier, invoice},
            {supplier.toLowerCase(), invoice},
            {" " + supplier + " ", invoice.toLowerCase()},
            {supplier, invoice.replace(" ", "")},
        };
        for (String[] variant : variants) {
            assertStatus(409, submit("REQ-0002", variant[0], variant[1], "11.00"), "duplicate " + variant[0] + "/" + variant[1]);
        }
        assertEquals(before, api.get("/payment-requests", "REQ-0001").list().size(), "a duplicate must not be stored");
    }

    @Test
    @DisplayName("invalid lifecycle moves answer 409 and leave the status unchanged")
    void invalidMoves() {
        int rejected = submitted("REQ-0001", data.supplier(), data.invoice(), "20.00");
        assertStatus(200, api.post("/payment-requests/" + rejected + "/reject", "APR-0001", Api.reasonBody("no PO")), "reject");
        assertStatus(409, api.post("/payment-requests/" + rejected + "/approve", "APR-0001", null), "approve rejected");
        assertStatus(409, api.post("/payment-requests/" + rejected + "/cancel", "REQ-0001", Api.reasonBody("x")), "cancel rejected");
        assertEquals("REJECTED", statusOf(rejected));

        int approved = submitted("REQ-0001", data.supplier(), data.invoice(), "30.00");
        assertStatus(200, api.post("/payment-requests/" + approved + "/approve", "APR-0001", null), "approve");
        assertStatus(409, api.post("/payment-requests/" + approved + "/approve", "APR-0002", null), "approve twice");
        assertStatus(409, api.post("/payment-requests/" + approved + "/reject", "APR-0002", Api.reasonBody("late")), "reject approved");
        assertEquals("APPROVED", statusOf(approved));
        assertStatus(200, api.post("/payment-requests/" + approved + "/cancel", "REQ-0001", Api.reasonBody("supplier on hold")), "cancel approved");
        assertEquals("CANCELLED", statusOf(approved));
    }

    @Test
    @DisplayName("unknown ids answer 404 for reads and actions")
    void unknownIds() {
        assertStatus(404, api.get("/payment-requests/987654", "REQ-0001"), "GET unknown");
        assertStatus(404, api.post("/payment-requests/987654/approve", "APR-0001", null), "approve unknown");
        assertStatus(404, api.post("/payment-requests/987654/reject", "APR-0001", Api.reasonBody("x")), "reject unknown");
        assertStatus(404, api.post("/payment-requests/987654/cancel", "REQ-0001", Api.reasonBody("x")), "cancel unknown");
    }

    @Test
    @DisplayName("malformed ids, bodies, amounts and a missing or unknown actor answer 400 and change nothing")
    void badInput() {
        int before = api.get("/payment-requests", "REQ-0001").list().size();
        assertStatus(400, api.get("/payment-requests/abc", "REQ-0001"), "non-numeric id");
        assertStatus(400, api.post("/payment-requests", "REQ-0001", "{\"supplierId\":"), "malformed JSON");
        for (String amount : new String[] {"1.5", "abc", "-5.00", "0.00", "10000000.00", "1,000.00", "", "12.345"}) {
            assertStatus(400, submit("REQ-0001", data.supplier(), data.invoice(), amount), "amount '" + amount + "'");
        }
        assertStatus(400, api.post("/payment-requests", "REQ-0001", Api.submitBody(data.supplier(), data.invoice(), "1.00", "EUR")), "EUR");
        assertStatus(400, api.post("/payment-requests", (String) null, Api.submitBody(data.supplier(), data.invoice(), "1.00", "USD")), "missing X-Actor-Id");
        assertStatus(400, api.post("/payment-requests", "REQ-9999", Api.submitBody(data.supplier(), data.invoice(), "1.00", "USD")), "unknown actor");
        int id = submitted("REQ-0001", data.supplier(), data.invoice(), "2.00");
        assertStatus(400, api.post("/payment-requests/" + id + "/approve", (String) null, null), "approve without actor");
        assertEquals("SUBMITTED", statusOf(id));
        assertEquals(before + 1, api.get("/payment-requests", "REQ-0001").list().size(), "only the one valid request is stored");
        Api.Response refused = submit("REQ-0001", data.supplier(), data.invoice(), "abc");
        assertFalse(refused.body().contains("Exception") || refused.body().contains("\tat "), "no stack traces: " + refused);
    }

    @Test
    @DisplayName("the status filter: empty means all, a status selects, a non-matching status gives an empty list")
    void statusFilter() {
        int a = submitted("REQ-0001", data.supplier(), data.invoice(), "40.00");
        int b = submitted("REQ-0002", data.supplier(), data.invoice(), "41.00");
        assertStatus(200, api.post("/payment-requests/" + b + "/approve", "APR-0001", null), "approve");
        Set<Long> all = idsOf(api.get("/payment-requests?status=", "REQ-0001"));
        assertTrue(all.contains((long) a) && all.contains((long) b), "empty status means no filter");
        Set<Long> approved = idsOf(api.get("/payment-requests?status=APPROVED", "REQ-0001"));
        assertTrue(approved.contains((long) b) && !approved.contains((long) a), "status=APPROVED: " + approved);
        assertTrue(idsOf(api.get("/payment-requests?status=PAID", "REQ-0001")).isEmpty(), "no PAID requests exist");
        Api.Response unknown = api.get("/payment-requests?status=NOT-A-STATUS", "REQ-0001");
        assertStatus(200, unknown, "a status matching nothing");
        assertTrue(unknown.list().isEmpty(), unknown.toString());
    }

    protected Set<Long> idsOf(Api.Response response) {
        assertStatus(200, response, "list");
        Set<Long> ids = new HashSet<>();
        for (Object item : response.list()) {
            ids.add((Long) ((Map<?, ?>) item).get("id"));
        }
        return ids;
    }
}
