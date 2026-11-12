package payments.domain;

/**
 * A recorded settlement for one payment request (supplied in release 5; do not edit).
 * Synthetic data only: the reference is an invented settlement id, never a real bank reference.
 *
 * @param reference unique settlement reference, for example "STL-1001"
 * @param requestId the payment request that was settled
 * @param cents     the settled amount in cents
 * @param currency  the settled currency code (the platform accepts only "USD")
 */
public record Settlement(String reference, int requestId, long cents, String currency) {
}
