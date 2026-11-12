package payments.api;

import java.util.ArrayList;
import java.util.List;

import payments.domain.PaymentEvent;
import payments.domain.PaymentRequest;
import payments.intake.PaymentRules;

/**
 * JSON shape of a payment request (supplied in release 4; do not edit).
 * The amount is rendered at the edge with PaymentRules.formatCents, e.g. "1234.50";
 * money stays in long cents everywhere else.
 */
public record PaymentView(int id, String supplierId, String invoiceNumber, String amount, String currency,
                          String requestedBy, String status, List<EventView> events) {

    public static PaymentView of(PaymentRequest request) {
        List<EventView> events = new ArrayList<>();
        for (PaymentEvent event : request.events()) {
            events.add(EventView.of(event));
        }
        return new PaymentView(request.getId(), request.getSupplierId(), request.getInvoiceNumber(),
                PaymentRules.formatCents(request.getAmountCents()), request.getCurrency(),
                request.getRequestedBy(), request.getStatus().name(), List.copyOf(events));
    }
}
