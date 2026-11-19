package payments.reports;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.intake.PaymentRules;

/**
 * Exact money reports over payment requests (supplied baseline; participant-editable).
 * Sums long cents and formats only at the edge. Never returns null.
 */
public final class PaymentReports {

    private PaymentReports() {
    }

    /** Cents per status, with ALL statuses present (0 where there are none). */
    public static Map<PaymentStatus, Long> totalsByStatus(List<PaymentRequest> requests) {
        Map<PaymentStatus, Long> totals = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            totals.put(status, 0L);
        }
        for (PaymentRequest request : requests) {
            totals.merge(request.getStatus(), request.getAmountCents(), Long::sum);
        }
        return totals;
    }

    /** Cents per supplier id for requests in the given status (suppliers with none are absent). */
    public static Map<String, Long> totalsBySupplier(List<PaymentRequest> requests, PaymentStatus status) {
        Map<String, Long> totals = new TreeMap<>();
        requests.stream()
                .filter(r -> r.getStatus() == status)
                .forEach(r -> totals.merge(r.getSupplierId(), r.getAmountCents(), Long::sum));
        return totals;
    }

    /**
     * The n largest APPROVED requests by amount, largest first; equal amounts by id ascending.
     *
     * @throws IllegalArgumentException when n < 0
     */
    public static List<PaymentRequest> largestOutstanding(List<PaymentRequest> requests, int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative");
        }
        return requests.stream()
                .filter(r -> r.getStatus() == PaymentStatus.APPROVED)
                .sorted(Comparator.comparingLong(PaymentRequest::getAmountCents).reversed()
                        .thenComparingInt(PaymentRequest::getId))
                .limit(n)
                .toList();
    }

    /** The request with this invoice key, or Optional.empty() when there is none or the key is invalid. */
    public static Optional<PaymentRequest> findByInvoice(List<PaymentRequest> requests, String supplierId,
                                                         String invoiceNumber) {
        String key = PaymentRules.invoiceKey(supplierId, invoiceNumber);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return requests.stream().filter(r -> r.getInvoiceKey().equals(key)).findFirst();
    }
}
