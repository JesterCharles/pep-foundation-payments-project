package payments.jdbc;

import java.util.Map;

/**
 * Database configuration from the environment (supplied baseline; participant-editable).
 *
 * <p>Never hard-code a host, database, user or password, and never write a password into the
 * project. The default store is a local H2 file database in PostgreSQL-compatible mode under the
 * git-ignored {@code data/} folder, so the baseline runs offline with no database server.
 */
public final class DbConfig {

    /** Default local database: H2 file under data/, PostgreSQL-compatible mode. */
    public static final String DEFAULT_LOCAL_URL =
            "jdbc:h2:file:./data/payments;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    private DbConfig() {
    }

    /**
     * Builds {@code jdbc:postgresql://HOST:PORT/DATABASE} from PGHOST, PGPORT and PGDATABASE,
     * with the defaults localhost, 5432 and postgres for any missing or blank value.
     */
    public static String jdbcUrl(Map<String, String> env) {
        return "jdbc:postgresql://" + value(env, "PGHOST", "localhost") + ":" + value(env, "PGPORT", "5432")
                + "/" + value(env, "PGDATABASE", "postgres");
    }

    /**
     * The JDBC URL the baseline uses: PAYMENTS_JDBC_URL when set, otherwise the local H2 file
     * database {@link #DEFAULT_LOCAL_URL}.
     */
    public static String localUrl(Map<String, String> env) {
        return value(env, "PAYMENTS_JDBC_URL", DEFAULT_LOCAL_URL);
    }

    private static String value(Map<String, String> env, String name, String fallback) {
        String value = env == null ? null : env.get(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
