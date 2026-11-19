package payments.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import payments.desk.PaymentConflictException;
import payments.desk.PaymentRepository;
import payments.domain.PaymentEvent;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.domain.Settlement;

/**
 * PaymentRepository using plain JDBC (supplied baseline; participant-editable starting source).
 *
 * <ul>
 *   <li>PreparedStatement with {@code ?} parameters for every value; SQL is never built from data.</li>
 *   <li>One transaction per save and per settlement: autocommit off, commit, roll back and rethrow
 *       on any failure; every resource is closed with try-with-resources.</li>
 *   <li>Reads rebuild requests with PaymentRequest.restore(...); lists are ordered by id.</li>
 * </ul>
 * Tables and columns are defined in {@code src/main/resources/db/schema.sql}.
 */
public final class JdbcPaymentRepository implements PaymentRepository {

    private final String jdbcUrl;
    private final String user;
    private final String password;

    /**
     * @param jdbcUrl  for example {@link DbConfig#localUrl(java.util.Map)}
     * @param user     the database user, or null for H2's default
     * @param password the password, or null (PostgreSQL then reads ~/.pgpass)
     */
    public JdbcPaymentRepository(String jdbcUrl, String user, String password) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("a JDBC URL is required");
        }
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    /** Applies {@code db/schema.sql} (idempotent: every table uses IF NOT EXISTS). */
    public void createSchemaIfMissing() {
        try (Connection c = connect(); Statement statement = c.createStatement()) {
            for (String sql : schemaStatements()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not apply db/schema.sql: " + e.getMessage(), e);
        }
    }

    @Override
    public void save(PaymentRequest request) {
        inTransaction(c -> {
            upsertRequest(c, request);
            insertNewEvents(c, request);
        });
    }

    @Override
    public void saveSettlement(PaymentRequest request, Settlement settlement) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE payment_requests SET status = ? WHERE id = ?")) {
                ps.setString(1, request.getStatus().name());
                ps.setInt(2, request.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO settlements (reference, request_id, amount_cents, currency) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, settlement.reference());
                ps.setInt(2, settlement.requestId());
                ps.setLong(3, settlement.cents());
                ps.setString(4, settlement.currency());
                ps.executeUpdate();
            }
            insertNewEvents(c, request);
        });
    }

    @Override
    public PaymentRequest findById(int id) {
        List<PaymentRequest> found = query("WHERE id = ?", ps -> ps.setInt(1, id));
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public List<PaymentRequest> findAll() {
        return query("", ps -> { });
    }

    @Override
    public PaymentRequest findByInvoiceKey(String invoiceKey) {
        if (invoiceKey == null || invoiceKey.indexOf('|') < 0) {
            return null;
        }
        String supplier = invoiceKey.substring(0, invoiceKey.indexOf('|'));
        String invoice = invoiceKey.substring(invoiceKey.indexOf('|') + 1);
        List<PaymentRequest> found = query("WHERE supplier_id = ? AND invoice_number = ?", ps -> {
            ps.setString(1, supplier);
            ps.setString(2, invoice);
        });
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public int nextId() {
        try (Connection c = connect();
             PreparedStatement max = c.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM payment_requests");
             PreparedStatement next = c.prepareStatement("SELECT nextval('payment_request_ids')")) {
            int highest;
            try (ResultSet rs = max.executeQuery()) {
                rs.next();
                highest = rs.getInt(1);
            }
            while (true) {
                try (ResultSet rs = next.executeQuery()) {
                    rs.next();
                    int id = rs.getInt(1);
                    if (id > highest) {
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not allocate a request id: " + e.getMessage(), e);
        }
    }

    private void upsertRequest(Connection c, PaymentRequest request) throws SQLException {
        try (PreparedStatement update = c.prepareStatement(
                "UPDATE payment_requests SET amount_cents = ?, status = ? WHERE id = ?")) {
            update.setLong(1, request.getAmountCents());
            update.setString(2, request.getStatus().name());
            update.setInt(3, request.getId());
            if (update.executeUpdate() == 1) {
                return;
            }
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO payment_requests (id, supplier_id, invoice_number, amount_cents, currency, requested_by, status)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            insert.setInt(1, request.getId());
            insert.setString(2, request.getSupplierId());
            insert.setString(3, request.getInvoiceNumber());
            insert.setLong(4, request.getAmountCents());
            insert.setString(5, request.getCurrency());
            insert.setString(6, request.getRequestedBy());
            insert.setString(7, request.getStatus().name());
            insert.executeUpdate();
        }
    }

    private void insertNewEvents(Connection c, PaymentRequest request) throws SQLException {
        int stored;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(MAX(seq), 0) FROM payment_events WHERE request_id = ?")) {
            ps.setInt(1, request.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                stored = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payment_events (request_id, seq, actor_id, action, detail) VALUES (?, ?, ?, ?, ?)")) {
            for (PaymentEvent event : request.events()) {
                if (event.seq() > stored) {
                    ps.setInt(1, request.getId());
                    ps.setInt(2, event.seq());
                    ps.setString(3, event.actorId());
                    ps.setString(4, event.action());
                    ps.setString(5, event.detail());
                    ps.executeUpdate();
                }
            }
        }
    }

    private List<PaymentRequest> query(String where, Binder binder) {
        String sql = "SELECT id, supplier_id, invoice_number, amount_cents, currency, requested_by, status"
                + " FROM payment_requests " + where + " ORDER BY id";
        List<PaymentRequest> requests = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    requests.add(PaymentRequest.restore(id, rs.getString("supplier_id"),
                            rs.getString("invoice_number"), rs.getLong("amount_cents"), rs.getString("currency"),
                            rs.getString("requested_by"), PaymentStatus.valueOf(rs.getString("status")),
                            events(c, id)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not read payment requests: " + e.getMessage(), e);
        }
        return requests;
    }

    private static List<PaymentEvent> events(Connection c, int requestId) throws SQLException {
        List<PaymentEvent> events = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT seq, actor_id, action, detail FROM payment_events WHERE request_id = ? ORDER BY seq")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new PaymentEvent(requestId, rs.getInt("seq"), rs.getString("actor_id"),
                            rs.getString("action"), rs.getString("detail")));
                }
            }
        }
        return events;
    }

    private void inTransaction(Work work) {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException failure) {
                c.rollback();
                if (failure instanceof SQLException sql && isUniqueViolation(sql)) {
                    throw new PaymentConflictException("stored data already exists: " + sql.getMessage());
                }
                throw failure;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database write failed and was rolled back: " + e.getMessage(), e);
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        // 23505 is the SQL standard state for a unique-constraint violation (PostgreSQL and H2).
        return "23505".equals(e.getSQLState());
    }

    private Connection connect() throws SQLException {
        Properties props = new Properties();
        if (user != null) {
            props.setProperty("user", user);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return DriverManager.getConnection(jdbcUrl, props);
    }

    static List<String> schemaStatements() {
        try (InputStream in = JdbcPaymentRepository.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("db/schema.sql is not on the classpath");
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder cleaned = new StringBuilder();
            for (String line : text.split("\n")) {
                if (!line.trim().startsWith("--")) {
                    cleaned.append(line).append('\n');
                }
            }
            List<String> statements = new ArrayList<>();
            for (String part : cleaned.toString().split(";")) {
                if (!part.isBlank()) {
                    statements.add(part.trim());
                }
            }
            return statements;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface Work {
        void run(Connection c) throws SQLException;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
