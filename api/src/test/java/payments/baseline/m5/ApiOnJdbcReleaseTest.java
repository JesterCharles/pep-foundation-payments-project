package payments.baseline.m5;

import payments.desk.PaymentDesk;
import payments.domain.ApprovalPolicy;
import payments.identity.ActorDirectory;
import payments.jdbc.JdbcPaymentRepository;
import payments.baseline.m4.ApiReleaseTest;
import payments.baseline.db.TestDatabase;

/**
 * The complete baseline HTTP suite again, with JdbcPaymentRepository storage on a
 * fresh in-memory H2 database (PostgreSQL mode) per test (supplied; do not edit). Runs with every {@code mvn -q test}.
 */
class ApiOnJdbcReleaseTest extends ApiReleaseTest {

    private TestDatabase db;

    @Override
    protected PaymentDesk newDesk(ActorDirectory directory, ApprovalPolicy policy) {
        db = TestDatabase.create();
        return new PaymentDesk(new JdbcPaymentRepository(db.url(), db.user(), db.password()), directory, policy);
    }

    @Override
    protected void closeStorage() {
        if (db != null) {
            db.close();
        }
    }
}
