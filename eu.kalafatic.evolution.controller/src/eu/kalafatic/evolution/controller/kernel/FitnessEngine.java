package eu.kalafatic.evolution.controller.kernel;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;

/**
 * Interface for evaluating and scoring variants.
 */
public interface FitnessEngine {
    EvaluationResult evaluate(File projectRoot, TaskContext context) throws Exception;
    Evaluator.Evaluation evaluateWithSnapshot() throws Exception;
}
