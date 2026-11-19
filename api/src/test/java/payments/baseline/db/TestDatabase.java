package payments.baseline.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

/**
 * A throw-away database for one baseline test (supplied baseline test support).
 *
 * <p>{@link #create()} opens a uniquely named in-memory H2 database in PostgreSQL mode, applies
 * {@code src/main/resources/db/schema.sql}, and {@link #close()} drops it. No database server,
 * network or password is needed. Use it with try-with-resources.
 */
public final class TestDatabase implements AutoCloseable {

    private final String url;
    private boolean dropped;

    private TestDatabase(String url) {
        this.url = url;
    }

    /** Creates a fresh database with the schema applied. */
    public static TestDatabase create() {
        String name = "payments_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toLowerCase(Locale.ROOT);
        TestDatabase db = new TestDatabase("jdbc:h2:mem:" + name
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        try (Connection connection = db.connect(); Statement statement = connection.createStatement()) {
            for (String sql : schema().split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException | IOException schemaProblem) {
            db.close();
            Assertions.fail("Could not apply src/main/resources/db/schema.sql: " + schemaProblem.getMessage());
        }
        return db;
    }

    /** JDBC URL of this test database. */
    public String url() {
        return url;
    }

    /** Database user (H2's default user needs none). */
    public String user() {
        return null;
    }

    /** No password is used. */
    public String password() {
        return null;
    }

    /** A new connection to this test database (close it yourself). */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /** Drops the database. Safe to call twice. */
    @Override
    public void close() {
        if (dropped) {
            return;
        }
        dropped = true;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            System.err.println("TestDatabase: could not drop " + url + ": " + e.getMessage());
        }
    }

    private static String schema() throws IOException {
        try (InputStream in = TestDatabase.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IOException("db/schema.sql is not on the classpath (expected under src/main/resources)");
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            return sql.toString();
        }
    }
}
