package payments.project.w15;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import payments.project.support.Api;
import payments.project.support.Milestone;
import payments.project.support.ProviderStub;
import payments.project.support.SecuredCheck;

/**
 * Standard W15 cross-area scenario (supplied; do not edit): one request is submitted, approved by a different
 * approver, sent once despite a provider timeout, found mismatched on the settlement statement
 * (project/w15/scenario-statement.csv), opened as an exception case and resolved by a payables clerk with an
 * attributable note. Exception-case routes: docs/contracts/W13-exception-cases.yaml.
 */
@Milestone("standard-w15")
@DisplayName("Standard W15: end-to-end payment invariants")
class StandardW15ScenarioTest extends SecuredCheck {

    @Test
    @DisplayName("one payment per request, statement mismatch to exception, attributable resolution")
    @SuppressWarnings("unchecked")
    void scenario() throws IOException {
        int id = submitted("REQ-0001", data.supplier(), data.invoice(), "1234.50");
        assertStatus(403, api.post("/payment-requests/" + id + "/approve", as("REQ-0001"), null), "requester cannot approve");
        assertStatus(200, api.post("/payment-requests/" + id + "/approve", as("APR-0002"), null), "approve");
        PROVIDER.then(ProviderStub.Behaviour.SLOW);
        Api.Response sent = api.post("/payment-requests/" + id + "/send", as("PAY-0001"), null);
        assertStatus(200, sent, "send despite one timeout");
        assertEquals("SENT", sent.json().get("status"));
        assertEquals(1, PROVIDER.paymentsCreated(), "one payment per request");
        String paymentId = PROVIDER.paymentIdFor(PROVIDER.calls().get(0).idempotencyKey());

        String[] line = statementLine("PR-" + id, paymentId);
        assertEquals("PR-" + id, line[0]);
        String requestAmount = (String) api.get("/payment-requests/" + id, as("PAY-0001")).json().get("amount");
        assertNotEquals(requestAmount, line[2], "the supplied statement line is a mismatch");
        String reason = "statement amount " + line[2] + " differs from " + requestAmount;

        assertStatus(403, api.post("/exception-cases", as("APR-0002"),
                Api.toJson(Map.of("paymentRequestId", (long) id, "reason", reason))), "only payables clerks open cases");
        Api.Response opened = api.post("/exception-cases", as("PAY-0001"),
                Api.toJson(Map.of("paymentRequestId", (long) id, "reason", reason)));
        assertStatus(201, opened, "open exception case");
        assertEquals("OPEN", opened.json().get("status"));
        long caseId = (Long) opened.json().get("id");
        assertEquals("EXCEPTION", statusOf(id));

        String resolve = "/exception-cases/" + caseId + "/resolve";
        assertStatus(400, api.post(resolve, as("PAY-0001"), Api.toJson(Map.of("note", "  "))), "a note is required");
        assertStatus(403, api.post(resolve, as("APR-0002"), Api.toJson(Map.of("note", "ok"))), "approvers do not resolve");
        Api.Response resolved = api.post(resolve, as("PAY-0001"),
                Api.toJson(Map.of("note", "supplier confirmed a 34.50 early-payment discount (synthetic)")));
        assertStatus(200, resolved, "resolve");
        assertEquals("RESOLVED", resolved.json().get("status"));
        assertEquals("PAY-0001", resolved.json().get("resolvedBy"));
        assertStatus(409, api.post(resolve, as("PAY-0001"), Api.toJson(Map.of("note", "again"))), "resolve twice");

        List<Map<String, Object>> events = events(api.get("/payment-requests/" + id, as("REQ-0001")).json());
        Map<String, Object> last = events.get(events.size() - 1);
        assertEquals("exception-resolved", last.get("action"));
        assertEquals("PAY-0001", last.get("actorId"));
        assertEquals("supplier confirmed a 34.50 early-payment discount (synthetic)", last.get("detail"));
        assertEquals("EXCEPTION", statusOf(id), "the request keeps its EXCEPTION status; nothing is deleted");

        List<Object> open = api.get("/exception-cases?status=OPEN", as("PAY-0001")).list();
        assertFalse(open.stream().anyMatch(c -> ((Map<String, Object>) c).get("id").equals(caseId)), "resolved case is not open");
        assertStatus(409, api.post("/payment-requests/" + id + "/send", as("PAY-0001"), null), "never sent twice");
        assertEquals(1, PROVIDER.paymentsCreated());
    }

    private String[] statementLine(String reference, String paymentId) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/project/w15/scenario-statement.csv")) {
            for (String raw : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (raw.isBlank() || raw.startsWith("#") || raw.startsWith("reference")) {
                    continue;
                }
                return raw.replace("{REF}", reference).replace("{PAYMENT}", paymentId).trim().split(",");
            }
        }
        throw new AssertionError("no line in project/w15/scenario-statement.csv");
    }
}
