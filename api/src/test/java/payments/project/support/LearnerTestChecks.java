package payments.project.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Presence of your own control tests (supplied; do not edit). They live under src/test/java outside
 * payments/project/ and payments/baseline/ and run with every {@code mvn -q test}; their quality is reviewed.
 */
public abstract class LearnerTestChecks {

    @Test
    @DisplayName("at least one learner-written test class exists outside payments/project and payments/baseline")
    void learnerTestsExist() throws IOException {
        Path root = Path.of("src", "test", "java");
        List<Path> own;
        try (Stream<Path> files = Files.walk(root)) {
            own = files.filter(p -> p.toString().endsWith("Test.java") || p.toString().endsWith("Tests.java"))
                    .filter(p -> !p.startsWith(root.resolve(Path.of("payments", "project"))))
                    .filter(p -> !p.startsWith(root.resolve(Path.of("payments", "baseline"))))
                    .toList();
        }
        assertTrue(!own.isEmpty(), "write your own tests of the payment controls under api/src/test/java (outside payments/project)");
    }
}
