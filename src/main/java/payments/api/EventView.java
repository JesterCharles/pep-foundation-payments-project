package payments.api;

import payments.domain.PaymentEvent;

/**
 * JSON shape of one history event (supplied in release 4; do not edit).
 */
public record EventView(int seq, String actorId, String action, String detail) {

    public static EventView of(PaymentEvent event) {
        return new EventView(event.seq(), event.actorId(), event.action(), event.detail());
    }
}
