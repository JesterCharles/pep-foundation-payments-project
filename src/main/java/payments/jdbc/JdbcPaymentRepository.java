package payments.jdbc;

import java.util.List;

import payments.desk.PaymentRepository;
import payments.domain.PaymentRequest;
import payments.domain.Settlement;

/**
 * PostgreSQL PaymentRepository using plain JDBC.
 *
 * <p>LEARNER-OWNED (milestone M5). Rules:
 * <ul>
 *   <li>PreparedStatement with {@code ?} parameters for every value; never build SQL by
 *       joining strings with data.</li>
 *   <li>One transaction per save and per settlement: setAutoCommit(false), commit, roll back
 *       and rethrow on any failure, and close every resource (try-with-resources).</li>
 *   <li>Reads rebuild requests with PaymentRequest.restore(...); lists are ordered by id;
 *       findByInvoiceKey uses the normalised key (supplier_id and invoice_number as stored).</li>
 * </ul>
 * Tables and columns are defined in src/main/resources/db/schema.sql (also yours).
 */
public final class JdbcPaymentRepository implements PaymentRepository {

    /**
     * @param jdbcUrl  from DbConfig.jdbcUrl(System.getenv()) or the supplied TestDatabase
     * @param user     the database user (PGUSER)
     * @param password the password, or null to let the driver read ~/.pgpass
     */
    public JdbcPaymentRepository(String jdbcUrl, String user, String password) {
        // TODO(M5): keep the connection settings; open connections with DriverManager when needed.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository constructor");
    }

    @Override
    public void save(PaymentRequest request) {
        // TODO(M5): in ONE transaction, insert the payment_requests row or update its status and
        //   amount, then insert every event whose seq is not stored yet.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.save");
    }

    /**
     * Stores the PAID status, the settlement row and the "paid" event together, or not at all.
     * (This becomes an @Override once you apply release/m5/saveSettlement-interface-addition.md.)
     */
    public void saveSettlement(PaymentRequest request, Settlement settlement) {
        // TODO(M5): in ONE transaction: update the status to PAID, insert the settlements row and
        //   insert the new "paid" event. On any SQLException roll back and rethrow (wrapped in an
        //   unchecked exception) so the request stays APPROVED with no settlement and no paid event.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.saveSettlement");
    }

    @Override
    public PaymentRequest findById(int id) {
        // TODO(M5): select the row and its events (ordered by seq); null when absent.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.findById");
    }

    @Override
    public List<PaymentRequest> findAll() {
        // TODO(M5): every request ordered by id, each with its events.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.findAll");
    }

    @Override
    public PaymentRequest findByInvoiceKey(String invoiceKey) {
        // TODO(M5): split "SUPPLIER|INVOICE" and look up (supplier_id, invoice_number); null when absent.
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.findByInvoiceKey");
    }

    @Override
    public int nextId() {
        // TODO(M5): return a new id that no stored request uses (for example from the identity
        //   sequence of payment_requests.id).
        throw new UnsupportedOperationException("TODO M5: JdbcPaymentRepository.nextId");
    }
}
