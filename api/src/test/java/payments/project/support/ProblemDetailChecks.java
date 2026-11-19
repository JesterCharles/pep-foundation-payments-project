package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HTTP error response checks (supplied; do not edit): invalid request bodies answer 400 ProblemDetail
 * with field errors; refusals, conflicts and unknown ids answer ProblemDetail too.
 */
public abstract class ProblemDetailChecks extends SecuredCheck {

    @Test
    @DisplayName("the invalid DTO matrix answers 400 application/problem+json naming each bad field")
    void invalidDtoMatrix() {
        Object[][] cases = {
            {"SUP-0101", "INV-1", "1.5", "USD", "amount"},
            {"SUP-0101", "INV-2", "abc", "USD", "amount"},
            {"SUP-0101", "INV-3", "-5.00", "USD", "amount"},
            {"SUP-0101", "INV-4", "10000000.00", "USD", "amount"},
            {"SUP-0101", "INV-5", "1.00", "EUR", "currency"},
            {"SUP-12", "INV-6", "1.00", "USD", "supplierId"},
            {"SUP-0101", "  ", "1.00", "USD", "invoiceNumber"},
        };
        for (Object[] c : cases) {
            Api.Response response = api.post("/payment-requests", as("REQ-0001"),
                    Api.submitBody((String) c[0], (String) c[1], (String) c[2], (String) c[3]));
            String what = c[4] + " case " + c[0] + "/" + c[1] + "/" + c[2] + "/" + c[3];
            assertProblem(400, response, what);
            assertTrue(fields(response).contains(c[4]), what + ": expected a field error for " + c[4] + " in " + response);
        }
        Api.Response several = api.post("/payment-requests", as("REQ-0001"), Api.submitBody("bad", "", "x", "EUR"));
        assertProblem(400, several, "several bad fields");
        assertTrue(fields(several).containsAll(List.of("supplierId", "invoiceNumber", "amount", "currency")),
                "every bad field is reported at once: " + several);
    }

    @Test
    @DisplayName("404, 403 and 409 answers are ProblemDetail without stack traces")
    void otherProblems() {
        assertProblem(404, api.get("/payment-requests/987654", as("REQ-0001")), "unknown id");
        int id = submitted("REQ-0001", data.supplier(), "INV-PD-" + data.invoice(), "6000.00");
        assertProblem(403, api.post("/payment-requests/" + id + "/approve", as("APR-0001"), null), "over limit");
        Map<String, Object> view = api.get("/payment-requests/" + id, as("REQ-0001")).json();
        Api.Response duplicate = api.post("/payment-requests", as("REQ-0002"),
                Api.submitBody((String) view.get("supplierId"), (String) view.get("invoiceNumber"), "1.00", "USD"));
        assertProblem(409, duplicate, "duplicate invoice");
    }

    protected static void assertProblem(int status, Api.Response response, String what) {
        assertEquals(status, response.status(), what + ": " + response);
        String type = response.header("Content-Type");
        assertTrue(type != null && type.startsWith("application/problem+json"), what + ": Content-Type " + type);
        Map<String, Object> body = response.json();
        assertEquals((long) status, body.get("status"), what + ": " + response);
        assertTrue(body.get("title") instanceof String, what + ": a title is required: " + response);
        assertFalse(response.body().contains("Exception") || response.body().contains("\tat "), what + ": no stack traces");
    }

    @SuppressWarnings("unchecked")
    private static List<String> fields(Api.Response response) {
        Object errors = response.json().get("errors");
        assertTrue(errors instanceof List, "expected an errors array: " + response);
        List<String> names = new ArrayList<>();
        for (Object error : (List<Object>) errors) {
            names.add(String.valueOf(((Map<String, Object>) error).get("field")));
        }
        return names;
    }
}
