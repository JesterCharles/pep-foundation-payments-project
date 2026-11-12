package payments.reports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One line of a synthetic bank-statement export (supplied in release 6; do not edit).
 * No real bank, account or reference is ever used.
 *
 * @param reference the statement's settlement reference, e.g. "STL-1001"
 * @param requestId the payment request the line claims to settle
 * @param cents     the settled amount in cents
 * @param currency  the currency code exactly as exported (only "USD" can match)
 */
public record StatementLine(String reference, int requestId, long cents, String currency) {

    /** The header line of every supplied statement CSV. */
    public static final String HEADER = "reference,request_id,cents,currency";

    /**
     * Parses one CSV line {@code reference,request_id,cents,currency}.
     *
     * @throws IllegalArgumentException when the line does not have that shape
     */
    public static StatementLine parse(String csvLine) {
        if (csvLine == null) {
            throw new IllegalArgumentException("statement line is missing");
        }
        String[] fields = csvLine.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("a statement line needs 4 fields");
        }
        try {
            return new StatementLine(fields[0].trim(), Integer.parseInt(fields[1].trim()),
                    Long.parseLong(fields[2].trim()), fields[3].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("request_id and cents must be whole numbers");
        }
    }

    /** Reads a statement CSV: the header line, then one line per entry. Blank lines are ignored. */
    public static List<StatementLine> readCsv(Path file) throws IOException {
        List<String> raw = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (raw.isEmpty() || !raw.get(0).trim().equals(HEADER)) {
            throw new IllegalArgumentException("statement must start with the header " + HEADER);
        }
        List<StatementLine> lines = new ArrayList<>();
        for (int i = 1; i < raw.size(); i++) {
            if (!raw.get(i).isBlank()) {
                lines.add(parse(raw.get(i)));
            }
        }
        return lines;
    }
}
