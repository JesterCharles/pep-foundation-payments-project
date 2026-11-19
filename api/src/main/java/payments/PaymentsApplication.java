package payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Boot application you build in W07 by porting the supplied payments baseline
 * (learner-owned from the W07 release; the class name is fixed by docs/contracts/W07-openapi.yaml).
 *
 * <p>Start it with {@code mvn spring-boot:run} in api/ (port 8080). The supplied Javalin baseline in
 * {@code payments.App} keeps working beside it until you retire it.
 */
@SpringBootApplication
public class PaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }

    // TODO(W07): add REST controllers, a service and an in-memory repository that keep the baseline's
    //            payment rules (see docs/milestones/W07.md and docs/contracts/W07-openapi.yaml).
}
