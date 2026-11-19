package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bounded retries (supplied; do not edit): attempts stop at {@code provider.max-attempts} (3 in the tests). */
public abstract class RetryBoundChecks extends ProviderChecks {

    @Test
    @DisplayName("retries stop at the configured bound and the request stays APPROVED")
    void retriesStopAtBound() {
        int id = approved("150.00");
        PROVIDER.then(ProviderStub.Behaviour.HANG, ProviderStub.Behaviour.HANG, ProviderStub.Behaviour.HANG,
                ProviderStub.Behaviour.HANG, ProviderStub.Behaviour.HANG);
        Api.Response sent = send(id);
        assertTrue(sent.status() >= 500, "an unavailable provider answers 5xx: " + sent);
        assertEquals(3, PROVIDER.calls().size(), "exactly provider.max-attempts calls");
        assertEquals(0, PROVIDER.paymentsCreated());
        assertEquals("APPROVED", statusOf(id), "the request can be sent later");
    }
}
