package payments.release.m6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import payments.api.PaymentApi;
import payments.desk.PaymentDesk;
import payments.domain.Policies;
import payments.identity.InMemoryActorDirectory;
import payments.release.support.Http;
import payments.release.support.Http.Response;

/**
 * Release 6 public tests for GET /reports/reconciliation over real HTTP (supplied; do not edit).
 * Run with {@code mvn -q test}. The expected values below are worked out by hand from the supplied
 * statement fixtures in src/test/resources/payments/release/m6/statements. All data is synthetic.
 */
class ReportsRouteReleaseTest {

    private static final Path STATEMENTS = Path.of("src", "test", "resources", "payments", "release", "m6", "statements");
    private static final List<String> CATEGORIES = List.of("matched", "amountMismatch", "currencyMismatch",
            "unknownReference", "duplicateLine", "paidWithoutLine");

    private Javalin app;
    private Http http;

    @BeforeEach
    void startWithSeededDesk() {
        InMemoryActorDirectory directory = InMemoryActorDirectory.withSampleActors();
        PaymentDesk desk = PaymentDesk.create(directory, Policies.named("standard"));
        String[][] seed = {
            {"SUP-0101", "INV-A1", "1250.00", "STL-1001"},
            {"SUP-0101", "INV-A2", "300.00", "STL-1002"},
            {"SUP-0202", "INV-B1", "4999.99", "STL-1003"},
            {"SUP-0202", "INV-B2", "75.25", "STL-1004"},
            {"SUP-0303", "INV-C1", "10.00", "STL-1005"},
            {"SUP-0303", "INV-C2", "640.00", null},
            {"SUP-0404", "INV-D1", "99.99", null},
        };
        for (int i = 0; i < seed.length; i++) {
            int id = desk.submit(seed[i][0], seed[i][1], seed[i][2], "USD", "REQ-0001").getId();
            assertEquals(i + 1, id, "seeded request ids are 1-7");
            if (i < 6) {
                desk.approve(id, "APR-0003");
            }
            if (seed[i][3] != null) {
                desk.markPaid(id, "APR-0002", seed[i][3], desk.find(id).getAmountCents(), "USD");
            }
        }
        app = PaymentApi.start(desk, directory, STATEMENTS, 0);
        http = new Http(app.port());
    }

    @AfterEach
    void stop() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @DisplayName("mixed-discrepancies.csv: every category with ids and exact totals")
    void mixedDiscrepancies() {
        Map<String, Object> body = report("mixed-discrepancies.csv");
        assertEquals("mixed-discrepancies.csv", body.get("statement"));
        assertCategory(body, "matched", List.of(1L, 6L), "1890.00");
        assertCategory(body, "amountMismatch", List.of(2L), "300.01");
        assertCategory(body, "currencyMismatch", List.of(3L), "4999.99");
        assertCategory(body, "unknownReference", List.of(99L), "50.00");
        assertCategory(body, "duplicateLine", List.of(1L), "1250.00");
        assertCategory(body, "paidWithoutLine", List.of(4L, 5L), "85.25");
    }

    @Test
    @DisplayName("precedence.csv: a duplicate EUR line for an unknown request counts only as a duplicate")
    void precedence() {
        Map<String, Object> body = report("precedence.csv");
        assertCategory(body, "matched", List.of(5L, 7L), "109.99");
        assertCategory(body, "amountMismatch", List.of(), "0.00");
        assertCategory(body, "currencyMismatch", List.of(4L), "75.26");
        assertCategory(body, "unknownReference", List.of(98L), "10.00");
        assertCategory(body, "duplicateLine", List.of(5L, 98L), "20.00");
        assertCategory(body, "paidWithoutLine", List.of(1L, 2L, 3L), "6549.99");
    }

    @Test
    @DisplayName("clean-match.csv and header-only.csv")
    void cleanAndEmpty() {
        Map<String, Object> clean = report("clean-match.csv");
        assertCategory(clean, "matched", List.of(1L, 2L, 3L, 4L, 5L), "6635.24");
        assertCategory(clean, "paidWithoutLine", List.of(), "0.00");
        Map<String, Object> empty = report("header-only.csv");
        assertCategory(empty, "matched", List.of(), "0.00");
        assertCategory(empty, "duplicateLine", List.of(), "0.00");
        assertCategory(empty, "paidWithoutLine", List.of(1L, 2L, 3L, 4L, 5L), "6635.24");
    }

    @Test
    @DisplayName("missing, unknown and path-traversal statement names answer 400")
    void onlySuppliedFixtures() {
        for (String query : new String[] {"", "?statement=", "?statement=missing.csv", "?statement=../../../pom.xml",
                "?statement=..%2F..%2F..%2Fpom.xml", "?statement=%2Fetc%2Fhosts", "?statement=..",
                "?statement=statements%2F..%2Fclean-match.csv", "?statement=..%5C..%5Cpom.xml"}) {
            Response response = http.get("/reports/reconciliation" + query, "REQ-0001");
            assertEquals(400, response.status(), query + ": " + response);
            Object error = response.json().get("error");
            assertTrue(error instanceof String && !((String) error).isBlank(), "JSON error for " + query);
            assertFalse(response.body().contains("<project"), "never serve other files: " + query);
        }
        assertEquals(200, http.get("/reports/reconciliation?statement=clean-match.csv", "REQ-0001").status(),
                "the server keeps serving after refusals");
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> report(String name) {
        Response response = http.get("/reports/reconciliation?statement=" + name, "REQ-0001");
        assertEquals(200, response.status(), name + ": " + response);
        Map<String, Object> body = response.json();
        for (String category : CATEGORIES) {
            assertTrue(body.get(category) instanceof Map, "missing category " + category + " in " + response);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static void assertCategory(Map<String, Object> body, String category, List<Long> ids, String total) {
        Map<String, Object> entry = (Map<String, Object>) body.get(category);
        assertEquals((long) ids.size(), entry.get("count"), category + " count");
        assertEquals(ids, entry.get("requestIds"), category + " requestIds");
        assertEquals(total, entry.get("total"), category + " total");
    }
}
