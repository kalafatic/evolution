package eu.kalafatic.evolution.controller.execution;

import java.util.List;
import java.util.Collections;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract;

/**
 * Fallback scheduler for manual continuation mode.
 * Preserves all variants and awaits user branch selection.
 */
public class ManualContinuationScheduler implements ICapability, ISchedulingContract {
    private CapabilityStatus status = CapabilityStatus.STOPPED;

    @Override
    public String getCapabilityId() {
        return "capability.scheduling.manual";
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
    public ScheduledExecutionPlan schedule(List<BranchVariant> variants, TaskContext context) {
        context.log("[SCHEDULER] Manual continuation active. Preserving all " + variants.size() + " variants.");
        return new ScheduledExecutionPlan(variants, "Manual continuation mode", ExecutionBudget.defaultProfile());
    }
}
