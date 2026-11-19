package payments.desk;

import java.util.List;

import payments.domain.PaymentRequest;
import payments.domain.Settlement;

/**
 * Storage seam for payment requests (supplied baseline).
 *
 * <p>Absent values are returned as null. Implementations: {@link InMemoryPaymentRepository} and
 * {@code payments.jdbc.JdbcPaymentRepository}.
 */
public interface PaymentRepository {

    /** Inserts a new request or updates a stored one with the same id, including any new events. */
    void save(PaymentRequest request);

    /** The stored request with this id, or null. */
    PaymentRequest findById(int id);

    /** Every stored request, ordered by id ascending. */
    List<PaymentRequest> findAll();

    /** The stored request whose PaymentRequest.getInvoiceKey() equals this key, or null. */
    PaymentRequest findByInvoiceKey(String invoiceKey);

    /** A new id, greater than every id handed out before (1, 2, 3, ...). */
    int nextId();

    /** Stores the PAID status, the settlement and the "paid" event together, or not at all. */
    void saveSettlement(PaymentRequest request, Settlement settlement);
}
