package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import java.io.File;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * Contract for evaluators.
 */
public interface IEvaluationContract {
    String ID = "contract.evaluation";

    EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.tools.ITool mavenTool) throws Exception;

    /**
     * Pragma A: Tiered Evaluation
     */
    EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.orchestration.selfdev.RealityLevel level) throws Exception;

    // For signal emission and backward compatibility
    List<EvaluationSignal> evaluate(String variantId);
}
