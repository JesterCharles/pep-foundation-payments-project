package payments.desk;

import java.util.List;

import payments.domain.PaymentRequest;

/**
 * In-memory PaymentRepository.
 *
 * <p>LEARNER-OWNED (milestone M4). Must pass src/test/java/payments/release/m4/RepositoryContractTest.
 * In M5 you also add the saveSettlement method described in
 * release/m5/saveSettlement-interface-addition.md.
 */
public final class InMemoryPaymentRepository implements PaymentRepository {

    // TODO(M4): hold the requests (for example a HashMap from id to request) and the last id.

    @Override
    public void save(PaymentRequest request) {
        // TODO(M4): store or replace the request by its id.
        throw new UnsupportedOperationException("TODO M4: InMemoryPaymentRepository.save");
    }

    @Override
    public PaymentRequest findById(int id) {
        // TODO(M4): return the request or null.
        throw new UnsupportedOperationException("TODO M4: InMemoryPaymentRepository.findById");
    }

    @Override
    public List<PaymentRequest> findAll() {
        // TODO(M4): return a new list ordered by id ascending.
        throw new UnsupportedOperationException("TODO M4: InMemoryPaymentRepository.findAll");
    }

    @Override
    public PaymentRequest findByInvoiceKey(String invoiceKey) {
        // TODO(M4): return the request with this invoice key, or null. The desk uses this to
        //   refuse a duplicate invoice, whatever the earlier request's status is.
        throw new UnsupportedOperationException("TODO M4: InMemoryPaymentRepository.findByInvoiceKey");
    }

    @Override
    public int nextId() {
        // TODO(M4): hand out 1, 2, 3, ...
        throw new UnsupportedOperationException("TODO M4: InMemoryPaymentRepository.nextId");
    }
}
