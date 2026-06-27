package eu.kalafatic.evolution.controller.orchestration.capability.contracts;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import java.util.List;

/**
 * Contract for scheduling systems.
 */
public interface ISchedulingContract {
    String ID = "contract.scheduling";

    ScheduledExecutionPlan schedule(List<BranchVariant> variants, TaskContext context);
}
