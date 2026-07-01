package eu.kalafatic.evolution.controller.kernel;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;

public class DefaultAuthorityEngine implements AuthorityEngine {
    private final AuthorityController controller;

    public DefaultAuthorityEngine(AuthorityController controller) {
        this.controller = controller;
    }

    @Override
    public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
        return controller.decide(iterationId, variants, context, manualSelectionId);
    }

    @Override
    public void updateLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context) {
        controller.updateLifecycle(variants, targetId, newState, context);
    }
}
