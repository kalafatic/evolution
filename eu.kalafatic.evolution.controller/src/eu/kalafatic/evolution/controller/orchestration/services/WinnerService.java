package eu.kalafatic.evolution.controller.orchestration.services;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;

public interface WinnerService {
    String handleVariantSelection(TaskContext context, List<BranchVariant> variants, String goal, IterationManager manager) throws Exception;
    EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId, IterationManager manager);
    EvaluationResult processWinners(TaskContext context, EvolutionDecision decision, List<BranchVariant> variants, GoalModel goal, IterationManager manager) throws Exception;
}
