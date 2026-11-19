package payments.reports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;

/**
 * Reconciles a synthetic statement export against recorded payment requests
 * (supplied baseline; participant-editable starting source).
 */
public final class Reconciler {

    private static final Comparator<StatementLine> LINE_ORDER =
            Comparator.comparingInt(StatementLine::requestId).thenComparing(StatementLine::reference);

    private Reconciler() {
    }

    /**
     * Classifies every statement line exactly once, checking in this order:
     * <ol>
     *   <li>its reference already appeared on an EARLIER line of the list: duplicateLine;</li>
     *   <li>its request id matches no request: unknownReference;</li>
     *   <li>its currency is not exactly "USD": currencyMismatch;</li>
     *   <li>its cents differ from the request's amount: amountMismatch;</li>
     *   <li>otherwise: matched.</li>
     * </ol>
     * Then every PAID request with no matched, amountMismatch or currencyMismatch line goes to
     * paidWithoutLine. Every list is ordered by request id, then reference. Any difference is a
     * discrepancy: no conversion, fees or partial payments.
     */
    public static ReconciliationResult reconcile(List<PaymentRequest> requests, List<StatementLine> lines) {
        Map<Integer, PaymentRequest> byId = new HashMap<>();
        for (PaymentRequest request : requests) {
            byId.put(request.getId(), request);
        }
        List<StatementLine> matched = new ArrayList<>();
        List<StatementLine> amountMismatch = new ArrayList<>();
        List<StatementLine> currencyMismatch = new ArrayList<>();
        List<StatementLine> unknown = new ArrayList<>();
        List<StatementLine> duplicate = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<Integer> covered = new HashSet<>();
        for (StatementLine line : lines) {
            PaymentRequest request = byId.get(line.requestId());
            if (!seen.add(line.reference())) {
                duplicate.add(line);
            } else if (request == null) {
                unknown.add(line);
            } else if (!"USD".equals(line.currency())) {
                currencyMismatch.add(line);
                covered.add(line.requestId());
            } else if (line.cents() != request.getAmountCents()) {
                amountMismatch.add(line);
                covered.add(line.requestId());
            } else {
                matched.add(line);
                covered.add(line.requestId());
            }
        }
        List<PaymentRequest> paidWithoutLine = requests.stream()
                .filter(r -> r.getStatus() == PaymentStatus.PAID && !covered.contains(r.getId()))
                .sorted(Comparator.comparingInt(PaymentRequest::getId))
                .toList();
        return new ReconciliationResult(sorted(matched), sorted(amountMismatch), sorted(currencyMismatch),
                sorted(unknown), sorted(duplicate), paidWithoutLine);
    }

    private static List<StatementLine> sorted(List<StatementLine> lines) {
        return lines.stream().sorted(LINE_ORDER).toList();
    }
}
