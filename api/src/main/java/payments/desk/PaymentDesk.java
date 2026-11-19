package payments.desk;

import java.util.List;

import payments.domain.ApprovalPolicy;
import payments.domain.Approver;
import payments.domain.PaymentRequest;
import payments.domain.PaymentStatus;
import payments.domain.Settlement;
import payments.identity.ActorDirectory;
import payments.intake.PaymentRules;

/**
 * The payment desk: submits, approves and settles payment requests.
 *
 * <p>SUPPLIED BASELINE (participant-editable starting source). This is the service layer the
 * Spring Boot port keeps: it depends on the {@link PaymentRepository} seam and the
 * {@link ApprovalPolicy} interface only, so limits and own-request rules are never checked here
 * directly. Collaborators arrive through the constructor.
 */
public final class PaymentDesk {

    private final PaymentRepository repository;
    private final ActorDirectory directory;
    private final ApprovalPolicy policy;

    /** Constructor injection of the storage seam, the identity stub and the approval control. */
    public PaymentDesk(PaymentRepository repository, ActorDirectory directory, ApprovalPolicy policy) {
        if (repository == null || directory == null || policy == null) {
            throw new IllegalArgumentException("repository, directory and policy are required");
        }
        this.repository = repository;
        this.directory = directory;
        this.policy = policy;
    }

    /** Creates a desk with in-process storage. */
    public static PaymentDesk create(ActorDirectory directory, ApprovalPolicy policy) {
        return new PaymentDesk(new InMemoryPaymentRepository(), directory, policy);
    }

    /**
     * Submits a new request. Ids are 1, 2, 3, ... in submission order.
     *
     * @throws IllegalArgumentException  when the amount text or another field is invalid
     * @throws PaymentConflictException  when the invoice key was already used by ANY earlier request
     */
    public PaymentRequest submit(String supplierId, String invoiceNumber, String amountText,
                                 String currency, String requestedBy) {
        long cents = PaymentRules.parseAmountCents(amountText);
        if (cents < 0) {
            throw new IllegalArgumentException("amount must look like 1234.50 (0.01 to 9999999.99)");
        }
        String key = PaymentRules.invoiceKey(supplierId, invoiceNumber);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("supplier id must be SUP- and four digits, invoice number non-blank");
        }
        if (repository.findByInvoiceKey(key) != null) {
            throw new PaymentConflictException("invoice " + key + " was already requested");
        }
        // Validate everything before an id is handed out, so a refused request uses no id.
        new PaymentRequest(1, supplierId, invoiceNumber, cents, currency, requestedBy);
        PaymentRequest request = new PaymentRequest(repository.nextId(), supplierId, invoiceNumber, cents,
                currency, requestedBy);
        repository.save(request);
        return request;
    }

    /**
     * Approves a request if the policy allows it.
     *
     * @throws NotFoundException         for an unknown request id or an id that is not an approver
     * @throws ApprovalRefusedException  when the policy refuses (recorded as "approval-refused")
     * @throws PaymentConflictException  when the request is not SUBMITTED
     */
    public PaymentRequest approve(int id, String approverId) {
        PaymentRequest request = existing(id);
        Approver approver = directory.approverFor(approverId);
        if (approver == null) {
            throw new NotFoundException("approver " + approverId + " not found");
        }
        if (!request.getStatus().canMoveTo(PaymentStatus.APPROVED)) {
            throw new PaymentConflictException("request " + id + " is " + request.getStatus() + ", not SUBMITTED");
        }
        if (!policy.allows(approver, request)) {
            request.recordApprovalRefused(approver.id(), policy.name());
            repository.save(request);
            throw new ApprovalRefusedException("approval of request " + id + " by " + approver.id()
                    + " refused by " + policy.name());
        }
        return change(request, () -> request.approve(approver.id()));
    }

    /** Delegates to PaymentRequest.reject. */
    public PaymentRequest reject(int id, String actorId, String reason) {
        PaymentRequest request = existing(id);
        return change(request, () -> request.reject(actorId, reason));
    }

    /** Delegates to PaymentRequest.cancel. */
    public PaymentRequest cancel(int id, String actorId, String reason) {
        PaymentRequest request = existing(id);
        return change(request, () -> request.cancel(actorId, reason));
    }

    /**
     * Delegates to PaymentRequest.markPaid, then stores the status change, the Settlement and the
     * "paid" event together through repository.saveSettlement.
     */
    public PaymentRequest markPaid(int id, String actorId, String settlementRef, long settledCents,
                                   String settledCurrency) {
        PaymentRequest request = existing(id);
        change(request, () -> request.markPaid(actorId, settlementRef, settledCents, settledCurrency), false);
        repository.saveSettlement(request, new Settlement(settlementRef.trim(), id, settledCents, "USD"));
        return request;
    }

    /** Delegates to PaymentRequest.correctAmount. */
    public PaymentRequest correctAmount(int id, String actorId, long newCents, String reason) {
        PaymentRequest request = existing(id);
        return change(request, () -> request.correctAmount(actorId, newCents, reason));
    }

    /** The request with this id, or null when absent. */
    public PaymentRequest find(int id) {
        return repository.findById(id);
    }

    /** Every request, ordered by id. */
    public List<PaymentRequest> all() {
        return repository.findAll();
    }

    /** The sum, in cents, of every request that is APPROVED and not yet PAID. */
    public long approvedUnpaidCents() {
        long total = 0;
        for (PaymentRequest request : repository.findAll()) {
            if (request.getStatus() == PaymentStatus.APPROVED) {
                total += request.getAmountCents();
            }
        }
        return total;
    }

    private PaymentRequest existing(int id) {
        PaymentRequest request = repository.findById(id);
        if (request == null) {
            throw new NotFoundException("payment request " + id + " not found");
        }
        return request;
    }

    private PaymentRequest change(PaymentRequest request, Runnable transition) {
        return change(request, transition, true);
    }

    private PaymentRequest change(PaymentRequest request, Runnable transition, boolean save) {
        try {
            transition.run();
        } catch (PaymentConflictException | NotFoundException alreadyMapped) {
            throw alreadyMapped;
        } catch (IllegalStateException refusedMove) {
            throw new PaymentConflictException(refusedMove.getMessage());
        }
        if (save) {
            repository.save(request);
        }
        return request;
    }
}
