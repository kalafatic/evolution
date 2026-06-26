package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;

public interface EvolutionService {
    OrchestratorResponse orchestrate(TaskRequest taskRequest, IterationManager manager) throws Exception;
    void evolve(String request, IterationManager manager, EvolutionAssessment initialAssessment) throws Exception;
    void evolve(TaskContext context, IterationManager iterationManager) throws Exception;
    EvaluationResult runDarwinIteration(TaskContext context, IterationManager manager) throws Exception;
}
