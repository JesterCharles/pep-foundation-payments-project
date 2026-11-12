package payments.reports;

import java.util.List;

import payments.domain.PaymentRequest;

/**
 * The outcome of reconciling a statement against recorded payment requests
 * (supplied in release 6; do not edit). Each statement line appears in exactly one of the first
 * five lists. Every list is ordered by request id, then reference.
 *
 * @param matched          known request, currency "USD", exact cents
 * @param amountMismatch   known request, "USD", different cents
 * @param currencyMismatch known request, currency other than "USD"
 * @param unknownReference line whose request id matches no request
 * @param duplicateLine    line whose reference already appeared earlier in the statement
 * @param paidWithoutLine  PAID requests with no matched or mismatched line
 */
public record ReconciliationResult(List<StatementLine> matched, List<StatementLine> amountMismatch,
                                   List<StatementLine> currencyMismatch, List<StatementLine> unknownReference,
                                   List<StatementLine> duplicateLine, List<PaymentRequest> paidWithoutLine) {

    public ReconciliationResult {
        matched = List.copyOf(matched);
        amountMismatch = List.copyOf(amountMismatch);
        currencyMismatch = List.copyOf(currencyMismatch);
        unknownReference = List.copyOf(unknownReference);
        duplicateLine = List.copyOf(duplicateLine);
        paidWithoutLine = List.copyOf(paidWithoutLine);
    }
}
