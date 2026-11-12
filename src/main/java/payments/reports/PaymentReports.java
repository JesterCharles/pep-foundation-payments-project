package payments.reports;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;

/**
 * Exact money reports over payment requests.
 *
 * <p>LEARNER-OWNED (milestone M6). Sum long cents and format only at the edge. Never return null.
 */
public final class PaymentReports {

    private PaymentReports() {
    }

    /** Cents per status, with ALL five statuses present (0 where there are none). */
    public static Map<PaymentStatus, Long> totalsByStatus(List<PaymentRequest> requests) {
        // TODO(M6)
        throw new UnsupportedOperationException("TODO M6: PaymentReports.totalsByStatus");
    }

    /** Cents per supplier id for requests in the given status (suppliers with none are absent). */
    public static Map<String, Long> totalsBySupplier(List<PaymentRequest> requests, PaymentStatus status) {
        // TODO(M6)
        throw new UnsupportedOperationException("TODO M6: PaymentReports.totalsBySupplier");
    }

    /**
     * The n largest APPROVED requests by amount, largest first; equal amounts by id ascending.
     * n = 0 gives an empty list; n larger than the count gives them all.
     *
     * @throws IllegalArgumentException when n < 0
     */
    public static List<PaymentRequest> largestOutstanding(List<PaymentRequest> requests, int n) {
        // TODO(M6)
        throw new UnsupportedOperationException("TODO M6: PaymentReports.largestOutstanding");
    }

    /**
     * The request whose invoice key equals PaymentRules.invoiceKey(supplierId, invoiceNumber),
     * or Optional.empty() when there is none or the key is invalid.
     */
    public static Optional<PaymentRequest> findByInvoice(List<PaymentRequest> requests, String supplierId,
                                                         String invoiceNumber) {
        // TODO(M6)
        throw new UnsupportedOperationException("TODO M6: PaymentReports.findByInvoice");
    }
}
