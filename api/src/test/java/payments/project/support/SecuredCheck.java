package payments.project.support;

import java.util.List;

import org.springframework.test.context.ActiveProfiles;

/**
 * Checks that call the API with signed tokens (supplied; do not edit). They run with the Spring profile
 * {@code test}, never {@code dev}, so the X-Actor-Id stub must not be trusted here.
 */
@ActiveProfiles("test")
public abstract class SecuredCheck extends ApiCheck {

    public static final List<String> REQUESTER = List.of("REQUESTER");
    public static final List<String> APPROVER = List.of("APPROVER");
    public static final List<String> CLERK = List.of("PAYABLES_CLERK");

    protected static Api.Who as(String userId) {
        return Api.Who.token(Jwt.token(userId, rolesOf(userId)));
    }

    protected static List<String> rolesOf(String userId) {
        if (userId.startsWith("REQ-")) {
            return REQUESTER;
        }
        if (userId.startsWith("APR-")) {
            return APPROVER;
        }
        if (userId.startsWith("PAY-")) {
            return CLERK;
        }
        return List.of();
    }

    protected int submitted(String userId, String supplier, String invoice, String amount) {
        Api.Response created = api.post("/payment-requests", as(userId), Api.submitBody(supplier, invoice, amount, "USD"));
        assertStatus(201, created, "submit as " + userId);
        return idOf(created);
    }

    protected String statusOf(int id) {
        Api.Response read = api.get("/payment-requests/" + id, as("REQ-0001"));
        assertStatus(200, read, "GET " + id);
        return (String) read.json().get("status");
    }
}
