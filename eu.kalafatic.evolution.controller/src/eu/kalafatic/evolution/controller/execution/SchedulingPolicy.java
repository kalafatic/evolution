package eu.kalafatic.evolution.controller.execution;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Determines execution priority and selection for Darwin variants.
 */
public interface SchedulingPolicy {
    List<BranchVariant> selectVariants(List<BranchVariant> proposals, ExecutionBudget budget);
}
