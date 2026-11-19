package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JWT role and actor checks (supplied; do not edit). Tokens are minted by the tests
 * with a secret generated at test time ({@link Jwt}); your API verifies them with {@code security.jwt.secret}.
 */
public abstract class SecurityChecks extends SecuredCheck {

    @Test
    @DisplayName("role and separation-of-duties matrix across every action")
    void roleMatrix() {
        Api.Who anonymous = Api.Who.anonymous();
        String body = Api.submitBody(data.supplier(), data.invoice(), "40.00", "USD");
        assertStatus(401, api.post("/payment-requests", anonymous, body), "anonymous submit");
        assertStatus(403, api.post("/payment-requests", as("PAY-0001"), body), "payables clerk submit");
        assertStatus(201, api.post("/payment-requests", as("REQ-0001"), body), "requester submit");
        assertStatus(201, api.post("/payment-requests", as("APR-0001"),
                Api.submitBody(data.supplier(), data.invoice(), "41.00", "USD")), "approver submits a request");

        int id = submitted("REQ-0001", data.supplier(), data.invoice(), "4000.00");
        assertStatus(401, api.get("/payment-requests/" + id, anonymous), "anonymous read");
        for (String reader : List.of("REQ-0002", "APR-0002", "PAY-0001")) {
            assertStatus(200, api.get("/payment-requests/" + id, as(reader)), reader + " reads");
        }
        assertStatus(401, api.post("/payment-requests/" + id + "/approve", anonymous, null), "anonymous approve");
        assertStatus(403, api.post("/payment-requests/" + id + "/approve", as("REQ-0002"), null), "requester approves");
        assertStatus(403, api.post("/payment-requests/" + id + "/approve", as("PAY-0001"), null), "clerk approves");
        assertEquals("SUBMITTED", statusOf(id));
        assertStatus(200, api.post("/payment-requests/" + id + "/approve", as("APR-0001"), null), "approver within limit");

        int over = submitted("REQ-0001", data.supplier(), data.invoice(), "6000.00");
        assertStatus(403, api.post("/payment-requests/" + over + "/approve", as("APR-0001"), null), "approver over limit");
        assertEquals("SUBMITTED", statusOf(over));
        int own = submitted("APR-0001", data.supplier(), data.invoice(), "10.00");
        assertStatus(403, api.post("/payment-requests/" + own + "/approve", as("APR-0001"), null), "approver on own request");
        assertEquals("SUBMITTED", statusOf(own));

        int toReject = submitted("REQ-0001", data.supplier(), data.invoice(), "12.00");
        assertStatus(403, api.post("/payment-requests/" + toReject + "/reject", as("REQ-0002"), Api.reasonBody("no")), "requester rejects");
        assertStatus(403, api.post("/payment-requests/" + toReject + "/reject", as("PAY-0001"), Api.reasonBody("no")), "clerk rejects");
        assertStatus(200, api.post("/payment-requests/" + toReject + "/reject", as("APR-0002"), Api.reasonBody("no PO")), "approver rejects");

        int toCancel = submitted("REQ-0001", data.supplier(), data.invoice(), "13.00");
        assertStatus(403, api.post("/payment-requests/" + toCancel + "/cancel", as("REQ-0002"), Api.reasonBody("x")), "other requester cancels");
        assertStatus(403, api.post("/payment-requests/" + toCancel + "/cancel", as("PAY-0001"), Api.reasonBody("x")), "clerk cancels");
        assertStatus(200, api.post("/payment-requests/" + toCancel + "/cancel", as("REQ-0001"), Api.reasonBody("duplicate order")), "own requester cancels");

        assertStatus(403, api.post("/payment-requests/" + id + "/send", as("REQ-0001"), null), "requester sends");
        assertStatus(403, api.post("/payment-requests/" + id + "/send", as("APR-0002"), null), "approver sends");
        assertStatus(200, api.post("/payment-requests/" + id + "/send", as("PAY-0001"), null), "clerk sends");
    }

    @Test
    @DisplayName("an approver's approval queue never lists the approver's own requests")
    void queueExcludesOwnRequests() {
        int byApr1 = submitted("APR-0001", data.supplier(), data.invoice(), "15.00");
        int byApr2 = submitted("APR-0002", data.supplier(), data.invoice(), "16.00");
        int byReq = submitted("REQ-0001", data.supplier(), data.invoice(), "17.00");
        List<Integer> seenBy1 = queueIds("APR-0001");
        List<Integer> seenBy2 = queueIds("APR-0002");
        assertEquals(false, seenBy1.contains(byApr1), "APR-0001 must not see its own request " + byApr1);
        assertEquals(true, seenBy1.contains(byApr2) && seenBy1.contains(byReq), "APR-0001 sees others' requests");
        assertEquals(false, seenBy2.contains(byApr2), "APR-0002 must not see its own request " + byApr2);
        assertEquals(true, seenBy2.contains(byApr1), "APR-0002 sees APR-0001's request");
    }

    private List<Integer> queueIds(String approver) {
        List<Integer> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 50; page++) {
            Api.Response response = api.get("/approval-queue?size=100&page=" + page, as(approver));
            assertStatus(200, response, "GET /approval-queue as " + approver);
            List<?> content = (List<?>) response.json().get("content");
            for (Object item : content) {
                ids.add(((Number) ((Map<?, ?>) item).get("id")).intValue());
            }
            if (content.isEmpty() || page + 1 >= ((Number) response.json().get("totalPages")).intValue()) {
                break;
            }
        }
        return ids;
    }

    @Test
    @DisplayName("tampered, expired, wrongly signed and malformed tokens answer 401")
    void badTokens() {
        String good = Jwt.token("REQ-0001", REQUESTER);
        int id = submitted("REQ-0001", data.supplier(), data.invoice(), "15.00");
        String path = "/payment-requests/" + id + "/approve";
        assertStatus(401, api.post(path, Api.Who.token(Jwt.tampered(good, "APR-0003", APPROVER)), null), "tampered payload");
        assertStatus(401, api.post(path, Api.Who.token(Jwt.expired("APR-0003", APPROVER)), null), "expired");
        assertStatus(401, api.post(path, Api.Who.token(Jwt.signedWith("another-secret-another-secret-0123456789", "APR-0003", APPROVER)), null), "wrong key");
        assertStatus(401, api.post(path, Api.Who.token("not.a.token"), null), "malformed");
        assertEquals("SUBMITTED", statusOf(id));
    }

    @Test
    @DisplayName("the recorded actor is the token subject, even when the client sends a different X-Actor-Id")
    void actorIsTokenSubject() {
        Api.Response created = api.post("/payment-requests",
                new Api.Who(Jwt.token("REQ-0001", REQUESTER), "REQ-0003"), Api.submitBody(data.supplier(), data.invoice(), "6000.00", "USD"));
        assertStatus(201, created, "submit");
        assertEquals("REQ-0001", created.json().get("requestedBy"));
        int id = idOf(created);
        assertStatus(403, api.post("/payment-requests/" + id + "/approve",
                new Api.Who(Jwt.token("APR-0001", APPROVER), "APR-0003"), null), "the header must not raise the approver's limit");
        assertStatus(200, api.post("/payment-requests/" + id + "/approve",
                new Api.Who(Jwt.token("APR-0002", APPROVER), "APR-0001"), null), "approve");
        List<Map<String, Object>> events = events(api.get("/payment-requests/" + id, as("REQ-0001")).json());
        Map<String, Object> last = events.get(events.size() - 1);
        assertEquals("approved", last.get("action"));
        assertEquals("APR-0002", last.get("actorId"), "history names the authenticated approver");
    }
}
