package payments.release.m4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import payments.api.PaymentApi;
import payments.desk.PaymentDesk;
import payments.domain.ApprovalPolicy;
import payments.domain.Policies;
import payments.identity.ActorDirectory;
import payments.identity.InMemoryActorDirectory;
import payments.release.support.Http;
import payments.release.support.Http.Response;

/**
 * Release 4 public tests over real HTTP on a free localhost port (supplied; do not edit).
 * Run with {@code mvn -q test -Pm4}. Release 5 reruns this suite on JDBC storage by extending it.
 *
 * <p>The X-Actor-Id header used here is a stub identity, NOT authentication.
 */
public class ApiReleaseTest {

    /** The supplied statement fixtures (read by the M6 route; harmless before then). */
    protected static final Path STATEMENTS = Path.of("src", "test", "resources", "payments", "release", "m6", "statements");

    protected InMemoryActorDirectory directory;
    protected PaymentDesk desk;
    protected Javalin app;
    protected Http http;

    /** The desk under test (in memory here; JDBC in release 5). */
    protected PaymentDesk newDesk(ActorDirectory directory, ApprovalPolicy policy) {
        return PaymentDesk.create(directory, policy);
    }

    /** Releases storage after each test (JDBC: drops the test database). */
    protected void closeStorage() {
    }

    @BeforeEach
    void startServer() {
        directory = new InMemoryActorDirectory()
                .addRequester("REQ-0001")
                .addRequester("REQ-0002")
                .addApprover("APR-0001", 500_000L)
                .addApprover("APR-0002", 2_500_000L)
                .addApprover("APR-0003", 999_999_999L);
        desk = newDesk(directory, Policies.named("standard"));
        app = PaymentApi.start(desk, directory, STATEMENTS, 0);
        http = new Http(app.port());
    }

    @AfterEach
    void stopServer() {
        try {
            if (app != null) {
                app.stop();
            }
        } finally {
            closeStorage();
        }
    }

    @Test
    @DisplayName("POST /payment-requests answers 201 with the request; GET reads it back")
    void submitAndRead() {
        Response created = http.post("/payment-requests", "REQ-0001", Http.submitBody(" sup-0042 ", "inv 7781", "1234.50", "USD"));
        assertEquals(201, created.status(), created.toString());
        Map<String, Object> body = created.json();
        int id = idOf(created);
        assertEquals("SUP-0042", body.get("supplierId"));
        assertEquals("INV7781", body.get("invoiceNumber"));
        assertEquals("1234.50", body.get("amount"));
        assertEquals("USD", body.get("currency"));
        assertEquals("REQ-0001", body.get("requestedBy"));
        assertEquals("SUBMITTED", body.get("status"));
        Response read = http.get("/payment-requests/" + id, "REQ-0002");
        assertEquals(200, read.status(), read.toString());
        assertEquals((long) id, read.json().get("id"));
        assertEquals(List.of("submitted"), actions(read));
    }

    @Test
    @DisplayName("approval within and exactly at the limit answers 200 APPROVED")
    void approveWithinAndAtLimit() {
        int within = submit("REQ-0001", "SUP-0001", "L-1", "4999.99");
        int atLimit = submit("REQ-0001", "SUP-0001", "L-2", "5000.00");
        for (int id : new int[] {within, atLimit}) {
            Response approved = http.post("/payment-requests/" + id + "/approve", "APR-0001", null);
            assertEquals(200, approved.status(), approved.toString());
            assertEquals("APPROVED", approved.json().get("status"));
            assertEquals("APPROVED", statusOf(id));
        }
    }

    @Test
    @DisplayName("one cent over the limit answers 403, records the refusal and leaves the status unchanged")
    void approveOneCentOverLimit() {
        int id = submit("REQ-0001", "SUP-0001", "L-3", "5000.01");
        Response refused = http.post("/payment-requests/" + id + "/approve", "APR-0001", null);
        assertEquals(403, refused.status(), refused.toString());
        String error = errorOf(refused);
        assertTrue(error.contains(String.valueOf(id)) && error.toUpperCase().contains("APR-0001"),
                "the 403 message names the request id and the approver: " + error);
        Response after = http.get("/payment-requests/" + id, "REQ-0001");
        assertEquals("SUBMITTED", after.json().get("status"));
        assertEquals(List.of("submitted", "approval-refused"), actions(after));
        assertEquals("APR-0001", lastEvent(after).get("actorId"));
    }

    @Test
    @DisplayName("approving your own request answers 403")
    void ownRequestRefused() {
        int id = submit("APR-0002", "SUP-0001", "OWN-1", "10.00");
        Response refused = http.post("/payment-requests/" + id + "/approve", "apr-0002", null);
        assertEquals(403, refused.status(), refused.toString());
        errorOf(refused);
        assertEquals("SUBMITTED", statusOf(id));
    }

    @Test
    @DisplayName("a duplicate invoice (case and space variety) answers 409 and is not stored")
    void duplicateSubmission() {
        int id = submit("REQ-0001", "SUP-0007", "INV 900", "10.00");
        for (String[] variant : new String[][] {{"sup-0007", "inv900"}, {"SUP-0007", " I N V 9 0 0 "}}) {
            Response duplicate = http.post("/payment-requests", "REQ-0002",
                    Http.submitBody(variant[0], variant[1], "10.00", "USD"));
            assertEquals(409, duplicate.status(), duplicate.toString());
            errorOf(duplicate);
        }
        assertEquals(1, desk.all().size(), "the duplicate must not be stored");
        assertEquals("SUBMITTED", statusOf(id));
    }

    @Test
    @DisplayName("invalid moves answer 409 and change nothing")
    void invalidMoves() {
        int rejected = submit("REQ-0001", "SUP-0001", "M-1", "10.00");
        Response reject = http.post("/payment-requests/" + rejected + "/reject", "APR-0001", Http.reasonBody("no purchase order"));
        assertEquals(200, reject.status(), reject.toString());
        assertEquals("REJECTED", reject.json().get("status"));
        int eventsBefore = actions(http.get("/payment-requests/" + rejected, "REQ-0001")).size();
        Response approveRejected = http.post("/payment-requests/" + rejected + "/approve", "APR-0001", null);
        assertEquals(409, approveRejected.status(), approveRejected.toString());
        errorOf(approveRejected);
        Response afterReject = http.get("/payment-requests/" + rejected, "REQ-0001");
        assertEquals("REJECTED", afterReject.json().get("status"));
        assertEquals(eventsBefore, actions(afterReject).size());

        int paid = submit("REQ-0001", "SUP-0001", "M-2", "20.00");
        assertEquals(200, http.post("/payment-requests/" + paid + "/approve", "APR-0001", null).status());
        desk.markPaid(paid, "APR-0003", "STL-4001", 2_000L, "USD");
        assertEquals(409, http.post("/payment-requests/" + paid + "/approve", "APR-0002", null).status());
        assertEquals(409, http.post("/payment-requests/" + paid + "/cancel", "APR-0002", Http.reasonBody("too late")).status());
        assertEquals(409, http.post("/payment-requests/" + paid + "/reject", "APR-0002", Http.reasonBody("too late")).status());
        assertEquals("PAID", statusOf(paid));
    }

    @Test
    @DisplayName("reject and cancel record the actor and reason")
    void rejectAndCancel() {
        int a = submit("REQ-0001", "SUP-0001", "R-1", "10.00");
        Response rejected = http.post("/payment-requests/" + a + "/reject", "APR-0002", Http.reasonBody("supplier unknown"));
        assertEquals(200, rejected.status(), rejected.toString());
        assertEquals("rejected", lastEvent(rejected).get("action"));
        assertEquals("supplier unknown", lastEvent(rejected).get("detail"));
        assertEquals("APR-0002", lastEvent(rejected).get("actorId"));

        int b = submit("REQ-0002", "SUP-0001", "R-2", "10.00");
        assertEquals(200, http.post("/payment-requests/" + b + "/approve", "APR-0001", null).status());
        Response cancelled = http.post("/payment-requests/" + b + "/cancel", "REQ-0002", Http.reasonBody("ordered twice"));
        assertEquals(200, cancelled.status(), cancelled.toString());
        assertEquals("CANCELLED", cancelled.json().get("status"));
        assertEquals("ordered twice", lastEvent(cancelled).get("detail"));
    }

    @Test
    @DisplayName("unknown request ids answer 404 with a JSON error")
    void unknownIds() {
        assertEquals(404, http.get("/payment-requests/999", "REQ-0001").status());
        for (String action : new String[] {"approve", "reject", "cancel"}) {
            Response response = http.post("/payment-requests/999/" + action, "APR-0001",
                    action.equals("approve") ? null : Http.reasonBody("x"));
            assertEquals(404, response.status(), action + ": " + response);
            errorOf(response);
        }
    }

    @Test
    @DisplayName("a missing or unknown X-Actor-Id answers 400 and changes nothing")
    void actorHeaderStub() {
        Response missing = http.post("/payment-requests", null, Http.submitBody("SUP-0001", "H-1", "1.00", "USD"));
        assertEquals(400, missing.status(), missing.toString());
        errorOf(missing);
        Response unknown = http.post("/payment-requests", "ZZZ-9999", Http.submitBody("SUP-0001", "H-1", "1.00", "USD"));
        assertEquals(400, unknown.status(), unknown.toString());
        assertTrue(desk.all().isEmpty(), "nothing stored for a missing or unknown actor");
        int id = submit("REQ-0001", "SUP-0001", "H-2", "1.00");
        assertEquals(400, http.post("/payment-requests/" + id + "/approve", null, null).status());
        assertEquals(400, http.post("/payment-requests/" + id + "/approve", "APR-9999", null).status());
        assertEquals(400, http.post("/payment-requests/" + id + "/cancel", "REQ-9999", Http.reasonBody("x")).status());
        assertEquals("SUBMITTED", statusOf(id));
        assertEquals(1, actions(http.get("/payment-requests/" + id, "REQ-0001")).size());
    }

    @Test
    @DisplayName("bad bodies, amounts and ids answer 400 without stack traces or echoed payload; the server keeps serving")
    void badRequests() {
        String marker = "PAYLOAD-MARKER-7Q2";
        Response malformed = http.post("/payment-requests", "REQ-0001", "{\"supplierId\":\"SUP-0001\",\"note\":\"" + marker + "\"");
        assertEquals(400, malformed.status(), malformed.toString());
        assertCleanError(malformed, marker);
        for (String amount : new String[] {"12.5", "-1.00", "1,000.00", "0.00"}) {
            Response bad = http.post("/payment-requests", "REQ-0001", Http.submitBody("SUP-0001", "B-1", amount, "USD"));
            assertEquals(400, bad.status(), "amount " + amount + ": " + bad);
            assertCleanError(bad, marker);
        }
        Response numeric = http.post("/payment-requests", "REQ-0001",
                "{\"supplierId\":\"SUP-0001\",\"invoiceNumber\":\"B-2\",\"amount\":12.5,\"currency\":\"USD\"}");
        assertEquals(400, numeric.status(), numeric.toString());
        assertEquals(400, http.post("/payment-requests", "REQ-0001", Http.submitBody("SUP-0001", "B-3", "1.00", "EUR")).status());
        assertEquals(400, http.post("/payment-requests", "REQ-0001", "").status());
        assertEquals(400, http.get("/payment-requests/abc", "REQ-0001").status());
        int id = submit("REQ-0001", "SUP-0001", "B-4", "1.00");
        Response badReason = http.post("/payment-requests/" + id + "/reject", "APR-0001", "{\"reason\": " + marker);
        assertEquals(400, badReason.status(), badReason.toString());
        assertCleanError(badReason, marker);
        assertEquals("SUBMITTED", statusOf(id), "the server keeps serving and nothing changed");
    }

    @Test
    @DisplayName("GET shows the actor and action of every change and refusal, in order")
    void historyViaGet() {
        int id = submit("REQ-0002", "SUP-0005", "HIS-1", "6000.00");
        assertEquals(403, http.post("/payment-requests/" + id + "/approve", "APR-0001", null).status());
        assertEquals(200, http.post("/payment-requests/" + id + "/approve", "APR-0002", null).status());
        assertEquals(200, http.post("/payment-requests/" + id + "/cancel", "REQ-0002", Http.reasonBody("supplier on hold")).status());
        Response read = http.get("/payment-requests/" + id, "APR-0003");
        assertEquals(List.of("submitted", "approval-refused", "approved", "cancelled"), actions(read));
        List<String> actors = new ArrayList<>();
        for (Map<String, Object> event : events(read)) {
            actors.add(String.valueOf(event.get("actorId")).toUpperCase());
        }
        assertEquals(List.of("REQ-0002", "APR-0001", "APR-0002", "REQ-0002"), actors);
    }

    @Test
    @DisplayName("hand-checked approvals over HTTP follow the limit and own-request rules")
    void tableOfApprovals() {
        String[][] rows = {
            // amount, requester, approver, expected HTTP status
            {"0.01", "REQ-0001", "APR-0001", "200"},
            {"5000.00", "REQ-0002", "APR-0001", "200"},
            {"5000.01", "REQ-0001", "APR-0001", "403"},
            {"25000.00", "REQ-0001", "APR-0002", "200"},
            {"25000.01", "REQ-0002", "APR-0002", "403"},
            {"77000.00", "REQ-0001", "APR-0003", "200"},
            {"4999.99", "APR-0001", "APR-0001", "403"},
            {"0.01", "APR-0002", "APR-0002", "403"},
            {"24999.99", "APR-0001", "APR-0002", "200"},
            {"77000.00", "APR-0002", "APR-0001", "403"},
        };
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            int id = submit(row[1], "SUP-0" + (500 + i), "TBL-" + i, row[0]);
            Response response = http.post("/payment-requests/" + id + "/approve", row[2], null);
            assertEquals(Integer.parseInt(row[3]), response.status(),
                    "amount " + row[0] + " by " + row[2] + " for " + row[1] + ": " + response);
            assertEquals(row[3].equals("200") ? "APPROVED" : "SUBMITTED", statusOf(id));
        }
    }

    // ---------------------------------------------------------------- helpers

    protected int submit(String actor, String supplier, String invoice, String amount) {
        Response created = http.post("/payment-requests", actor, Http.submitBody(supplier, invoice, amount, "USD"));
        assertEquals(201, created.status(), "submit " + supplier + "/" + invoice + ": " + created);
        return idOf(created);
    }

    protected static int idOf(Response response) {
        Object id = response.json().get("id");
        assertTrue(id instanceof Long, "expected a numeric id in " + response);
        return ((Long) id).intValue();
    }

    protected String statusOf(int id) {
        Response read = http.get("/payment-requests/" + id, "REQ-0001");
        assertEquals(200, read.status(), read.toString());
        return String.valueOf(read.json().get("status"));
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> events(Response response) {
        Object events = response.json().get("events");
        assertTrue(events instanceof List, "expected an events array in " + response);
        return (List<Map<String, Object>>) events;
    }

    protected static List<String> actions(Response response) {
        List<String> actions = new ArrayList<>();
        for (Map<String, Object> event : events(response)) {
            actions.add(String.valueOf(event.get("action")));
        }
        return actions;
    }

    protected static Map<String, Object> lastEvent(Response response) {
        List<Map<String, Object>> events = events(response);
        assertFalse(events.isEmpty(), "no events in " + response);
        return events.get(events.size() - 1);
    }

    protected static String errorOf(Response response) {
        Object error = response.json().get("error");
        assertTrue(error instanceof String && !((String) error).isBlank(),
                "expected {\"error\":\"...\"} but got " + response);
        return (String) error;
    }

    private static void assertCleanError(Response response, String marker) {
        errorOf(response);
        assertFalse(response.body().contains(marker), "the response must not echo the request body: " + response);
        assertFalse(response.body().contains("Exception") || response.body().contains("\tat ")
                || response.body().contains(".java:"), "no stack traces in responses: " + response);
    }
}
