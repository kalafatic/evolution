package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import java.util.List;

import eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Contract for scheduling systems.
 */
public interface ISchedulingContract {
    String ID = "contract.scheduling";

    ScheduledExecutionPlan schedule(List<BranchVariant> variants, TaskContext context);
}
