package payments.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import payments.domain.PaymentRequest;
import payments.domain.Settlement;

/**
 * In-memory PaymentRepository (supplied baseline; participant-editable starting source).
 * Passes {@code payments.baseline.m4.RepositoryContractTest}.
 *
 * <p>Like a database, it stores and hands out COPIES: changing a request you read changes nothing
 * stored until you save it, so a refused change can never leave half-written state behind.
 */
public final class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<Integer, PaymentRequest> requests = new TreeMap<>();
    private final Map<String, Settlement> settlements = new HashMap<>();
    private int lastId;

    @Override
    public synchronized void save(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        requests.put(request.getId(), copy(request));
        lastId = Math.max(lastId, request.getId());
    }

    @Override
    public synchronized PaymentRequest findById(int id) {
        PaymentRequest stored = requests.get(id);
        return stored == null ? null : copy(stored);
    }

    @Override
    public synchronized List<PaymentRequest> findAll() {
        List<PaymentRequest> all = new ArrayList<>();
        for (PaymentRequest stored : requests.values()) {
            all.add(copy(stored));
        }
        return all;
    }

    @Override
    public synchronized PaymentRequest findByInvoiceKey(String invoiceKey) {
        if (invoiceKey == null) {
            return null;
        }
        for (PaymentRequest request : requests.values()) {
            if (request.getInvoiceKey().equals(invoiceKey)) {
                return copy(request);
            }
        }
        return null;
    }

    @Override
    public synchronized int nextId() {
        lastId++;
        return lastId;
    }

    @Override
    public synchronized void saveSettlement(PaymentRequest request, Settlement settlement) {
        if (settlement == null || settlements.containsKey(settlement.reference())) {
            throw new PaymentConflictException("settlement reference already recorded");
        }
        for (Settlement stored : settlements.values()) {
            if (stored.requestId() == settlement.requestId()) {
                throw new PaymentConflictException("request " + settlement.requestId() + " is already settled");
            }
        }
        settlements.put(settlement.reference(), settlement);
        save(request);
    }

    private static PaymentRequest copy(PaymentRequest request) {
        return PaymentRequest.restore(request.getId(), request.getSupplierId(), request.getInvoiceNumber(),
                request.getAmountCents(), request.getCurrency(), request.getRequestedBy(), request.getStatus(),
                request.events());
    }
}
