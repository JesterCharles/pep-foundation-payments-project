package payments.reports;

import java.util.List;

import payments.domain.PaymentRequest;

/**
 * Reconciles a synthetic statement export against recorded payment requests.
 *
 * <p>LEARNER-OWNED (milestone M6). Collections, Comparator chains, streams and Optional are all
 * available from this milestone.
 */
public final class Reconciler {

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
     * paidWithoutLine. Every list is ordered by request id, then reference. No conversion, fees
     * or partial payments: any difference is a discrepancy.
     */
    public static ReconciliationResult reconcile(List<PaymentRequest> requests, List<StatementLine> lines) {
        // TODO(M6): implement. Use a Set for references already seen and long cents throughout.
        throw new UnsupportedOperationException("TODO M6: Reconciler.reconcile");
    }
}
