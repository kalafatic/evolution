package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.engines.DimensionEngine;
import eu.kalafatic.evolution.controller.orchestration.engines.LineageEngine;

public interface GenerationService {
    List<BranchVariant> generateProposals(TaskContext context, GoalModel goal, IterationManager manager) throws Exception;
    List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory, EvolutionaryPressureVector pressure, TaskContext context, IterationManager manager) throws Exception;
    void setWinnerService(WinnerService winnerService);
    void setAiService(AiService aiService);
    DimensionEngine getDimensionEngine();
    LineageEngine getLineageEngine();
}
