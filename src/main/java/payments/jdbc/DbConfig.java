package payments.jdbc;

import java.util.Map;

/**
 * Database configuration from the environment.
 *
 * <p>LEARNER-OWNED (milestone M5). Never hard-code a host, database, user or password, and never
 * write a password into the project: PostgreSQL's JDBC driver reads it from your password file
 * (~/.pgpass) like psql does, or from PGPASSWORD in your own shell.
 */
public final class DbConfig {

    private DbConfig() {
    }

    /**
     * Builds {@code jdbc:postgresql://HOST:PORT/DATABASE} from PGHOST, PGPORT and PGDATABASE,
     * with the defaults localhost, 5432 and postgres for any missing or blank value.
     * For example an empty map gives {@code jdbc:postgresql://localhost:5432/postgres}.
     *
     * @param env the environment, usually {@code System.getenv()}
     */
    public static String jdbcUrl(Map<String, String> env) {
        // TODO(M5): implement as described above (read only from `env`, never from constants).
        throw new UnsupportedOperationException("TODO M5: DbConfig.jdbcUrl");
    }
}
