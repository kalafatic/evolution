package eu.kalafatic.evolution.controller.kernel;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * Interface for evaluating and scoring variants.
 */
public interface FitnessEngine {
    EvaluationResult evaluate(File projectRoot, TaskContext context, EvolutionaryPressureVector pressure) throws Exception;
    EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel level) throws Exception;
    Evaluator.Evaluation evaluateWithSnapshot() throws Exception;
}
