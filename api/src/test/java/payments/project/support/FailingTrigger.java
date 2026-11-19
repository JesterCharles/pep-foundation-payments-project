package payments.project.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.h2.api.Trigger;

/**
 * H2 trigger used by the supplied approval-transaction checks to force a database failure in the middle
 * of an approval (supplied; do not edit). It fails a write that belongs to the armed request id only:
 * an approvals insert, a payment_requests update to APPROVED, or a payment_events insert of "approved".
 */
public final class FailingTrigger implements Trigger {

    /** The request id whose approval must fail, or -1. */
    public static volatile long armedRequestId = -1;

    private String table;
    private List<String> columns;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        table = tableName.toLowerCase(Locale.ROOT);
        columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (armedRequestId < 0 || newRow == null) {
            return;
        }
        boolean hit = switch (table) {
            case "approvals" -> armedRequestId == number(newRow, "request_id");
            case "payment_requests" -> armedRequestId == number(newRow, "id")
                    && "APPROVED".equalsIgnoreCase(String.valueOf(value(newRow, "status")));
            case "payment_events" -> armedRequestId == number(newRow, "request_id")
                    && "approved".equalsIgnoreCase(String.valueOf(value(newRow, "action")));
            default -> false;
        };
        if (hit) {
            throw new SQLException("forced failure for the supplied approval-transaction check", "P0001");
        }
    }

    private Object value(Object[] row, String column) {
        int index = columns.indexOf(column);
        return index < 0 || index >= row.length ? null : row[index];
    }

    private long number(Object[] row, String column) {
        Object value = value(row, column);
        return value instanceof Number n ? n.longValue() : -2;
    }
}
