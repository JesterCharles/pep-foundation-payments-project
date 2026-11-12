package payments.learner;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * YOUR database test (LEARNER-OWNED, milestone M5).
 *
 * <p>Write at least one test that uses the supplied {@code payments.support.TestDatabase}
 * (try-with-resources) to: save a request through JdbcPaymentRepository, approve it, settle it
 * through a PaymentDesk on that repository, then reload it with a NEW repository instance and
 * assert the amount in cents, the PAID status, the settlement row and the actor of every event.
 * Never write a password into this file: TestDatabase reads the PG* environment variables.
 *
 * <p>Run with {@code mvn -q test -Pm5} (needs a local PostgreSQL).
 */
class JdbcPaymentRepositoryTest {

    @Test
    void replaceThisPlaceholderWithYourTest() {
        // TODO(M5): delete this placeholder and write your database test (see the class comment).
        fail("TODO M5: write your database test in src/test/java/payments/learner/JdbcPaymentRepositoryTest.java");
    }
}
