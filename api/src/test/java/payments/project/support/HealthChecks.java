package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Provider health (Standard only; supplied; do not edit): {@code GET /actuator/health} is public and shows a
 * {@code provider} component that follows the stub's {@code GET /health}.
 */
public abstract class HealthChecks extends SecuredCheck {

    @AfterEach
    void providerUp() {
        PROVIDER.healthy(true);
    }

    @Test
    @DisplayName("health reports UP with the provider up and DOWN (503) with the provider down")
    @SuppressWarnings("unchecked")
    void providerHealth() {
        Api.Response up = api.get("/actuator/health", Api.Who.anonymous());
        assertStatus(200, up, "health with the provider up");
        assertEquals("UP", up.json().get("status"));
        Map<String, Object> components = (Map<String, Object>) up.json().get("components");
        assertEquals("UP", ((Map<String, Object>) components.get("provider")).get("status"), up.toString());
        PROVIDER.healthy(false);
        Api.Response down = api.get("/actuator/health", Api.Who.anonymous());
        assertStatus(503, down, "health with the provider down");
        assertEquals("DOWN", ((Map<String, Object>) ((Map<String, Object>) down.json().get("components")).get("provider")).get("status"));
    }
}
