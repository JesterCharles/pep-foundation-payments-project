package payments.desk;

import java.util.List;

import payments.domain.ApprovalPolicy;
import payments.domain.PaymentRequest;
import payments.identity.ActorDirectory;

/**
 * The payment desk: submits, approves and settles payment requests.
 *
 * <p>LEARNER-OWNED. Built up over three milestones; keep every public signature:
 * <ul>
 *   <li>M3: every method marked TODO(M3). Hold the requests yourself (ArrayList/HashMap).</li>
 *   <li>M4: add the repository constructor, keep {@link #create} (now using an
 *       InMemoryPaymentRepository), remove the desk's own collections, and throw
 *       NotFoundException / PaymentConflictException / ApprovalRefusedException.</li>
 *   <li>M5: make {@link #markPaid} store the status change, settlement and event together
 *       through {@code repository.saveSettlement(request, settlement)}.</li>
 * </ul>
 * The desk depends on the {@link ApprovalPolicy} interface only: never check limits or
 * own-request rules here directly. Released tests create desks only through
 * {@link #create}, so your M4 refactor keeps them valid.
 */
public final class PaymentDesk {

    // TODO(M3): fields for the directory, the policy and the requests.
    // TODO(M4): replace your own request collections with a PaymentRepository field.

    /**
     * M4: constructor injection of the storage seam.
     */
    public PaymentDesk(PaymentRepository repository, ActorDirectory directory, ApprovalPolicy policy) {
        // TODO(M4): store the three collaborators (reject nulls with IllegalArgumentException).
        throw new UnsupportedOperationException("TODO M4: PaymentDesk(PaymentRepository, ActorDirectory, ApprovalPolicy)");
    }

    /**
     * Creates a desk with in-process storage. Released tests always use this factory.
     */
    public static PaymentDesk create(ActorDirectory directory, ApprovalPolicy policy) {
        // TODO(M3): return a new desk that keeps its requests in memory.
        // TODO(M4): return new PaymentDesk(new InMemoryPaymentRepository(), directory, policy).
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.create");
    }

    /**
     * Submits a new request. Ids are 1, 2, 3, ... in submission order.
     *
     * @throws IllegalArgumentException when amountText is not a valid amount
     *         (PaymentRules.parseAmountCents returns -1) or the request is otherwise invalid
     * @throws IllegalStateException    (recommended; PaymentConflictException from M4) when the
     *         invoice key was already used by ANY earlier request, whatever its status
     */
    public PaymentRequest submit(String supplierId, String invoiceNumber, String amountText,
                                 String currency, String requestedBy) {
        // TODO(M3): parse with PaymentRules, refuse duplicates BEFORE creating or storing anything,
        //   then create, store and return the request.
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.submit");
    }

    /**
     * Approves a request if the policy allows it.
     *
     * @throws IllegalArgumentException (NotFoundException from M4) for an unknown request id, or
     *         an approverId the directory does not list as an approver
     * @throws IllegalStateException    (recommended; ApprovalRefusedException from M4) when the
     *         policy refuses: first record (approver, "approval-refused", policy.name()) on the
     *         request; its status stays unchanged
     */
    public PaymentRequest approve(int id, String approverId) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.approve");
    }

    /** Delegates to PaymentRequest.reject. Unknown id: IllegalArgumentException. */
    public PaymentRequest reject(int id, String actorId, String reason) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.reject");
    }

    /** Delegates to PaymentRequest.cancel. Unknown id: IllegalArgumentException. */
    public PaymentRequest cancel(int id, String actorId, String reason) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.cancel");
    }

    /**
     * Delegates to PaymentRequest.markPaid. Unknown id: IllegalArgumentException.
     * From M5 the status change, the Settlement and the "paid" event are stored together
     * through repository.saveSettlement, or not at all.
     */
    public PaymentRequest markPaid(int id, String actorId, String settlementRef, long settledCents,
                                   String settledCurrency) {
        // TODO(M3): delegate.
        // TODO(M5): after the domain check passes, call repository.saveSettlement(request,
        //   new Settlement(settlementRef, id, settledCents, "USD")).
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.markPaid");
    }

    /** Delegates to PaymentRequest.correctAmount. Unknown id: IllegalArgumentException. */
    public PaymentRequest correctAmount(int id, String actorId, long newCents, String reason) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.correctAmount");
    }

    /** The request with this id, or null when absent. */
    public PaymentRequest find(int id) {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.find");
    }

    /** Every request, ordered by id (used by the M6 reconciliation route). */
    public List<PaymentRequest> all() {
        // TODO(M3): return a new list of every request, ordered by id.
        // TODO(M4): return repository.findAll().
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.all");
    }

    /** The sum, in cents, of every request that is APPROVED and not yet PAID. */
    public long approvedUnpaidCents() {
        // TODO(M3)
        throw new UnsupportedOperationException("TODO M3: PaymentDesk.approvedUnpaidCents");
    }
}
