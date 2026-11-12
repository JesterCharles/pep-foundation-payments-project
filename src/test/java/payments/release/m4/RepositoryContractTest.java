package payments.release.m4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import payments.desk.InMemoryPaymentRepository;
import payments.desk.PaymentRepository;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;

/**
 * Release 4 contract for every PaymentRepository (supplied; do not edit).
 * Run with {@code mvn -q test -Pm4}. Release 5 reruns the same contract against
 * JdbcPaymentRepository by extending this class.
 */
public class RepositoryContractTest {

    protected PaymentRepository repository;

    /** The repository under test. */
    protected PaymentRepository newRepository() {
        return new InMemoryPaymentRepository();
    }

    /** A repository reading the same storage (in memory: the same object; JDBC: a new instance). */
    protected PaymentRepository reopen() {
        return repository;
    }

    /** Releases resources after each test (JDBC: drops the test database). */
    protected void closeRepository() {
    }

    @BeforeEach
    void openRepository() {
        repository = newRepository();
    }

    @AfterEach
    void releaseRepository() {
        closeRepository();
    }

    @Test
    @DisplayName("an empty repository finds nothing")
    void emptyRepository() {
        assertTrue(repository.findAll().isEmpty());
        assertNull(repository.findById(1));
        assertNull(repository.findByInvoiceKey("SUP-0001|INV1"));
    }

    @Test
    @DisplayName("nextId hands out increasing positive ids")
    void nextIdIncreases() {
        int previous = 0;
        for (int i = 0; i < 5; i++) {
            int id = repository.nextId();
            assertTrue(id > previous, "nextId must increase: " + previous + " then " + id);
            previous = id;
        }
    }

    @Test
    @DisplayName("a saved request reads back with the same fields and history")
    void saveAndFind() {
        PaymentRequest r = new PaymentRequest(repository.nextId(), "sup-0001", "inv 1", 12_345L, "USD", "REQ-0001");
        repository.save(r);
        PaymentRequest found = reopen().findById(r.getId());
        assertNotNull(found, "findById after save");
        assertSameRequest(r, found);
        assertNull(reopen().findById(r.getId() + 1000));
    }

    @Test
    @DisplayName("findAll is ordered by id whatever the save order")
    void findAllOrderedById() {
        int a = repository.nextId();
        int b = repository.nextId();
        int c = repository.nextId();
        repository.save(new PaymentRequest(c, "SUP-0003", "C", 300L, "USD", "REQ-0001"));
        repository.save(new PaymentRequest(a, "SUP-0001", "A", 100L, "USD", "REQ-0001"));
        repository.save(new PaymentRequest(b, "SUP-0002", "B", 200L, "USD", "REQ-0002"));
        List<Integer> ids = new ArrayList<>();
        for (PaymentRequest r : reopen().findAll()) {
            ids.add(r.getId());
        }
        assertEquals(List.of(a, b, c), ids);
    }

    @Test
    @DisplayName("findByInvoiceKey finds by the normalised key and supports the duplicate rule")
    void findByInvoiceKey() {
        PaymentRequest first = new PaymentRequest(repository.nextId(), "sup-0001", "inv 7781", 100L, "USD", "REQ-0001");
        PaymentRequest second = new PaymentRequest(repository.nextId(), "SUP-0002", "INV7781", 200L, "USD", "REQ-0001");
        repository.save(first);
        repository.save(second);
        first.reject("APR-0001", "wrong amount");
        repository.save(first);
        PaymentRequest found = reopen().findByInvoiceKey("SUP-0001|INV7781");
        assertNotNull(found, "a rejected request still owns its invoice key");
        assertEquals(first.getId(), found.getId());
        assertEquals(second.getId(), reopen().findByInvoiceKey("SUP-0002|INV7781").getId());
        assertNull(reopen().findByInvoiceKey("SUP-0003|INV7781"));
    }

    @Test
    @DisplayName("saving again updates the stored request and appends its new events")
    void saveUpdatesExisting() {
        PaymentRequest r = new PaymentRequest(repository.nextId(), "SUP-0001", "U-1", 50_000L, "USD", "REQ-0001");
        repository.save(r);
        r.correctAmount("REQ-0001", 45_000L, "early-payment discount");
        r.recordApprovalRefused("APR-0001", "within-limit");
        r.approve("APR-0002");
        repository.save(r);
        r.cancel("APR-0002", "supplier asked to hold");
        repository.save(r);
        assertEquals(1, reopen().findAll().size(), "saving twice must not duplicate the request");
        PaymentRequest found = reopen().findById(r.getId());
        assertEquals(PaymentStatus.CANCELLED, found.getStatus());
        assertEquals(45_000L, found.getAmountCents());
        assertSameRequest(r, found);
    }

    /** Compares every field and the full history of two requests. */
    public static void assertSameRequest(PaymentRequest expected, PaymentRequest actual) {
        assertEquals(expected.getId(), actual.getId(), "id");
        assertEquals(expected.getSupplierId(), actual.getSupplierId(), "supplierId");
        assertEquals(expected.getInvoiceNumber(), actual.getInvoiceNumber(), "invoiceNumber");
        assertEquals(expected.getInvoiceKey(), actual.getInvoiceKey(), "invoiceKey");
        assertEquals(expected.getAmountCents(), actual.getAmountCents(), "amountCents");
        assertEquals(expected.getCurrency(), actual.getCurrency(), "currency");
        assertEquals(expected.getRequestedBy(), actual.getRequestedBy(), "requestedBy");
        assertEquals(expected.getStatus(), actual.getStatus(), "status");
        assertEquals(expected.events(), actual.events(), "events (actor, action, detail and seq)");
    }
}
