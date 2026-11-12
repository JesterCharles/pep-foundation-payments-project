package payments.desk;

import java.util.List;

import payments.domain.PaymentRequest;

/**
 * Storage seam for payment requests (supplied in release 4).
 *
 * <p>Release 5 adds ONE method to this interface as a written instruction that you apply
 * yourself (see release/m5/saveSettlement-interface-addition.md). No release ever overwrites
 * this file. Absent values are returned as null (Optional is taught later, in F26).
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
}
