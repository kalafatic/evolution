package eu.kalafatic.evolution.controller.execution;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Outcome of a KernelScheduler scheduling cycle.
 */
public class ScheduledExecutionPlan {
    private final List<BranchVariant> scheduledVariants;
    private final String decisionReason;
    private final ExecutionBudget appliedBudget;

    public ScheduledExecutionPlan(List<BranchVariant> scheduledVariants, String decisionReason, ExecutionBudget appliedBudget) {
        this.scheduledVariants = scheduledVariants;
        this.decisionReason = decisionReason;
        this.appliedBudget = appliedBudget;
    }

    public List<BranchVariant> getScheduledVariants() { return scheduledVariants; }
    public String getDecisionReason() { return decisionReason; }
    public ExecutionBudget getAppliedBudget() { return appliedBudget; }

    public boolean isApproved(String variantId) {
        return scheduledVariants.stream().anyMatch(v -> v.getId().equals(variantId));
    }
}
