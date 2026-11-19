package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Payment-provider request checks (supplied; do not edit). The tests set {@code provider.timeout=400ms} and
 * {@code provider.max-attempts=3}; the stub's slow answers take 1500 ms.
 */
public abstract class ProviderChecks extends SecuredCheck {

    protected int approved(String amount) {
        int id = submitted("REQ-0002", data.supplier(), data.invoice(), amount);
        assertStatus(200, api.post("/payment-requests/" + id + "/approve", as("APR-0002"), null), "approve");
        return id;
    }

    protected Api.Response send(int id) {
        return api.post("/payment-requests/" + id + "/send", as("PAY-0001"), null);
    }

    @Test
    @DisplayName("a timeout then success sends exactly one payment, with one idempotency key, and records it")
    void timeoutThenSuccess() {
        int id = approved("120.00");
        PROVIDER.then(ProviderStub.Behaviour.SLOW);
        Api.Response sent = send(id);
        assertStatus(200, sent, "send after one timeout");
        assertEquals("SENT", sent.json().get("status"), sent.toString());
        assertEquals(1, PROVIDER.paymentsCreated(), "exactly one payment at the provider");
        List<ProviderStub.Call> calls = PROVIDER.calls();
        assertTrue(calls.size() >= 2, "the timed-out call must be retried: " + calls);
        Set<String> keys = new HashSet<>();
        calls.forEach(call -> keys.add(call.idempotencyKey()));
        assertEquals(1, keys.size(), "every attempt carries the same Idempotency-Key: " + keys);
        String key = keys.iterator().next();
        assertNotNull(key, "Idempotency-Key header is required");
        Map<String, Object> body = Json.object(calls.get(0).body());
        assertEquals("120.00", body.get("amount"), "the provider receives the exact amount");
        assertEquals("USD", body.get("currency"));
        List<Map<String, Object>> events = events(api.get("/payment-requests/" + id, as("PAY-0001")).json());
        Map<String, Object> last = events.get(events.size() - 1);
        assertEquals("sent", last.get("action"));
        assertEquals("PAY-0001", last.get("actorId"));
        assertEquals(PROVIDER.paymentIdFor(key), last.get("detail"), "the sent event records the provider payment id");
    }

    @Test
    @DisplayName("a duplicate-key replay returns the original result; sending again never pays twice")
    void duplicateKeyReplay() {
        int id = approved("130.00");
        PROVIDER.then(ProviderStub.Behaviour.FAIL_500, ProviderStub.Behaviour.SLOW);
        assertStatus(200, send(id), "send after a 500 and a timeout");
        assertEquals(1, PROVIDER.paymentsCreated());
        assertEquals("SENT", statusOf(id));
        Api.Response again = send(id);
        assertStatus(409, again, "sending a SENT request again");
        assertEquals(1, PROVIDER.paymentsCreated(), "no second payment");
    }

    @Test
    @DisplayName("REJECTED moves the request to EXCEPTION without retrying")
    void rejectedToException() {
        int id = approved("140.00");
        PROVIDER.then(ProviderStub.Behaviour.REJECT);
        Api.Response sent = send(id);
        assertStatus(200, sent, "send");
        assertEquals("EXCEPTION", sent.json().get("status"), sent.toString());
        assertEquals(1, PROVIDER.calls().size(), "a REJECTED answer is final: no retry");
        boolean recorded = events(sent.json()).stream().anyMatch(e -> "provider-rejected".equals(e.get("action")));
        assertTrue(recorded, "history records provider-rejected: " + sent);
    }
}
