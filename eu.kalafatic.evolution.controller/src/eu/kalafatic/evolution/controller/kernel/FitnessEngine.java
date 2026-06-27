package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;

/**
 * Interface for evaluating and scoring variants.
 */
public interface FitnessEngine {
    EvaluationResult evaluate(File projectRoot, TaskContext context, EvolutionaryPressureVector pressure) throws Exception;
    EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel level) throws Exception;
    Evaluator.Evaluation evaluateWithSnapshot() throws Exception;
}
