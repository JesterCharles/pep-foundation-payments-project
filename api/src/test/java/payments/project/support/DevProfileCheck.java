package payments.project.support;

import org.springframework.test.context.ActiveProfiles;

/**
 * Checks that identify the caller with the X-Actor-Id stub header (supplied; do not edit). They run with the
 * Spring profile {@code dev}. Once your API has real authentication (Standard W09, Fast W08), the dev profile
 * must keep this stub working so these earlier checks still pass; outside dev the stub must be ignored.
 */
@ActiveProfiles("dev")
public abstract class DevProfileCheck extends ApiCheck {

    protected Api.Response submit(String actor, String supplier, String invoice, String amount) {
        return api.post("/payment-requests", actor, Api.submitBody(supplier, invoice, amount, "USD"));
    }

    protected int submitted(String actor, String supplier, String invoice, String amount) {
        Api.Response created = submit(actor, supplier, invoice, amount);
        assertStatus(201, created, "submit " + supplier + "/" + invoice + " " + amount);
        return idOf(created);
    }

    protected String statusOf(int id) {
        Api.Response read = api.get("/payment-requests/" + id, "REQ-0001");
        assertStatus(200, read, "GET " + id);
        return (String) read.json().get("status");
    }
}
