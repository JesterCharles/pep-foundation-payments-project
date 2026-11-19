package payments.project.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Reads the persistence tables named in the contract (docs/contracts/W07-openapi.yaml {@code x-persistence};
 * W08 adds constraints and the approvals table) with plain JDBC (supplied; do not edit).
 */
public final class Tables {

    private final DataSource dataSource;

    public Tables(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Rows of a query, each row a map from lower-case column label to value. */
    public List<Map<String, Object>> rows(String sql, Object... params) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                int columns = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new AssertionError("query failed (check the table and column names in the contract): " + sql
                    + ": " + e.getMessage(), e);
        }
    }

    public long count(String sql, Object... params) {
        Object value = rows(sql, params).get(0).values().iterator().next();
        return ((Number) value).longValue();
    }

    /** Runs a statement; returns the SQLException it raised, or null when it succeeded. */
    public SQLException tryUpdate(String sql, Object... params) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return e;
        }
    }

    public void execute(String sql) {
        SQLException problem = tryUpdate(sql);
        if (problem != null) {
            throw new AssertionError(sql + ": " + problem.getMessage(), problem);
        }
    }
}
