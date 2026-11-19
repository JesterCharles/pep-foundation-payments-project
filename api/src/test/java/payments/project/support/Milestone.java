package payments.project.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a supplied milestone check class with the first milestone(s) it belongs to (supplied; do not edit).
 *
 * <p>{@code @Milestone("standard-w08")} runs the class for {@code -Dmilestone=standard-w08} and every later
 * Standard week; it never runs for a Fast selection. Without {@code -Dmilestone} no supplied milestone check runs,
 * so a bare {@code mvn -q test} runs only the baseline suite and your own tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(MilestoneCondition.class)
public @interface Milestone {

    /** First milestones, e.g. {@code "standard-w08"} or {@code "fast-w07"}; at most one per track. */
    String[] value();
}
