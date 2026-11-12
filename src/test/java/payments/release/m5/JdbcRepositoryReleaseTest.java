package payments.release.m5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import payments.desk.PaymentDesk;
import payments.desk.PaymentRepository;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.domain.Policies;
import payments.identity.InMemoryActorDirectory;
import payments.jdbc.DbConfig;
import payments.jdbc.JdbcPaymentRepository;
import payments.release.m4.RepositoryContractTest;
import payments.support.TestDatabase;

/**
 * Release 5 public tests for PostgreSQL persistence (supplied; do not edit).
 * Run with {@code mvn -q test -Pm5} against a LOCAL PostgreSQL reachable through PG* variables.
 * No database server is started for you and no container engine is used. All data is synthetic.
 */
class JdbcRepositoryReleaseTest {

    // ---------------------------------------------------------------- configuration (no database needed)

    @Test
    @DisplayName("DbConfig.jdbcUrl uses PGHOST, PGPORT and PGDATABASE with defaults")
    void jdbcUrlFromEnvironment() {
        assertEquals("jdbc:postgresql://localhost:5432/postgres", DbConfig.jdbcUrl(Map.of()));
        assertEquals("jdbc:postgresql://db.internal.example:6543/payments_dev",
                DbConfig.jdbcUrl(Map.of("PGHOST", "db.internal.example", "PGPORT", "6543", "PGDATABASE", "payments_dev")));
        assertEquals("jdbc:postgresql://127.0.0.1:5432/postgres", DbConfig.jdbcUrl(Map.of("PGHOST", "127.0.0.1")));
        Map<String, String> blank = new HashMap<>();
        blank.put("PGHOST", " ");
        blank.put("PGPORT", "");
        assertEquals("jdbc:postgresql://localhost:5432/postgres", DbConfig.jdbcUrl(blank));
        assertEquals("jdbc:postgresql://localhost:5433/ledger_training",
                DbConfig.jdbcUrl(Map.of("PGPORT", "5433", "PGDATABASE", "ledger_training", "PGPASSWORD", "change-me-local-only")));
    }

    // ---------------------------------------------------------------- the M4 contract, on JDBC

    @Nested
    @DisplayName("RepositoryContract on JdbcPaymentRepository")
    class Contract extends RepositoryContractTest {

        private TestDatabase db;

        @Override
        protected PaymentRepository newRepository() {
            db = TestDatabase.create();
            return new JdbcPaymentRepository(db.url(), db.user(), db.password());
        }

        @Override
        protected PaymentRepository reopen() {
            return new JdbcPaymentRepository(db.url(), db.user(), db.password());
        }

        @Override
        protected void closeRepository() {
            if (db != null) {
                db.close();
            }
        }
    }

    // ---------------------------------------------------------------- database invariants

    @Test
    @DisplayName("PostgreSQL itself rejects every invalid row, after accepting a valid one")
    void constraintsRejectInvalidRows() throws SQLException {
        try (TestDatabase db = TestDatabase.create(); Connection c = db.connect()) {
            int valid = insertRequest(c, "SUP-0001", "INV-1", 100L, "USD", "REQ-0001", "SUBMITTED");
            insertRequest(c, "SUP-0001", "INV-2", 999_999_999L, "USD", "REQ-0001", "PAID");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-3", 100L, "EUR", "REQ-0001", "SUBMITTED"), "EUR");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-4", 0L, "USD", "REQ-0001", "SUBMITTED"), "0 cents");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-5", 1_000_000_000L, "USD", "REQ-0001", "SUBMITTED"), "1000000000 cents");
            rejected(c, () -> insertRequest(c, "SUP-12", "INV-6", 100L, "USD", "REQ-0001", "SUBMITTED"), "bad supplier");
            rejected(c, () -> insertRequest(c, "XYZ-0001", "INV-6", 100L, "USD", "REQ-0001", "SUBMITTED"), "bad supplier prefix");
            rejected(c, () -> insertRequest(c, "SUP-0001", "", 100L, "USD", "REQ-0001", "SUBMITTED"), "blank invoice");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-7", 100L, "USD", null, "SUBMITTED"), "null requester");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-8", 100L, "USD", "REQ-0001", "ON_HOLD"), "unknown status");
            rejected(c, () -> insertRequest(c, "SUP-0001", "INV-1", 500L, "USD", "REQ-0002", "SUBMITTED"), "duplicate invoice key");

            insertEvent(c, valid, 1, "REQ-0001", "submitted", "1.00");
            rejected(c, () -> insertEvent(c, valid, 1, "APR-0001", "approved", "1.00"), "duplicate (request_id, seq)");
            rejected(c, () -> insertEvent(c, valid, 2, null, "approved", "1.00"), "null actor");
            rejected(c, () -> insertEvent(c, 999_999, 1, "REQ-0001", "submitted", ""), "event for a missing request");

            insertSettlement(c, "STL-1", valid, 100L, "USD");
            rejected(c, () -> insertSettlement(c, "STL-2", valid, 100L, "USD"), "second settlement for one request");
            rejected(c, () -> insertSettlement(c, "STL-1", valid, 100L, "USD"), "duplicate settlement reference");
            int other = insertRequest(c, "SUP-0002", "INV-1", 100L, "USD", "REQ-0001", "APPROVED");
            rejected(c, () -> insertSettlement(c, "STL-3", other, 100L, "EUR"), "settlement in EUR");
            rejected(c, () -> insertSettlement(c, "STL-4", 999_999, 100L, "USD"), "settlement for a missing request");
        }
    }

    @Test
    @DisplayName("quotes, semicolons and SQL comments in text are stored verbatim (parameter binding)")
    void injectionLikeTextStoredVerbatim() {
        try (TestDatabase db = TestDatabase.create()) {
            JdbcPaymentRepository repo = new JdbcPaymentRepository(db.url(), db.user(), db.password());
            String reason = "it's a 'test'; DROP TABLE payment_requests; -- \" /* comment */";
            PaymentRequest r = new PaymentRequest(repo.nextId(), "SUP-0001", "INV'; DELETE FROM settlements;--", 4_200L,
                    "USD", "REQ-0001");
            repo.save(r);
            r.reject("APR-0001", reason);
            repo.save(r);
            PaymentRequest found = new JdbcPaymentRepository(db.url(), db.user(), db.password()).findById(r.getId());
            assertNotNull(found);
            assertEquals("INV';DELETEFROMSETTLEMENTS;--", found.getInvoiceNumber());
            assertEquals(reason, found.events().get(1).detail());
            assertEquals(r.getInvoiceKey(), found.getInvoiceKey());
            assertNotNull(new JdbcPaymentRepository(db.url(), db.user(), db.password()).findByInvoiceKey(r.getInvoiceKey()));
        }
    }

    @Test
    @DisplayName("seeded requests with every status, corrections and refusals round-trip exactly")
    void seededRoundTrip() {
        try (TestDatabase db = TestDatabase.create()) {
            JdbcPaymentRepository repo = new JdbcPaymentRepository(db.url(), db.user(), db.password());
            Random random = new Random(55L);
            Map<Integer, PaymentRequest> saved = new HashMap<>();
            for (int i = 0; i < 30; i++) {
                long cents = 1 + random.nextInt(999_999_999);
                PaymentRequest r = new PaymentRequest(repo.nextId(), "SUP-" + (1000 + random.nextInt(9000)), "RT-" + i,
                        cents, "USD", random.nextBoolean() ? "REQ-0001" : "REQ-0002");
                repo.save(r);
                if (random.nextBoolean()) {
                    r.correctAmount("REQ-0001", 1 + random.nextInt(999_999_999), "corrected #" + i);
                }
                if (random.nextBoolean()) {
                    r.recordApprovalRefused("APR-0001", "standard");
                }
                switch (random.nextInt(5)) {
                    case 0 -> { }
                    case 1 -> r.reject("APR-0002", "rejected #" + i);
                    case 2 -> r.cancel("REQ-0002", "cancelled #" + i);
                    case 3 -> r.approve("APR-0003");
                    default -> {
                        r.approve("APR-0003");
                        r.cancel("APR-0003", "held #" + i);
                    }
                }
                repo.save(r);
                saved.put(r.getId(), r);
            }
            List<PaymentRequest> all = new JdbcPaymentRepository(db.url(), db.user(), db.password()).findAll();
            assertEquals(saved.size(), all.size());
            int previous = 0;
            for (PaymentRequest found : all) {
                assertTrue(found.getId() > previous, "findAll ordered by id");
                previous = found.getId();
                RepositoryContractTest.assertSameRequest(saved.get(found.getId()), found);
            }
        }
    }

    // ---------------------------------------------------------------- atomic settlement

    @Test
    @DisplayName("markPaid stores the PAID status, the settlement row and the paid event together")
    void settlementStoredTogether() throws SQLException {
        try (TestDatabase db = TestDatabase.create()) {
            PaymentDesk desk = jdbcDesk(db);
            PaymentRequest r = desk.submit("SUP-0001", "S-1", "1250.00", "USD", "REQ-0001");
            desk.approve(r.getId(), "APR-0003");
            desk.markPaid(r.getId(), "APR-0002", "STL-1001", 125_000L, "USD");
            PaymentRequest found = new JdbcPaymentRepository(db.url(), db.user(), db.password()).findById(r.getId());
            assertEquals(PaymentStatus.PAID, found.getStatus());
            assertEquals("paid", found.events().get(found.events().size() - 1).action());
            assertEquals("APR-0002", found.events().get(found.events().size() - 1).actorId());
            try (Connection c = db.connect()) {
                assertEquals(1L, count(c, "SELECT count(*) FROM settlements WHERE reference = 'STL-1001' AND request_id = "
                        + r.getId() + " AND amount_cents = 125000 AND currency = 'USD'"));
            }
        }
    }

    @Test
    @DisplayName("a failure inside saveSettlement rolls back: still APPROVED, no settlement, no paid event")
    void settlementFailureRollsBack() throws SQLException {
        try (TestDatabase db = TestDatabase.create()) {
            PaymentDesk desk = jdbcDesk(db);
            PaymentRequest a = desk.submit("SUP-0001", "A-1", "300.00", "USD", "REQ-0001");
            PaymentRequest b = desk.submit("SUP-0001", "B-1", "400.00", "USD", "REQ-0001");
            desk.approve(a.getId(), "APR-0003");
            desk.approve(b.getId(), "APR-0003");
            try (Connection c = db.connect()) {
                insertSettlement(c, "STL-CLASH", b.getId(), 40_000L, "USD");
            }
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> desk.markPaid(a.getId(), "APR-0002", "STL-CLASH", 30_000L, "USD"));
            if (failure instanceof UnsupportedOperationException) {
                fail("still a TODO stub: " + failure.getMessage());
            }
            try (Connection c = db.connect()) {
                assertEquals(1L, count(c, "SELECT count(*) FROM payment_requests WHERE id = " + a.getId()
                        + " AND status = 'APPROVED'"), "the status update must be rolled back");
                assertEquals(0L, count(c, "SELECT count(*) FROM settlements WHERE request_id = " + a.getId()));
                assertEquals(0L, count(c, "SELECT count(*) FROM payment_events WHERE request_id = " + a.getId()
                        + " AND action = 'paid'"), "the paid event must be rolled back");
            }
            assertEquals(PaymentStatus.APPROVED,
                    new JdbcPaymentRepository(db.url(), db.user(), db.password()).findById(a.getId()).getStatus());
            assertEquals(PaymentStatus.APPROVED, desk.find(a.getId()).getStatus());
            desk.markPaid(a.getId(), "APR-0002", "STL-2002", 30_000L, "USD");
            assertEquals(PaymentStatus.PAID, desk.find(a.getId()).getStatus(), "a later valid settlement still works");
        }
    }

    @Test
    @DisplayName("duplicate invoices are refused through the JDBC desk as well")
    void duplicateRefusedOnJdbc() {
        try (TestDatabase db = TestDatabase.create()) {
            PaymentDesk desk = jdbcDesk(db);
            desk.submit("SUP-0009", "INV 77", "10.00", "USD", "REQ-0001");
            RuntimeException refused = assertThrows(RuntimeException.class,
                    () -> desk.submit("sup-0009", "inv77", "10.00", "USD", "REQ-0002"));
            if (refused instanceof UnsupportedOperationException) {
                fail("still a TODO stub: " + refused.getMessage());
            }
            assertEquals(1, desk.all().size());
        }
    }

    // ---------------------------------------------------------------- helpers

    private static PaymentDesk jdbcDesk(TestDatabase db) {
        InMemoryActorDirectory directory = new InMemoryActorDirectory()
                .addRequester("REQ-0001").addRequester("REQ-0002")
                .addApprover("APR-0001", 500_000L).addApprover("APR-0002", 2_500_000L)
                .addApprover("APR-0003", 999_999_999L);
        return new PaymentDesk(new JdbcPaymentRepository(db.url(), db.user(), db.password()), directory,
                Policies.named("standard"));
    }

    private interface SqlAction {
        void run() throws SQLException;
    }

    private static void rejected(Connection c, SqlAction insert, String what) {
        try {
            insert.run();
        } catch (SQLException expected) {
            return;
        }
        fail("the schema must reject: " + what);
    }

    private static int insertRequest(Connection c, String supplier, String invoice, long cents, String currency,
                                     String requester, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payment_requests (supplier_id, invoice_number, amount_cents, currency, requested_by, status)"
                        + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, supplier);
            ps.setString(2, invoice);
            ps.setLong(3, cents);
            ps.setString(4, currency);
            ps.setString(5, requester);
            ps.setString(6, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void insertEvent(Connection c, int requestId, int seq, String actor, String action, String detail)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payment_events (request_id, seq, actor_id, action, detail) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, requestId);
            ps.setInt(2, seq);
            ps.setString(3, actor);
            ps.setString(4, action);
            ps.setString(5, detail);
            ps.executeUpdate();
        }
    }

    private static void insertSettlement(Connection c, String reference, int requestId, long cents, String currency)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO settlements (reference, request_id, amount_cents, currency) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, reference);
            ps.setInt(2, requestId);
            ps.setLong(3, cents);
            ps.setString(4, currency);
            ps.executeUpdate();
        }
    }

    private static long count(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
