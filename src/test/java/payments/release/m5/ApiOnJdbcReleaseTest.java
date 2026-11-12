package payments.release.m5;

import payments.desk.PaymentDesk;
import payments.domain.ApprovalPolicy;
import payments.identity.ActorDirectory;
import payments.jdbc.JdbcPaymentRepository;
import payments.release.m4.ApiReleaseTest;
import payments.support.TestDatabase;

/**
 * Release 5: the complete release 4 HTTP suite again, with JdbcPaymentRepository storage on a
 * fresh local PostgreSQL database per test (supplied; do not edit). Run with {@code mvn -q test -Pm5}.
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
