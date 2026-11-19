package payments.project.support;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/** Enables a {@link Milestone} class only for a matching {@code -Dmilestone} selection (supplied; do not edit). */
public final class MilestoneCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<Milestone> milestone = context.getTestClass()
                .flatMap(type -> AnnotationSupport.findAnnotation(type, Milestone.class));
        if (milestone.isEmpty()) {
            return ConditionEvaluationResult.enabled("not a milestone check");
        }
        Optional<Milestones.Selection> selected = Milestones.selected();
        if (selected.isEmpty()) {
            return ConditionEvaluationResult.disabled(
                    "supplied milestone check; run with -Dmilestone=<track>-w<NN>, e.g. -Dmilestone=standard-w07");
        }
        for (String first : milestone.get().value()) {
            Milestones.Selection start = Milestones.parse(first);
            if (start.track().equals(selected.get().track()) && start.week() <= selected.get().week()) {
                return ConditionEvaluationResult.enabled("selected by " + selected.get());
            }
        }
        return ConditionEvaluationResult.disabled("not part of " + selected.get());
    }
}
