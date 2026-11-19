package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base of every supplied Spring milestone check (supplied; do not edit).
 *
 * <p>Starts YOUR application ({@code payments.PaymentsApplication}) on a random local port with a fresh,
 * private in-memory H2 database in PostgreSQL mode per test class, a test-time JWT secret, and the in-test
 * provider stub. Talks to it over real HTTP only, so the checks do not depend on your class names.
 * Subclasses choose the profile: {@link DevProfileCheck} (X-Actor-Id stub) or {@link SecuredCheck} (tokens).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "provider.timeout=400ms",
        "provider.max-attempts=3",
        "logging.level.org.hibernate.stat=warn",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=warn"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class ApiCheck {

    /**
     * The in-test payment provider of the running check class. It lives only while that class runs, so its
     * port is free again before any other test (for example the baseline's Javalin tests) asks for one.
     */
    protected static ProviderStub PROVIDER;

    private static synchronized ProviderStub provider() {
        if (PROVIDER == null) {
            PROVIDER = new ProviderStub();
        }
        return PROVIDER;
    }

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        String url = "jdbc:h2:mem:p2_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("security.jwt.secret", () -> Jwt.SECRET);
        String providerUrl = provider().baseUrl();
        registry.add("provider.base-url", () -> providerUrl);
    }

    @Autowired
    protected Environment environment;

    @Autowired
    protected ApplicationContext context;

    protected Api api;
    protected final Generated data = new Generated();

    @BeforeEach
    void connect() {
        api = new Api(Integer.parseInt(environment.getRequiredProperty("local.server.port")));
        provider().reset();
    }

    @AfterAll
    static void disarm() {
        FailingTrigger.armedRequestId = -1;
        synchronized (ApiCheck.class) {
            if (PROVIDER != null) {
                PROVIDER.close();
                PROVIDER = null;
            }
        }
    }

    protected DataSource dataSource() {
        return context.getBean(DataSource.class);
    }

    // ---------------------------------------------------------------- shared assertions

    protected static int idOf(Api.Response response) {
        Object id = response.json().get("id");
        assertTrue(id instanceof Long, "expected a numeric id in " + response);
        return ((Long) id).intValue();
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> events(Map<String, Object> view) {
        Object events = view.get("events");
        assertTrue(events instanceof List, "expected an events array in " + view);
        return (List<Map<String, Object>>) events;
    }

    protected static void assertStatus(int expected, Api.Response response, String what) {
        assertEquals(expected, response.status(), what + ": " + response);
    }
}
