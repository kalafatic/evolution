package eu.kalafatic.evolution.controller.orchestration.scheduling;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract;
import java.util.Collections;

/**
 * Central execution governor for Darwin variants and evaluation tasks.
 */
public class KernelScheduler implements ICapability, ISchedulingContract {
    private final ExecutionBudget budget;
    private final BackpressureController backpressure;
    private SchedulingPolicy policy;
    private CapabilityStatus status = CapabilityStatus.STOPPED;

    public KernelScheduler() {
        this(ExecutionBudget.defaultProfile());
    }

    public KernelScheduler(ExecutionBudget budget) {
        this.budget = budget;
        this.backpressure = BackpressureController.getInstance();
        this.policy = new DefaultSchedulingPolicy();
    }

    public void setPolicy(SchedulingPolicy policy) {
        this.policy = policy;
    }

    @Override
    public String getCapabilityId() {
        return "capability.scheduling";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) throws CapabilityException {
        status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() throws CapabilityException {
        status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() throws CapabilityException {
        status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return Collections.singletonList(ISchedulingContract.ID);
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CapabilityHealth getHealth() {
        return new CapabilityHealth(1.0, "Healthy", 0);
    }

    @Override
    public ScheduledExecutionPlan schedule(List<BranchVariant> proposals, TaskContext context) {
        BackpressureStatus status = backpressure.currentStatus();
        context.log("[SCHEDULER] Analyzing " + proposals.size() + " proposals under " + status.getMemoryPressure() + " memory pressure.");

        // Apply adaptive budget adjustment if necessary
        ExecutionBudget activeBudget = budget;
        if (status.isOverloaded(budget)) {
            context.log("[SCHEDULER] Backpressure detected. Reducing allowed variants.");
            activeBudget = new ExecutionBudget();
            activeBudget.setMaxVariantsAllowed(Math.max(1, budget.getMaxVariantsAllowed() / 2));
            activeBudget.setMaxParallelEvaluations(Math.max(1, budget.getMaxParallelEvaluations() / 2));
            activeBudget.setMaxSignalThroughput(budget.getMaxSignalThroughput() / 2);
        }

        // Additional adaptive logic based on iteration complexity
        if (context.getOrchestrationState().getIterationCount() > 5) {
            context.log("[SCHEDULER] High iteration count detected. Increasing resource limits.");
            activeBudget.setMaxVariantsAllowed(activeBudget.getMaxVariantsAllowed() + 2);
            activeBudget.setTimeBudgetMs(activeBudget.getTimeBudgetMs() + 60000);
        }

        List<BranchVariant> selected = policy.selectVariants(proposals, activeBudget);
        String reason = "Scheduled " + selected.size() + " out of " + proposals.size() + " proposals based on " + policy.getClass().getSimpleName();

        // DIAGNOSTICS: Emit causal node for scheduling decision
        context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "scheduler-decision-" + System.currentTimeMillis(),
            "SCHEDULING",
            "KernelScheduler",
            proposals.stream().map(v -> v.getId()).collect(Collectors.toList()),
            selected.stream().map(v -> v.getId()).collect(Collectors.toList()),
            1.0,
            reason
        ));

        return new ScheduledExecutionPlan(selected, reason, activeBudget);
    }

    public BackpressureController getBackpressure() {
        return backpressure;
    }

    public ExecutionBudget getBudget() {
        return budget;
    }
}
