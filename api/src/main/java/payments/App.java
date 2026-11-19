package payments;

import java.nio.file.Path;
import java.util.Map;

import payments.api.PaymentApi;
import payments.desk.InMemoryPaymentRepository;
import payments.desk.PaymentDesk;
import payments.desk.PaymentRepository;
import payments.domain.Policies;
import payments.identity.InMemoryActorDirectory;
import payments.jdbc.DbConfig;
import payments.jdbc.JdbcPaymentRepository;

/**
 * Starts the supplied Javalin baseline service (supplied baseline; participant-editable).
 *
 * <p>Configuration comes from environment variables only (see ../.env.example):
 * <ul>
 *   <li>PAYMENTS_PORT: HTTP port, default 7070.</li>
 *   <li>PAYMENTS_STORE: "memory" (default) or "jdbc". jdbc uses PAYMENTS_JDBC_URL, or by default a
 *       local H2 file database in PostgreSQL mode under the git-ignored data/ folder.</li>
 *   <li>PAYMENTS_POLICY: approval policy name for Policies.named, default "standard".</li>
 *   <li>PAYMENTS_STATEMENTS_DIR: folder of statement fixtures for the reconciliation report route,
 *       default src/test/resources/payments/baseline/statements.</li>
 * </ul>
 * Identity uses the supplied synthetic actor directory and the X-Actor-Id header: a stub, not
 * authentication. All data is synthetic; no real money moves.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        int port = Integer.parseInt(valueOr(env, "PAYMENTS_PORT", "7070"));
        String store = valueOr(env, "PAYMENTS_STORE", "memory");
        String policy = valueOr(env, "PAYMENTS_POLICY", "standard");
        Path statements = Path.of(valueOr(env, "PAYMENTS_STATEMENTS_DIR",
                "src/test/resources/payments/baseline/statements"));

        PaymentRepository repository;
        if (store.equals("memory")) {
            repository = new InMemoryPaymentRepository();
        } else if (store.equals("jdbc")) {
            JdbcPaymentRepository jdbc = new JdbcPaymentRepository(DbConfig.localUrl(env), env.get("PAYMENTS_DB_USER"),
                    env.get("PAYMENTS_DB_PASSWORD"));
            jdbc.createSchemaIfMissing();
            repository = jdbc;
        } else {
            throw new IllegalArgumentException("PAYMENTS_STORE must be memory or jdbc");
        }

        InMemoryActorDirectory directory = InMemoryActorDirectory.withSampleActors();
        PaymentDesk desk = new PaymentDesk(repository, directory, Policies.named(policy));
        PaymentApi.start(desk, directory, statements, port);
        System.out.println("Payment baseline service (training project, synthetic data) on http://127.0.0.1:" + port
                + " store=" + store + " policy=" + policy);
    }

    private static String valueOr(Map<String, String> env, String name, String fallback) {
        String value = env.get(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
