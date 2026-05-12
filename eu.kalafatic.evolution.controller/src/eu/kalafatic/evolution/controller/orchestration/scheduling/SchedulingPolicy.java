package eu.kalafatic.evolution.controller.orchestration.scheduling;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Determines execution priority and selection for Darwin variants.
 */
public interface SchedulingPolicy {
    List<BranchVariant> selectVariants(List<BranchVariant> proposals, ExecutionBudget budget);
}
