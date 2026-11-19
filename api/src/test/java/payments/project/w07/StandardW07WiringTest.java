package payments.project.w07;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;

import payments.project.support.DevProfileCheck;
import payments.project.support.Milestone;
import payments.project.support.Milestones;

/**
 * Standard W07 context-load check (supplied; do not edit): your beans are wired by Spring through
 * constructors, and in W07 your configuration files hold no datasource settings.
 */
@Milestone("standard-w07")
@DisplayName("Standard W07: constructor injection and no datasource configuration")
class StandardW07WiringTest extends DevProfileCheck {

    @Test
    @DisplayName("the context loads, has at least one REST controller, and no bean uses field or setter injection")
    void constructorInjection() {
        List<String> problems = new ArrayList<>();
        int controllers = 0;
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null) {
                continue;
            }
            Class<?> user = ClassUtils.getUserClass(type);
            if (!user.getName().startsWith("payments.") || user.getName().startsWith("payments.project.")) {
                continue;
            }
            if (hasAnnotation(user.getAnnotations(), "RestController")) {
                controllers++;
            }
            for (Field field : user.getDeclaredFields()) {
                if (hasAnnotation(field.getAnnotations(), "Autowired", "Inject", "Resource")) {
                    problems.add(user.getSimpleName() + "." + field.getName() + " uses field injection");
                }
            }
            for (Method method : user.getDeclaredMethods()) {
                if (hasAnnotation(method.getAnnotations(), "Autowired", "Inject", "Resource")) {
                    problems.add(user.getSimpleName() + "." + method.getName() + "() uses method injection");
                }
            }
        }
        assertTrue(controllers > 0, "expected at least one @RestController in package payments");
        assertTrue(problems.isEmpty(), "wire collaborators through constructors: " + problems);
    }

    @Test
    @DisplayName("W07 only: application configuration files contain no spring.datasource settings")
    void noDatasourceConfiguration() {
        assumeTrue(Milestones.isExactly("standard-w07"), "applies to the W07 selection only (W08 adds JPA)");
        ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
        List<String> found = new ArrayList<>();
        for (PropertySource<?> source : env.getPropertySources()) {
            if (!source.getName().contains("application") || !(source instanceof EnumerablePropertySource<?> e)) {
                continue;
            }
            for (String key : e.getPropertyNames()) {
                if (key.startsWith("spring.datasource")) {
                    found.add(key + " in " + source.getName());
                }
            }
        }
        assertFalse(!found.isEmpty(), "W07 keeps an in-memory repository; remove " + found);
    }

    private static boolean hasAnnotation(Annotation[] annotations, String... simpleNames) {
        for (Annotation annotation : annotations) {
            for (String name : simpleNames) {
                if (annotation.annotationType().getSimpleName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
