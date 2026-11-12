package payments.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * A throw-away PostgreSQL database for one test (supplied in release 5; do not edit).
 *
 * <p>{@link #create()} connects to your LOCAL PostgreSQL with the same PG* variables psql uses,
 * creates a uniquely named database (payments_test_...), applies src/main/resources/db/schema.sql,
 * and {@link #close()} drops it again. Use it with try-with-resources.
 * <ul>
 *   <li>PGHOST (default localhost), PGPORT (default 5432), PGUSER (default: your login name).</li>
 *   <li>PGDATABASE (default postgres): an existing database used only to run CREATE/DROP DATABASE.</li>
 *   <li>The password comes from your password file (~/.pgpass, or PGPASSFILE), which the driver
 *       reads like psql does; PGPASSWORD set in your own shell also works. Never commit one.</li>
 * </ul>
 * When no database is reachable the test FAILS with a message saying what to set. Only if you
 * run {@code mvn -q test -Pm5 -Dpayments.db=skip} are database tests reported as skipped instead;
 * such a run is not evidence that milestone 5 works.
 */
public final class TestDatabase implements AutoCloseable {

    private final String name;
    private final String url;
    private final String user;
    private final String password;
    private boolean dropped;

    private TestDatabase(String name, String url, String user, String password) {
        this.name = name;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Creates a fresh database with the schema applied. */
    public static TestDatabase create() {
        String host = env("PGHOST", "localhost");
        if (host.startsWith("/")) {
            Assertions.fail("PGHOST is a socket folder (" + host + "). The tests connect over TCP: set PGHOST=localhost.");
        }
        String base = "jdbc:postgresql://" + host + ":" + env("PGPORT", "5432") + "/";
        String user = env("PGUSER", System.getProperty("user.name"));
        String password = System.getenv("PGPASSWORD");
        String name = "payments_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toLowerCase(Locale.ROOT);
        try (Connection admin = open(base + env("PGDATABASE", "postgres"), user, password);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException unreachable) {
            unreachable(unreachable);
        }
        TestDatabase db = new TestDatabase(name, base + name, user, password);
        try (Connection connection = db.connect(); Statement statement = connection.createStatement()) {
            statement.execute(schema());
        } catch (SQLException | IOException schemaProblem) {
            db.close();
            Assertions.fail("Could not apply src/main/resources/db/schema.sql: " + schemaProblem.getMessage());
        }
        boolean tablesExist;
        try (Connection connection = db.connect(); Statement statement = connection.createStatement();
             java.sql.ResultSet rs = statement.executeQuery("SELECT to_regclass('payment_requests') IS NOT NULL"
                     + " AND to_regclass('payment_events') IS NOT NULL AND to_regclass('settlements') IS NOT NULL")) {
            tablesExist = rs.next() && rs.getBoolean(1);
        } catch (SQLException checkProblem) {
            tablesExist = false;
        }
        if (!tablesExist) {
            db.close();
            Assertions.fail("TODO M5: create the payment_requests, payment_events and settlements tables"
                    + " in src/main/resources/db/schema.sql");
        }
        return db;
    }

    /** JDBC URL of this test database. */
    public String url() {
        return url;
    }

    /** Database user (PGUSER or your login name). */
    public String user() {
        return user;
    }

    /** PGPASSWORD from your shell, or null so the driver reads your password file. */
    public String password() {
        return password;
    }

    /** A new connection to this test database (close it yourself). */
    public Connection connect() throws SQLException {
        return open(url, user, password);
    }

    /** Drops the database. Safe to call twice. */
    @Override
    public void close() {
        if (dropped) {
            return;
        }
        dropped = true;
        String adminUrl = url.substring(0, url.lastIndexOf('/') + 1) + env("PGDATABASE", "postgres");
        try (Connection admin = open(adminUrl, user, password); Statement statement = admin.createStatement()) {
            try {
                statement.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
            } catch (SQLException olderServer) {
                statement.execute("DROP DATABASE IF EXISTS " + name);
            }
        } catch (SQLException e) {
            System.err.println("TestDatabase: could not drop " + name + ": " + e.getMessage());
        }
    }

    private static Connection open(String url, String user, String password) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", user);
        if (password != null) {
            props.setProperty("password", password);
        }
        props.setProperty("connectTimeout", "5");
        return DriverManager.getConnection(url, props);
    }

    private static String schema() throws IOException {
        try (InputStream in = TestDatabase.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IOException("db/schema.sql is not on the classpath (expected under src/main/resources)");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void unreachable(SQLException cause) {
        String why = cause.getMessage() != null && cause.getMessage().contains("No suitable driver")
                ? "the PostgreSQL JDBC driver is missing: apply release/m5/pom-additions.md"
                : "PostgreSQL is not reachable (" + cause.getMessage() + ")";
        String message = "M5 database tests need a local PostgreSQL: " + why + ". "
                + "Set PGHOST, PGPORT, PGUSER and PGDATABASE as in the JDBC lab, keep the password in ~/.pgpass, "
                + "and check `psql -c 'select 1'` works in the same shell.";
        if ("skip".equals(System.getProperty("payments.db"))) {
            Assumptions.abort("SKIPPED (-Dpayments.db=skip; not milestone evidence): " + message);
        }
        Assertions.fail(message);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
