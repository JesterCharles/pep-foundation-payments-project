package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * W08 database rollback and query-count checks (supplied; do not edit).
 *
 * <p>A database trigger forces a failure in the middle of one approval: on the approvals insert, the
 * status update or the history insert. Whatever order your code writes in, the approval record, the
 * APPROVED status and the "approved" history event must then all be absent. The queue check counts the
 * SQL statements Hibernate prepares for one page of 20 requests with history (no N+1).
 */
public abstract class TransactionChecks extends DevProfileCheck {

    private static final String TRIGGER_CLASS = "payments.project.support.FailingTrigger";

    @AfterEach
    void dropTriggers() {
        FailingTrigger.armedRequestId = -1;
        Tables tables = new Tables(dataSource());
        for (String name : List.of("p2_fail_approvals", "p2_fail_requests", "p2_fail_events")) {
            tables.tryUpdate("DROP TRIGGER IF EXISTS " + name);
        }
    }

    @ParameterizedTest(name = "forced failure on {0}")
    @ValueSource(strings = {"approvals", "payment_requests", "payment_events"})
    @DisplayName("a failure during approval rolls back the approval record, the status and the history event together")
    void approvalIsAtomic(String table) {
        Tables tables = new Tables(dataSource());
        String trigger = switch (table) {
            case "approvals" -> "CREATE TRIGGER p2_fail_approvals BEFORE INSERT ON approvals FOR EACH ROW CALL '" + TRIGGER_CLASS + "'";
            case "payment_requests" -> "CREATE TRIGGER p2_fail_requests BEFORE UPDATE ON payment_requests FOR EACH ROW CALL '" + TRIGGER_CLASS + "'";
            default -> "CREATE TRIGGER p2_fail_events BEFORE INSERT ON payment_events FOR EACH ROW CALL '" + TRIGGER_CLASS + "'";
        };
        tables.execute(trigger);
        int id = submitted("REQ-0001", data.supplier(), data.invoice(), "75.00");
        FailingTrigger.armedRequestId = id;
        Api.Response response = api.post("/payment-requests/" + id + "/approve", "APR-0001", null);
        FailingTrigger.armedRequestId = -1;
        assertNotEquals(200, response.status(), "the forced failure must not be reported as an approval: " + response);
        assertTrue(response.status() >= 500, "a server-side failure answers 5xx: " + response);
        assertEquals("SUBMITTED", statusOf(id), "status must be rolled back");
        assertEquals(0, tables.count("SELECT COUNT(*) FROM approvals WHERE request_id = ?", id), "approval record must be rolled back");
        assertEquals(0, tables.count("SELECT COUNT(*) FROM payment_events WHERE request_id = ? AND action = 'approved'", id),
                "history event must be rolled back");
        assertStatus(200, api.post("/payment-requests/" + id + "/approve", "APR-0001", null), "approval works after the failure");
        assertEquals(1, tables.count("SELECT COUNT(*) FROM approvals WHERE request_id = ? AND approver_id = 'APR-0001'", id));
    }

    @Test
    @DisplayName("one queue page of 20 requests with history takes a bounded number of SQL statements (no N+1)")
    void queueWithoutNPlusOne() {
        for (int i = 0; i < 20; i++) {
            int id = submitted("REQ-000" + (1 + i % 3), data.supplier(), data.invoice(), Generated.amount(600_000 + i));
            assertStatus(403, api.post("/payment-requests/" + id + "/approve", "APR-0001", null), "over-limit refusal adds history");
        }
        Statistics statistics = context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class).getStatistics();
        assertTrue(statistics.isStatisticsEnabled(), "hibernate.generate_statistics must stay enabled for this check");
        statistics.clear();
        Api.Response page = api.get("/approval-queue?page=0&size=20", "APR-0003");
        assertStatus(200, page, "queue page");
        long statements = statistics.getPrepareStatementCount();
        assertTrue(statements >= 1, "the queue must be read through JPA/Hibernate");
        assertTrue(statements <= 4, "one page of 20 requests with history used " + statements
                + " SQL statements; fetch the history with the page (join fetch or an entity graph), not per request");
    }
}
