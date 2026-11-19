package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/**
 * W08 schema checks (supplied; do not edit): Flyway builds the contract schema on an empty database, the
 * database itself rejects rows that break the payment invariants, and tracked configuration holds no secret.
 */
public abstract class MigrationChecks extends DevProfileCheck {

    private static final String INSERT = "INSERT INTO payment_requests "
            + "(id, supplier_id, invoice_number, amount_cents, currency, requested_by, status, submitted_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
    private static final Pattern SENSITIVE_PROPERTY = Pattern.compile(
            "(?i)(password|secret|token|api-key|apikey|private-key)$");
    private static final Pattern ENVIRONMENT_PLACEHOLDER = Pattern.compile(
            "^\\$\\{[A-Za-z_][A-Za-z0-9_.-]*:?}$");

    @Test
    @DisplayName("Flyway migrations applied to the empty test database and created the contract tables")
    void flywayApplied() {
        Tables tables = new Tables(dataSource());
        long applied = tables.count("SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE");
        assertTrue(applied >= 1, "expected at least one successful Flyway migration");
        for (String table : List.of("payment_requests", "payment_events", "approvals")) {
            tables.count("SELECT COUNT(*) FROM " + table);
        }
    }

    @Test
    @DisplayName("the database accepts a valid row and rejects non-USD, out-of-range cents, unknown statuses and duplicate invoices")
    void constraints() {
        Tables tables = new Tables(dataSource());
        String supplier = "SUP-0777";
        String invoice = data.invoice();
        assertNull(tables.tryUpdate(INSERT, 900_001, supplier, invoice, 100L, "USD", "REQ-0001", "SUBMITTED"),
                "a valid row must be accepted (extra columns you add need a DEFAULT or must be nullable)");
        assertRejected(tables, "EUR currency", 900_002, data.invoice(), 100L, "EUR", "SUBMITTED");
        assertRejected(tables, "zero cents", 900_003, data.invoice(), 0L, "USD", "SUBMITTED");
        assertRejected(tables, "negative cents", 900_004, data.invoice(), -1L, "USD", "SUBMITTED");
        assertRejected(tables, "cents above 999999999", 900_005, data.invoice(), 1_000_000_000L, "USD", "SUBMITTED");
        assertRejected(tables, "unknown status", 900_006, data.invoice(), 100L, "USD", "BOGUS");
        assertRejected(tables, "duplicate (supplier_id, invoice_number)", 900_007, invoice, 100L, "USD", "SUBMITTED");
        assertEquals(1, tables.count("SELECT COUNT(*) FROM payment_requests WHERE supplier_id = ?", supplier));
    }

    @Test
    @DisplayName("tracked configuration files hold no literal password, secret or key")
    void noSecretInConfig() throws IOException {
        List<String> problems = new ArrayList<>();
        Path resources = Path.of("src", "main", "resources");
        if (!Files.isDirectory(resources)) {
            return;
        }
        try (Stream<Path> files = Files.walk(resources)) {
            for (Path file : files.filter(p -> p.getFileName().toString().matches("application.*\\.(ya?ml|properties)")).toList()) {
                FileSystemResource resource = new FileSystemResource(file);
                List<PropertySource<?>> sources = file.getFileName().toString().matches(".*\\.ya?ml")
                        ? new YamlPropertySourceLoader().load(file.toString(), resource)
                        : new PropertiesPropertySourceLoader().load(file.toString(), resource);
                for (PropertySource<?> source : sources) {
                    if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                        continue;
                    }
                    for (String name : enumerable.getPropertyNames()) {
                        Object raw = enumerable.getProperty(name);
                        String value = raw == null ? "" : raw.toString().trim();
                        if (SENSITIVE_PROPERTY.matcher(name).find()
                                && !value.isEmpty()
                                && !ENVIRONMENT_PLACEHOLDER.matcher(value).matches()) {
                            problems.add(file + ": " + name);
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "read secrets from the environment (${NAME}) instead: " + problems);
    }

    private static void assertRejected(Tables tables, String what, int id, String invoice, long cents, String currency, String status) {
        SQLException problem = tables.tryUpdate(INSERT, id, "SUP-0777", invoice, cents, currency, "REQ-0001", status);
        assertNotNull(problem, "the schema must reject: " + what);
    }
}
