package payments.learner;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * YOUR tests for the money controls (LEARNER-OWNED, milestone M4).
 *
 * <p>Write at least three JUnit tests that build a PaymentDesk with
 * {@code new PaymentDesk(repository, directory, policy)}, where the repository is a fake
 * (a small class of your own implementing PaymentRepository) or a Mockito double, and cover:
 * <ol>
 *   <li>an approval over the approver's limit is refused, and the request stays SUBMITTED;</li>
 *   <li>an own-request approval OR a duplicate invoice is refused, and nothing changes;</li>
 *   <li>an unknown request id gives NotFoundException.</li>
 * </ol>
 * Every test needs assertions, including that state did not change after a refusal.
 * Use synthetic ids and amounts only. Mockito is already in pom.xml (see release/m4/pom-additions.md).
 *
 * <p>Run with {@code mvn -q test -Pm4}.
 */
class PaymentDeskTest {

    @Test
    void replaceThisPlaceholderWithYourTests() {
        // TODO(M4): delete this placeholder and write your tests (see the class comment).
        fail("TODO M4: write your PaymentDesk tests in src/test/java/payments/learner/PaymentDeskTest.java");
    }
}
