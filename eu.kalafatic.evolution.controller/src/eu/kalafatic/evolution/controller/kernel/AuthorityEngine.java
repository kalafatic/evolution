package eu.kalafatic.evolution.controller.kernel;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;

/**
 * Interface for centralized selection decisions.
 */
public interface AuthorityEngine {
    EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId);
    void updateLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context);
}
