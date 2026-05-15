package eu.kalafatic.evolution.controller.execution;

import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Default policy: Selects first N variants up to the budget limit.
 */
public class DefaultSchedulingPolicy implements SchedulingPolicy {
    @Override
    public List<BranchVariant> selectVariants(List<BranchVariant> proposals, ExecutionBudget budget) {
        return proposals.stream()
                .limit(budget.getMaxVariantsAllowed())
                .collect(Collectors.toList());
    }
}
